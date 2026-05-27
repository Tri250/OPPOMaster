# OPPOMaster AI一键闪记集成指南

基于 ColorOS 16 "AI一键闪记" 功能的OPPO Master集成实现

## 📋 功能概述

AI一键闪记是ColorOS 16的核心AI功能，可以快速记录旅行攻略、美食种草、穿搭指南等信息。本项目实现了将相机预设快速保存到闪记的功能，让用户可以一键记录大师配方。

## 🎯 核心功能

### 1. 预设一键闪记
- 一键保存预设参数到闪记
- 自动提取相机参数（ISO、快门、曝光、白平衡等）
- 支持附带封面图片
- 自动添加场景标签

### 2. 快速保存
- 最少点击次数（仅需一次点击）
- 实时反馈保存状态
- 自动跳转到闪记应用

### 3. 批量保存
- 支持多个预设批量保存
- 统计保存成功/失败数量
- 容错处理，单个失败不影响其他

## 🚀 快速开始

### 基本用法

```kotlin
// 1. 初始化服务
val flashNoteService = OneTapFlashNoteService(context)

// 2. 快速保存单个预设
val result = flashNoteService.quickSavePreset(preset)
if (result.success) {
    // 保存成功
}

// 3. 保存带自定义标签的预设
val customTags = listOf("人像", "逆光")
val result = flashNoteService.savePresetWithCustomTags(preset, customTags)
```

### 使用辅助工具类

```kotlin
// 在Activity或Fragment中
FlashNoteHelper.init(context)

// 快速保存
FlashNoteHelper.quickSavePreset(context, preset)

// 批量保存
FlashNoteHelper.batchSavePresets(context, listOf(preset1, preset2))
```

## 📱 界面集成

### 使用自定义按钮组件

```xml
<com.omaster.app.ui.components.OneTapFlashNoteButton
    android:id="@+id/btn_flash_note"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content" />
```

```kotlin
val flashNoteButton = findViewById<OneTapFlashNoteButton>(R.id.btn_flash_note)
flashNoteButton.setPreset(preset)
flashNoteButton.setOnSaveSuccessListener {
    // 保存成功回调
}
```

### 在预设卡片中集成

```kotlin
presetCard.setOnLongClickListener {
    showFlashNoteDialog(preset)
    true
}

private fun showFlashNoteDialog(preset: Preset) {
    AlertDialog.Builder(this)
        .setTitle("保存到闪记")
        .setMessage("是否将 ${preset.name} 保存到AI闪记？")
        .setPositiveButton("保存") { _, _ ->
            FlashNoteHelper.quickSavePreset(this, preset)
        }
        .setNegativeButton("取消", null)
        .show()
}
```

## 🎨 数据结构

### FlashNoteData

```kotlin
data class FlashNoteData(
    val title: String,              // 标题
    val content: String,            // 内容（支持Markdown）
    val category: String,           // 分类
    val tags: List<String>,         // 标签列表
    val source: String,             // 来源
    val attachmentUri: Uri?,        // 附件URI
    val attachmentType: String,      // 附件类型
    val timestamp: Long,            // 时间戳
    val metadata: FlashNoteMetadata? // 元数据
)
```

### FlashNoteMetadata

```kotlin
data class FlashNoteMetadata(
    val presetId: String?,          // 预设ID
    val cameraParams: CameraParams?, // 相机参数
    val deviceModel: String?,      // 设备型号
    val author: String?,            // 作者
    val rating: Float?,             // 评分
    val usageCount: Int?,           // 使用次数
    val deviceInfo: DeviceInfo?     // 设备信息
)
```

## 📂 文件结构

```
app/src/main/
├── java/com/omaster/app/
│   ├── config/
│   │   └── FlashNoteConstants.kt      # 常量和配置
│   ├── model/
│   │   ├── FlashNoteModels.kt        # 数据模型
│   │   └── ...
│   ├── service/
│   │   └── OneTapFlashNoteService.kt # 核心服务
│   ├── ui/components/
│   │   └── OneTapFlashNoteButton.kt  # UI组件
│   └── util/
│       └── FlashNoteHelper.kt        # 辅助工具类
└── res/
    ├── layout/
    │   └── view_one_tap_flash_note.xml
    └── drawable/
        └── bg_flash_note_button.xml
```

## 🔧 配置选项

### FlashNoteConstants

