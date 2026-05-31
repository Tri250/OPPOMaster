# Android 16 适配指南

本文档说明 OMaster 应用如何适配 Android 16 的各项变更。

## 1. 参数部署架构（重要声明）

### 无法直接写入系统相机

由于 Android 安全限制，普通应用无法直接修改系统相机设置。我们采用插件化部署架构：

- **ManualGuideDeployer**（默认）：提供清晰的分步操作指南
- **ClipboardDeployer**：将参数复制到剪贴板供用户参考
- **ShizukuDeployer**（可选）：需要 Shizuku 权限的高级部署方式

### 相关文件

- `app/src/main/java/com/omaster/app/deploy/ParamDeployer.kt`
- `app/src/main/java/com/omaster/app/deploy/ParamDeployerManager.kt`

## 2. Schema 版本控制和数据验证

### 防止数据腐化

所有参数都包含 `schemaVersion` 字段，确保数据兼容性：

```kotlin
object SchemaVersions {
    const val CURRENT_SCHEMA = 1
    const val MIN_SUPPORTED_SCHEMA = 1
    const val MAX_SUPPORTED_SCHEMA = 1
}
```

### 验证和自动修复

- `validateFull()`：完整验证，包含 Schema 检查
- `sanitize()`：自动修正范围溢出的参数

### 相关文件

- `app/src/main/java/com/omaster/app/model/CameraParams.kt`

## 3. Android 16 变更适配

### 3.1 Photo Picker 强制使用

**现状**：已在 `SceneDetectionScreen` 中实现 `PickVisualMedia`，无需 `READ_MEDIA_IMAGES` 权限。

**代码位置**：`app/src/main/java/com/omaster/app/ui/screens/SceneDetectionScreen.kt`

### 3.2 Edge-to-Edge 显示

**实现方式**：
- 使用 `enableEdgeToEdge()`（Activity 级别）
- 使用 `WindowInsetsCompat` 处理 padding
- 避免 UI 被系统栏遮挡

### 3.3 预测性返回手势

**实现方式**：
- 在 Fragment 中注册 `OnBackInvokedCallback`
- 自定义返回动画时配合系统预测性返回

### 3.4 动态广播接收器限制

**实现方式**：
- 避免使用 `BOOT_COMPLETED` 自启
- 改用 WorkManager 进行延迟初始化

### 3.5 更严格的 Intent 解析

**实现方式**：
- 使用显式 Intent
- 使用 `packageManager.resolveActivity()` 检查
- 提供降级方案

## 4. 权限管理

### 权限说明

| 权限 | 用途 | 是否必需 |
|------|------|----------|
| CAMERA | 应用内相机预览 | 否 |
| READ_MEDIA_IMAGES | 已被 Photo Picker 替代 | 否 |
| SYSTEM_ALERT_WINDOW | 悬浮窗显示 | 否 |

### 相关文件

- `app/src/main/java/com/omaster/app/ui/screens/ProSettingsScreen.kt`

## 5. 国产 ROM 适配

包含对 OPPO、vivo、小米、华为、魅族的权限引导：

```kotlin
// 跳转到电池优化忽略页面
RomUtils.openBatteryOptimizationSettings(context)

// 跳转到悬浮窗权限页面
RomUtils.openOverlayPermissionSettings(context)

// 跳转到自启动管理页面
RomUtils.openAutostartSettings(context)
```

**相关文件**：`app/src/main/java/com/omaster/app/utils/RomUtils.kt`

## 6. 架构文档

详细的架构说明请查看：[DEPLOY_ARCH.md](./DEPLOY_ARCH.md)

## 总结

✅ **已完成的 Android 16 适配**：

1. 插件化参数部署架构
2. Schema 版本控制和数据验证
3. Photo Picker 使用
4. 国产 ROM 权限引导
5. 生物识别/应用锁
6. 悬浮窗增强（左右滑动切换预设）

✅ **已存在的良好实现**：

- Edge-to-Edge 显示
- 预测性返回手势处理
- WorkManager 使用
