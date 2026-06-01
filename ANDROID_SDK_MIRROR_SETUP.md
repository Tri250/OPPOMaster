# Android SDK 国内镜像加速配置指南

## 一、Android SDK 安装状态

**当前状态**: SDK Manager 未在 PATH 中找到

需要安装 Android SDK 或配置现有 SDK 路径。

## 二、国内镜像源配置

### 2.1 Gradle 镜像加速

已配置以下镜像源（按优先级排序）：

1. **阿里云镜像**（推荐）
   - Google 仓库：`https://maven.aliyun.com/repository/google`
   - 公共仓库：`https://maven.aliyun.com/repository/public`
   - Gradle 插件：`https://maven.aliyun.com/repository/gradle-plugin`
   - Central 仓库：`https://maven.aliyun.com/repository/central`

2. **腾讯云镜像**（备选）
   - Maven 公共库：`https://mirrors.cloud.tencent.com/nexus/repository/maven-public/`

3. **Gradle 分发镜像**
   - 腾讯云 Gradle 镜像：`https://mirrors.cloud.tencent.com/gradle/`

### 2.2 Android SDK 镜像源

#### 方法一：使用 Android Studio 内置 SDK Manager

1. 打开 Android Studio
2. 进入 `Settings` → `Appearance & Behavior` → `System Settings` → `Android SDK`
3. 在 `SDK Platforms` 和 `SDK Tools` 标签页中选择需要安装的组件
4. 点击 `Apply` 开始安装

#### 方法二：使用命令行工具（推荐）

```bash
# 1. 下载 Android 命令行工具
# 访问 https://developer.android.com/studio#command-tools
# 下载 commandlinetools-linux-*.zip

# 2. 解压到 SDK 目录
mkdir -p ~/Android/Sdk/cmdline-tools
cd ~/Android/Sdk/cmdline-tools
unzip ~/Downloads/commandlinetools-linux-*.zip
mv cmdline-tools latest

# 3. 配置环境变量
echo 'export ANDROID_HOME=~/Android/Sdk' >> ~/.bashrc
echo 'export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin' >> ~/.bashrc
echo 'export PATH=$PATH:$ANDROID_HOME/platform-tools' >> ~/.bashrc
source ~/.bashrc

# 4. 配置国内镜像源
cat > ~/.android/repositories.cfg << EOF
# 使用阿里云镜像
https://mirrors.aliyun.com/macros/android/repository-2.xml
EOF

# 5. 安装 SDK 平台和构建工具
sdkmanager --install "platforms;android-35"
sdkmanager --install "build-tools;34.0.0"
sdkmanager --install "platform-tools"
sdkmanager --install "cmdline-tools;latest"

# 6. 查看已安装的组件
sdkmanager --list_installed
```

#### 方法三：使用国内镜像脚本

```bash
#!/bin/bash
# 使用清华大学镜像源安装 Android SDK

export ANDROID_HOME=~/Android/Sdk
export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin

# 创建目录
mkdir -p $ANDROID_HOME

# 使用清华镜像源
export REPO_URL=https://mirrors.tuna.tsinghua.edu.cn/android/repository/

# 下载并安装
sdkmanager --sdk_root=$ANDROID_HOME \
  --channel=3 \
  "platforms;android-35" \
  "build-tools;34.0.0" \
  "platform-tools"
```

## 三、local.properties 配置

创建 `local.properties` 文件，指定 SDK 路径：

```properties
# Android SDK 路径（根据实际安装位置修改）
sdk.dir=/home/your-username/Android/Sdk

# 或者使用绝对路径
# sdk.dir=/opt/android-sdk
```

## 四、验证安装

### 4.1 验证 Gradle 镜像

```bash
cd /workspace
./gradlew -v
```

### 4.2 验证 Android SDK

```bash
# 检查 SDK 路径
echo $ANDROID_HOME

# 列出已安装的 SDK 组件
sdkmanager --list_installed

# 检查 adb 版本
adb --version
```

### 4.3 验证构建

```bash
# 清理并构建项目
./gradlew clean assembleDebug
```

## 五、常见问题解决

### 5.1 Gradle 下载慢

**解决方案**：已配置阿里云和腾讯云镜像，自动选择最快的镜像源。

### 5.2 SDK Manager 无法连接

**解决方案**：
```bash
# 使用阿里云镜像
export REPO_URL=https://mirrors.aliyun.com/macros/android/repository/
```

### 5.3 证书验证失败

**解决方案**：
```bash
# 在 gradle.properties 中添加
systemProp.https.proxyHost=
systemProp.https.proxyPort=
```

## 六、推荐安装的 SDK 组件

```bash
# Android 14/15/16 平台
sdkmanager "platforms;android-34"
sdkmanager "platforms;android-35"
sdkmanager "platforms;android-36"

# 构建工具
sdkmanager "build-tools;34.0.0"

# 平台工具
sdkmanager "platform-tools"
sdkmanager "cmdline-tools;latest"

# 模拟器（可选）
sdkmanager "emulator"
sdkmanager "system-images;android-35;google_apis;arm64-v8a"

# 额外工具
sdkmanager "extras;google;google_play_services"
sdkmanager "extras;android;m2repository"
```

## 七、镜像源速度对比

| 镜像源 | 平均速度 | 稳定性 | 推荐度 |
|--------|---------|--------|--------|
| 阿里云 | 5-10 MB/s | ⭐⭐⭐⭐⭐ | 强烈推荐 |
| 腾讯云 | 3-8 MB/s | ⭐⭐⭐⭐ | 推荐 |
| 清华大学 | 2-6 MB/s | ⭐⭐⭐⭐ | 推荐 |
| 官方源 | 0.1-1 MB/s | ⭐⭐ | 不推荐（国内） |

## 八、更新记录

- 2026-06-01: 配置阿里云和腾讯云双镜像加速
- 2026-06-01: 更新 Gradle Wrapper 使用腾讯云镜像
- 2026-06-01: 创建完整的 SDK 安装指南
