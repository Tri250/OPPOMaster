# OMaster Android APK 构建指南

## 📱 项目信息

- **应用名称**: OMaster
- **包名**: com.omaster.app
- **版本**: 1.5.0 (314)
- **目标 SDK**: 36 (Android 16)
- **最低 SDK**: 26 (Android 8.0)
- **Kotlin**: 2.0.0
- **Compose BOM**: 2024.06.00

---

## 🚀 快速开始

### 方式一：使用构建脚本（推荐）

```bash
# 1. 进入项目目录
cd /path/to/OMaster

# 2. 添加执行权限
chmod +x build.sh

# 3. 运行构建脚本
./build.sh debug
```

### 方式二：直接使用 Gradle

```bash
# 清理并构建 Debug APK
./gradlew clean assembleDebug

# 构建 Release APK
./gradlew clean assembleRelease
```

### 方式三：跳过测试快速构建

```bash
./gradlew assembleDebug -x test -x lint
```

---

## 📦 构建输出

构建成功后，APK 文件位于：

```
app/build/outputs/apk/debug/OMaster-debug.apk
app/build/outputs/apk/release/OMaster-release.apk
```

---

## 🔧 环境要求

### 必需环境

1. **Java Development Kit (JDK) 21**
   
   ```bash
   # 检查版本
   java -version
   
   # macOS 安装
   brew install openjdk@21
   
   # Ubuntu 安装
   sudo apt install openjdk-21-jdk
   ```

2. **Gradle 8.14.4+**
   
   ```bash
   # 检查版本
   gradle --version
   
   # macOS 安装
   brew install gradle
   
   # Ubuntu 安装
   sudo apt install gradle
   ```

3. **Android SDK (API 36)**
   
   ```bash
   # 设置环境变量
   export ANDROID_HOME=$HOME/Android/Sdk
   export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools
   
   # 安装必要的 SDK 组件
   sdkmanager "platforms;android-36" "build-tools;36.0.0" "cmdline-tools;latest"
   ```

---

## ⚙️ 构建配置

### Gradle 属性 (gradle.properties)

```properties
# 内存配置
org.gradle.jvmargs=-Xmx4096m -XX:+UseParallelGC

# 并行构建
org.gradle.parallel=true

# Gradle 缓存
org.gradle.caching=true

# AndroidX
android.useAndroidX=true
android.enableJetifier=true

# Kotlin
kotlin.code.style=official

# 非转译 R 类
android.nonTransitiveRClass=true
```

### 版本目录 (gradle/libs.versions.toml)

核心依赖版本：
- Android Gradle Plugin: 8.5.0
- Kotlin: 2.0.0
- Compose BOM: 2024.06.00
- Hilt: 2.51.1
- CameraX: 1.4.0-alpha05
- ML Kit: 17.0.8

---

## 🐛 故障排除

### 问题 1：Plugin [id: 'com.android.application'...] was not found

**原因**: Android Gradle Plugin 未下载

**解决方案**:
```bash
# 下载依赖
./download-deps.sh

# 或手动配置镜像
# 在 ~/.gradle/init.gradle 中添加:
allprojects {
    repositories {
        maven { url 'https://maven.aliyun.com/repository/google' }
        maven { url 'https://maven.aliyun.com/repository/central' }
        google()
        mavenCentral()
    }
}
```

### 问题 2：Java 版本不匹配

**解决方案**:
```bash
# macOS
export JAVA_HOME=$(/usr/libexec/java_home -v 21)

# Linux
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
```

### 问题 3：依赖下载超时

**解决方案**:
```bash
# 使用阿里云镜像加速
# 在 gradle.properties 中添加:
systemProp.https.proxyHost=127.0.0.1
systemProp.https.proxyPort=7890
```

### 问题 4：Android SDK 缺失

