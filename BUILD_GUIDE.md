# OMaster 本地构建指南

## 项目概述

OMaster 是一个基于 Jetpack Compose 的 Android 相机预设管理应用。

- **最低 SDK**: Android 8.0 (API 26)
- **目标 SDK**: Android 15 (API 35)
- **Kotlin 版本**: 1.9.22
- **Gradle 版本**: 8.14.4
- **AGP 版本**: 8.2.2

---

## 网络限制解决方案

### 方案一：使用阿里云镜像（已配置）

项目已预配置阿里云镜像源，位于 `settings.gradle.kts`：

```kotlin
pluginManagement {
    repositories {
        maven { url = uri("https://maven.aliyun.com/repository/gradle-plugin") }
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        google()
        mavenCentral()
    }
}
```

### 方案二：手动下载 Gradle Wrapper

如果 Gradle Wrapper 无法自动下载，可以手动下载：

1. **查看需要的 Gradle 版本**：
   ```bash
   cat gradle/wrapper/gradle-wrapper.properties
   ```
   当前版本：`gradle-8.14.4-bin.zip`

2. **手动下载 Gradle**：
   - 官方地址：https://services.gradle.org/distributions/gradle-8.14.4-bin.zip
   - 阿里云镜像：https://mirrors.aliyun.com/gradle/distributions/gradle-8.14.4-bin.zip
   - 腾讯云镜像：https://mirrors.cloud.tencent.com/gradle/distributions/gradle-8.14.4-bin.zip

3. **放置到正确位置**：
   ```bash
   # 创建目标目录
   mkdir -p ~/.gradle/wrapper/dists/gradle-8.14.4-bin
   
   # 解压下载的文件
   unzip gradle-8.14.4-bin.zip -d ~/.gradle/wrapper/dists/gradle-8.14.4-bin/
   
   # 或者直接放在项目目录
   cp gradle-8.14.4-bin.zip gradle/wrapper/
   ```

4. **修改 gradle-wrapper.properties 使用本地文件**：
   ```properties
   distributionUrl=file\:/path/to/gradle-8.14.4-bin.zip
   ```

### 方案三：使用已安装的 Gradle

如果系统已安装 Gradle：

```bash
# 使用系统 Gradle 替代 Wrapper
gradle build --no-daemon

# 或者设置环境变量
export GRADLE_USER_HOME=/path/to/gradle/home
./gradlew build
```

### 方案四：配置全局镜像

在 `~/.gradle/init.gradle` 中配置全局镜像：

```groovy
allprojects {
    buildscript {
        repositories {
            maven { url 'https://maven.aliyun.com/repository/gradle-plugin' }
            maven { url 'https://maven.aliyun.com/repository/google' }
            maven { url 'https://maven.aliyun.com/repository/public' }
            mavenCentral()
            google()
        }
    }
    
    repositories {
        maven { url 'https://maven.aliyun.com/repository/google' }
        maven { url 'https://maven.aliyun.com/repository/public' }
        mavenCentral()
        google()
    }
}
```

---

## 构建步骤

### 1. 环境准备

**必需环境**：
- JDK 17 或更高版本
- Android SDK (API 35)
- Build Tools 35.0.0

**检查环境**：
```bash
java -version    # 应显示 17.x.x
echo $ANDROID_HOME  # 应指向 Android SDK 目录
```

### 2. 设置 Android SDK

如果未设置 ANDROID_HOME：

```bash
# Linux/Mac
export ANDROID_HOME=/path/to/android-sdk
export PATH=$PATH:$ANDROID_HOME/tools:$ANDROID_HOME/platform-tools

# Windows (PowerShell)
$env:ANDROID_HOME="C:\path\to\android-sdk"
$env:Path="$env:Path;$env:ANDROID_HOME\tools;$env:ANDROID_HOME\platform-tools"
```

### 3. 构建命令

```bash
# 清理项目
./gradlew clean

# 构建 Debug APK
./gradlew assembleDebug

# 构建 Release APK
./gradlew assembleRelease

# 安装到设备
./gradlew installDebug

# 运行测试
./gradlew test
```

### 4. 离线构建

如果依赖已下载，可使用离线模式：

```bash
./gradlew assembleDebug --offline
```

---

## APK 输出位置

构建完成后，APK 文件位于：

- **Debug APK**: `app/build/outputs/apk/debug/app-debug.apk`
- **Release APK**: `app/build/outputs/apk/release/app-release.apk`

---

## 依赖清单

### 核心依赖

| 依赖 | 版本 | 用途 |
|------|------|------|
| Kotlin | 1.9.22 | 编程语言 |
| Compose BOM | 2024.09.00 | UI 框架 |
| Material3 | - | UI 组件 |
| Hilt | 2.48 | 依赖注入 |
| Navigation | 2.7.7 | 页面导航 |
| Lifecycle | 2.8.3 | 生命周期管理 |
| Coroutines | 1.8.1 | 异步处理 |
| OkHttp | 4.12.0 | 网络请求 |
| Retrofit | 2.11.0 | API 客户端 |
| Coil | 2.7.0 | 图片加载 |
| CameraX | 1.3.4 | 相机功能 |
| DataStore | 1.1.1 | 数据存储 |
| Timber | 5.0.1 | 日志 |
| ML Kit | 16.0.1 | 文字识别 |

