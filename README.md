# OMaster - OPPO 哈苏影像系统级参数中枢

## 项目简介

OMaster 是一个为 OPPO 哈苏影像系统设计的专业调色参数库应用，完全重构以拥抱 ColorOS 16 和 Android 16 的系统级能力。

## 核心特性

- **流体云胶囊集成**: 替代传统悬浮窗，实现无缝参数流转
- **一键闪记支持**: 与系统相机深度集成，实现参数的快速保存与应用
- **金标设计语言**: 采用 ColorOS 16 Aqua Design 水生设计风格
- **哈苏专业体验**: HNCS 认证预设，拟物化参数控件
- **机型自适应**: 根据设备特性智能调整参数

## 技术栈

- **语言**: Kotlin
- **UI 框架**: Jetpack Compose + Material 3
- **最低 SDK**: API 26 (Android 8.0)
- **目标 SDK**: API 35 (Android 16)

## 项目结构

```
OMaster/
├── app/
│   ├── src/
│   │   └── main/
│   │       ├── java/com/omaster/app/
│   │       │   ├── data/          # 数据层 (预设仓库)
│   │       │   ├── model/         # 数据模型
│   │       │   ├── service/       # 系统服务 (流体云等)
│   │       │   ├── ui/
│   │       │   │   ├── components/ # 可复用 UI 组件
│   │       │   │   ├── screens/   # 页面组件
│   │       │   │   └── theme/     # 主题配置
│   │       │   └── MainActivity.kt
│   │       └── res/               # 资源文件
│   └── build.gradle.kts
├── build.gradle.kts
├── settings.gradle.kts
└── gradle.properties
```

## 构建指南

### 前置要求

- Android Studio Hedgehog (2023.1.1) 或更高版本
- JDK 17
- Android SDK (API 35)
- Gradle 8.7

### 构建步骤

1. 克隆或下载项目
2. 在 Android Studio 中打开项目
3. 等待 Gradle 同步完成
4. 连接 Android 设备或启动模拟器
5. 点击 **Run** 按钮或使用命令:

```bash
# Debug 构建
./gradlew assembleDebug

# Release 构建
./gradlew assembleRelease
```

构建完成后，APK 文件将位于:
- Debug: `app/build/outputs/apk/debug/app-debug.apk`
- Release: `app/build/outputs/apk/release/app-release.apk`

## 使用说明

### 基本操作

1. **浏览预设**: 在首页浏览各种专业调色预设
2. **查看详情**: 点击预设卡片查看详细参数和说明
3. **应用预设**: 在详情页点击"应用预设"按钮激活流体云胶囊
4. **收藏预设**: 点击卡片上的收藏图标保存常用预设
5. **设置管理**: 在设置页面配置系统能力选项

### ColorOS 16 功能

- **流体云胶囊**: 选中预设后会在系统侧边栏显示快速访问入口
- **一键闪记**: 支持与系统相机的大师模式无缝集成
- **智能侧边栏**: 支持从智能侧边栏快速启动和访问

## 数据模型

### 预设数据结构

```kotlin
data class Preset(
    val id: String,
    val name: String,
    val coverPath: String,
    val sections: List<Section>,
    val cameraParams: CameraParams?,
    val deviceModel: String,
    val source: String,
    val isFavorite: Boolean
)
```

### 相机参数

```kotlin
data class CameraParams(
    val mode: String,
    val filter: String,
    val iso: Int,
    val shutter: String,
    val ev: String,
    val wb: String,
    val hasselblad_hncs: Boolean
)
```

## 许可证

本项目遵循相关开源许可证。

## 致谢

- OPPO 哈苏影像系统
- ColorOS 设计团队
- OMaster 社区贡献者
