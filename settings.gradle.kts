pluginManagement {
    repositories {
        // 阿里云镜像 - 优先
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin") }
        // 腾讯云镜像 - 备选
        maven { url = uri("https://mirrors.cloud.tencent.com/nexus/repository/maven-public/") }
        // 官方源 - 最后
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        // 阿里云镜像 - 优先
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        // 腾讯云镜像 - 备选
        maven { url = uri("https://mirrors.cloud.tencent.com/nexus/repository/maven-public/") }
        // 华为云镜像 - 第三备选
        maven { url = uri("https://repo.huaweicloud.com/repository/maven/") }
        // 官方源 - 最后
        google()
        mavenCentral()
    }
}

rootProject.name = "OMaster"
include(":app")
