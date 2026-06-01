# ⚡️ 3步构建 Release APK - 快速开始

## 🚀 3步搞定！

### 第1步：获取项目
```bash
git clone https://github.com/Tri250/OPPOMaster.git
cd OPPOMaster
git checkout trae/solo-agent-ZBIegB
```

### 第2步：构建 APK
```bash
# 方法A - 一键构建（推荐）
./build_fast.sh
# 选择 2 (Release)

# 方法B - 命令行构建
./gradlew clean assembleRelease

# 方法C - Android Studio
# Build → Generate Signed Bundle / APK → Release
```

### 第3步：安装到 Android 16
```bash
adb install -r app/build/outputs/apk/release/app-release.apk
adb shell am start -n com.omaster.app/.MainActivity
```

---

## 📦 签名信息

- **Keystore**: `release.keystore`
- **Password**: `omaster123`
- **Alias**: `omaster`
- **Key Password**: `omaster123`

---

## 🔧 如果在中国大陆，先配置镜像

镜像已配置在项目中，直接使用即可：
- 阿里云（优先）
- 腾讯云
- 华为云
- 中科大

---

## 📋 验证清单

安装后验证：
- [ ] 应用启动成功
- [ ] 预设列表正常
- [ ] AI 场景识别可用
- [ ] 无崩溃或异常

---

## 📚 完整文档

- [FINAL_BUILD_GUIDE.md](file:///workspace/FINAL_BUILD_GUIDE.md) - 终极构建指南
- [MIRROR_OPTIMIZATION_GUIDE.md](file:///workspace/MIRROR_OPTIMIZATION_GUIDE.md) - 镜像加速说明

---

## 🎉 完成！

APK 位置：`app/build/outputs/apk/release/app-release.apk`
