# OMaster - OPPO 哈苏影像系统级参数中枢

## 项目简介

OMaster 是一个为 OPPO 哈苏影像系统设计的专业调色参数库应用，完全重构以拥抱 ColorOS 16 和 Android 16 的系统级能力。

## 🎯 核心特性

- **流体云胶囊集成**: 替代传统悬浮窗，实现无缝参数流转
- **一键闪记支持**: 与系统相机深度集成，实现参数的快速保存与应用
- **金标设计语言**: 采用 ColorOS 16 Aqua Design 水生设计风格
- **哈苏专业体验**: HNCS 认证预设，拟物化参数控件
- **机型自适应**: 根据设备特性智能调整参数
- **主题系统**: 支持深色、浅色和跟随系统主题切换
- **数据持久化**: 收藏状态和设置自动保存
- **搜索筛选**: 快速找到你想要的预设

## 🛠️ 技术栈

- **语言**: Kotlin
- **UI 框架**: Jetpack Compose + Material 3
- **依赖注入**: Hilt
- **数据持久化**: DataStore Preferences
- **网络请求**: Retrofit + OkHttp
- **图片加载**: Coil
- **最低 SDK**: API 26 (Android 8.0)
- **目标 SDK**: API 35 (Android 16)

## 📁 项目结构

```
OMaster/
├── app/
│   ├── src/
│   │   └── main/
│   │       ├── java/com/omaster/app/
│   │       │   ├── data/          # 数据层 (仓库、数据存储)
│   │       │   ├── di/            # Hilt 依赖注入模块
│   │       │   ├── model/         # 数据模型
│   │       │   ├── navigation/    # 导航定义
│   │       │   ├── network/       # 网络请求 API
│   │       │   ├── service/       # 系统服务 (流体云等)
│   │       │   ├── ui/
│   │       │   │   ├── components/ # 可复用 UI 组件
│   │       │   │   ├── screens/   # 页面组件
│   │       │   │   └── theme/     # 主题配置
│   │       │   ├── viewmodel/     # ViewModel
│   │       │   ├── MainActivity.kt
│   │       │   └── OMasterApplication.kt
│   │       └── res/               # 资源文件
│   └── build.gradle.kts
├── build.gradle.kts
├── settings.gradle.kts
└── gradle.properties
```

## 🏗️ 如何构建和运行

### 前置要求

- Android Studio Hedgehog (2023.1.1) 或更高版本
- JDK 17
- Android SDK (API 35)
- Gradle 8.7

### 构建步骤 (在 Android Studio 中)

1. **打开项目**: 在 Android Studio 中打开项目根目录
2. **同步 Gradle**: 等待 Android Studio 自动同步 Gradle 依赖
3. **选择设备**: 连接 Android 设备或启动模拟器
4. **运行项目**: 点击 **Run** 按钮 (绿色三角形) 或按 **Shift+F10**

### 命令行构建

```bash
# Debug 构建
./gradlew assembleDebug

# 安装到设备
./gradlew installDebug

# Release 构建
./gradlew assembleRelease
```

构建完成后，APK 文件将位于:
- Debug: `app/build/outputs/apk/debug/app-debug.apk`
- Release: `app/build/outputs/apk/release/app-release.apk`

## 📱 使用说明

### 基本操作

1. **浏览预设**: 在首页浏览各种专业调色预设
2. **搜索筛选**: 使用搜索栏查找特定预设，或使用筛选按钮分类显示
3. **查看详情**: 点击预设卡片查看详细参数和说明
4. **收藏预设**: 点击卡片或详情页上的收藏图标保存常用预设
5. **设置主题**: 在设置页面切换浅色、深色或跟随系统主题
6. **系统设置**: 在设置页面配置流体云等系统能力选项

### 主要功能

#### 🏠 首页
- 预设卡片网格布局
- 搜索栏和筛选功能
- 支持收藏标记

#### 📄 详情页
- 预设封面大图
- 相机参数详细展示
- 详细使用说明
- 收藏和分享功能

#### ⚙️ 设置页
- 主题选择（深色/浅色/跟随系统）
- 系统能力开关
- 关于应用信息

## 📊 数据模型

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

## 🎨 主题系统

应用支持三种主题模式：
- **跟随系统**: 自动根据系统设置切换
- **浅色模式**: 明亮清晰的界面
- **深色模式**: 专业优雅的暗色主题

## ✨ 最近更新

### 最新功能
- ✅ 完整的主题系统支持
- ✅ DataStore 数据持久化
- ✅ 搜索和筛选功能
- ✅ Hilt 依赖注入架构
- ✅ Retrofit 网络层
- ✅ 流体云服务框架
- ✅ 单元测试基础

### 下一步开发
- [ ] 实现真实的流体云胶囊（需要 OPPO SDK）
- [ ] 添加云端同步功能
- [ ] 完善应用图标资源
- [ ] 添加更多预设内容
- [ ] 实现参数导出功能

## 📄 许可证

本项目遵循相关开源许可证。

## 🤝 致谢

- OPPO 哈苏影像系统
- ColorOS 设计团队
- OMaster 社区贡献者
- OPPO Official Presets

---

Made with ❤️ by **带娃的小陈工**
