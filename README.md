# OMaster - OPPO 哈苏影像系统级参数中枢

## 项目简介

OMaster 是一个为 OPPO 哈苏影像系统设计的专业调色参数库应用，完全重构以拥抱 ColorOS 16 和 Android 16 的系统级能力。

## 作者

**带娃的小陈工** 👨‍👧💻

## ✨ 核心特性

### 🎨 哈苏大师预设
- 10+ 官方哈苏大师预设
- HNCS 认证专业色彩
- 适配多种场景（风景、人像、夜景等）

### 🤖 AI 智能功能
- **AI 场景识别**: 智能分析照片场景，推荐最匹配的预设
- **AI 样张微调**: 根据所选预设自动优化拍摄的照片

### 🚀 系统级集成
- **流体云胶囊集成**: 替代传统悬浮窗，实现无缝参数流转
- **一键闪记支持**: 与系统相机深度集成，实现参数的快速保存与应用

### 🎭 金标设计
- **ColorOS 16 Aqua Design**: 采用水生设计风格
- **玻璃质感 UI**: 流畅的动效和精美的卡片设计
- **哈苏专业色调**: Hasselblad Orange 品牌色彩

### 🔧 技术特点
- **机型自适应**: 根据设备特性智能调整参数
- **收藏管理**: 快速保存和管理常用预设
- **参数预览**: 网格布局展示详细设置

## 🛠️ 技术栈

- **语言**: Kotlin
- **UI 框架**: Jetpack Compose + Material 3
- **最低 SDK**: API 26 (Android 8.0)
- **目标 SDK**: API 33 (Android 13)
- **架构**: MVVM + Repository Pattern

## 📂 项目结构

```
OMaster/
├── app/
│   ├── src/
│   │   └── main/
│   │       ├── java/com/omaster/app/
│   │       │   ├── data/              # 数据层 (预设仓库)
│   │       │   ├── model/             # 数据模型
│   │       │   │   ├── Preset.kt
│   │       │   │   ├── CameraParams.kt
│   │       │   │   ├── SceneType.kt        # 场景类型
│   │       │   │   └── AiAdjustmentParams.kt # AI调整参数
│   │       │   ├── service/           # 服务层
│   │       │   │   ├── FluidCloudService.kt
│   │       │   │   └── AiService.kt        # AI服务核心
│   │       │   ├── ui/
│   │       │   │   ├── components/    # 可复用 UI 组件
│   │       │   │   ├── screens/       # 页面组件
│   │       │   │   │   ├── HomeScreen.kt
│   │       │   │   │   ├── DetailScreen.kt
│   │       │   │   │   ├── SceneDetectionScreen.kt  # AI场景识别
│   │       │   │   │   ├── AiFineTuneScreen.kt    # AI微调
│   │       │   │   │   └── SettingsScreen.kt
│   │       │   │   └── theme/         # 主题配置
│   │       │   └── MainActivity.kt
│   │       └── res/                   # 资源文件
│   └── build.gradle.kts
├── build.gradle.kts
├── settings.gradle.kts
└── gradle.properties
```

## 🚀 构建指南

### 前置要求

- Android Studio Hedgehog (2023.1.1) 或更高版本
- JDK 17
- Android SDK (API 33)
- Gradle 8.0+

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

## 📱 使用说明

### 基本操作

1. **浏览预设**: 在首页浏览各种专业调色预设
2. **AI 场景识别**: 点击首页"AI 场景识别"上传照片，获取智能推荐
3. **查看详情**: 点击预设卡片查看详细参数和说明
4. **AI 样张微调**: 在详情页点击"AI 微调"优化你的照片
5. **应用预设**: 点击"应用预设"激活使用
6. **收藏预设**: 点击卡片上的收藏图标保存常用预设

### 场景类型

应用支持识别9种场景：
- 🏔️ 风景
- 👤 人像
- 🌙 夜景
- 🌅 日落
- 🍔 美食
- 🚶 街头
- 🌿 自然
- 🏛️ 建筑
- 🔍 微距

## 📊 数据模型

### 预设数据结构

```kotlin
data class Preset(
    val id: String,
    val name: String,
    val coverPath: String,
    val galleryImages: List<String> = emptyList(),
    val author: String = "",
    val isNew: Boolean = false,
    val sections: List<Section> = emptyList(),
    val tags: List<String> = emptyList(),
    val description: Description? = null,
    val cameraParams: CameraParams?,
    val deviceModel: String,
    val source: String,
    val isFavorite: Boolean,
    val applicableScenes: List<SceneType> = emptyList()
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

### AI 调整参数

```kotlin
data class AiAdjustmentParams(
    val brightness: Float,
    val contrast: Float,
    val saturation: Float,
    val warmth: Float,
    val tint: Float,
    val highlights: Float,
    val shadows: Float,
    val clarity: Float,
    val vignette: Float
)
```

## 🎯 预设列表

包含官方哈苏大师预设：
- 🇩🇪 德味预设
- 📷 富士胶片
- 🎞️ 胶片感
- 🏰 童话
- ⬛ 高对比黑白
- 💚 理光绿
- 💙 理光蓝
- 🌃 蓝调时刻
- ✨ 梦幻黑柔
- 🇮🇹 哈苏 X2D - 佛罗伦萨

## 📜 许可证

本项目遵循相关开源许可证。

## 👏 致谢

- **带娃的小陈工** - 作者与主要开发者
- OPPO 哈苏影像系统
- ColorOS 设计团队
- OMaster 社区贡献者
- OPPO Official Presets

---

Made with ❤️ by **带娃的小陈工**