```kotlin
// 支持的功能
const val FEATURE_PRESET_SAVE = "preset_save"          // 预设保存
const val FEATURE_CAMERA_PARAMS_SAVE = "camera_params_save" // 参数保存
const val FEATURE_IMAGE_ATTACHMENT = "image_attachment" // 图片附件
const val FEATURE_AUTO_TAG = "auto_tag"               // 自动标签
const val FEATURE_ONE_TAP_SAVE = "one_tap_save"       // 一键保存
const val FEATURE_SMART_CATEGORY = "smart_category"   // 智能分类

// 分类
const val CATEGORY_PRESET = "preset"           // 预设分类
const val CATEGORY_CAMERA_PARAMS = "camera_params"  // 参数分类
const val CATEGORY_PHOTO_STYLE = "photo_style" // 风格分类

// 标签
const val TAG_PHOTO = "photo"
const val TAG_PRESET = "preset"
const val TAG_CAMERA = "camera"
const val TAG_STYLE = "style"
```

## 🎯 最佳实践

### 1. 权限检查

```kotlin
if (FlashNoteHelper.isServiceAvailable()) {
    // 使用闪记功能
} else {
    // 显示降级提示
    Toast.makeText(context, "ColorOS版本不支持此功能", Toast.LENGTH_SHORT).show()
}
```

### 2. 批量操作

```kotlin
CoroutineScope(Dispatchers.Main).launch {
    FlashNoteHelper.batchSavePresets(context, selectedPresets) { results ->
        val successCount = results.count { it.success }
        Toast.makeText(
            context,
            "成功保存 $successCount 个预设",
            Toast.LENGTH_SHORT
        ).show()
    }
}
```

### 3. 错误处理

```kotlin
FlashNoteHelper.quickSavePreset(
    context = context,
    preset = preset,
    onSuccess = {
        // 显示成功反馈
        Snackbar.make(view, "已保存到闪记", Snackbar.LENGTH_SHORT).show()
    },
    onError = { error ->
        // 显示错误提示
        Snackbar.make(view, "保存失败: $error", Snackbar.LENGTH_LONG)
            .setAction("重试") {
                // 重新尝试
            }
            .show()
    }
)
```

## 📱 设备兼容性

### 支持的ColorOS版本

| ColorOS版本 | API级别 | 支持程度 |
|------------|--------|---------|
| ColorOS 16 | 30+ | ✅ 完整功能 |
| ColorOS 15 | 29+ | ⚠️ 部分功能 |
| ColorOS 14 | 28+ | ⚠️ 基础功能 |
| ColorOS 13 | 27+ | ❌ 不支持 |

### 检查兼容性

```kotlin
val isAvailable = FlashNoteConstants.isFlashNoteAvailable()
val isFullFeature = FlashNoteConstants.isFullFeatureAvailable()

if (isAvailable) {
    val features = FlashNoteHelper.getSupportedFeatures()
    // 根据支持的功能显示/隐藏UI
}
```

## 🎨 样式定制

### 自定义按钮背景

在 `res/drawable/bg_flash_note_button.xml` 中修改：

```xml
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="rectangle">
    <gradient
        android:angle="135"
        android:startColor="#您的起始颜色"
        android:endColor="#您的结束颜色"
        android:type="linear" />
    <corners android:radius="24dp" />
</shape>
```

### 自定义按钮图标

```kotlin
flashNoteButton.setIconResource(R.drawable.your_custom_icon)
```

## 🔍 调试技巧

### 检查闪记应用是否安装

```kotlin
fun isFlashNoteInstalled(context: Context): Boolean {
    return try {
        context.packageManager.getPackageInfo("com.coloros.flashnote", 0)
        true
    } catch (e: PackageManager.NameNotFoundException) {
        false
    }
}
```

### 查看保存结果

```kotlin
val result = flashNoteService.quickSavePreset(preset)
Timber.d("Save result: success=${result.success}, noteId=${result.noteId}")
```

## 📚 参考资料

- [ColorOS 16 AI一键闪记](https://www.coloros.com/article/A00000099)
- [OPPO AI功能介绍](https://www.oppo.com/cn/discover/technology/oppo-ai)
- [ColorOS 16特性](https://www.coloros.com/version/coloros16)

## ⚠️ 注意事项

1. **权限要求**: 闪记应用权限
2. **设备要求**: ColorOS 14+ (API 28+)
3. **性能**: 批量操作建议使用后台线程
4. **数据**: 附件图片需要有效的URI

## 📄 许可证

本项目遵循OPPO开放平台相关协议。
