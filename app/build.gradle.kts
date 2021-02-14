plugins {
    id("com.android.application")
    id("com.google.gms.google-services")
    id("kotlin-android")
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.4.30")
    implementation("com.google.firebase:firebase-appindexing:19.2.0")
}

android {
    compileSdkVersion(30)

    defaultConfig {
        minSdkVersion(21)
        targetSdkVersion(30)
        versionCode(16)
        versionName("1.6.0")
        buildConfigField("String", "LOG_TAG", "\"uSticker\"")
    }

    buildTypes {
        create("debug") {
            buildConfigField("int", "LOG_LEVEL", "2")
        }

        create("release")  {
            postprocessing {
                isRemoveUnusedCode = true
                isRemoveUnusedResources = true
                isObfuscate = false
                isOptimizeCode = true
            }
            buildConfigField("int", "LOG_LEVEL", "4")
        }
    }

    lintOptions {
        isAbortOnError = false
    }
}
