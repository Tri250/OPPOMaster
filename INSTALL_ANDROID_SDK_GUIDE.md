# Android SDK 安装和构建指南

## 🚨 当前状态
- ✅ Java JDK 21 已安装
- ❌ Android SDK 未安装
- ✅ 项目配置已完成（已优化 for Android 14-16）

---

## 📥 方案一：使用 Android Studio（推荐）

### 步骤 1：下载 Android Studio
访问：https://developer.android.com/studio

下载最新版本（推荐：Android Studio Hedgehog 或更新）

### 步骤 2：安装并配置 SDK
1. 安装 Android Studio
2. 首次启动时，SDK 会自动安装
3. 确保安装以下组件：
   - Android SDK Platform 34 或 35
   - Android SDK Build-Tools 34 或 35
   - Android SDK Platform-Tools
   - Android Emulator（可选）

### 步骤 3：打开项目
1. 解压项目包：`unzip OMaster_Full_Final_Build_Package.zip`
2. 在 Android Studio 中打开 `omaster_final_build` 目录
3. 等待 Gradle 同步完成（首次需要 5-15 分钟）

### 步骤 4：构建 APK
```
Build → Build Bundle(s) / APK(s) → Build APK(s)
```

或使用快捷键：`Ctrl+F9` (Windows/Linux) 或 `Cmd+F9` (Mac)

---

## 📥 方案二：命令行安装 SDK（高级）

### 步骤 1：下载 Command Line Tools
```bash
# 下载
wget https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip

# 解压
mkdir -p ~/Android/Sdk/cmdline-tools
unzip commandlinetools-linux-11076708_latest.zip -d ~/Android/Sdk/cmdline-tools
mv ~/Android/Sdk/cmdline-tools/cmdline-tools ~/Android/Sdk/cmdline-tools/latest
```

### 步骤 2：配置环境变量
```bash
export ANDROID_HOME=$HOME/Android/Sdk
export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools
```

### 步骤 3：接受许可证并安装组件
```bash
# 接受许可证
yes | sdkmanager --licenses

# 安装必要组件
sdkmanager "platform-tools"
sdkmanager "platforms;android-35"
sdkmanager "build-tools;35.0.0"
```

### 步骤 4：构建 APK
```bash
cd omaster_final_build
chmod +x gradlew
./gradlew assembleDebug
```

APK 输出位置：`app/build/outputs/apk/debug/app-debug.apk`

---

## 📥 方案三：使用国内镜像（中国大陆用户推荐）

### 使用阿里云镜像

#### 修改 settings.gradle.kts
项目已配置阿里云镜像源，无需修改。

#### 或使用腾讯镜像
```bash
export GRADLE_USER_HOME=~/.gradle
echo "systemProp.http.proxyHost=" >> gradle.properties
echo "systemProp.https.proxyHost=" >> gradle.properties
```

---

## 🔧 常见问题解决

### 问题 1：Gradle 下载慢
**解决方案**：项目已配置阿里云镜像源，自动加速

### 问题 2：SDK 下载失败
**解决方案**：
```bash
# 使用国内镜像
export ANDROID_SDK_MIRROR=https://mirrors.tuna.tsinghua.edu.cn/android/repository
```

### 问题 3：构建内存不足
**解决方案**：gradle.properties 已配置 4GB 内存，如需调整：
```properties
org.gradle.jvmargs=-Xmx4096m
```

---

## 📱 Android 14-16 兼容性确认

### 已配置项
- ✅ `compileSdk = 35` (Android 15)
- ✅ `targetSdk = 35` (Android 15)
- ✅ `minSdk = 26` (Android 8.0+)
- ✅ Android 14+ 前台服务权限
- ✅ Android 14+ 导出组件配置
- ✅ Android 14+ 返回手势支持

### 安装要求
- Android 14 (API 34) ✅
- Android 15 (API 35) ✅
- Android 16 (API 36) ⚠️ (基于预览版，基本兼容)

---

## 🎯 快速开始

### Windows 用户
1. 安装 Android Studio
2. 解压项目
3. 打开项目
4. 点击 Build APK

### macOS 用户
1. 安装 Android Studio
2. 解压项目
3. 打开项目
4. 点击 Build APK

### Linux 用户
```bash
# 安装 Android Studio
sudo snap install android-studio --classic

# 或手动安装
# 1. 下载 Android Studio
# 2. 解压到 /opt
# 3. 运行 /opt/android-studio/bin/studio.sh
```

---

## 📦 构建产物

### Debug APK
- 位置：`app/build/outputs/apk/debug/app-debug.apk`
- 用途：开发和测试
- 签名：自动使用 debug.keystore

### Release APK
- 位置：`app/build/outputs/apk/release/app-release.apk`
- 用途：正式发布
- 签名：需要配置 release.keystore

---

## ✅ 验证安装

构建完成后，检查：
1. APK 文件大小应该在 20-50MB 之间
2. 使用 APK 分析器查看权限和功能
3. 在 Android 14-16 设备上测试安装

---

**准备就绪！选择适合您的方案开始构建。**
