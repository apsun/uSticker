plugins {
    id("com.android.application")
    id("kotlin-android")
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.4.30")
}

android {
    compileSdkVersion(30)

    defaultConfig {
        minSdkVersion(25)
        targetSdkVersion(30)
        versionCode(17)
        versionName("2.0.0-dev")
        buildConfigField("String", "LOG_TAG", "\"uSticker\"")
    }

    buildTypes {
        getByName("debug") {
            buildConfigField("int", "LOG_LEVEL", "2")
        }

        getByName("release") {
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
