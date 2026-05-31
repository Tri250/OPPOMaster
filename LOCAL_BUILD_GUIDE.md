# OMaster Android APK 本地构建完整指南

## 项目状态
✅ 所有代码已优化并提交到 https://github.com/Tri250/OPPOMaster
✅ 构建配置已优化，适配Android 16 (API 35)
✅ Debug签名密钥已生成

---

## 当前问题
❌ 当前环境缺少：
- gradle-wrapper.jar
- Android SDK
- Android Studio或构建工具

---

## 本地环境构建步骤（在您的电脑上执行）

### 第一步：克隆仓库并切换分支
```bash
# 克隆项目
git clone https://github.com/Tri250/OPPOMaster.git
cd OPPOMaster

# 切换到最新分支
git checkout trae/solo-agent-h6h8z1
```

### 第二步：检查环境要求
```bash
# 检查Java版本（需要Java 17+）
java -version

# 推荐使用Java 17或21
```

### 第三步：检查Gradle Wrapper
```bash
cd OPPOMaster

# 检查gradlew是否有执行权限
ls -la gradlew

# 如果没有执行权限，添加
chmod +x gradlew

# 检查gradle-wrapper目录
ls -la gradle/wrapper/
```

#### 如果缺少 gradle-wrapper.jar
从以下任一方式获取：
1. 使用Android Studio同步项目（推荐）
2. 从Android项目模板复制
3. 使用官方gradle wrapper：
```bash
# 使用gradle wrapper初始化
# 需要先安装gradle
gradle wrapper --gradle-version 8.7
```

### 第四步：配置local.properties
```bash
cd OPPOMaster
```

创建 `local.properties` 文件：
```properties
# 如果有Android SDK
sdk.dir=/path/to/your/android/sdk

# Windows系统示例
# sdk.dir=C\:\\Users\\YourName\\AppData\\Local\\Android\\Sdk

# macOS系统示例
# sdk.dir=/Users/YourName/Library/Android/sdk

# Linux系统示例
# sdk.dir=/home/YourName/Android/Sdk
```

### 第五步：构建Debug APK

#### 方法一：使用命令行（推荐）
```bash
cd OPPOMaster

# 1. 清理之前的构建
./gradlew clean

# 2. 构建Debug APK
./gradlew assembleDebug

# 构建成功后，APK位置：
# app/build/outputs/apk/debug/app-debug.apk

# 3. 直接安装到连接的设备（可选）
./gradlew installDebug
```

#### 方法二：使用Android Studio
1. 打开Android Studio
2. 选择 "Open an Existing Project"
3. 选择 `OPPOMaster` 目录
4. 等待Gradle同步完成
5. 选择 "Build" → "Build Bundle(s)/APK(s)" → "Build APK(s)"
6. APK将在：`app/build/outputs/apk/debug/app-debug.apk`

### 第六步：验证APK
```bash
cd OPPOMaster

# 检查APK文件
ls -lh app/build/outputs/apk/debug/

# 如果构建成功，应该看到：
# app-debug.apk
```

---

## 构建Release APK（可选）

### 生成Release签名密钥
```bash
cd OPPOMaster/app

keytool -genkey -v \
  -keystore release.keystore \
  -storepass your_store_password \
  -keyalias omaster \
  -keypass your_key_password \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000
```

### 构建Release APK
```bash
cd OPPOMaster
./gradlew assembleRelease
```

输出位置：`app/build/outputs/apk/release/app-release.apk`

---

## 故障排除

### 问题1：Gradle下载超时
```bash
# 使用本地代理或设置镜像源
./gradlew assembleDebug --offline  # 尝试离线模式
```

### 问题2：Java版本错误
确保使用Java 17或21：
```bash
export JAVA_HOME=/path/to/java-17
./gradlew assembleDebug
```

### 问题3：缺少Android SDK
1. 安装Android Studio
2. 通过SDK Manager安装：
   - Android SDK Platform 35
   - Android SDK Build-Tools 34.0.0
   - Android SDK Command-line Tools

### 问题4：依赖下载问题
配置Gradle镜像源，在 `build.gradle.kts` 中添加：
```kotlin
repositories {
    maven { url = uri("https://maven.aliyun.com/repository/google") }
    maven { url = uri("https://maven.aliyun.com/repository/central") }
    maven { url = uri("https://maven.aliyun.com/repository/public") }
    google()
    mavenCentral()
}
```

---

## APK安装测试

### 在Android 16设备上测试
```bash
# 通过ADB安装
adb install -r app/build/outputs/apk/debug/app-debug.apk

# 如果需要授予权限
adb install -g -r app/build/outputs/apk/debug/app-debug.apk
```

### 验证应用
1. 在设备上打开应用
2. 检查是否正常启动
3. 测试首页、详情页、设置页
4. 确认无崩溃

---

## Android 16兼容性验证

### 已配置参数
- compileSdk = 35
- targetSdk = 35
- minSdk = 26

### Material Design 3
- 使用最新Compose版本
- 完整的深色/浅色主题支持

### 权限适配
- 分区存储适配
- 动态权限请求
- 通知权限适配

---

## 项目核心功能

### 哈苏大师预设库
- 6个专业预设
- HNCS认证标识
- 样张展示
- 完整相机参数

### UI界面
- 首页 - 预设列表和筛选
- 详情页 - 参数展示
- 设置页 - 主题和功能开关
- AI微调 - 自定义参数
- 场景检测 - 智能识别

---

## 快速开始（一键构建）

### 创建构建脚本（Linux/macOS）
```bash
cd OPPOMaster
cat > build_apk.sh << 'EOF'
#!/bin/bash
# OMaster APK 构建脚本

echo "=== OMaster APK 构建 ==="
echo ""

# 检查Java
echo "[1/4] 检查Java..."
if ! java -version; then
    echo "请先安装Java 17或21"
    exit 1
fi

# 检查gradlew
echo "[2/4] 检查Gradle Wrapper..."
chmod +x gradlew

# 清理构建
echo "[3/4] 清理旧构建..."
./gradlew clean

# 构建APK
echo "[4/4] 构建Debug APK..."
./gradlew assembleDebug

# 检查结果
if [ -f "app/build/outputs/apk/debug/app-debug.apk" ]; then
    echo ""
    echo "✅ APK 构建成功！"
    echo "📍 输出位置：$PWD/app/build/outputs/apk/debug/app-debug.apk"
    echo ""
    echo "安装到设备：./gradlew installDebug"
    ls -lh app/build/outputs/apk/debug/
else
    echo ""
    echo "❌ APK 构建失败，请检查错误信息"
    exit 1
fi
EOF

chmod +x build_apk.sh

# 运行构建
./build_apk.sh
```

---

## 仓库信息

- **GitHub**: https://github.com/Tri250/OPPOMaster
- **分支**: trae/solo-agent-h6h8z1
- **最新提交**: 882309b
- **状态**: 所有配置已优化，准备好构建！

---

## 支持
如有问题，请查看：
- [BUILD_GUIDE.md](BUILD_GUIDE.md)
- [UI_DEMO.md](UI_DEMO.md)
- [WORK_RECORD.md](WORK_RECORD.md)
