buildscript {
    repositories {
        google()
        mavenCentral()
        jcenter()
    }

    dependencies {
        classpath("com.android.tools.build:gradle:4.1.2")
        classpath("com.google.gms:google-services:4.3.5")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.4.30")
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
        jcenter()
    }
}
