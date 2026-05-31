# OMaster APK 构建指南

## 项目概述
- 应用名称: OMaster
- 版本: 1.2.1 (versionCode: 121)
- 目标: Android 16 (API 35)
- 最低: Android 8.0 (API 26)
- 架构: MVVM + Hilt + Jetpack Compose

## 前置要求
1. JDK 17 或更高
2. Android SDK API 35
3. 网络连接 (用于下载依赖)

## 快速开始

### 1. 生成调试密钥库
```bash
cd app
keytool -genkey -v -keystore debug.keystore -storepass android -alias androiddebugkey -keypass android -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=Android Debug,O=Android,C=US"
```

### 2. 生成发布密钥库 (可选)
```bash
cd app
keytool -genkey -v -keystore release.keystore -storepass changeme -alias omaster -keypass changeme -keyalg RSA -keysize 2048 -validity 10000
```

### 3. 构建 Debug APK
```bash
cd /workspace
./gradlew clean assembleDebug
```
输出位置: `app/build/outputs/apk/debug/app-debug.apk`

### 4. 构建 Release APK
```bash
cd /workspace
./gradlew clean assembleRelease
```
输出位置: `app/build/outputs/apk/release/app-release.apk`

## 项目结构

### 核心文件
- [MainActivity.kt](app/src/main/java/com/omaster/app/MainActivity.kt) - 应用入口
- [OMasterApplication.kt](app/src/main/java/com/omaster/app/OMasterApplication.kt) - 应用实例
- [MainViewModel.kt](app/src/main/java/com/omaster/app/viewmodel/MainViewModel.kt) - 视图模型

### 数据层
- [PresetRepository.kt](app/src/main/java/com/omaster/app/data/PresetRepository.kt) - 预设仓库
- [PreferencesDataStore.kt](app/src/main/java/com/omaster/app/data/PreferencesDataStore.kt) - 数据存储

### UI层
- [ProHomeScreenV2.kt](app/src/main/java/com/omaster/app/ui/screens/ProHomeScreenV2.kt) - 首页
- [ProDetailScreen.kt](app/src/main/java/com/omaster/app/ui/screens/ProDetailScreen.kt) - 详情页
- [ProSettingsScreenV2.kt](app/src/main/java/com/omaster/app/ui/screens/ProSettingsScreenV2.kt) - 设置页

### 组件
- [ProComponents.kt](app/src/main/java/com/omaster/app/ui/components/ProComponents.kt) - 专业组件
- [GlassPresetCard.kt](app/src/main/java/com/omaster/app/ui/components/GlassPresetCard.kt) - 预设卡片
- [GlassFilterChips.kt](app/src/main/java/com/omaster/app/ui/components/GlassFilterChips.kt) - 筛选组件

## 功能特性

### 已实现功能
1. ✅ 预设浏览与筛选
2. ✅ 哈苏 HNCS 认证显示
3. ✅ 收藏管理
4. ✅ 主题切换 (浅色/深色/跟随系统)
5. ✅ 网络同步
6. ✅ AI 场景检测
7. ✅ AI 参数微调
8. ✅ 相机配置
9. ✅ 流体云胶囊
10. ✅ 全局崩溃防护

### 预设库
包含 6 个专业预设:
1. 哈苏人像经典 (OPPO Find X8 Ultra)
2. 哈苏风景大师 (OPPO Find X8 Ultra)
3. 哈苏夜景大师 (OPPO Find X8 Ultra)
4. 哈苏美食摄影 (OPPO Find X8 Ultra)
5. 哈苏街拍模式 (OnePlus 13 Pro)
6. 海岛风情 (realme GT7 Pro)

## Android 16 兼容性

### 已验证
- ✅ targetSdk 35
- ✅ compileSdk 35
- ✅ Material Design 3
- ✅ Jetpack Compose 最新稳定版
- ✅ DataStore 数据持久化
- ✅ 网络安全配置正确

### 权限声明
```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
<uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />
<uses-permission android:name="android.permission.READ_MEDIA_IMAGES" />
```

## 构建说明

### 依赖版本
- Gradle: 8.7
- Android Gradle Plugin: 8.7.3
- Kotlin: 2.0.21
- Compose BOM: 2024.09.00
- Hilt: 2.51.1

### 构建命令
```bash
# 清理构建
./gradlew clean

# Debug 构建
./gradlew assembleDebug

# Release 构建
./gradlew assembleRelease

# 安装到设备
./gradlew installDebug

# 运行测试
./gradlew test
```

## 安装验证

在 Android 16 设备上:
1. 安装 APK
2. 打开应用
3. 浏览预设列表
4. 验证无崩溃

## 故障排除

### 构建失败
1. 检查 JDK 版本: `java -version`
2. 清理构建缓存: `./gradlew clean`
3. 网络超时: 配置代理或重试

### 运行时崩溃
1. 检查 Logcat 日志
2. 验证所有依赖正确
3. 确认最低SDK版本支持

## 联系方式
如有问题，请查看项目 README 或联系开发团队。
