pluginManagement {
    repositories {
        mavenLocal()
        maven { url = uri("https://dl.google.com/dl/android/maven2/") }
        maven { url = uri("https://repo1.maven.org/maven2/") }
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "com.android.application") {
                useModule("com.android.tools.build:gradle:${requested.version}")
            }
            if (requested.id.id == "org.jetbrains.kotlin.android") {
                useModule("org.jetbrains.kotlin:kotlin-gradle-plugin:${requested.version}")
            }
            if (requested.id.id == "com.google.dagger.hilt.android") {
                useModule("com.google.dagger:hilt-android-gradle-plugin:${requested.version}")
            }
            if (requested.id.id == "org.jetbrains.kotlin.kapt") {
                useModule("org.jetbrains.kotlin:kotlin-gradle-plugin:${requested.version}")
            }
            if (requested.id.id == "org.jetbrains.kotlin.plugin.parcelize") {
                useModule("org.jetbrains.kotlin:kotlin-gradle-plugin:${requested.version}")
            }
        }
    }
}

dependencyResolutionManagement {
    repositories {
        mavenLocal()
        maven { url = uri("https://dl.google.com/dl/android/maven2/") }
        maven { url = uri("https://repo1.maven.org/maven2/") }
        google()
        mavenCentral()
    }
}

rootProject.name = "OMaster"
include(":app")
