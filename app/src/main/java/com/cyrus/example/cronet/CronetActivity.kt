package com.cyrus.example.cronet

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.chromium.net.CronetEngine
import org.chromium.net.CronetException
import org.chromium.net.UrlRequest
import org.chromium.net.UrlResponseInfo
import java.nio.ByteBuffer
import java.nio.charset.Charset
import java.util.concurrent.Executor
import java.util.concurrent.Executors

class CronetActivity : ComponentActivity() {
    private lateinit var cronetEngine: CronetEngine
    private val executor: Executor = Executors.newSingleThreadExecutor()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 初始化 Cronet 引擎
        cronetEngine = CronetEngine.Builder(this)
            .enableHttp2(true)
            .enableQuic(true)
            .build()

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    CronetScreen(cronetEngine, executor)
                }
            }
        }
    }
}

@Composable
fun CronetScreen(cronetEngine: CronetEngine, executor: Executor) {
    var resultText by remember { mutableStateOf("点击按钮通过 Cronet 请求 IP 地理位置信息...") }
    var isLoading by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState)
    ) {
        Text(
            text = "Cronet 网络请求示例",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Button(
            onClick = {
                isLoading = true
                resultText = "正在请求 http://ip-api.com/json/ ..."
                startCronetRequest(cronetEngine, executor) { result ->
                    resultText = result
                    isLoading = false
                }
            },
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (isLoading) "正在加载..." else "获取 IP 信息")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "请求结果:",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = resultText,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

fun startCronetRequest(
    cronetEngine: CronetEngine,
    executor: Executor,
    onResult: (String) -> Unit
) {
    val callback = object : UrlRequest.Callback() {
        private val bytesReceived = java.io.ByteArrayOutputStream()

        override fun onRedirectReceived(
            request: UrlRequest?,
            info: UrlResponseInfo?,
            newLocationUrl: String?
        ) {
            request?.followRedirect()
        }

        override fun onResponseStarted(request: UrlRequest?, info: UrlResponseInfo?) {
            request?.read(ByteBuffer.allocateDirect(32 * 1024))
        }

        override fun onReadCompleted(
            request: UrlRequest?,
            info: UrlResponseInfo?,
            byteBuffer: ByteBuffer?
        ) {
            byteBuffer?.flip()
            val bytes = ByteArray(byteBuffer!!.remaining())
            byteBuffer.get(bytes)
            bytesReceived.write(bytes)
            byteBuffer.clear()
            request?.read(byteBuffer)
        }

        override fun onSucceeded(request: UrlRequest?, info: UrlResponseInfo?) {
            val body = bytesReceived.toString(Charset.defaultCharset().name())
            onResult("请求成功!\n\nHTTP 状态码: ${info?.httpStatusCode}\n\n协议: ${info?.negotiatedProtocol}\n\n内容:\n$body")
        }

        override fun onFailed(
            request: UrlRequest?,
            info: UrlResponseInfo?,
            error: CronetException?
        ) {
            onResult("请求失败!\n错误信息: ${error?.message}\n原因: ${error?.cause?.message}")
        }
    }

    // 使用 ip-api.com 获取 IP 信息
    val requestBuilder = cronetEngine.newUrlRequestBuilder(
        "http://ip-api.com/json/?lang=zh-CN",
        callback,
        executor
    )
    requestBuilder.build().start()
}
