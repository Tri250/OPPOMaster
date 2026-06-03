pluginManagement {
    repositories {
        maven { url = uri("https://dl.google.com/dl/android/maven2/") }
        google()
        mavenCentral()
        gradlePluginPortal()
        maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin") }
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        maven { url = uri("https://maven.aliyun.com/repository/public") }
    }
}

rootProject.name = "OMaster"
include(":app")