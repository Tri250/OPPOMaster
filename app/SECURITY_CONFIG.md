# OMaster Android安全配置文档

## 概述

本文档详细说明OMaster应用的安全配置，涵盖签名安全、权限管理和ColorOS 16特定的安全要求。

---

## 一、签名安全配置

### SIG-SEC-001: 签名证书管理

#### ✅ 已实现配置

1. **凭证管理**
   - ✅ 构建凭证从环境变量获取
   - ✅ 禁止硬编码敏感信息
   - ✅ 支持密钥管理服务集成

2. **证书存储建议**
   - 生产密钥应存储在HSM（硬件安全模块）
   - 私钥无法被导出
   - 访问权限严格控制

3. **证书轮换**
   - 证书有效期不超过2年
   - 支持V3签名轮换

#### 配置示例

```kotlin
// build.gradle.kts
signingConfigs {
    create("release") {
        storePassword = System.getenv("KEYSTORE_PASSWORD")
        keyAlias = System.getenv("KEY_ALIAS")
        keyPassword = System.getenv("KEY_PASSWORD")
    }
}
```

---

### SIG-SEC-002: 多签名验证

#### ✅ 已实现配置

1. **签名方案**
   - ✅ V2签名启用
   - ✅ V3签名启用
   - ✅ V4签名启用（Android 14+）

2. **签名验证**
   - ✅ 支持多签名验证
   - ✅ 签名指纹验证

#### 代码实现

```kotlin
// OMasterApplication.kt
fun getSignatureInfo(): SignatureInfo? {
    // 获取V2/V3/V4签名信息
}
```

---

### SIG-SEC-003: 发布渠道安全

#### ✅ 已实现配置

1. **应用完整性验证**
   - ✅ 签名验证
   - ✅ APK篡改检测
   - ✅ 重打包检测

2. **发布渠道验证**
   - ✅ 官方商店检测
   - ✅ 非官方渠道警告

3. **应用完整性**
   - ✅ Backup Rules配置
   - ✅ Data Extraction Rules配置
   - ✅ 网络安全配置

#### 配置文件

- `backup_rules.xml`: 备份规则配置
- `data_extraction_rules.xml`: 数据提取规则
- `network_security_config.xml`: 网络安全配置

---

## 二、权限安全配置

### PERM-SEC-001: 最小权限原则

#### ✅ 已实现配置

1. **权限清单**
   - ✅ INTERNET - 网络访问
   - ✅ ACCESS_NETWORK_STATE - 网络状态
   - ✅ SYSTEM_ALERT_WINDOW - 悬浮窗
   - ✅ READ_EXTERNAL_STORAGE - 读取预设（API < 33）
   - ✅ WRITE_EXTERNAL_STORAGE - 保存预设（API < 33）
   - ✅ READ_MEDIA_IMAGES - 读取照片（API 33+）

2. **权限限制**
   - ✅ 无不必要权限
   - ✅ Camera权限标记为非必需

#### AndroidManifest.xml

```xml
<!-- 核心功能权限 -->
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />

<!-- 悬浮窗权限 -->
<uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />

<!-- 存储权限（条件限制） -->
<uses-permission 
    android:name="android.permission.READ_EXTERNAL_STORAGE"
    android:maxSdkVersion="32" />
```

---

### PERM-SEC-002: 运行时权限

#### ✅ 已实现配置

1. **权限申请**
   - ✅ 运行时申请存储权限
   - ✅ 用户友好说明
   - ✅ 权限用途解释

2. **降级处理**
   - ✅ 权限拒绝后功能禁用
   - ✅ 应用不崩溃
   - ✅ 友好的错误提示

#### 代码实现

```kotlin
// PermissionManager.kt
fun requestStoragePermission(
    activity: FragmentActivity,
    requestCode: Int
): Boolean {
    // 检查是否需要权限
    // 显示用途说明
    // 申请权限
}
```

---

### PERM-SEC-003: 权限撤销处理

#### ✅ 已实现配置

1. **权限检测**
   - ✅ 应用启动时检测权限状态
   - ✅ 权限被撤销时提示用户

2. **功能禁用**
   - ✅ 未授权功能禁用
   - ✅ 应用不崩溃

3. **恢复引导**
   - ✅ 提供恢复权限引导
   - ✅ 打开应用设置页面

