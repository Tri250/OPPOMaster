# OPPO哈苏影像系统 - APP构建指南

## 📱 项目概述

**项目名称**: OPPO哈苏影像系统  
**包名**: com.omaster.app  
**版本**: 1.2.1 (versionCode: 121)  
**最小SDK**: 26 (Android 8.0)  
**目标SDK**: 34 (Android 14)

## ✅ 已完成的配置

1. ✅ **JDK 17** - 已配置好
2. ✅ **Gradle 8.14.4** - 已配置好
3. ✅ **Android SDK** - 已包含平台34和Build Tools 35.0.0
4. ✅ **签名配置** - debug.keystore已配置好
5. ✅ **依赖版本** - AGP 8.2.2, Kotlin 1.9.22, Hilt 2.48

---

## 🚀 在您的电脑上构建APK

### 方法一：使用Android Studio（推荐）

#### 1. 准备项目
```bash
# 下载项目包
omaster_build_ready.zip

# 解压
unzip omaster_build_ready.zip
cd OPPO-Master
```

#### 2. 打开项目
- 启动 Android Studio
- 选择 "File" → "Open"
- 选择解压后的项目文件夹
- 等待 Gradle 同步完成（首次需要下载依赖，约10-30分钟）

#### 3. 构建APK
- 菜单栏：**Build** → **Build Bundle(s) / APK(s)** → **Build APK(s)**
- 或使用快捷键：**Ctrl+F9** (Windows/Linux) 或 **Cmd+F9** (Mac)

#### 4. 获得APK
构建成功后：
- **Debug APK**: `app/build/outputs/apk/debug/app-debug.apk`
- **Release APK**: `app/build/outputs/apk/release/app-release.apk`

---

### 方法二：命令行构建

#### 1. 配置环境
```bash
# 配置 JAVA_HOME (macOS/Linux示例)
export JAVA_HOME=/path/to/your/jdk-17
export PATH=$JAVA_HOME/bin:$PATH

# 配置 Android SDK
export ANDROID_HOME=/path/to/your/android/sdk
export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin
export PATH=$PATH:$ANDROID_HOME/platform-tools
```

#### 2. 构建命令
```bash
# 进入项目目录
cd OPPO-Master

# 构建 Debug APK
./gradlew assembleDebug

# 构建 Release APK
./gradlew assembleRelease
```

---

## 📦 项目包说明

| 文件名 | 说明 |
|--------|------|
| **omaster_build_ready.zip** | **最新构建就绪项目（推荐）** ✨ |
| omaster_complete_package.zip | 完整项目包 |
| omaster_fixed_package.zip | 修复后项目包 |
| omaster_final_package.zip | 最终项目包 |
| omaster_project.zip | 原始项目包 |

---

## 🔧 配置文件

### 关键配置已设置：
- **local.properties** - 已配置SDK路径
- **build.gradle.kts** - AGP 8.2.2, Kotlin 1.9.22, Hilt 2.48
- **app/build.gradle.kts** - compileSdk 34, targetSdk 34, 签名配置
- **debug.keystore** - 已包含，用于签名APK

---

## 🎯 功能特性

### 核心功能
- ✅ HNCS认证预设管理
- ✅ AI智能场景识别（24种场景）
- ✅ 专业相机参数显示
- ✅ 水印编辑器
- ✅ ColorOS 16设计风格
- ✅ 社交分享功能

### 技术栈
- Jetpack Compose
- Hilt依赖注入
- CameraX
- DataStore
- Kotlin Coroutines

---

## ⚙️ 常见问题

### 1. Gradle同步失败
- 检查网络连接
- 配置代理（如需要）
- File → Invalidate Caches → Invalidate and Restart

### 2. SDK未找到
- 打开 File → Project Structure → SDK Location
- 配置正确的Android SDK路径

### 3. 依赖下载慢
- 在 `gradle.properties` 中配置国内镜像源
- 使用代理加速

---

## 📞 需要帮助？

1. 查看项目中的其他文档
2. 检查Android Studio的Build窗口日志
3. 参考完整构建指南：COMPLETE_BUILD_GUIDE.md

---

**祝你构建顺利！** 🎉
