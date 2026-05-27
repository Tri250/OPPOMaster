# OMaster - OPPO 哈苏影像系统级参数中枢

## 项目简介

OMaster 是一个为 OPPO 哈苏影像系统设计的专业调色参数库应用，完全重构以拥抱 ColorOS 16 和 Android 16 的系统级能力。

## 🎯 核心特性

### 🔮 系统级集成
- **流体云胶囊集成**: 替代传统悬浮窗，实现无缝参数流转（基于 OPPO 官方 Fluid Cloud API）
- **一键闪记支持**: 与 ColorOS 16 AI 闪记深度集成，实现参数的快速保存与应用

### 🤖 AI 智能能力
- **AI 场景识别**: MediaPipe + TensorFlow Lite 端侧推理，实时分析拍摄场景
- **色调反向解析**: OpenCV + K-Means 聚类算法，自动提取照片色调并匹配预设
- **智能参数推荐**: 四维加权算法（场景40% + 色调30% + 时间15% + 偏好15%）

### 📷 专业影像
- **GPU 实时预览**: OpenGL ES 3.2 Shader 管线，60fps 所见即所得
- **相机参数注入**: Camera2 Extensions + OPPO Camera SDK 双引擎支持
- **3D LUT 色彩分级**: 支持哈苏/富士/柯达标准 .cube 文件

### 🎨 用户体验
- **金标设计语言**: 采用 ColorOS 16 Aqua Design 水生设计风格
- **哈苏专业体验**: HNCS 认证预设，拟物化参数控件
- **主题系统**: 支持深色、浅色和跟随系统主题切换
- **数据持久化**: Room 数据库 + DataStore Preferences 双重保障
- **搜索筛选**: 快速找到你想要的预设

## 🛠️ 技术栈

- **语言**: Kotlin
- **UI 框架**: Jetpack Compose + Material 3
- **依赖注入**: Hilt
- **本地数据库**: Room 2.6.1
- **数据持久化**: DataStore Preferences
- **网络请求**: Retrofit + OkHttp
- **图片加载**: Coil
- **AI 推理**: MediaPipe 0.10.9 + TensorFlow Lite 2.15.0
- **图像处理**: OpenCV Android 4.8.0
- **GPU 渲染**: OpenGL ES 3.2
- **相机**: CameraX 1.3.1 + Camera2 Extensions
- **最低 SDK**: API 26 (Android 8.0)
- **目标 SDK**: API 35 (Android 16)

## 📁 项目结构

```
OMaster/
├── app/
│   ├── src/
│   │   └── main/
│   │       ├── java/com/omaster/app/
│   │       │   ├── config/              # 配置常量
│   │       │   ├── data/                # 数据层 (仓库、数据库、DataStore)
│   │       │   ├── di/                  # Hilt 依赖注入模块
│   │       │   ├── model/               # 数据模型 (含实体类)
│   │       │   ├── navigation/           # 导航定义
│   │       │   ├── network/              # 网络请求 API
│   │       │   ├── service/              # 核心服务
│   │       │   │   ├── FluidCloudCapsuleManager.kt    # 流体云胶囊
│   │       │   │   ├── OneTapFlashNoteService.kt     # AI 一键闪记
│   │       │   │   ├── SceneRecognitionEngine.kt     # AI 场景识别
│   │       │   │   ├── ColorExtractionEngine.kt      # 色调反向解析
│   │       │   │   ├── RealtimePreviewRenderer.kt    # GPU 实时预览
│   │       │   │   ├── CameraParameterInjector.kt    # 相机参数注入
│   │       │   │   └── CloudPresetService.kt         # 社区预设服务
│   │       │   ├── ui/
│   │       │   │   ├── components/       # 可复用 UI 组件
│   │       │   │   ├── screens/          # 页面组件
│   │       │   │   └── theme/            # 主题配置
│   │       │   ├── viewmodel/            # ViewModel
│   │       │   ├── util/                 # 工具类
│   │       │   ├── MainActivity.kt
│   │       │   └── OMasterApplication.kt
│   │       ├── assets/                   # 资源文件 (流体云模板)
│   │       └── res/                      # Android 资源
│   └── build.gradle.kts
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
├── FLUID_CLOUD_INTEGRATION.md            # 流体云集成指南
└── ONE_TAP_FLASH_NOTE_INTEGRATION.md     # 一键闪记集成指南
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

# 清理并重新构建
./gradlew clean assembleDebug
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
5. **AI 场景识别**: 在 AI 页面体验智能场景检测和推荐
6. **色调分析**: 上传照片，自动提取色调并匹配预设
7. **一键闪记**: 长按预设卡片，一键保存到 ColorOS 闪记
8. **设置主题**: 在设置页面切换浅色、深色或跟随系统主题

### 核心功能详解

#### 🏠 首页
- 预设卡片网格布局，支持瀑布流展示
- 搜索栏和筛选功能（按场景、设备、评分筛选）
- 支持收藏标记和快速操作

#### 📄 详情页
- 预设封面大图预览
- 相机参数详细展示（ISO、快门、曝光、白平衡等）
- GPU 实时预览效果（对比度、饱和度、暗角调整）
- 一键闪记保存功能
- 收藏和分享功能

#### 🤖 AI 场景识别
- MediaPipe 端侧场景分类（人像/风景/美食/夜景/街拍/微距）
- 四维推荐算法智能匹配
- GPU 加速推理，<100ms 响应

#### 🎨 色调分析
- OpenCV LAB 色彩空间分析
- K-Means 聚类提取 5 个主导色
- 余弦相似度匹配预设
- 自动生成自定义预设

#### ⚙️ 设置页
- 主题选择（深色/浅色/跟随系统）
- 流体云功能开关
- AI 闪记功能开关
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
    val isFavorite: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
    val usageCount: Int,
    val rating: Float,
    val author: String
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
    val hasselblad_hncs: Boolean,
    val contrast: Float,
    val saturation: Float,
    val sharpness: Float,
    val vignette: Float,
    val videoLut: String,
    val sceneTags: List<String>,
    val colorProfile: ColorProfile?
)
```