### 完整依赖列表

```kotlin
// Core Android
implementation("androidx.core:core-ktx:1.13.1")
implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.3")
implementation("androidx.activity:activity-compose:1.9.0")

// Jetpack Compose
implementation(platform("androidx.compose:compose-bom:2024.09.00"))
implementation("androidx.compose.ui:ui")
implementation("androidx.compose.ui:ui-graphics")
implementation("androidx.compose.ui:ui-tooling-preview")
implementation("androidx.compose.material3:material3")
implementation("androidx.compose.material:material-icons-extended")

// Navigation
implementation("androidx.navigation:navigation-compose:2.7.7")
implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.3")
implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.3")

// Coroutines
implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

// JSON
implementation("com.google.code.gson:gson:2.11.0")

// Network
implementation("com.squareup.okhttp3:okhttp:4.12.0")
implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
implementation("com.squareup.retrofit2:retrofit:2.11.0")
implementation("com.squareup.retrofit2:converter-gson:2.11.0")

// Security
implementation("androidx.security:security-crypto:1.1.0-alpha06")

// ML Kit
implementation("com.google.mlkit:text-recognition:16.0.1")

// Image Loading
implementation("io.coil-kt:coil-compose:2.7.0")

// Hilt DI
implementation("com.google.dagger:hilt-android:2.48")
kapt("com.google.dagger:hilt-android-compiler:2.48")
implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

// DataStore
implementation("androidx.datastore:datastore-preferences:1.1.1")

// Logging
implementation("com.jakewharton.timber:timber:5.0.1")

// CameraX
implementation("androidx.camera:camera-core:1.3.4")
implementation("androidx.camera:camera-camera2:1.3.4")
implementation("androidx.camera:camera-lifecycle:1.3.4")
implementation("androidx.camera:camera-view:1.3.4")

// WorkManager
implementation("androidx.work:work-runtime-ktx:2.9.1")
implementation("androidx.hilt:hilt-work:1.2.0")
kapt("androidx.hilt:hilt-compiler:1.2.0")
implementation("androidx.startup:startup-runtime:1.1.1")
```

---

## 常见问题解决

### Q1: Gradle Wrapper 下载失败

**解决方案**：
```bash
# 使用国内镜像下载
wget https://mirrors.aliyun.com/gradle/distributions/gradle-8.14.4-bin.zip

# 放置到 Gradle 缓存目录
mkdir -p ~/.gradle/wrapper/dists/gradle-8.14.4-bin/xxx
mv gradle-8.14.4-bin.zip ~/.gradle/wrapper/dists/gradle-8.14.4-bin/xxx/
```

### Q2: 依赖下载超时

**解决方案**：
在 `gradle.properties` 中增加超时时间：
```properties
org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
systemProp.http.connectionTimeout=600000
systemProp.http.socketTimeout=600000
```

### Q3: Android SDK 未找到

**解决方案**：
```bash
# 创建 local.properties 文件
echo "sdk.dir=/path/to/android-sdk" > local.properties
```

### Q4: Kotlin 编译错误

**解决方案**：
确保 JDK 版本正确：
```bash
java -version  # 应为 17+
./gradlew clean
./gradlew build
```

### Q5: Hilt 依赖注入错误

**解决方案**：
确保 kapt 配置正确：
```kotlin
kapt {
    correctErrorTypes = true
}
```

---

## 项目结构

```
OMaster/
├── app/
│   ├── src/main/
│   │   ├── java/com/omaster/app/
│   │   │   ├── camera/         # 相机控制
│   │   │   ├── data/           # 数据层
│   │   │   ├── di/             # 依赖注入模块
│   │   │   ├── floating/       # 悬浮窗功能
│   │   │   ├── model/          # 数据模型
│   │   │   ├── navigation/     # 导航路由
│   │   │   ├── ui/             # UI 层
│   │   │   │   ├── animation/  # 动画效果
│   │   │   │   ├── components/ # UI 组件
│   │   │   │   ├── screens/    # 页面
│   │   │   │   └── theme/      # 主题样式
│   │   │   ├── viewmodel/      # ViewModel
│   │   │   └── watermark/      # 水印处理
│   │   ├── res/                # 资源文件
│   │   └── AndroidManifest.xml
│   └── build.gradle.kts
├── gradle/
│   └── wrapper/
│       ├── gradle-wrapper.jar
│       └── gradle-wrapper.properties
├── build.gradle.kts            # 根项目配置
├── settings.gradle.kts         # 项目设置（含镜像配置）
├── gradle.properties           # Gradle 属性
└── BUILD_GUIDE.md              # 本文档
```

---

## 开发工具推荐

- **IDE**: Android Studio Hedgehog (2023.1) 或更高版本
- **JDK**: JDK 17 (推荐 OpenJDK 17)
- **Android SDK**: API 35 (Android 15)

---

## 联系与支持

如有构建问题，请检查：
1. JDK 版本是否为 17+
2. Android SDK 是否正确配置
3. 网络连接是否正常
4. Gradle 缓存是否完整

---

*文档版本: 1.0*
*最后更新: 2025-01-01*