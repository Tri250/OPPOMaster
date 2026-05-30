# OPPOMaster v1.2.1 - 完整APK构建指南

> **构建日期**: 2026-05-30  
> **分支**: trae/solo-agent-g4xAg3  
> **版本**: v1.2.1 (versionCode: 121)  
> **目标**: Android 16 (SDK 36)

---

## 📋 项目状态确认

### ✅ 当前状态

| 检查项 | 状态 | 说明 |
|--------|------|------|
| 代码完整性 | ✅ | 所有代码已同步 |
| 构建错误修复 | ✅ | 5个构建错误已修复 |
| 安全配置 | ✅ | 完整的安全配置 |
| ColorOS 16适配 | ✅ | 已完成 |
| DeepSeek AI集成 | ✅ | 已完成 |

---

## 🛠️ 本地构建准备

### 第一步：环境要求

#### 1.1 安装 JDK 17

```bash
# 检查当前 Java 版本
java -version

# 如果未安装或版本过低，下载安装：
# - Windows/macOS/Linux: https://adoptium.net/temurin/releases/?version=17
# - 选择 JDK 17 (LTS)
```

#### 1.2 安装 Android Studio

```bash
# 下载地址
# https://developer.android.com/studio

# 推荐版本
# - Android Studio Ladybug (2024.1.1) 或更新
# - 或 Android Studio Hedgehog (2023.1.1)
```

#### 1.3 安装 Android SDK 36

在 Android Studio 中：

1. **打开 SDK Manager**
   - Tools → SDK Manager
   - 或 File → Settings → Appearance & Behavior → System Settings → Android SDK

2. **安装必要组件**
   - ✅ Android 16.0 (API 36) → SDK Platform
   - ✅ Android SDK Build-Tools 36.0.0 (或最新版本)
   - ✅ Android SDK Platform-Tools
   - ✅ Android SDK Tools

---

## 🚀 方法一：使用 Android Studio 构建（推荐）

### 步骤 1：打开项目

```bash
# 确保当前在项目根目录
cd /path/to/OPPOMaster
```

1. **启动 Android Studio**
2. **打开项目**
   - 选择 "Open an Existing Project"
   - 选择项目根目录
   - 等待 Gradle Sync 完成

### 步骤 2：配置 local.properties

首次打开时，Android Studio 会自动创建 `local.properties`，如果没有：

```properties
# 复制模板
cp local.properties.template local.properties

# 编辑 local.properties，设置正确的 SDK 路径
# Windows: sdk.dir=C\:\\Users\\YourName\\AppData\\Local\\Android\\Sdk
# macOS: sdk.dir=/Users/YourName/Library/Android/sdk
# Linux: sdk.dir=/home/YourName/Android/Sdk
```

### 步骤 3：构建 Debug APK

**方法 A：使用菜单**
1. Build → Build Bundle(s) / APK(s) → Build APK(s)
2. 等待构建完成
3. 点击通知中的 "locate" 找到 APK

**方法 B：使用快捷键**
- Windows/Linux: `Ctrl + F9`
- macOS: `Cmd + F9`

**方法 C：使用终端**
```bash
# 在项目根目录
./gradlew assembleDebug

# 构建产物位置
# app/build/outputs/apk/debug/app-debug.apk
```

### 步骤 4：构建 Release APK（可选）

如果需要 Release 版本：

```bash
# 创建测试签名密钥（如果没有）
keytool -genkey -v -keystore release.keystore -keyalg RSA -keysize 2048 -validity 10000 -alias omaster

# 或使用环境变量
export KEYSTORE_PASSWORD=your_password
export KEY_ALIAS=omaster
export KEY_PASSWORD=your_password

# 构建 Release
./gradlew assembleRelease
```

---

## 🔧 方法二：纯命令行构建

### 步骤 1：设置环境变量

```bash
# 1. 设置 ANDROID_HOME
# Windows
set ANDROID_HOME=C:\Users\YourName\AppData\Local\Android\Sdk

# macOS
export ANDROID_HOME=$HOME/Library/Android/sdk

# Linux
export ANDROID_HOME=$HOME/Android/Sdk

# 2. 添加到 PATH
export PATH=$PATH:$ANDROID_HOME/tools:$ANDROID_HOME/platform-tools

# 3. 验证
echo $ANDROID_HOME
adb version
```

### 步骤 2：构建 APK

