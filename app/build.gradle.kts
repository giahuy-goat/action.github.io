plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.gamecorner.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.gamecorner.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 15200
        versionName = "1.5.2-15200"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
        debug {
            isMinifyEnabled = false
        }
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    // Keep the original media bytes so the APK size and playback quality
    // remain predictable. These files are intentionally not compressed.
    androidResources {
        noCompress += listOf("mp4", "wav", "mp3", "png")
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
}