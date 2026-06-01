# OPPO Master Android 16 完整APK构建指南

## 📋 项目配置状态

### Android版本适配
- ✅ **targetSdk**: 35 (Android 16)
- ✅ **compileSdk**: 35 (Android 16)
- ✅ **minSdk**: 26 (Android 8.0)
- ✅ **versionCode**: 121
- ✅ **versionName**: 1.2.1

### 核心功能模块
- ✅ AI场景识别 (含图片预处理/超时/降级)
- ✅ AI微调功能
- ✅ 哈苏相机预设
- ✅ ColorOS动画系统
- ✅ 悬浮窗服务
- ✅ 实时相机参数
- ✅ 水印编辑器
- ✅ 系统集成 (文件分享等)

### 签名配置
- ✅ **Debug签名**: 已配置 (debug.keystore)
- ✅ **Release签名**: 已配置 (release.keystore占位)
- ✅ **V2/V3签名**: 已启用

---

## 🔧 APK构建步骤

### 前置要求
1. JDK 17 或更高版本
2. Android SDK 35 (Android 16 SDK)
3. 网络连接 (首次下载依赖)

### 构建Debug APK
```bash
cd /workspace
./gradlew clean assembleDebug
```

### 构建Release APK
```bash
cd /workspace
./gradlew clean assembleRelease
```

### 构建输出位置
```
Debug APK: /workspace/app/build/outputs/apk/debug/app-debug.apk
Release APK: /workspace/app/build/outputs/apk/release/app-release.apk
```

---

## 📱 Android 16 安装与兼容性确认

### 安装APK到设备
```bash
# 安装Debug版本
adb install -r /workspace/app/build/outputs/apk/debug/app-debug.apk

# 安装Release版本
adb install -r /workspace/app/build/outputs/apk/release/app-release.apk
```

### 验证安装成功
```bash
# 检查应用是否安装
adb shell pm list packages | grep omaster

# 启动应用
adb shell am start -n com.omaster.app/.MainActivity
```

---

## 🛡️ 功能验证检查清单

### 启动与基本导航
- [ ] 应用启动无崩溃
- [ ] 各页面导航正常
- [ ] Material You动态颜色适配正常

### AI场景识别功能
- [ ] 选择相册图片并识别
- [ ] 拍照并识别
- [ ] 场景识别结果正确
- [ ] 推荐预设正常显示
- [ ] 超时/异常降级处理正常

### 相机功能
- [ ] 相机参数读取正常
- [ ] 参数卡片显示完整
- [ ] 动画效果流畅

### 悬浮窗
- [ ] 悬浮窗权限请求正常
- [ ] 悬浮窗显示正常
- [ ] 悬浮窗交互正常

### 水印功能
- [ ] 水印编辑器打开正常
- [ ] 预设水印应用正常

---

## 🔍 常见问题解决

### 问题1: Gradle下载依赖慢
**解决方法**:
```bash
# 配置国内镜像源 (如果需要)
# 在项目根目录gradle.properties中添加:
# systemProp.https.proxyHost=mirrors.cloud.tencent.com
# systemProp.https.proxyPort=443
```

### 问题2: Android SDK 35未安装
**解决方法**:
```bash
# 使用Android SDK Manager安装API 35
# 或者在local.properties中指定已有的SDK路径:
sdk.dir=/path/to/your/android/sdk
```

### 问题3: 构建时内存不足
**解决方法**:
```bash
# 在gradle.properties中调整JVM内存:
org.gradle.jvmargs=-Xmx4096m -XX:MaxMetaspaceSize=1024m
```

### 问题4: Android 16上安装失败
**检查项**:
1. 是否启用了"未知来源"安装
2. APK是否使用v2/v3签名 (本项目已启用)
3. targetSdk=35是否符合设备要求

---

## ✅ 项目文件关键配置

### app/build.gradle.kts
```kotlin
android {
    namespace = "com.omaster.app"
    compileSdk = 35
    
    defaultConfig {
        applicationId = "com.omaster.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 121
        versionName = "1.2.1"
    }
}
```

### AndroidManifest.xml
```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools">
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />
    <uses-permission android:name="android.permission.READ_MEDIA_IMAGES" />
    
    <application
        android:name=".OMasterApplication"
        android:allowBackup="true"
        android:fullBackupContent="@xml/backup_rules"
        android:usesCleartextTraffic="false"
        tools:targetApi="35">
        ...
    </application>
</manifest>
```

---

## 🚀 快速开始 (推荐)

如果您有完整的Android开发环境:

1. **设置Android SDK路径**:
   编辑 `/workspace/local.properties`:
   ```
   sdk.dir=/path/to/your/android/sdk
   ```

2. **构建Debug APK**:
   ```bash
   cd /workspace
   ./gradlew clean assembleDebug
   ```

3. **安装到设备**:
   ```bash
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```

---

## 📞 获取帮助

如遇其他问题，请检查:
1. `COMPLETE_BUILD_GUIDE.md` - 完整构建指导
2. `BUILD_ERRORS_FIXED.md` - 已修复的构建错误
3. `COLOROS_16_EXPERT_ACCEPTANCE_REPORT.md` - 验收报告

---

**祝构建顺利! 🎉**

