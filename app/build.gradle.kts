plugins {
    id("com.android.application")
    id("kotlin-android")
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.5.21")
}

android {
    compileSdk = 31

    defaultConfig {
        minSdk = 25
        targetSdk = 31
        versionCode = 17
        versionName = "2.0.0-dev"
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
}
