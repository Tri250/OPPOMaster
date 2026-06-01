# 🚀 快速开始 - OMaster APK 构建

## ⚡ 3 分钟快速构建（使用 Android Studio）

### 步骤 1：安装 Android Studio（5 分钟）
**下载地址**：https://developer.android.com/studio

选择您的系统：
- Windows: `android-studio-2024.1.1.26-windows.exe`
- macOS: `android-studio-2024.1.1.26-mac_arm.dmg` (Apple Silicon)
- macOS: `android-studio-2024.1.1.26-mac.dmg` (Intel)
- Linux: `android-studio-2024.1.1.26-linux.tar.gz`

### 步骤 2：解压项目（1 分钟）
```bash
# 解压项目包
unzip OMaster_Full_Final_Build_Package.zip
cd omaster_final_build
```

### 步骤 3：打开项目（2 分钟）
1. 启动 Android Studio
2. 选择 **Open an Existing Project**
3. 选择 `omaster_final_build` 文件夹
4. 点击 **OK**

### 步骤 4：等待同步（3-10 分钟）
- 首次打开会自动下载 Gradle 和依赖
- 底部状态栏会显示同步进度
- 等待提示 "BUILD SUCCESSFUL"

### 步骤 5：构建 APK（1-3 分钟）
**方法 1**：菜单栏
```
Build → Build Bundle(s) / APK(s) → Build APK(s)
```

**方法 2**：快捷键
- Windows/Linux: `Ctrl + F9`
- macOS: `Cmd + F9`

**方法 3**：命令行
```bash
./gradlew assembleDebug
```

### 步骤 6：获取 APK
构建成功后会弹出通知：
1. 点击 **locate** 或 **show**
2. APK 位置：`app/build/outputs/apk/debug/app-debug.apk`

---

## 🎯 一键构建（已安装 Android SDK）

### Linux/macOS
```bash
# 设置环境变量
export ANDROID_HOME=$HOME/Android/Sdk
export PATH=$PATH:$ANDROID_HOME/platform-tools:$ANDROID_HOME/cmdline-tools/latest/bin

# 构建
cd /workspace
./build_apk.sh
```

### Windows
```cmd
REM 设置环境变量（在 CMD 中）
set ANDROID_HOME=C:\Users\YourName\AppData\Local\Android\Sdk
set PATH=%PATH%;%ANDROID_HOME%\platform-tools

REM 构建
cd C:\workspace
build_apk.bat
```

---

## 📱 安装到手机

### 方法 1：USB 传输
1. 用 USB 连接手机和电脑
2. 复制 `app-debug.apk` 到手机
3. 在手机上点击 APK 安装

### 方法 2：ADB 安装
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### 方法 3：无线传输
- 通过微信、QQ、蓝牙等方式传输 APK
- 在手机上点击安装

---

## ✅ Android 14-16 兼容性

### 已配置
- ✅ **Android 14 (API 34)** - 完全兼容
- ✅ **Android 15 (API 35)** - 完全兼容  
- ✅ **Android 16 (API 36)** - 基本兼容

### 权限配置
- ✅ INTERNET - 网络访问
- ✅ ACCESS_NETWORK_STATE - 网络状态
- ✅ SYSTEM_ALERT_WINDOW - 悬浮窗
- ✅ READ_MEDIA_IMAGES - 读取图片（Android 13+）
- ✅ FOREGROUND_SERVICE - 前台服务（Android 14+）

---

## 🔧 常见问题

### Q1: Gradle 同步很慢
**A**: 项目已配置阿里云镜像，应该很快。如果仍然慢，检查网络连接。

### Q2: 构建失败 - 内存不足
**A**: gradle.properties 已配置 4GB 内存。如果还不够：
```properties
org.gradle.jvmargs=-Xmx6144m
```

### Q3: 找不到 Java
**A**: 
- Windows: 确保 JAVA_HOME 环境变量指向 JDK 安装目录
- macOS: `export JAVA_HOME=$(/usr/libexec/java_home)`
- Linux: `export JAVA_HOME=/usr/lib/jvm/java-17-openjdk`

### Q4: SDK 未找到
**A**: 
- 使用 Android Studio 会自动配置
- 手动配置：设置 ANDROID_HOME 环境变量

---

## 📦 构建产物说明

### Debug APK
- **文件**: `app-debug.apk`
- **大小**: ~30-50MB
- **签名**: 自动签名（debug 密钥）
- **用途**: 开发、测试

### Release APK
- **文件**: `app-release.apk`
- **大小**: ~20-40MB（经过压缩和优化）
- **签名**: 需要 release 密钥
- **用途**: 正式发布

---

## 🎉 完成！

现在您已经有了可以在 Android 14-16 系统上安装的 APK 文件！

**下一步**：
1. 在真机上测试安装
2. 验证所有功能正常
3. 如需发布，配置 release 签名

---

**需要帮助？** 查看 [INSTALL_ANDROID_SDK_GUIDE.md](INSTALL_ANDROID_SDK_GUIDE.md)
