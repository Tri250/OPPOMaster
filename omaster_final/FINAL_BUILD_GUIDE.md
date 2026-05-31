# OMaster - 完整 APK 构建指南

## 📋 项目概述
- **应用名称**: OMaster (小O帮帮)
- **包名**: com.omaster.app
- **版本**: 1.2.1 (versionCode: 121)
- **目标 SDK**: 35 (Android 15)
- **最低 SDK**: 26 (Android 8.0)
- **架构**: Kotlin + Jetpack Compose + Hilt

---

## 🛠️ 在您的电脑上构建 APK

### 前置要求

#### 1. 安装 JDK 17 或更高版本
```bash
# 检查 Java 版本
java -version

# 输出应该类似：
# openjdk version "17.x.x" 或更高
```

如果没有安装，请从以下地址下载：
- **Adoptium (推荐)**: https://adoptium.net/temurin/releases/?version=17
- **Oracle JDK**: https://www.oracle.com/java/technologies/downloads/

#### 2. 安装 Android Studio
- **下载地址**: https://developer.android.com/studio
- **推荐版本**: Android Studio Hedgehog (2023.1.1) 或更新版本

#### 3. 安装 Android SDK
Android Studio 首次启动时会引导您安装 SDK，确保安装以下组件：
- Android SDK Platform 34 或 35
- Android SDK Build-Tools (最新版)
- Android SDK Platform-Tools
- Android Emulator (可选，用于测试)

---

## 🚀 构建步骤

### 方法一：使用 Android Studio (推荐)

1. **解压项目文件**
   - 找到您下载的 `omaster_final_package.zip`
   - 解压到您喜欢的目录，例如 `~/Projects/OMaster`

2. **在 Android Studio 中打开项目**
   - 启动 Android Studio
   - 选择 **Open an Existing Project**
   - 导航到解压后的项目目录，选择文件夹并打开
   - 等待 **Gradle Sync** 完成（底部状态栏会显示进度）
   - 首次同步可能需要 5-15 分钟，需要下载依赖

3. **构建 Debug APK**
   - 菜单栏选择 **Build** → **Build Bundle(s) / APK(s)** → **Build APK(s)**
   - 或者使用快捷键：**Ctrl+F9** (Windows/Linux) 或 **Cmd+F9** (Mac)
   - 等待构建完成

4. **获取 APK**
   - 构建成功后，会弹出通知，点击 **locate**
   - APK 位置通常在：`app/build/outputs/apk/debug/app-debug.apk`
   - 将此 APK 传输到您的 Android 设备即可安装

5. **（可选）构建 Release APK**
   - 确保您有签名密钥（项目中有一个示例配置）
   - 在 `app/build.gradle.kts` 中配置您的签名信息
   - 选择 **Build** → **Generate Signed Bundle / APK**

---

### 方法二：使用命令行

1. **解压项目文件**
```bash
# 解压项目
unzip omaster_final_package.zip -d OMaster
cd OMaster
```

2. **配置环境变量**
```bash
# Linux / macOS
export ANDROID_HOME=~/Android/Sdk
export PATH=$PATH:$ANDROID_HOME/platform-tools:$ANDROID_HOME/cmdline-tools/latest/bin

# Windows (PowerShell)
set ANDROID_HOME=C:\Users\YourName\AppData\Local\Android\Sdk
set PATH=%PATH%;%ANDROID_HOME%\platform-tools
```

3. **执行构建**
```bash
# 确保gradlew有执行权限 (Linux/macOS)
chmod +x gradlew

# 构建 Debug APK
./gradlew assembleDebug

# 构建完成后，APK 在：
# app/build/outputs/apk/debug/app-debug.apk
```

---

## 📱 安装和测试

### 在 Android 设备上安装
1. 在设备上启用 **"未知来源"** 安装
   - **Android 8-11**: 设置 → 安全 → 未知来源
   - **Android 12+**: 设置 → 安全 → 更多安全设置 → 安装未知应用
2. 将 `app-debug.apk` 传输到您的设备
3. 点击 APK 文件进行安装
4. 打开应用开始使用！

### 功能测试清单
- [ ] 查看预设列表
- [ ] 搜索和筛选预设
- [ ] 查看预设详情
- [ ] 收藏功能
- [ ] 主题切换 (浅色/深色)
- [ ] AI 场景检测
- [ ] 水印编辑器

---

## 🔧 常见问题解决

### 问题 1：Gradle 同步失败
**解决方案**：
1. **File** → **Invalidate Caches** → **Invalidate and Restart**
2. 删除项目根目录下的 `.gradle` 文件夹和 `build` 文件夹
3. 重新打开项目

### 问题 2：SDK 找不到
**解决方案**：
1. **File** → **Settings** → **Appearance & Behavior** → **System Settings** → **Android SDK**
2. 检查 SDK 路径是否正确
3. 点击 **Edit** 安装缺失的 SDK 组件

### 问题 3：依赖下载失败
**解决方案**：
1. 检查网络连接
2. 如果需要代理，在 `gradle.properties` 中添加：
   ```properties
   systemProp.http.proxyHost=127.0.0.1
   systemProp.http.proxyPort=7890
   systemProp.https.proxyHost=127.0.0.1
   systemProp.https.proxyPort=7890
   ```
3. 或者使用国内镜像源

### 问题 4：内存不足
**解决方案**：
在 `gradle.properties` 中增加 JVM 内存：
```properties
org.gradle.jvmargs=-Xmx4096m -Dfile.encoding=UTF-8
```

---

## 📦 项目文件说明

### 核心文件
| 文件/文件夹 | 说明 |
|--------------|------|
| `app/src/main/java` | 应用源代码 (Kotlin) |
| `app/src/main/res` | 资源文件 (布局、图片、字符串等) |
| `app/build.gradle.kts` | app 模块的 Gradle 配置 |
| `build.gradle.kts` | 项目级 Gradle 配置 |
| `settings.gradle.kts` | 项目设置 |
| `gradle.properties` | Gradle 属性配置 |
| `local.properties.template` | 本地 SDK 路径配置模板 |

### 主要功能模块
- 📸 **相机配置**: Camera2 接口实现
- 🎨 **预设系统**: 哈苏色彩预设库
- 🤖 **AI 场景检测**: 图像识别与参数推荐
- 💧 **ColorOS 流体云**: 系统级侧边栏集成
- 🪟 **悬浮窗**: 全局参数显示
- 📝 **水印编辑器**: 专业水印功能
- 🔐 **安全存储**: 加密数据持久化

---

## 🎉 构建成功后

恭喜您！如果一切顺利，您现在已经拥有了：
1. 一个完整的 OMaster 应用 APK
2. 可以在真机或模拟器上安装和测试
3. 可以进一步修改和开发

---

## 📞 需要帮助？

如果您遇到问题：
1. 查看 Android Studio 的 **Build** 窗口中的错误信息
2. 查看 **Logcat** 日志
3. 搜索相关错误信息
4. 参考项目中的其他文档（如 COMPLETE_BUILD_GUIDE.md）

---

**祝您构建顺利！🚀**
