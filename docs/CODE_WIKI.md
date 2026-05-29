# OPPO Master 架构文档

## 项目概述

OPPO Master 是一个为 OPPO 哈苏影像系统设计的专业调色参数库应用，采用 ColorOS 16 设计语言。

## 技术栈

### Android 端

- **语言**: Kotlin
- **架构**: MVVM + Repository
- **UI**: Jetpack Compose
- **依赖注入**: Hilt
- **异步处理**: Kotlin Coroutines & Flow
- **数据存储**: DataStore Preferences
- **网络请求**: Retrofit + OkHttp
- **JSON 解析**: Gson

### Web 端

- **框架**: React 19.2+
- **语言**: TypeScript
- **构建工具**: Vite
- **样式**: Tailwind CSS + Framer Motion
- **状态管理**: Zustand
- **路由**: React Router

## 模块划分

### app 模块 (Android)

```
app/src/main/java/com/omaster/app/
├── data/              # 数据层
│   ├── PresetRepository.kt
│   └── PreferencesDataStore.kt
├── model/             # 数据模型
│   ├── Preset.kt
│   ├── CameraParams.kt
│   └── Section.kt
├── network/           # 网络层
│   └── PresetApi.kt
├── service/           # 服务层
│   └── FloatingWindowService.kt
├── ui/                # UI 层
│   ├── screens/       # 页面
│   ├── components/    # 组件
│   └── theme/         # 主题
└── MainActivity.kt    # 主 Activity
```

### opmaster-web 模块 (Web)

```
opmaster-web/src/
├── components/        # 组件
├── pages/            # 页面
├── store/            # 状态管理
├── data/             # 数据
├── App.tsx           # 应用主组件
└── main.tsx         # 入口文件
```

## 核心数据模型

### Preset

预设对象，包含相机参数和元数据：

```kotlin
data class Preset(
    val id: String,
    val name: String,
    val coverPath: String,
    val sections: List<Section> = emptyList(),
    val cameraParams: CameraParams? = null,
    val deviceModel: String = "",
    val source: String = "omaster_cloud",
    val isFavorite: Boolean = false,
    val isNew: Boolean = false,
    val isFeatured: Boolean = false,
    val isPremium: Boolean = false,
    val downloadCount: Int = 0,
    val rating: Float = 0f,
    val tags: List<String> = emptyList()
)
```

### CameraParams

相机参数对象：

```kotlin
data class CameraParams(
    val mode: String = "master",
    val filter: String = "",
    val iso: Int = 64,
    val shutter: String = "1/200",
    val ev: String = "0",
    val wb: String = "5500K",
    val focal_length: String = "24mm",
    val aperture: String = "f/1.8",
    val hdr: Boolean = false,
    val night_mode: Boolean = false,
    val portrait_mode: Boolean = false,
    val ai_optimization: Boolean = true,
    val hasselblad_hncs: Boolean = false,
    val hasselblad_natural_color: Boolean = true,
    val hasselblad_master_style: String = "",
    val color_profile: String = "Natural",
    val sharpness: Int = 50,
    val contrast: Int = 50,
    val saturation: Int = 50
)
```

## 数据流向

### 预设加载流程

1. **初始化**: `PresetRepository` 从 `assets/presets.json` 加载预设数据
2. **展示**: UI 通过 Flow 订阅预设列表更新
3. **交互**: 用户点击收藏 → 更新 `PreferencesDataStore`
4. **同步**: 收藏状态变化自动反映到 UI

### 悬浮窗流程

1. **启动**: `FloatingWindowService` 创建悬浮窗视图
2. **更新**: 从 `PresetRepository` 获取当前选中预设
3. **展示**: 在悬浮窗中显示预设参数

## 核心类和方法说明

### PresetRepository

核心数据仓库，负责预设数据的加载和管理：

- `loadPresets()`: 从 assets 加载预设数据
- `getPresets()`: 获取所有预设 Flow
- `toggleFavorite(presetId: String)`: 切换收藏状态
- `isFavorite(presetId: String)`: 检查收藏状态
- `getFavoritePresets()`: 获取收藏预设列表

### PreferencesDataStore

应用偏好设置存储：

- `favoritePresets`: 收藏预设 Flow
- `getFavorites()`: 获取收藏 ID 集合
- `saveFavorites(favorites: Set<String>)`: 保存收藏
- `toggleFavorite(presetId: String)`: 切换单个收藏

## 设计规范

### ColorOS 16 色彩系统

- **主色调**: `#FF6B35` (OPPO 橙)
- **哈苏色**: `#D4A574`
- **深色背景**: `#0F0F0F`
- **卡片表面**: `#1A1A1A`

### 组件库

所有 UI 组件遵循 ColorOS 16 设计规范，提供统一的用户体验。

## 状态管理

### Android

使用 Kotlin Flow 进行响应式数据流处理，配合 Jetpack Compose 的状态感知。

### Web

使用 Zustand 进行轻量级状态管理，配合 React 的 hooks。
