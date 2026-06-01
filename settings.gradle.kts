pluginManagement {
    repositories {
        // 阿里云镜像 - 优先级 1
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin") }
        maven { url = uri("https://maven.aliyun.com/repository/jcenter") }
        maven { url = uri("https://maven.aliyun.com/repository/spring") }
        // 腾讯云镜像 - 优先级 2
        maven { url = uri("https://mirrors.cloud.tencent.com/nexus/repository/maven-public/") }
        maven { url = uri("https://mirrors.cloud.tencent.com/gradle/") }
        // 华为云镜像 - 优先级 3
        maven { url = uri("https://repo.huaweicloud.com/repository/maven/") }
        // 中科大镜像 - 优先级 4
        maven { url = uri("https://mirrors.ustc.edu.cn/maven-mirror/") }
        // 清华大学镜像 - 优先级 5
        maven { url = uri("https://maven.aliyun.com/repository/apache-snapshots") }
        // 官方源 - 最后兜底
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        // 阿里云镜像 - 优先级 1
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        maven { url = uri("https://maven.aliyun.com/repository/jcenter") }
        maven { url = uri("https://maven.aliyun.com/repository/central") }
        maven { url = uri("https://maven.aliyun.com/repository/public/") }
        // 腾讯云镜像 - 优先级 2
        maven { url = uri("https://mirrors.cloud.tencent.com/nexus/repository/maven-public/") }
        // 华为云镜像 - 优先级 3
        maven { url = uri("https://repo.huaweicloud.com/repository/maven/") }
        // 中科大镜像 - 优先级 4
        maven { url = uri("https://mirrors.ustc.edu.cn/maven-mirror/") }
        // 官方源 - 最后兜底
        google()
        mavenCentral()
    }
}

rootProject.name = "OMaster"
include(":app")
