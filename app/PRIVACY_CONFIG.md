# OMaster 隐私安全配置文档

## 概述

本文档详细说明OMaster应用的隐私安全配置，涵盖ColorOS 16特定的权限要求和用户数据保护。

---

## 一、ColorOS 特定权限配置

### PERM-COL-001: 悬浮窗权限

#### ✅ 已实现配置

1. **权限申请**
   - ✅ 跳转到ColorOS专用设置页面
   - ✅ 权限用途说明

2. **悬浮窗规格**
   - ✅ 悬浮窗大小不超过屏幕1/4
   - ✅ 可自由拖动和关闭
   - ✅ 不遮挡状态栏和导航栏

#### 实现代码

```kotlin
// PermissionManager.kt
fun requestOverlayPermission(activity: Activity) {
    // 跳转到ColorOS悬浮窗设置页面
    val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
    activity.startActivity(intent)
}
```

---

### PERM-COL-002: 后台运行权限

#### ✅ 已实现配置

1. **后台保活**
   - ✅ 后台运行时不被强制杀死
   - ✅ 后台同步功能正常

2. **电池优化**
   - ✅ 支持忽略电池优化
   - ✅ 电池使用优化

#### 实现代码

```kotlin
// PermissionManager.kt
fun requestIgnoreBatteryOptimizations(activity: Activity) {
    if (!isIgnoringBatteryOptimizations(activity)) {
        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
        activity.startActivity(intent)
    }
}
```

---

### PERM-COL-003: 隐私权限提醒

#### ✅ 已实现配置

1. **隐私提醒**
   - ✅ 系统级隐私提醒支持
   - ✅ 隐私替身功能支持

2. **权限管理**
   - ✅ 存储权限隐私保护
   - ✅ 数据隔离

---

## 二、隐私政策配置 (DATA-PRV-001)

### ✅ 已实现功能

1. **首次启动隐私政策**
   - ✅ 首次启动显示完整隐私政策
   - ✅ 隐私政策符合中国个人信息保护法
   - ✅ 用户可以拒绝隐私政策

2. **隐私政策内容**
   - ✅ 明确说明收集哪些个人信息及用途
   - ✅ 用户拒绝后应用只能使用基础功能
   - ✅ 隐私政策可在设置中随时查看

### 实现代码

```kotlin
// PrivacyPolicyManager.kt
val isPrivacyPolicyAccepted: Flow<Boolean>

suspend fun acceptPrivacyPolicy()
suspend fun declinePrivacyPolicy()
```

---

## 三、数据收集最小化 (DATA-PRV-002)

### ✅ 已实现配置

1. **收集的数据**
   - ✅ 用户自定义的滤镜预设（匿名）
   - ✅ 应用使用统计数据（匿名）
   - ✅ 崩溃日志（匿名）

2. **不收集的数据**
   - ✅ 不收集设备唯一标识符（IMEI、MAC地址等）
   - ✅ 不收集用户的照片、视频等媒体文件

3. **实现方式**
   - ✅ 使用匿名设备ID
   - ✅ 数据收集可由用户控制

### 实现代码

```kotlin
// DataCollectionTracker.kt
fun trackAppOpen()
fun trackPresetCreated(presetName: String, presetType: String)
fun trackFeatureUsed(featureName: String, duration: Long? = null)
fun trackError(errorMessage: String, stackTrace: String? = null)

fun getAnonymousDeviceId(): String
```

---

## 四、用户数据控制权 (DATA-PRV-003)

### ✅ 已实现功能

1. **数据导出**
   - ✅ 用户可以导出所有自定义预设数据
   - ✅ 支持JSON格式导出
   - ✅ 支持分享导出文件

2. **数据删除**
   - ✅ 用户可以删除账户及所有相关数据
   - ✅ 删除后不可恢复

3. **数据收集开关**
   - ✅ 用户可以在设置中关闭匿名数据收集
   - ✅ 实时生效

4. **数据统计**
   - ✅ 显示预设数量
   - ✅ 显示数据占用空间

### 实现代码

```kotlin
// UserDataManager.kt
suspend fun exportUserData(presets: List<Preset>): Result<File>
suspend fun deleteAllUserData(): Result<Unit>
suspend fun getUserDataStats(): UserDataStats
```

---

## 五、隐私配置文件

### PrivacyPolicyContent.kt

```kotlin
object PrivacyPolicyContent {
    fun getPrivacyPolicyText(): String {
        // 返回完整的隐私政策文本
    }
}
```

### PrivacyPolicyManager.kt

```kotlin
@Singleton
class PrivacyPolicyManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    val isPrivacyPolicyAccepted: Flow<Boolean>
    val isDataCollectionEnabled: Flow<Boolean>
    
    suspend fun acceptPrivacyPolicy()
    suspend fun declinePrivacyPolicy()
    suspend fun setDataCollectionEnabled(enabled: Boolean)
}
```