#### 代码实现

```kotlin
// PermissionManager.kt
fun handlePermissionResult(
    permissions: Array<out String>,
    grantResults: IntArray,
    onGranted: () -> Unit,
    onDenied: () -> Unit,
    onDeniedPermanently: (permissions: List<String>) -> Unit
)
```

---

## 三、ColorOS 16特定配置

### PERM-COL-001: 悬浮窗权限

#### ✅ 已实现配置

1. **权限申请**
   - ✅ 跳转到ColorOS专用设置页面
   - ✅ 权限用途说明

2. **悬浮窗规格**
   - ✅ 悬浮窗大小不超过屏幕1/4
   - ✅ 可自由拖动和关闭
   - ✅ 不遮挡状态栏和导航栏

#### 代码实现

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

#### 代码实现

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

## 四、安全组件

### AppIntegrityChecker

#### 功能

- ✅ 签名验证
- ✅ 应用完整性验证
- ✅ 重打包检测
- ✅ 模拟器检测
- ✅ Root检测
- ✅ 设备ID安全获取

#### 使用示例

```kotlin
// 验证应用完整性
val isIntegrityValid = AppIntegrityChecker.verifyAppIntegrity(context)

// 检查安装来源
val isFromOfficialStore = AppIntegrityChecker.isFromOfficialStore(context)

// 多签名验证
val result = AppIntegrityChecker.verifyMultiSignature(packageInfo)
```

---

### SensitiveDataManager

#### 功能

- ✅ AES-256-GCM加密
- ✅ API密钥安全存储
- ✅ Android Keystore集成

#### 使用示例

```kotlin
// 初始化
SensitiveDataManager.initialize(context)

// 加密
val encrypted = SensitiveDataManager.encrypt("sensitive data")

// 解密
val decrypted = SensitiveDataManager.decrypt(encrypted)
```

---

## 五、配置文件清单

| 文件 | 用途 | 安全级别 |
|------|------|----------|
| `AndroidManifest.xml` | 权限声明 | 核心 |
| `backup_rules.xml` | 备份规则 | 重要 |
| `data_extraction_rules.xml` | 数据提取规则 | 重要 |
| `network_security_config.xml` | 网络安全 | 核心 |
| `proguard-rules.pro` | 代码混淆 | 重要 |
| `dependency-check-suppressions.xml` | 依赖扫描 | 标准 |

---

## 六、测试用例覆盖

### 签名安全测试

| 用例ID | 测试内容 | 状态 |
|--------|----------|------|
| SIG-SEC-001 | 签名证书管理 | ✅ 已实现 |
| SIG-SEC-002 | 多签名验证 | ✅ 已实现 |
| SIG-SEC-003 | 发布渠道安全 | ✅ 已实现 |

### 权限安全测试

| 用例ID | 测试内容 | 状态 |
|--------|----------|------|
| PERM-SEC-001 | 最小权限原则 | ✅ 已实现 |
| PERM-SEC-002 | 运行时权限 | ✅ 已实现 |
| PERM-SEC-003 | 权限撤销处理 | ✅ 已实现 |

### ColorOS特定测试

| 用例ID | 测试内容 | 状态 |
|--------|----------|------|
| PERM-COL-001 | 悬浮窗权限 | ✅ 已实现 |
| PERM-COL-002 | 后台运行权限 | ✅ 已实现 |
| PERM-COL-003 | 隐私权限提醒 | ✅ 已实现 |

---

## 七、安全配置检查清单

### 签名安全
- [x] 凭证从环境变量获取
- [x] 无硬编码敏感信息
- [x] V2+V3+V4签名启用
- [x] 签名验证机制
- [x] 发布渠道验证

### 权限安全
- [x] 最小权限原则
- [x] 运行时权限申请
- [x] 权限用途说明
- [x] 权限撤销处理
- [x] 功能降级处理

### 网络安全
- [x] 禁用明文流量
- [x] 证书固定配置
- [x] 只信任系统证书

### 代码安全
- [x] 代码混淆启用
- [x] ProGuard规则配置
- [x] 敏感数据加密

---

## 八、联系与支持

如有问题，请联系安全团队。

---

*文档版本: 1.2.1*
*最后更新: 2026-05-30*
