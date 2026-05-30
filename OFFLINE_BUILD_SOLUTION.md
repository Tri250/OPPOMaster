# OMaster Android APK 离线构建方案

## 概述

本文档提供在网络受限环境中构建 OMaster Android APK 的完整解决方案。

## 当前环境状态

### 已就绪的环境
- ✅ Java 21.0.2 已安装
- ✅ Gradle 8.14.4 已安装（系统级）
- ✅ 项目结构完整
- ✅ 所有源代码已就绪
- ✅ AI 场景识别功能已完善

### 需要解决的限制
- ❌ Gradle Wrapper zip 文件下载受限（18MB/200MB 已下载）
- ❌ Android SDK 未配置
- ❌ Android Gradle Plugin 8.5.0 无法下载

## 解决方案

### 方案一：继续等待下载完成（推荐）

如果当前环境的网络连接稳定，可以继续等待 Gradle 下载完成：

```bash
# 检查下载进度
ls -lh /workspace/gradle/wrapper/gradle-8.14.4-bin.zip

# 如果下载完成，运行构建
./gradlew clean assembleDebug --no-daemon
```

### 方案二：在网络畅通环境中构建

#### 步骤 1: 准备构建环境

在具有稳定网络连接的环境中：

```bash
# 克隆或复制项目代码
git clone <repository-url>
cd OMaster

# 或者复制当前工作区的所有文件
rsync -avz user@source:/workspace/ /destination/workspace/
```

#### 步骤 2: 下载 Gradle Wrapper

```bash
# 下载 Gradle Wrapper（推荐从官方源）
curl -L -o gradle/wrapper/gradle-8.14.4-bin.zip \
  https://services.gradle.org/distributions/gradle-8.14.4-bin.zip

# 或者从阿里云镜像（国内更快）
curl -L -o gradle/wrapper/gradle-8.14.4-bin.zip \
  https://mirrors.aliyun.com/gradle/gradle-8.14.4-bin.zip
```

#### 步骤 3: 配置 Android SDK

```bash
# 设置环境变量
export ANDROID_HOME=$HOME/Android/Sdk
export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools

# 下载 Android command line tools（如果尚未安装）
mkdir -p $ANDROID_HOME/cmdline-tools
cd $ANDROID_HOME/cmdline-tools
curl -L -o cmdline-tools.zip \
  "https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip"
unzip -q cmdline-tools.zip
mv cmdline-tools latest
rm cmdline-tools.zip
cd -

# 接受许可证
yes | $ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager --licenses

# 安装必要的 SDK 组件
$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager \
  "platforms;android-36" \
  "build-tools;36.0.0" \
  "platform-tools"
```

#### 步骤 4: 执行构建

```bash
# Debug 构建（推荐首次使用）
./gradlew clean assembleDebug --no-daemon

# 快速构建（跳过测试）
./gradlew assembleDebug -x test -x lint --no-daemon

# Release 构建
./gradlew clean assembleRelease --no-daemon
```

#### 步骤 5: 验证 APK

```bash
# 检查 APK 文件
ls -lh app/build/outputs/apk/debug/OMaster-debug.apk

# 验证 APK 信息
aapt dump badging app/build/outputs/apk/debug/OMaster-debug.apk | grep -E \
  "package:|application-label:|sdkVersion:|targetSdkVersion:"
```

## 自动化构建脚本

项目已包含以下自动化脚本：

### 1. prepare_build.sh
环境检查脚本，验证构建前所有依赖是否就绪。

```bash
./prepare_build.sh
```

### 2. quick_build.sh
快速构建脚本，下载所有必要的依赖并执行构建。

```bash
./quick_build.sh debug   # Debug 构建
./quick_build.sh release # Release 构建
```

### 3. build.sh
主构建脚本，包含完整的构建流程。

```bash
./build.sh debug   # Debug 构建
./build.sh release # Release 构建
```

## APK 输出信息

### 文件位置
- **Debug APK**: `app/build/outputs/apk/debug/OMaster-debug.apk`
- **Release APK**: `app/build/outputs/apk/release/OMaster-release.apk`

### APK 规格
- **应用名称**: OMaster
- **包名**: com.omaster.app
- **版本名**: 2.0.0
- **版本码**: 200
- **目标 SDK**: Android 16 (API 36)
- **最低 SDK**: Android 8.0 (API 26)
- **编译 SDK**: Android 16 (API 36)

### 主要功能
- AI 场景识别（ML Kit 本地识别 + DeepSeek API 增强）
- ColorOS 16 设计风格
- Material3 UI 组件
- 相机水印功能
- OPPO 哈苏预设支持

## 依赖清单

### 核心依赖
- Android Gradle Plugin: 8.5.0
- Kotlin: 2.0.0
- Compose BOM: 2024.06.00
- Hilt: 2.51.1
- CameraX: 1.3.4
- ML Kit Image Labeling: 17.0.2

### 完整依赖列表
见 `app/build.gradle.kts` 中的 `dependencies` 块。

## 故障排除

### 问题 1: Gradle Wrapper 下载超时

**解决方案**:
- 增加 curl 超时时间: `curl --max-time 3600 ...`
- 使用后台下载: `nohup curl -L -o ... &`
- 配置代理: `export HTTPS_PROXY=http://proxy:port`

### 问题 2: Android SDK 组件下载失败

**解决方案**:
- 使用国内镜像:
  ```bash
  export ANDROID_MIRROR=https://mirrors.aliyun.com/android-sdk/
  ```

- 手动下载并安装:
  ```bash
  # 下载 platform-tools
  curl -L -o platform-tools.zip \
    "https://dl.google.com/android/repository/platform-tools_r35.0.0-linux.zip"
  unzip -q platform-tools.zip -d $ANDROID_HOME/
  ```

### 问题 3: 依赖解析失败

**解决方案**:
- 清理 Gradle 缓存: `./gradlew clean --refresh-dependencies`
- 禁用离线模式: 移除 `--offline` 标志
- 清理本地缓存: `rm -rf ~/.gradle/caches/`

## 部署说明

### 发布到应用商店
1. 构建 Release APK（已签名）
2. 使用 `zipalign` 对齐 APK
3. 使用 `apksigner` 签名 APK
4. 上传到 Google Play Console 或其他应用商店

### 真机测试
1. 启用设备开发者选项
2. 通过 ADB 安装: `adb install app/build/outputs/apk/debug/OMaster-debug.apk`
3. 测试所有功能模块

## 联系与支持

如遇构建问题，请参考：
- 项目 Wiki 文档
- BUILD_INSTRUCTIONS.md
- APK_BUILD_REPORT.md

---

**最后更新**: 2026-05-30
**项目版本**: 2.0.0
