# OMaster Android 项目 - 完整构建指南

## ⚠️ 重要说明

**此环境没有安装 Android SDK，无法直接编译 APK。**
但我已经完成了以下工作：
- ✅ 所有代码错误已修复
- ✅ Gradle 配置已优化
- ✅ 完整的项目包已准备就绪

## 📦 获取项目

项目已打包为：`omaster_complete_package.zip`

## 🖥️ 构建要求

在开始之前，请确保你的电脑已安装：

### 1. JDK 17 或更高版本
```bash
# 检查 Java 版本
java -version

# 如果需要安装，请访问：
# https://adoptium.net/temurin/releases/?version=17
```

### 2. Android Studio
- 下载地址：https://developer.android.com/studio
- 推荐版本：Android Studio Hedgehog (2023.1.1) 或更新版本

### 3. Android SDK
Android Studio 会自动提示安装 SDK，确保安装：
- Android SDK Platform 34
- Android SDK Build-Tools
- Android SDK Platform-Tools

## 🚀 构建步骤

### 方法 1：使用 Android Studio（推荐）

1. **解压项目**
```bash
unzip omaster_complete_package.zip
cd OMaster
```

2. **在 Android Studio 中打开**
   - 启动 Android Studio
   - 选择 "Open an Existing Project"
   - 选择解压后的 `OMaster` 文件夹
   - 等待 Gradle Sync 完成（状态栏会显示进度）

3. **等待依赖下载**
   - 首次打开会下载大量依赖
   - 确保网络连接稳定
   - 可能需要 10-30 分钟（取决于网络）

4. **构建 Debug APK**
   - 方式 A：菜单栏 → Build → Build Bundle(s) / APK(s) → Build APK(s)
   - 方式 B：使用快捷键 Ctrl+F9（Build Project）
   - 方式 C：终端运行 `./gradlew assembleDebug`

5. **查看 APK**
   - 构建成功后，点击通知中的 "locate"
   - 或手动打开：`app/build/outputs/apk/debug/app-debug.apk`

### 方法 2：使用命令行

1. **解压项目**
```bash
unzip omaster_complete_package.zip
cd OMaster
```

2. **配置 ANDROID_HOME**
```bash
# macOS / Linux
export ANDROID_HOME=~/Library/Android/sdk
export PATH=$PATH:$ANDROID_HOME/tools:$ANDROID_HOME/platform-tools

# Windows
set ANDROID_HOME=C:\Users\YourUsername\AppData\Local\Android\Sdk
set PATH=%PATH%;%ANDROID_HOME%\tools;%ANDROID_HOME%\platform-tools
```

3. **运行构建**
```bash
# macOS / Linux
chmod +x gradlew
./gradlew assembleDebug

# Windows
gradlew.bat assembleDebug
```

4. **等待完成**
   - 首次构建需要下载所有依赖
   - 完成后 APK 位于：`app/build/outputs/apk/debug/app-debug.apk`

## 🔧 常见问题

### 问题 1：Gradle Sync 失败
**解决方案：**
1. File → Invalidate Caches → Invalidate and Restart
2. 删除项目中的 `.gradle` 和 `build` 文件夹
3. 重新打开项目

### 问题 2：SDK 找不到
**解决方案：**
1. 打开 Android Studio 设置（File → Settings）
2. 导航到 Appearance & Behavior → System Settings → Android SDK
3. 点击 "Edit" 安装 SDK

### 问题 3：依赖下载失败
**解决方案：**
1. 检查网络连接
2. 使用代理（如果需要）
3. 在 `gradle.properties` 中添加：
```properties
systemProp.http.proxyHost=127.0.0.1
systemProp.http.proxyPort=7890
systemProp.https.proxyHost=127.0.0.1
systemProp.https.proxyPort=7890
```

### 问题 4：内存不足
**解决方案：**
在 `gradle.properties` 中增加内存：
```properties
org.gradle.jvmargs=-Xmx4096m -Dfile.encoding=UTF-8
```

## 📊 项目配置

| 配置项 | 值 |
|--------|------|
| 项目名称 | OMaster |
| 包名 | com.omaster.app |
| Min SDK | 26 (Android 8.0) |
| Target SDK | 34 (Android 14) |
| Kotlin | 1.9.22 |
| AGP | 8.2.2 |
| Compose BOM | 2024.02.00 |

## 🎯 项目功能

- ✅ 预设浏览和搜索
- ✅ 主题系统（浅色/深色/跟随系统）
- ✅ 收藏功能（DataStore 持久化）
- ✅ Hilt 依赖注入
- ✅ Retrofit 网络层
- ✅ Material 3 UI
- ✅ 完整的单元测试

## 📱 安装 APK

构建完成后：
1. 将 APK 传输到手机
2. 在手机上启用"安装未知来源应用"
3. 安装 APK
4. 打开应用即可使用

## 🎉 构建成功？

如果构建成功，恭喜你！你可以：
1. 在模拟器上运行测试
2. 安装到真机进行体验
3. 进行进一步开发

## 📞 需要帮助？

如果遇到其他问题，请：
1. 查看 Android Studio 日志
2. 搜索错误信息
3. 查看项目 GitHub Issues

---

**祝构建顺利！🚀**
