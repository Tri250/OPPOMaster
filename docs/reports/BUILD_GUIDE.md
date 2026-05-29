# OMaster 项目构建指南

## 项目文件
项目已打包为 `omaster_project.zip`

## 构建要求
- Android Studio Hedgehog (2023.1.1) 或更高版本
- JDK 17 或更高版本
- Android SDK (API 35)

## 构建步骤

### 1. 解压项目
```bash
unzip omaster_project.zip
cd OMaster
```

### 2. 在 Android Studio 中打开
1. 打开 Android Studio
2. 选择 "Open an Existing Project"
3. 选择刚刚解压的项目目录
4. 等待 Gradle 同步完成

### 3. 构建 APK

#### 调试 APK
```bash
./gradlew assembleDebug
```
APK 位置: `app/build/outputs/apk/debug/app-debug.apk`

#### 发布 APK
```bash
./gradlew assembleRelease
```
APK 位置: `app/build/outputs/apk/release/app-release.apk`

### 4. 直接在 Android Studio 构建
- 点击菜单: Build > Build Bundle(s) / APK(s) > Build APK(s)
- 构建完成后会显示通知，点击即可打开 APK 位置

## 项目特性
- 完整的 Jetpack Compose UI (Material 3)
- 主题系统（浅色/深色/跟随系统）
- 预设搜索和筛选
- Hilt 依赖注入
- DataStore 数据持久化
- Retrofit 网络层
- 完整的单元测试

## 技术栈
- Kotlin
- Jetpack Compose
- Hilt
- DataStore
- Retrofit
- Coil
