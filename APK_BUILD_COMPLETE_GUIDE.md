# OMaster Android APK 构建指南

## 📋 构建要求

### 系统要求
- **操作系统**: Linux / macOS / Windows (WSL2)
- **Java**: JDK 21 或更高版本
- **Gradle**: 8.14.4 或更高版本
- **Android SDK**: API Level 36 (Android 16)

### 环境检查

```bash
# 检查 Java 版本
java -version
# 应该显示: openjdk version "21.x.x"

# 检查 Gradle 版本
gradle --version
# 应该显示: Gradle 8.14.4

# 检查 Android SDK
echo $ANDROID_HOME
# 应该显示 Android SDK 路径，如: /Users/xxx/Library/Android/sdk
```

### 环境安装 (如缺失)

#### macOS
```bash
# 使用 Homebrew 安装
brew install openjdk@21 gradle android-sdk

# 配置环境变量
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
export ANDROID_HOME=/usr/local/share/android-sdk
```

#### Ubuntu/Debian
```bash
# 安装依赖
sudo apt update
sudo apt install openjdk-21-jdk gradle android-sdk

# 配置环境变量
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
export ANDROID_HOME=/usr/lib/android-sdk
```

#### Windows (WSL2)
```bash
# 在 WSL2 中安装
sudo apt update
sudo apt install openjdk-21-jdk gradle

# Windows 端需要安装 Android Studio
# 配置 ANDROID_HOME 指向 Android Studio SDK 目录
```

---

## 🔨 构建步骤

### 方式一: 自动构建 (推荐)

```bash
# 1. 进入项目目录
cd /path/to/OMaster

# 2. 添加执行权限
chmod +x build_apk_full.sh

# 3. 运行构建脚本
./build_apk_full.sh debug
```

### 方式二: 手动构建

```bash
# 1. 清理项目
./gradlew clean

# 2. 构建 Debug APK
./gradlew assembleDebug

# 3. 或构建 Release APK
./gradlew assembleRelease
```

### 方式三: 使用系统 Gradle

```bash
# 如果系统已安装 Gradle 8.14.4+
gradle clean assembleDebug --no-daemon
```

---

## 📱 APK 输出位置

构建成功后，APK 文件位于:

```
app/build/outputs/apk/debug/OMaster-debug.apk
app/build/outputs/apk/release/OMaster-release.apk
```

---

## 🔍 Android 16 系统兼容性

### 目标 SDK 配置
- **compileSdk**: 36 (Android 16)
- **targetSdk**: 36
- **minSdk**: 26 (Android 8.0)

### 权限说明

#### 运行时权限
| 权限 | 用途 | 必需 |
|------|------|------|
| CAMERA | 读取 Camera2 参数 | 否 (可选功能) |
| READ_MEDIA_IMAGES | 保存截图到相册 | 是 |
| POST_NOTIFICATIONS | Android 13+ 通知 | 是 |
| SYSTEM_ALERT_WINDOW | 悬浮窗显示 | 是 |

#### Android 16 新增要求
- 应用需要明确声明 `android:requestLegacyExternalStorage="false"`
- 使用 Photo Picker 而非传统存储权限
- 支持部分照片访问 (Partial Photo Access)

### 安装测试

```bash
# 通过 ADB 安装
adb install -r app/build/outputs/apk/debug/OMaster-debug.apk

# 查看安装日志
adb logcat | grep OMaster

# 启动应用
adb shell am start -n com.omaster.app/.MainActivity
```

---

## 🐛 常见问题

### 问题 1: Gradle 下载超时
**解决方案:**
```bash
# 使用阿里云镜像
# 在 gradle.properties 中添加:
org.gradle.jvmargs=-Xmx4096m
org.gradle.parallel=true
```

### 问题 2: Android SDK 缺失
**解决方案:**
```bash
# 安装 Android SDK Command Line Tools
# 然后运行:
sdkmanager "platforms;android-36" "build-tools;36.0.0"
```

### 问题 3: Java 版本不匹配
**解决方案:**
```bash
# macOS
export JAVA_HOME=$(/usr/libexec/java_home -v 21)

# Linux
export JAVA_HOME=/path/to/jdk-21
```

### 问题 4: 依赖下载失败
**解决方案:**
```bash
# 清除 Gradle 缓存
rm -rf ~/.gradle/caches/

# 重新构建
./gradlew --refresh-dependencies assembleDebug
```

---

## ✅ 验证清单

构建完成后，请验证以下内容:

- [ ] Debug APK 文件存在且大小 > 10MB
- [ ] APK 使用 `apksigner` 验证签名
- [ ] 在 Android 16 设备上成功安装
- [ ] 应用启动无崩溃
- [ ] 相机参数读取功能正常
- [ ] 预设列表加载正常
- [ ] 水印功能可用

### 签名验证
```bash
# 验证 APK 签名
apksigner verify --print-certs app/build/outputs/apk/debug/OMaster-debug.apk

# 查看 APK 元信息
aapt dump badging app/build/outputs/apk/debug/OMaster-debug.apk
```

---

## 📊 构建配置

### Gradle 配置
- **AGP 版本**: 8.5.0
- **Kotlin 版本**: 2.0.0
- **Compose BOM**: 2024.06.00
- **Hilt 版本**: 2.51.1

### ProGuard/R8 配置
- 启用代码混淆 (Release)
- 启用资源压缩
- 保留必要的 Keep 规则

---

## 🚀 性能优化

### 构建加速
```properties
# gradle.properties
org.gradle.jvmargs=-Xmx4096m -XX:+UseParallelGC
org.gradle.parallel=true
org.gradle.caching=true
android.useAndroidX=true
android.enableJetifier=true
```

### 增量构建
```bash
# 跳过不必要任务
./gradlew assembleDebug -x lint -x test
```

---

## 📞 技术支持

如遇到构建问题，请提供:
1. 操作系统和版本
2. Java 和 Gradle 版本
3. 完整的错误日志
4. 构建命令输出

---

## 📄 许可证

本项目遵循 Apache 2.0 许可证。