### DataCollectionTracker.kt

```kotlin
@Singleton
class DataCollectionTracker @Inject constructor(
    @ApplicationContext private val context: Context,
    private val privacyPolicyManager: PrivacyPolicyManager
) {
    fun trackAppOpen()
    fun trackPresetCreated(presetName: String, presetType: String)
    fun trackFeatureUsed(featureName: String, duration: Long? = null)
    fun trackError(errorMessage: String, stackTrace: String? = null)
    
    suspend fun canCollectData(): Boolean
    fun getAnonymousDeviceId(): String
}
```

### UserDataManager.kt

```kotlin
@Singleton
class UserDataManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dataCollectionTracker: DataCollectionTracker,
    private val privacyPolicyManager: PrivacyPolicyManager
) {
    suspend fun exportUserData(presets: List<Preset>): Result<File>
    suspend fun shareExportedData(file: File): Intent?
    suspend fun deleteAllUserData(): Result<Unit>
    suspend fun getUserDataSize(): Long
    suspend fun getUserDataStats(): UserDataStats
}
```

---

## 六、隐私UI组件

### PrivacyPolicyScreen.kt

```kotlin
@Composable
fun PrivacyPolicyScreen(
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    // 隐私政策阅读页面
    // 用户必须阅读并同意后才能使用完整功能
}
```

### PrivacySettingsScreen.kt

```kotlin
@Composable
fun PrivacySettingsScreen(
    onBack: () -> Unit
) {
    // 隐私设置页面
    // - 数据分析开关
    // - 查看隐私政策
    // - 数据统计
    // - 导出数据
    // - 删除所有数据
}
```

---

## 七、测试用例覆盖

### ColorOS特定测试

| 用例ID | 测试内容 | 状态 |
|--------|----------|------|
| PERM-COL-001 | 悬浮窗权限 | ✅ 已实现 |
| PERM-COL-002 | 后台运行权限 | ✅ 已实现 |
| PERM-COL-003 | 隐私权限提醒 | ✅ 已实现 |

### 隐私数据测试

| 用例ID | 测试内容 | 状态 |
|--------|----------|------|
| DATA-PRV-001 | 隐私政策 | ✅ 已实现 |
| DATA-PRV-002 | 数据收集最小化 | ✅ 已实现 |
| DATA-PRV-003 | 用户数据控制权 | ✅ 已实现 |

---

## 八、隐私安全检查清单

### 隐私政策
- [x] 首次启动显示隐私政策
- [x] 隐私政策符合中国个人信息保护法
- [x] 用户可以拒绝隐私政策
- [x] 拒绝后功能受限
- [x] 隐私政策可随时查看

### 数据收集
- [x] 仅收集必要数据
- [x] 数据收集可关闭
- [x] 不收集个人标识符
- [x] 数据匿名处理

### 用户控制
- [x] 数据导出功能
- [x] 数据删除功能
- [x] 数据收集开关
- [x] 数据统计显示

### ColorOS兼容性
- [x] 悬浮窗权限专用引导
- [x] 后台运行优化
- [x] 隐私提醒支持

---

## 九、相关文件清单

| 文件 | 用途 | 状态 |
|------|------|------|
| `PrivacyPolicyManager.kt` | 隐私政策管理 | ✅ |
| `DataCollectionTracker.kt` | 数据收集追踪 | ✅ |
| `UserDataManager.kt` | 用户数据管理 | ✅ |
| `PrivacyPolicyScreen.kt` | 隐私政策UI | ✅ |
| `PrivacySettingsScreen.kt` | 隐私设置UI | ✅ |
| `PrivacyModule.kt` | 依赖注入模块 | ✅ |

---

## 十、使用说明

### 首次启动流程

```kotlin
@Composable
fun SplashScreen() {
    val isPrivacyAccepted by privacyPolicyManager.isPrivacyPolicyAccepted.collectAsState()
    
    if (!isPrivacyAccepted) {
        PrivacyPolicyScreen(
            onAccept = { /* 进入主界面 */ },
            onDecline = { /* 显示基础功能 */ }
        )
    } else {
        MainScreen()
    }
}
```

### 数据导出流程

```kotlin
scope.launch {
    val result = userDataManager.exportUserData(presets)
    result.onSuccess { file ->
        val shareIntent = userDataManager.shareExportedData(file)
        startActivity(shareIntent)
    }
}
```

### 数据删除流程

```kotlin
AlertDialog(
    onConfirm = {
        scope.launch {
            userDataManager.deleteAllUserData()
        }
    }
)
```

---

*文档版本: 1.2.1*
*最后更新: 2026-05-30*
