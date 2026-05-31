# 相机参数部署架构 (DEPLOY_ARCH)

## 概述

本文档描述了 OPPO Master 应用中相机参数到系统相机的部署架构和设计原则。

## 重要声明

**此应用不承诺能够直接写入系统相机参数**

由于 Android 系统的安全限制和各厂商定制 ROM 的差异，普通应用无法直接修改系统相机的内部状态。所有的"参数部署"功能都是：

1. **用户辅助操作** - 提供清晰的步骤引导用户手动设置
2. **可选特权模式** - 仅在用户明确授权时尝试使用高级方式

## 设计原则

### 1. 插件化架构 (ParamDeployer)

使用接口设计，支持多种部署方式：

```kotlin
interface ParamDeployer {
    val name: String
    val isAvailable: Boolean
    val priority: Int
    suspend fun deploy(params: CameraParams): DeployResult
}
```

### 2. 分层部署策略

应用采用分层的部署策略，按优先级顺序尝试：

| 优先级 | 部署器 | 方式 | 权限要求 |
|--------|--------|------|----------|
| 1000 | SystemDeployer | 系统API写入（如果可用） | 系统签名/ROOT |
| 500 | ShizukuDeployer | Shizuku 提权后操作 | Shizuku授权 |
| 50 | ClipboardDeployer | 复制参数到剪贴板 | 无需 |
| 0 | ManualGuideDeployer | 分步操作指引 | 无需 |

### 3. 结果透明化

所有部署操作都有明确的结果状态：

```kotlin
sealed interface DeployResult {
    data class Success(val message: String) : DeployResult
    data class GuideUser(val steps: List<GuideStep>) : DeployResult
    data class Failure(val reason: String) : DeployResult
}
```

## 内置部署器说明

### ManualGuideDeployer (默认)

提供用户友好的分步操作指引：

1. 打开相机应用
2. 进入哈苏大师模式
3. 设置 ISO
4. 设置快门速度
5. 设置白平衡
6. 设置色彩风格
7. 启用哈苏色彩

### ClipboardDeployer

将完整参数格式化为可读文本并复制到剪贴板，用户可参考设置。

## Android 限制说明

### 1. Settings.Global 限制

从 Android 6.0 (API 23) 开始，普通应用无法写入 `Settings.Global`，即使有 WRITE_SETTINGS 权限也不行。

### 2. 隐藏API限制

从 Android 9 (API 28) 开始，系统严格限制对隐藏 API 的访问。反射调用相机服务内部方法会失败。

### 3. Android 16+ 限制

Android 16 将进一步限制应用对系统资源的访问，包括：
- 更严格的 Intent 解析规则
- Photo Picker 强制替换旧的文件选择方式
- 动态注册广播接收器限制
- Edge-to-edge 强制要求

## 参数 Schema 版本控制

为防止数据腐化，所有参数都带有版本信息：

```kotlin
data class CameraParams(
    // ... 其他字段
    val version: String = "3.0",
    val schemaVersion: Int = 1,
    val lastModified: Long = System.currentTimeMillis()
)
```

### Schema 变更历史

| 版本 | 变更 | 兼容性 |
|------|------|--------|
| 1 | 初始版本 | N/A |
| 2 | 新增色调映射参数 | 向后兼容 |
| 3 | 哈苏色彩科学 3.0 | 向后兼容 |

### 验证与迁移

导入外部参数时执行：

1. Schema 版本检查
2. 范围验证 (ISO 32-102400, 饱和度 0-100 等)
3. 自动迁移（如果需要）
4. 降级处理（如果版本太新）

## 权限声明

此应用需要以下权限，但**不会用于直接写入系统相机**：

- `CAMERA` - 用于在应用内预览相机效果
- `WRITE_EXTERNAL_STORAGE` - 用于保存预设和导出
- `SYSTEM_ALERT_WINDOW` - 用于悬浮窗显示参数

## 推荐做法

### 对于用户

1. 使用内置的手动引导方式最安全可靠
2. 如果需要高级功能，考虑使用 Shizuku 或 Root（风险自负）
3. 始终保持应用更新以获得最佳兼容性

### 对于开发者

1. 扩展 ParamDeployer 接口添加新部署方式
2. 保持向后兼容性
3. 详细记录权限需求和风险
4. 遵循 Material Design 和 ColorOS 设计规范

## 相关文档

- [Android 开发者文档 - 相机](https://developer.android.com/training/camera)
- [Android 16 行为变更](https://developer.android.com/about/versions/16/behavior-changes)
- [Shizuku 文档](https://shizuku.rikka.app/)

## 联系方式

如有问题或建议，请访问 GitHub Issues。
