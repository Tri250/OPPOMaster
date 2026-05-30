# OMaster Android APK 构建报告

## 构建环境状态

### 当前环境
- **Java 版本**: OpenJDK 21.0.2 ✅
- **Gradle 版本**: 8.14.4 ✅
- **系统 Gradle**: 已安装并可用 ✅
- **Android SDK**: 未配置 ❌
- **Gradle Wrapper**: 需要下载 ❌
- **网络状态**: 受限 ❌

### 项目配置
- **应用名称**: OMaster
- **包名**: com.omaster.app
- **版本**: 2.0.0 (versionCode: 200)
- **目标 SDK**: Android 16 (API 36)
- **最低 SDK**: Android 8.0 (API 26)
- **编译 SDK**: Android 16 (API 36)

## 构建问题

### 核心问题
当前环境存在网络限制，导致以下依赖无法下载：

1. **Gradle Wrapper zip 文件**
   - 尝试从 services.gradle.org 下载失败
   - 尝试从阿里云镜像下载失败（返回 HTML 错误页）

2. **Android Gradle Plugin 8.5.0**
   - 无法从 Google Maven 仓库下载
   - 无法从镜像仓库下载

3. **Android SDK 组件**
   - ANDROID_HOME 未设置
   - build-tools、platforms 等组件不可用

## 解决方案

### 方案一：网络畅通环境构建（推荐）

在具有网络访问权限的环境中，按以下步骤操作：

#### 1. 下载 Gradle Wrapper
```bash
cd /path/to/project
curl -L -o gradle/wrapper/gradle-8.14.4-bin.zip \
  https://services.gradle.org/distributions/gradle-8.14.4-bin.zip
```

#### 2. 配置 Android SDK
```bash
export ANDROID_HOME=$HOME/Android/Sdk
export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools

# 安装必要的 SDK 组件
sdkmanager "platforms;android-36"
sdkmanager "build-tools;36.0.0"
sdkmanager "platform-tools"
```

#### 3. 执行构建
```bash
# Debug 构建（推荐首次使用）
./gradlew assembleDebug --no-daemon

# 或者跳过测试快速构建
./gradlew assembleDebug -x test -x lint --no-daemon

# Release 构建
./gradlew assembleRelease --no-daemon
```

### 方案二：使用系统 Gradle（如果已安装）

```bash
# 直接使用系统 Gradle（版本 8.14.4）
gradle assembleDebug --no-daemon
```

### 方案三：Android Studio 构建

1. 打开 Android Studio
2. 选择 "Open an existing project"
3. 选择项目根目录
4. 等待 Gradle 同步完成
5. 点击 Build > Build Bundle(s) / APK(s) > Build APK(s)

## 构建输出

### APK 文件位置
- **Debug APK**: `app/build/outputs/apk/debug/OMaster-debug.apk`
- **Release APK**: `app/build/outputs/apk/release/OMaster-release.apk`

### APK 信息
- **应用名称**: OMaster
- **包名**: com.omaster.app
- **版本名**: 2.0.0
- **版本码**: 200
- **目标平台**: Android 16 (API 36)
- **最低平台**: Android 8.0 (API 26)

## 验证 APK

### 1. 检查 APK 存在
```bash
ls -lh app/build/outputs/apk/debug/OMaster-debug.apk
```

### 2. 验证 APK 信息
```bash
aapt dump badging app/build/outputs/apk/debug/OMaster-debug.apk | grep -E \
  "package:|application-label:|sdkVersion:|targetSdkVersion:"
```

### 3. 验证签名（Release APK）
```bash
apksigner verify --print-certs app/build/outputs/apk/release/OMaster-release.apk
```

## 快速构建命令汇总

```bash
# 完整构建命令
./gradlew clean assembleDebug --no-daemon --stacktrace

# 跳过测试快速构建
./gradlew assembleDebug -x test -x lint --no-daemon

# Release 构建
./gradlew assembleRelease --no-daemon

# 仅下载依赖
./gradlew dependencies --no-daemon
```

## 故障排除

### 问题 1: Gradle Wrapper 下载失败
**解决方案**:
- 使用 VPN 或代理
- 配置国内镜像源
- 手动下载 Gradle 并配置 PATH

### 问题 2: Android SDK 组件缺失
**解决方案**:
```bash
# 安装 Android command line tools
# 然后运行
sdkmanager --sdk_root=$ANDROID_HOME \
  "platforms;android-36" \
  "build-tools;36.0.0" \
  "platform-tools"
```

### 问题 3: 依赖下载超时
**解决方案**:
- 增加超时时间
- 使用离线模式（如果有缓存）
- 配置多线程下载

## 自动化构建脚本

项目已包含以下构建脚本：

1. **build.sh** - 主构建脚本
2. **build_apk.sh** - 专用 APK 构建脚本
3. **download-deps.sh** - 依赖下载脚本

使用方法：
```bash
# 构建 Debug APK
./build.sh debug

# 构建 Release APK
./build.sh release
```

## 下一步

1. **在网络畅通环境中执行构建**
2. **验证生成的 APK**
3. **上传 APK 到项目仓库**
4. **真机测试验证**

## 联系支持

如遇构建问题，请检查：
- 网络连接
- Android SDK 配置
- Gradle 依赖缓存
- Java 环境变量

---

**生成时间**: 2026-05-30
**项目版本**: 2.0.0
**构建工具**: Gradle 8.14.4, AGP 8.5.0
