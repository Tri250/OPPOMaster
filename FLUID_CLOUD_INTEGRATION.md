# OPPOMaster 流体云集成指南

基于 OPPO 开放平台官方 API (API 3.0.0+) 的流体云实现

## 📋 概述

本文档说明了如何在 OPPOMaster 项目中正确实现 ColorOS 16 流体云功能。

## 🎯 核心组件

### 1. 模板文件
- `fluid_cloud_preset_template.xml` - 完整的预设模板（胶囊态 + 展开态）
- `fluid_cloud_compact_template.xml` - 简化的胶囊模板

### 2. 管理器类
- `FluidCloudCapsuleManager.kt` - 流体云胶囊管理器

### 3. 配置类
- `FluidCloudConstants.kt` - 常量和配置

### 4. 数据模型
- `FluidCloudModels.kt` - 数据模型定义

## 🚀 快速开始

### 基本用法

```kotlin
// 1. 创建管理器实例
val capsuleManager = FluidCloudCapsuleManager(context)

// 2. 创建流体云胶囊
val preset = presetRepository.getPresetById("1")
capsuleManager.createCapsule(preset)

// 3. 更新胶囊数据
val newPreset = presetRepository.getPresetById("2")
capsuleManager.updateCapsule(newPreset)

// 4. 销毁胶囊
capsuleManager.destroy()
```

### 高级用法 - 使用 FluidCloudData

```kotlin
// 1. 创建数据
val data = capsuleManager.createFluidCloudData(preset)

// 2. 自定义数据
val customData = data.copy(
    bgGradient = FluidCloudConstants.createGradient(90, "#FF7043", "#15d5db"),
    showApplyBtn = false
)

// 3. 更新胶囊
capsuleManager.updateCapsuleWithData(customData)
```

## 📐 模板结构

### FluidCloudPresetData 结构

```
FluidCloudPresetData
├── presetId: String
├── title: String
├── subtitle: String
├── leading: LeadingData
│   ├── category: String (mirror/switches)
│   ├── iconPath: String
│   ├── titleText: String
│   └── subtitleText: String
├── center: CenterData
│   ├── category: String (common/mirror/graphic-highlight/text-highlight/switches)
│   ├── mainTitle: String
│   ├── cameraParams: CameraParamsDisplay
│   ├── coverImagePath: String
│   ├── buttons: List<ButtonData>
│   └── paramsDisplay: List<ParamDisplay>
├── trailing: TrailingData?
│   ├── category: String (multi-texts/multi-buttons/progress/diff-element)
│   └── texts: List<String>
├── backgroundColor: String (支持 linear-gradient)
├── borderColor: String
├── animationType: String (colorFlow/none)
└── updateTransform: String (up/down/none)
```

## 🎨 自定义样式

### 渐变背景

```kotlin
// 使用常量方法
val gradient = FluidCloudConstants.createGradient(180, "#6366F1", "#8B5CF6")

// 直接使用字符串
val gradient = "linear-gradient(45deg,#3D43EB,rgb(255, 112, 67))"
```

### 颜色格式

支持的颜色格式：
- `#RRGGBB` - 十六进制
- `rgb(R, G, B)` - RGB
- `rgba(R, G, B, A)` - RGBA 带透明度
- `linear-gradient(angle, color1, color2, ...)` - 线性渐变

## 🔧 事件处理

### 按钮事件

```kotlin
// 监听胶囊点击
view.setOnClickListener {
    // 处理点击事件
}

// 按钮点击需要通过 Intent 处理
```

### 展开/收起

```kotlin
// 检查展开状态
val isExpanded = capsuleManager.isExpanded()

// 展开时会显示更多信息
```

## 📱 权限要求

```xml
<uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />
```

### 动态权限申请

```kotlin
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
    if (!Settings.canDrawOverlays(context)) {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}")
        )
        context.startActivity(intent)
    }
}
```

## 🎯 最佳实践

### 1. API 级别检查

```kotlin
if (Build.VERSION.SDK_INT >= FluidCloudConstants.RECOMMENDED_API_LEVEL) {
    // 使用完整功能
} else if (Build.VERSION.SDK_INT >= FluidCloudConstants.MIN_API_LEVEL) {
    // 使用降级方案
} else {
    // 不支持
}
```

### 2. 生命周期管理

```kotlin
// 在 Activity/Fragment 中
override fun onDestroy() {
    super.onDestroy()
    capsuleManager.destroy()
}
```

### 3. 数据更新

```kotlin
// 优雅地更新数据
val newData = currentData.copy(
    title = "新标题",
    bgGradient = newGradient
)
capsuleManager.updateCapsuleWithData(newData)
```

## 📂 文件位置

```
app/src/main/
├── assets/
│   ├── fluid_cloud_preset_template.xml
│   └── fluid_cloud_compact_template.xml
├── java/com/omaster/app/
│   ├── config/
│   │   └── FluidCloudConstants.kt
│   ├── model/
│   │   ├── Preset.kt
│   │   ├── CameraParams.kt
│   │   └── FluidCloudModels.kt
│   └── service/
│       └── FluidCloudCapsuleManager.kt
└── res/
    ├── layout/
    │   └── fluid_cloud_capsule.xml
    └── drawable/
        └── capsule_background.xml
```

## ⚠️ 注意事项

1. **权限要求**: 需要 `SYSTEM_ALERT_WINDOW` 权限
2. **API 兼容性**: ColorOS 14+ (API 30+) 推荐使用
3. **性能**: 避免频繁更新，建议间隔 ≥ 500ms
4. **内存**: 使用完后记得调用 `destroy()` 释放资源

## 🔗 参考链接

- [OPPO 开放平台 - 流体云组件](https://open.oppomobile.com/new/developmentDoc/info?id=12703)
- [OPPO 开放平台 - 流体云卡片](https://open.oppomobile.com/new/developmentDoc/info?id=12965)
- [ColorOS 15 流体云介绍](https://www.coloros.com/article/A00000075)

## 📄 许可证

本项目遵循 OPPO 开放平台相关协议。
