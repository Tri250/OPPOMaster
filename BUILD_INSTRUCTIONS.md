# OPPO Master Android 项目 - APK 构建指南

## 📦 项目信息

- **GitHub 仓库**: https://github.com/Tri250/OPPOMaster
- **当前分支**: trae/solo-agent-ZBIegB
- **包名**: com.omaster.app
- **应用版本**: 1.0.0

## 🛠️ 构建环境要求

### 必需软件
1. **JDK 17 或更高版本**
   ```bash
   java -version  # 应该显示版本 >= 17
   ```

2. **Android SDK**
   - Android SDK Platform 34 (推荐) 或更高
   - Android SDK Build-Tools 34.0.0 或更高
   - Android SDK Platform-Tools

3. **Android Studio** (推荐)
   - 最新版本 (Hedgehog 2023.1.1 或更新)
   - 会自动管理 SDK 和依赖

### 推荐配置
- 内存：至少 8GB RAM
- 磁盘空间：至少 5GB 可用空间
- 网络连接：用于下载依赖（首次构建）

## 🚀 构建步骤

### 方法 1: 使用 Android Studio (最简单)

#### 步骤 1: 克隆项目
```bash
git clone https://github.com/Tri250/OPPOMaster.git
cd OPPOMaster
```

#### 步骤 2: 在 Android Studio 中打开
1. 启动 Android Studio
2. 选择 **File** → **Open**
3. 选择项目根目录
4. 等待 Gradle 同步完成

#### 步骤 3: 构建 APK
- **Debug 版本**: 
  - 菜单：**Build** → **Build Bundle(s) / APK(s)** → **Build APK(s)**
  - 或快捷键：`Ctrl+F9` (Windows/Linux) / `Cmd+F9` (Mac)

- **Release 版本**:
  - 菜单：**Build** → **Generate Signed Bundle / APK**
  - 选择 **APK**
  - 创建或选择签名密钥
  - 选择 **Release** 构建类型

#### 步骤 4: 获取 APK 文件
构建完成后，APK 位于：
```
app/build/outputs/apk/debug/app-debug.apk        (Debug 版本)
app/build/outputs/apk/release/app-release.apk    (Release 版本)
```

### 方法 2: 使用命令行

#### 步骤 1: 设置环境变量
```bash
# Linux/macOS
export ANDROID_HOME=~/Library/Android/sdk  # macOS
# 或
export ANDROID_HOME=~/Android/Sdk          # Linux

export PATH=$PATH:$ANDROID_HOME/tools:$ANDROID_HOME/platform-tools

# Windows (PowerShell)
$env:ANDROID_HOME="C:\Users\YourUsername\AppData\Local\Android\Sdk"
$env:PATH="$env:PATH;$env:ANDROID_HOME\tools;$env:ANDROID_HOME\platform-tools"
```

#### 步骤 2: 配置 local.properties
在项目根目录创建 `local.properties` 文件：
```properties
sdk.dir=/path/to/your/android/sdk
# 例如：
# sdk.dir=/Users/username/Library/Android/sdk  (macOS)
# sdk.dir=C:\\Users\\username\\AppData\\Local\\Android\\Sdk  (Windows)
```

#### 步骤 3: 执行构建
```bash
# 赋予 gradlew 执行权限 (Linux/macOS)
chmod +x gradlew

# 构建 Debug APK
./gradlew clean assembleDebug

# 构建 Release APK (需要先配置签名)
./gradlew clean assembleRelease
```

#### 步骤 4: 查找 APK
```bash
# Debug APK 位置
ls -lh app/build/outputs/apk/debug/app-debug.apk

# Release APK 位置
ls -lh app/build/outputs/apk/release/app-release.apk
```

## 📱 在 Android 16 上安装

### 方法 1: 使用 ADB
```bash
# 连接设备
adb devices

# 安装 APK
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### 方法 2: 直接传输
1. 将 APK 文件传输到手机
2. 在手机上找到 APK 文件
3. 点击安装（可能需要启用"未知来源"）

### 验证安装
```bash
# 检查应用是否已安装
adb shell pm list packages | grep omaster

# 启动应用
adb shell am start -n com.omaster.app/.MainActivity
```

## 🔧 常见问题解决

### 问题 1: Gradle 同步失败
**解决方案**:
```bash
# 清理 Gradle 缓存
./gradlew clean

# 删除 .gradle 目录
rm -rf .gradle

# 重新打开项目
```

### 问题 2: 找不到 Android SDK
**解决方案**:
1. 在 Android Studio 中：**File** → **Settings** → **Appearance & Behavior** → **System Settings** → **Android SDK**
2. 点击 **Edit** 并安装 SDK
3. 或手动设置 `local.properties` 中的 SDK 路径

### 问题 3: 依赖下载失败
**解决方案**:
- 检查网络连接
- 使用国内镜像（可选）：
  ```properties
  # 在 gradle.properties 中添加
  systemProp.http.proxyHost=mirrors.cloud.tencent.com
  systemProp.http.proxyPort=443
  ```

### 问题 4: 内存不足
**解决方案**:
```properties
# 在 gradle.properties 中增加内存
org.gradle.jvmargs=-Xmx4096m -Dfile.encoding=UTF-8
```

### 问题 5: 构建时出现签名错误
**解决方案**:
对于 Debug 版本，Android Studio 会自动使用 debug 签名。
对于 Release 版本，需要创建签名密钥：
```bash
keytool -genkey -v -keystore my-release-key.jks -keyalg RSA -keysize 2048 -validity 10000 -alias my-alias
```

## 📊 项目配置

### Android 版本
- **compileSdk**: 34
- **targetSdk**: 34
- **minSdk**: 26

### 技术栈
- **Kotlin**: 1.9.22
- **Android Gradle Plugin**: 8.2.2
- **Gradle**: 8.5
- **Compose BOM**: 2024.02.00
- **Hilt**: 2.48

### 核心依赖
- Jetpack Compose (Material 3)
- Hilt (依赖注入)
- Retrofit (网络)
- Coil (图片加载)
- DataStore (本地存储)

## ✨ 应用功能

- 🤖 AI 场景识别
- 🎨 ColorOS 风格界面
- 📷 哈苏相机预设
- 🖼️ 水印编辑器
- 🎭 悬浮窗功能
- 📊 实时相机参数

## 📝 构建产物说明

### Debug APK
- 包含调试信息
- 使用自动生成的 debug 密钥签名
- 适合开发和测试
- 文件较大（未混淆）

### Release APK
- 已优化和混淆
- 需要正式签名密钥
- 适合发布
- 文件较小

## 🔒 签名配置

### Debug 签名（自动）
项目已配置使用默认的 debug 签名，位于：
```
~/.android/debug.keystore
```

### Release 签名（可选）
如需配置 Release 签名，在 `app/build.gradle.kts` 中添加：
```kotlin
android {
    signingConfigs {
        create("release") {
            storeFile = file("release.keystore")
            storePassword = System.getenv("KEYSTORE_PASSWORD")
            keyAlias = System.getenv("KEY_ALIAS")
            keyPassword = System.getenv("KEY_PASSWORD")
        }
    }
    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
        }
    }
}
```

## 📞 获取帮助

如遇问题，请检查：
1. `README.md` - 项目说明
2. `APK_BUILD_GUIDE.md` - 详细构建指南
3. `COMPLETE_BUILD_GUIDE.md` - 完整构建说明
4. GitHub Issues - 查看已知问题

---

**祝您构建顺利！** 🎉

构建完成后，您将获得一个完全兼容 Android 16 的 APK 文件，可以直接安装使用。
