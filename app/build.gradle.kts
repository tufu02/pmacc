plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.pg.pmacc"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.pg.pmacc"
        minSdk = 24
        targetSdk = 34
        versionCode = 2
        versionName = "2.0"
    }

    // 固定签名：GitHub 构建出来的包和本地修改后的包签名一致，可以直接覆盖安装
    signingConfigs {
        create("fixed") {
            storeFile = rootProject.file("keystore/pmacc.jks")
            storePassword = "pmacc123"
            keyAlias = "pmacc"
            keyPassword = "pmacc123"
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("fixed")
        }
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("fixed")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
}
