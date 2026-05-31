# OMaster APK 构建指南

## 项目信息
- **应用**: OMaster - 哈苏大师预设库
- **版本**: 1.2.1 (versionCode: 121)
- **编译SDK**: 35 (Android 16)
- **最低SDK**: 26 (Android 8.0)
- **目标SDK**: 35 (Android 16)

## 前置要求
- Java 17 或更高版本（已安装Java 21）
- Android SDK 35
- 网络连接（用于下载依赖）

## 已完成配置
✅ 已生成Debug签名密钥库: `app/debug.keystore`
✅ 已配置签名方案: V2和V3签名
✅ 已优化Gradle配置: 移除代理，增加内存到4GB
✅ 已简化构建配置: 禁用混淆，简化依赖管理

## 构建步骤

### 1. 清理旧构建
```bash
cd /workspace
./gradlew clean
```

### 2. 构建Debug APK
```bash
cd /workspace
./gradlew assembleDebug
```

### 3. 输出位置
构建成功后，APK将在以下位置:
```
/workspace/app/build/outputs/apk/debug/app-debug.apk
```

### 4. 安装到设备（可选）
```bash
cd /workspace
./gradlew installDebug
```

## 构建Release APK

### 生成Release签名密钥（如果需要）
```bash
cd /workspace/app
keytool -genkey -v -keystore release.keystore -storepass changeme -alias omaster -keypass changeme -keyalg RSA -keysize 2048 -validity 10000
```

### 构建Release APK
```bash
cd /workspace
./gradlew assembleRelease
```

### 输出位置
```
/workspace/app/build/outputs/apk/release/app-release.apk
```

## Android 16兼容性说明

### 已验证配置
- ✅ `targetSdkVersion = 35`
- ✅ `compileSdkVersion = 35`
- ✅ Material Design 3 最新版本
- ✅ AndroidX 最新稳定库
- ✅ Hilt 2.51.1 依赖注入
- ✅ DataStore 数据持久化
- ✅ Coil 2.7.0 图片加载

### 权限配置
- `INTERNET` - 网络访问
- `ACCESS_NETWORK_STATE` - 网络状态
- `SYSTEM_ALERT_WINDOW` - 悬浮窗
- `READ_MEDIA_IMAGES` - 媒体读取

## 项目核心功能

### 预设库
- 6个哈苏认证预设
- 每个预设含样张展示
- 完整相机参数
- HNCS认证标识

### UI界面
- 首页 - 预设列表和筛选
- 详情页 - 预设参数展示
- 设置页 - 主题和功能开关
- 场景检测 - AI智能识别
- AI微调 - 参数自定义
- 相机配置 - 设备设置
- 用户资料 - 使用统计

## 故障排除

### Gradle下载问题
如果遇到Gradle下载超时:
```bash
cd /workspace
./gradlew --offline --no-daemon assembleDebug
```

### 内存不足错误
已配置足够内存: `org.gradle.jvmargs=-Xmx4096m`

### 依赖下载问题
确保网络连接稳定，或配置国内镜像源。

## 仓库信息

- **GitHub**: https://github.com/Tri250/OPPOMaster
- **分支**: main
- **最新提交**: 构建配置优化

## 快速开始（推荐）

### 单步构建（Debug）
```bash
cd /workspace/app && [ ! -f debug.keystore ] && keytool -genkey -v -keystore debug.keystore -storepass android -alias androiddebugkey -keypass android -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=Android Debug,O=Android,C=US" || true
cd /workspace
./gradlew clean assembleDebug
```

### 检查APK是否生成
```bash
ls -lh /workspace/app/build/outputs/apk/debug/
```

---

**提示**: 首次构建需要下载依赖，可能需要较长时间。
