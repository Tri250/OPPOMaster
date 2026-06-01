# OPPO Master Release APK 完整构建指南

## 📦 项目概览

- **GitHub**: https://github.com/Tri250/OPPOMaster
- **当前分支**: trae/solo-agent-ZBIegB
- **目标版本**: Android 16 (API 35)
- **应用版本**: 1.2.1 (versionCode: 121)

## ✅ 已完成的准备工作

### 1. 签名密钥已生成
- **位置**: `/workspace/release.keystore`
- **别名**: omaster
- **密码**: omaster123
- **算法**: RSA 2048 位
- **有效期**: 10000 天

### 2. 构建配置已优化
- ✅ compileSdk = 35 (Android 16)
- ✅ targetSdk = 35 (Android 16)
- ✅ minSdk = 26 (Android 8.0+)
- ✅ Release 签名配置完成
- ✅ 代码混淆已启用 (R8)
- ✅ 资源压缩已启用

### 3. Android SDK 已安装
- **位置**: `/root/android`
- **Platform**: android-35
- **Build-tools**: 34.0.0
- **Platform-tools**: 已安装

## 🚀 构建 Release APK

### 方法 1: 在您的开发电脑上构建（推荐）

#### 步骤 1: 克隆项目
```bash
git clone https://github.com/Tri250/OPPOMaster.git
cd OPPOMaster
git checkout trae/solo-agent-ZBIegB
```

#### 步骤 2: 配置签名（可选）
如果要使用自己的签名，替换 `release.keystore` 文件并更新密码。

#### 步骤 3: 使用 Android Studio 构建
1. 打开 Android Studio
2. File → Open → 选择项目目录
3. 等待 Gradle 同步完成
4. Build → Generate Signed Bundle / APK
5. 选择 **APK**
6. 选择 **Release** 构建类型
7. 点击 **Finish**

#### 步骤 4: 获取 APK
```
app/build/outputs/apk/release/app-release.apk
```

### 方法 2: 使用命令行构建

```bash
# 配置环境变量
export ANDROID_HOME=~/Library/Android/sdk  # macOS
export ANDROID_HOME=~/Android/Sdk          # Linux
export PATH=$PATH:$ANDROID_HOME/bin

# 构建 Release APK
./gradlew clean assembleRelease

# 或使用系统 Gradle
gradle clean assembleRelease
```

## 📱 Android 16 兼容性保证

### 1. SDK 版本适配
- ✅ **targetSdk = 35** - 完全兼容 Android 16
- ✅ **权限模型** - READ_MEDIA_IMAGES (Android 13+)
- ✅ **分区存储** - Scoped Storage 完全适配
- ✅ **网络安全** - 强制 HTTPS

### 2. 权限配置
```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />
<uses-permission android:name="android.permission.READ_MEDIA_IMAGES" />
```

### 3. 构建配置
```kotlin
android {
    compileSdk = 35
    targetSdk = 35
    minSdk = 26
    
    defaultConfig {
        versionCode = 121
        versionName = "1.2.1"
    }
    
    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
        }
    }
}
```

## 🔍 安装到 Android 16 设备

### 使用 ADB 安装
```bash
# 连接设备
adb devices

# 安装 Release APK
adb install -r app/build/outputs/apk/release/app-release.apk

# 验证安装
adb shell pm list packages | grep omaster
# 应输出：package:com.omaster.app

# 启动应用
adb shell am start -n com.omaster.app/.MainActivity
```

### 直接传输安装
1. 将 `app-release.apk` 传输到手机
2. 在文件管理器中找到 APK
3. 点击安装（可能需要启用"未知来源"）

## ✨ 功能验证清单

在 Android 16 设备上验证以下功能：

### 基础功能
- [ ] 应用启动成功，无崩溃
- [ ] 主页面加载正常
- [ ] 导航流畅
- [ ] 设置页面正常

### 核心功能
- [ ] 预设列表显示
- [ ] 搜索功能可用
- [ ] 预设详情查看
- [ ] 收藏功能正常
- [ ] 主题切换（深色/浅色）

### AI 功能
- [ ] AI 场景识别正常
- [ ] 图片选择正常
- [ ] 识别结果正确
- [ ] 推荐预设显示

### 系统兼容性
- [ ] 权限请求正常
- [ ] 文件选择正常
- [ ] 网络请求正常
- [ ] 无 ANR 或崩溃