```bash
# 进入项目目录
cd /path/to/OPPOMaster

# 确保 gradlew 可执行
chmod +x gradlew

# 清理构建
./gradlew clean

# 构建 Debug APK
./gradlew assembleDebug

# 构建完成后，APK 位于
# app/build/outputs/apk/debug/app-debug.apk
```

---

## 📱 安装到 Android 16 设备

### 方法一：通过 ADB 安装

```bash
# 1. 启用开发者选项和 USB 调试
# 设置 → 关于手机 → 连续点击版本号7次
# 设置 → 系统 → 开发者选项 → 启用 USB 调试

# 2. 连接设备
adb devices

# 3. 安装 APK
adb install -r app/build/outputs/apk/debug/app-debug.apk

# 4. 启动应用
adb shell am start -n com.omaster.app/.MainActivity
```

### 方法二：直接传输安装

1. 将 `app-debug.apk` 复制到手机
2. 在手机文件管理器中找到 APK
3. 点击安装（需要允许"安装未知来源应用"）
4. 安装完成后打开应用

---

## 🎯 验证安装

### 功能检查清单

| 功能 | 检查项 | 状态 |
|------|--------|------|
| **启动** | 应用正常启动，无崩溃 | ☐ |
| **UI** | 界面美观，符合 ColorOS 16 设计 | ☐ |
| **预设浏览** | 可以浏览预设列表 | ☐ |
| **搜索** | 预设搜索功能正常 | ☐ |
| **收藏** | 收藏功能正常工作 | ☐ |
| **AI场景识别** | DeepSeek AI 场景识别可用 | ☐ |
| **图像分析** | 选择图片后可以进行场景分析 | ☐ |
| **主题切换** | 浅色/深色主题切换 | ☐ |
| **设置** | 设置页面功能正常 | ☐ |

---

## 🔧 常见问题解决

### 问题 1：Gradle Sync 失败

**解决方案：**
```bash
# 方法 A：清理并重新同步
# Build → Clean Project
# Build → Rebuild Project
# File → Sync Project with Gradle Files

# 方法 B：删除缓存
rm -rf .gradle/
rm -rf app/build/
rm -rf build/

# 方法 C：重启 Android Studio
# File → Invalidate Caches → Invalidate and Restart
```

### 问题 2：SDK 找不到

**解决方案：**
1. 检查 `local.properties` 中的 `sdk.dir` 路径
2. 打开 Android Studio 的 SDK Manager 确认已安装 SDK 36
3. 重新同步 Gradle

### 问题 3：依赖下载失败

**解决方案：**
```properties
# 在 gradle.properties 中添加代理（如果需要）
systemProp.http.proxyHost=127.0.0.1
systemProp.http.proxyPort=7890
systemProp.https.proxyHost=127.0.0.1
systemProp.https.proxyPort=7890
```

### 问题 4：内存不足

**解决方案：**
```properties
# 在 gradle.properties 中增加内存
org.gradle.jvmargs=-Xmx4096m -Dfile.encoding=UTF-8
```

---

## 📊 项目配置信息

| 配置项 | 值 |
|--------|------|
| **包名** | com.omaster.app |
| **版本名** | 1.2.1 |
| **版本号** | 121 |
| **Min SDK** | 26 (Android 8.0) |
| **Target SDK** | 36 (Android 16) |
| **Compile SDK** | 36 (Android 16) |
| **Kotlin** | 2.0.0 |
| **AGP** | 8.5.0 |
| **Compose BOM** | 2024.06.00 |

---

## 🎉 成功标志

如果一切顺利，你将看到：

```
BUILD SUCCESSFUL in Xm Xs
XX actionable tasks: XX executed

# APK 位置
app/build/outputs/apk/debug/app-debug.apk

# 大小约：10-20 MB
```

---

## 📞 需要帮助？

如果遇到问题，请查看：

1. **错误日志** - Android Studio 的 Build Output 或 Logcat
2. **构建文档** - `COMPLETE_BUILD_GUIDE.md`
3. **修复报告** - `BUILD_FIXES_FINAL.md`
4. **验证报告** - `EXPERT_VERIFICATION_REPORT.md`

---

## ✨ 项目亮点

- ✅ **DeepSeek AI 场景识别** - 真正的 AI 智能识别
- ✅ **ColorOS 16 设计** - 完美适配 Android 16
- ✅ **安全配置** - 完整的安全和隐私保护
- ✅ **离线本地识别** - Google ML Kit 本地识别
- ✅ **16+ 哈苏预设** - 专业的摄影预设

---

**祝构建顺利！🚀**

**构建完成后，请将 `app-debug.apk` 安装到 Android 16 设备进行完整测试！**