**解决方案**:
```bash
# 安装 Android Studio
# 或使用命令行工具:
wget https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip
unzip commandlinetools-linux-11076708_latest.zip
./cmdline-tools/bin/sdkmanager "platforms;android-36" "build-tools;36.0.0"
```

---

## 📱 Android 16 特性支持

### 权限要求

应用需要以下权限（已在 AndroidManifest.xml 中声明）：

| 权限 | 用途 | 运行时 |
|------|------|--------|
| CAMERA | 相机参数读取 | 可选 |
| READ_MEDIA_IMAGES | 保存截图 | ✅ |
| READ_MEDIA_VIDEO | 视频截图 | ✅ |
| POST_NOTIFICATIONS | 通知 | ✅ |
| SYSTEM_ALERT_WINDOW | 悬浮窗 | ✅ |

### 新存储规范

- ✅ `requestLegacyExternalStorage="false"`
- ✅ 使用 Photo Picker
- ✅ 支持部分照片访问

---

## 🧪 测试

### 运行单元测试

```bash
./gradlew test
```

### 运行 Debug APK 测试

```bash
./gradlew connectedDebugAndroidTest
```

### 查看测试报告

```bash
# 单元测试报告
open app/build/reports/tests/test/index.html

# UI 测试报告
open app/build/reports/androidTests/connected/index.html
```

---

## 📊 构建产物验证

### 验证 APK 签名

```bash
apksigner verify --verbose app/build/outputs/apk/debug/OMaster-debug.apk
```

### 查看 APK 元信息

```bash
aapt dump badging app/build/outputs/apk/debug/OMaster-debug.apk
```

### 检查 APK 内容

```bash
unzip -l app/build/outputs/apk/debug/OMaster-debug.apk | head -30
```

---

## 🔨 高级构建选项

### 构建变体

```bash
# Debug (可调试，有日志)
./gradlew assembleDebug

# Release (优化，无日志)
./gradlew assembleRelease

# 所有变体
./gradlew assemble
```

### 增量构建

```bash
# 使用构建缓存
./gradlew assembleDebug --build-cache

# 使用配置缓存
./gradlew assembleDebug --configuration-cache
```

### 指定输出目录

```bash
./gradlew assembleDebug -PbUILD_OUTPUT_DIR=/custom/path
```

---

## 🚀 安装与测试

### 通过 ADB 安装

```bash
# 安装 Debug APK
adb install -r app/build/outputs/apk/debug/OMaster-debug.apk

# 安装 Release APK
adb install -r app/build/outputs/apk/release/OMaster-release.apk
```

### 查看日志

```bash
# 过滤应用日志
adb logcat | grep OMaster

# 完整日志
adb logcat > app.log
```

### 性能分析

```bash
# 启动性能分析
adb shell am start -n com.omaster.app/.MainActivity --start-profiler results.prof

# 查看方法追踪
adb pull /data/local/tmp/results.prof .
```

---

## 📞 技术支持

如遇到构建问题，请提供：

1. 操作系统和版本
2. Java 版本: `java -version`
3. Gradle 版本: `gradle --version`
4. ANDROID_HOME: `echo $ANDROID_HOME`
5. 完整的错误日志
6. 构建命令输出

---

## ✅ 成功标志

构建成功时会看到：

```
✓ BUILD SUCCESSFUL
✓ APK 生成位置: app/build/outputs/apk/debug/OMaster-debug.apk
```

---

## 🎉 功能清单

构建完成的 APK 包含以下功能：

- ✅ AI 智能场景识别（ML Kit 本地）
- ✅ OPPO/Realme 相机参数管理
- ✅ 哈苏大师模式预设
- ✅ 图像质量前置检查
- ✅ 手动场景选择功能
- ✅ 系统悬浮窗参数展示
- ✅ 水印功能
- ✅ 截图保存
- ✅ 完整的单元测试

---

**创建时间**: 2026-05-30  
**项目版本**: 1.5.0  
**许可证**: Apache 2.0
