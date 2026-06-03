buildscript {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://dl.google.com/dl/android/maven2/") }
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin") }
    }
    dependencies {
        classpath("com.android.tools.build:gradle:8.9.3")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.22")
        classpath("com.google.dagger:hilt-android-gradle-plugin:2.48")
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://dl.google.com/dl/android/maven2/") }
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        maven { url = uri("https://maven.aliyun.com/repository/public") }
    }
}

tasks.register("clean", Delete::class) {
    delete(rootProject.layout.buildDirectory)
}