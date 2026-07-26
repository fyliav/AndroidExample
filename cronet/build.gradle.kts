plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "org.chromium.net"
    compileSdk = 34

    defaultConfig {
        minSdk = 26

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        
        // Include the consumer proguard files provided with Cronet
        consumerProguardFiles(
            "cronet_shared_proguard.cfg",
            "cronet_impl_common_proguard.cfg",
            "cronet_impl_native_proguard.cfg",
            "cronet_impl_platform_proguard.cfg",
            "httpengine_native_provider_proguard.cfg"
        )
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    sourceSets {
        getByName("main") {
            jniLibs.srcDirs("libs")
            res.srcDirs("res")
        }
    }
}

dependencies {
    api(files("cronet_api.jar"))
    implementation(files(
        "cronet_shared_java.jar",
        "cronet_impl_common_java.jar",
        "cronet_impl_native_java.jar",
        "cronet_impl_platform_java.jar",
        "httpengine_native_provider_java.jar",
        "cronet_impl_native_sentinel_java.jar"
    ))
    
    // Cronet implementation dependencies
    implementation("com.google.protobuf:protobuf-javalite:3.25.1")
    implementation("androidx.annotation:annotation:1.7.0")
}