## 🎨 主题系统

应用支持三种主题模式：
- **跟随系统**: 自动根据系统设置切换
- **浅色模式**: 明亮清晰的界面
- **深色模式**: 专业优雅的暗色主题

## ✨ 最近更新

### 🔥 最新功能
- ✅ **流体云胶囊**: 基于 OPPO 官方 API 的完整实现
- ✅ **AI 一键闪记**: ColorOS 16 闪记深度集成
- ✅ **AI 场景识别**: MediaPipe + TFLite 端侧推理
- ✅ **色调反向解析**: OpenCV + K-Means 聚类算法
- ✅ **GPU 实时预览**: OpenGL ES 3.2 Shader 管线
- ✅ **相机参数注入**: Camera2 + OPPO SDK 双引擎
- ✅ **Room 数据库**: 本地数据持久化
- ✅ **完整主题系统**: 深色/浅色/跟随系统
- ✅ **搜索筛选**: 多维度筛选功能
- ✅ **社区预设同步**: 支持 OPPO/Realme 社区预设库

### 🌐 社区预设支持

OMaster 现已支持从社区预设库自动加载大师预设：

| 预设库 | 来源 | 数量 | 类型 |
|-------|------|------|------|
| OPPO 大师预设 | OMaster-Community | 10+ | 德味、富士胶片、理光绿/蓝等 |
| Realme GR预设 | OMaster-Community | 2+ | 理光正片、理光负片 |

**预设示例**:
- 德味预设 - 经典德系胶片质感
- 富士胶片 - 复古胶片风格
- 理光绿 - 清新自然绿色调
- 理光蓝 - 通透冷色调
- 高对比黑白 - 艺术黑白效果
- 童话 - 梦幻柔光效果
- 蓝调时刻 - 夜景蓝调风格

**加载机制**:
- 首次启动自动下载
- 24小时本地缓存
- 支持手动刷新

### 🔧 技术亮点
- **性能优化**: AI 推理 <100ms，实时预览 60fps
- **隐私安全**: 端侧 AI 处理，数据不上传云端
- **降级方案**: 非 ColorOS 16 设备自动降级
- **模块化设计**: 各功能独立，易于维护和扩展
- **智能缓存**: 云端预设本地缓存，减少网络请求

### 📋 集成文档
- [FLUID_CLOUD_INTEGRATION.md](FLUID_CLOUD_INTEGRATION.md) - 流体云集成指南
- [ONE_TAP_FLASH_NOTE_INTEGRATION.md](ONE_TAP_FLASH_NOTE_INTEGRATION.md) - 一键闪记集成指南

### 🚧 下一步开发
- [ ] 完善视频预设支持
- [ ] 添加更多 AI 模型
- [ ] 实现参数导出功能（CSV/JSON）
- [ ] 添加用户评分和评论系统
- [ ] 支持自定义预设上传分享

## 📄 许可证

本项目遵循相关开源许可证。

## 🤝 致谢

- OPPO 哈苏影像系统
- ColorOS 设计团队
- OPPO 开放平台
- OMaster 社区贡献者
- MediaPipe / TensorFlow Lite 团队
- OpenCV 社区
- OMaster-Community 预设库

---

Made with ❤️ by **带娃的小陈工**
