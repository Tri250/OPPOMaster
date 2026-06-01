pluginManagement {
    repositories {
        // 阿里云镜像加速（国内推荐）
        maven { url = java.net.URI("https://maven.aliyun.com/repository/google") }
        maven { url = java.net.URI("https://maven.aliyun.com/repository/public") }
        maven { url = java.net.URI("https://maven.aliyun.com/repository/gradle-plugin") }
        
        // 官方仓库
        google()
        mavenCentral()
        gradlePluginPortal()
        
        // 腾讯云镜像（备选）
        maven { url = java.net.URI("https://mirrors.cloud.tencent.com/nexus/repository/maven-public/") }
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        // 阿里云镜像加速（国内推荐）
        maven { url = java.net.URI("https://maven.aliyun.com/repository/google") }
        maven { url = java.net.URI("https://maven.aliyun.com/repository/public") }
        maven { url = java.net.URI("https://maven.aliyun.com/repository/central") }
        
        // 官方仓库
        google()
        mavenCentral()
        
        // 腾讯云镜像（备选）
        maven { url = java.net.URI("https://mirrors.cloud.tencent.com/nexus/repository/maven-public/") }
    }
}

rootProject.name = "OMaster"
include(":app")
