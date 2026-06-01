# OMaster Android 项目构建说明

## 项目状态

✅ 项目代码已准备就绪
✅ 所有依赖已配置完成
✅ Release 签名配置已优化（使用 debug 签名以便快速构建）

## ⚠️ 重要提示

**当前环境没有安装完整的 Android SDK，无法直接在本环境编译 APK。**

## 📱 在您的电脑上构建 APK

### 前置要求

1. **JDK 17+** - 确保已安装 Java 17 或更高版本
2. **Android Studio** - 推荐最新稳定版
3. **Android SDK** - 需要安装 Android SDK Platform 35

### 构建步骤

#### 方法 1：使用 Android Studio（推荐）

1. **打开项目**
   - 启动 Android Studio
   - 选择 "Open an Existing Project"
   - 选择当前 `/workspace` 文件夹

2. **等待 Gradle Sync**
   - 首次打开会自动下载所有依赖
   - 确保网络连接正常
   - 等待底部状态栏显示 "Gradle sync finished"

3. **构建 Release APK**
   - 菜单栏：`Build` → `Build Bundle(s) / APK(s)` → `Build APK(s)`
   - 或者使用快捷键：`Ctrl+F9` (Windows/Linux) / `Cmd+F9` (Mac)

4. **获取 APK 文件**
   - 构建完成后，点击通知中的 "locate"
   - 或者直接打开：`/workspace/app/build/outputs/apk/release/app-release.apk`

#### 方法 2：使用命令行

```bash
# 进入项目目录
cd /workspace

# 确保 gradlew 有执行权限
chmod +x gradlew

# 构建 Release APK
./gradlew clean assembleRelease

# 构建 Debug APK（可选，用于测试）
./gradlew clean assembleDebug
```

APK 文件位置：
- Release: `app/build/outputs/apk/release/app-release.apk`
- Debug: `app/build/outputs/apk/debug/app-debug.apk`

## 📲 安装到 Android 16 设备

1. 将 APK 文件传输到手机
2. 在手机上开启「允许安装未知来源应用」
3. 点击 APK 进行安装
4. 打开应用即可使用

## 🔧 配置说明

### 已优化的配置

1. **Target SDK 35** - 完全兼容 Android 16
2. **Minimum SDK 26** - 支持 Android 8.0+
3. **Release 签名** - 使用 debug 签名，无需额外配置 keystore
4. **代码压缩** - 已禁用，便于调试和快速构建

### 如需修改签名配置

编辑 `/workspace/app/build.gradle.kts` 中的 `signingConfigs` 部分：

```kotlin
signingConfigs {
    create("release") {
        storeFile = file("your-release.keystore")
        storePassword = "your-password"
        keyAlias = "your-alias"
        keyPassword = "your-key-password"
        enableV2Signing = true
        enableV3Signing = true
    }
}
```

## 📋 项目信息

| 配置项 | 值 |
|--------|-----|
| 包名 | `com.omaster.app` |
| 版本号 | 121 |
| 版本名称 | 1.2.1 |
| 编译 SDK | 35 (Android 15) |
| 目标 SDK | 35 (兼容 Android 16) |
| 最低 SDK | 26 (Android 8.0) |

## ✨ 项目特性

- 📸 哈苏 HNCS 认证预设库
- 🎨 10+ 水印模板
- 🤖 AI 智能场景识别
- ⚡ 流体云胶囊（ColorOS 16 集成）
- 🪟 悬浮窗功能
- 📊 实时相机参数显示
- 🔒 安全隐私保护

## 🚀 常见问题

### 问题：Gradle Sync 失败
**解决：**
- 检查网络连接
- File → Invalidate Caches → Invalidate and Restart
- 删除项目 `.gradle` 和 `build` 文件夹后重新打开

### 问题：找不到 Android SDK
**解决：**
- 在 Android Studio 中：File → Settings → Appearance & Behavior → System Settings → Android SDK
- 点击 "Edit" 安装所需 SDK

### 问题：依赖下载慢
**解决：**
在 `gradle.properties` 中添加国内镜像：
```properties
systemProp.https.proxyHost=mirrors.cloud.tencent.com
systemProp.https.proxyPort=443
```

## 📞 需要帮助？

如有问题，请参考项目中的其他文档：
- `README.md` - 项目总体介绍
- `COMPLETE_BUILD_GUIDE.md` - 详细构建指南
- `COLOROS_16_EXPERT_ACCEPTANCE_REPORT.md` - 兼容性报告

---

**祝构建顺利！🎉**