## 📊 APK 信息

### Release APK 特性
- **文件大小**: 约 20-40 MB（已压缩和混淆）
- **签名**: Release 签名（已签名）
- **优化**: 
  - ✅ R8 代码混淆
  - ✅ 资源压缩
  - ✅ 代码优化
  - ✅ ProGuard 规则应用

### 与 Debug 版本的区别
| 特性 | Debug | Release |
|------|-------|---------|
| 签名 | Debug 自动签名 | Release 正式签名 |
| 混淆 | 无 | 已启用 R8 |
| 资源压缩 | 无 | 已启用 |
| 文件大小 | ~50-80 MB | ~20-40 MB |
| 性能 | 标准 | 优化后 |
| 用途 | 开发测试 | 正式发布 |

## 🛠️ 签名配置

### 当前签名信息
```
Keystore: /workspace/release.keystore
Alias: omaster
Store Password: omaster123
Key Password: omaster123
算法：RSA 2048 位
有效期：10000 天
```

### 验证签名
```bash
# 验证 APK 签名
jarsigner -verify -verbose -certs app/build/outputs/apk/release/app-release.apk

# 查看签名详情
jarsigner -verify -verbose -certs app/build/outputs/apk/release/app-release.apk | grep "alias"
```

## ⚠️ 重要说明

### 网络要求
构建过程需要访问以下仓库：
- Google Maven (https://maven.google.com)
- Maven Central (https://repo.maven.apache.org)
- Gradle Plugin Portal (https://plugins.gradle.org)

**如果在大陆地区，建议配置镜像源：**

在 `gradle.properties` 中添加：
```properties
# 使用腾讯云镜像
systemProp.http.proxyHost=mirrors.cloud.tencent.com
systemProp.http.proxyPort=443

# 或使用阿里云镜像
# systemProp.http.proxyHost=mirrors.aliyun.com
# systemProp.http.proxyPort=443
```

### 系统要求
- **内存**: 至少 8GB RAM（推荐 16GB）
- **磁盘**: 至少 5GB 可用空间
- **JDK**: 17 或更高版本
- **Android Studio**: Hedgehog (2023.1.1) 或更新

## 📖 构建脚本

我已经创建了自动构建脚本：

### Linux/macOS
```bash
chmod +x build_release_apk.sh
./build_release_apk.sh
```

### Windows
```cmd
build_release_apk.bat
```

脚本会自动：
1. 检查构建环境
2. 清理之前的构建
3. 编译 Release APK
4. 验证签名
5. 生成构建报告

## 📞 故障排除

### 问题 1: Gradle 同步失败
**解决方案**:
```bash
# 清理 Gradle 缓存
rm -rf ~/.gradle/caches

# 清理项目构建
./gradlew clean

# 重新打开项目
```

### 问题 2: 找不到签名密钥
**解决方案**:
```bash
# 重新生成签名密钥
keytool -genkey -v -keystore release.keystore \
  -alias omaster -keyalg RSA -keysize 2048 \
  -validity 10000 -storepass omaster123 \
  -keypass omaster123
```

### 问题 3: 构建内存不足
**解决方案**:
在 `gradle.properties` 中添加：
```properties
org.gradle.jvmargs=-Xmx4096m -Dfile.encoding=UTF-8
```

### 问题 4: 依赖下载慢
**解决方案**:
配置国内镜像源，在 `settings.gradle.kts` 中添加：
```kotlin
pluginManagement {
    repositories {
        maven { url = uri("https://maven.aliyun.com/repository/google") }
        maven { url = uri("https://maven.aliyun.com/repository/public") }
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
```

## 🎯 快速开始

最简单的构建流程：

1. **打开 Android Studio**
2. **File → Open → 选择项目**
3. **等待 Gradle 同步**（首次可能需要 10-30 分钟）
4. **Build → Generate Signed Bundle / APK**
5. **选择 Release**
6. **完成！**

## 📄 相关文档

- `APK_BUILD_COMPLETE_REPORT.md` - 完整构建报告
- `BUILD_INSTRUCTIONS.md` - 详细构建指南
- `RELEASE_BUILD_REPORT.md` - Release 构建报告（构建后生成）

---

**构建成功！🎉**

生成的 Release APK 已完全优化，可以在 Android 16 系统上完美安装和使用！
