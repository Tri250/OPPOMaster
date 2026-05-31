# OMaster APK 快速开始指南

## 🚀 3步构建APK

### 方法1：使用 Android Studio (推荐)

1. **打开项目**
   ```bash
   # 启动 Android Studio
   # 选择 "Open an Existing Project"
   # 选择此项目根目录
   ```

2. **同步 Gradle**
   - 等待 Android Studio 自动同步项目
   - 首次同步可能需要几分钟下载依赖

3. **构建 APK**
   ```
   菜单栏: Build > Build Bundle(s) / APK(s) > Build APK(s)
   ```
   - 构建完成后，点击通知中的 "locate" 查看APK
   - APK 文件位置: `app/build/outputs/apk/debug/app-debug.apk`

### 方法2：命令行构建

```bash
# 进入项目目录
cd /path/to/omaster-project

# 构建Debug APK
./gradlew assembleDebug

# 构建Release APK
./gradlew assembleRelease

# APK位置
ls -la app/build/outputs/apk/
```

---

## 📱 安装到设备

### Android 16 设备

1. **启用开发者选项**
   - 打开设置 > 关于手机
   - 连续点击「版本号」7次

2. **启用USB调试**
   - 打开设置 > 系统 > 开发者选项
   - 启用「USB调试」

3. **安装APK**
   ```bash
   # 使用ADB安装
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```
   或直接复制APK文件到设备并点击安装

---

## ✅ 验证清单

安装前检查：
- [ ] 设备系统版本 >= Android 8.0
- [ ] 已启用「未知来源」安装
- [ ] 有足够存储空间 (>100MB)

安装后检查：
- [ ] 应用能正常启动
- [ ] 所有功能模块正常
- [ ] 无崩溃或异常

---

## 🔧 常见问题

### Q: Gradle同步失败？
A: 检查网络连接，或配置国内镜像源

### Q: 构建报错？
A: 确保使用 JDK 17+，检查 `JAVA_HOME` 环境变量

### Q: 安装失败？
A: 检查是否有相同包名的旧版本，先卸载

---

## 📞 获取帮助

如遇问题，请查看项目文档：
- `COMPLETE_BUILD_GUIDE.md` - 完整构建指南
- `ANDROID_16_READINESS_CHECKLIST.md` - 兼容性检查清单
- `CODE_REVIEW_FIX_REPORT.md` - 代码审查报告

---

**最后更新**: 2026-05-31
