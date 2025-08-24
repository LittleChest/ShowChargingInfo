plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "top.littlew.sci"
    compileSdk = 36

    defaultConfig {
        applicationId = "top.littlew.sci"
        minSdk = 33
        targetSdk = 36
        versionCode = 1
        versionName = "1.1"
    }

    buildTypes {
        release {
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    compileOnly("de.robv.android.xposed:api:82")
}
