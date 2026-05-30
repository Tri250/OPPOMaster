#!/bin/bash

# OMaster Android APK 完整构建包
# 包含所有必要的依赖和配置

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BUILD_DIR="$SCRIPT_DIR/android-build-package"
GRADLE_VERSION="8.14.4"
AGP_VERSION="8.5.0"

echo "========================================"
echo "OMaster Android APK 构建工具包"
echo "========================================"
echo ""

# 创建构建目录
mkdir -p "$BUILD_DIR"

# 复制项目文件
echo "复制项目文件..."
cp -r "$SCRIPT_DIR/." "$BUILD_DIR/" 2>/dev/null || true

# 创建 gradle.properties
cat > "$BUILD_DIR/gradle.properties" << 'EOF'
# Project-wide Gradle settings.
org.gradle.jvmargs=-Xmx4096m -XX:+UseParallelGC
org.gradle.parallel=true
org.gradle.caching=true
org.gradle.daemon=false

# AndroidX
android.useAndroidX=true
android.enableJetifier=true

# Kotlin
kotlin.code.style=official

# Non-transitive R classes
android.nonTransitiveRClass=true
EOF

# 创建构建脚本
cat > "$BUILD_DIR/build.sh" << 'EOF'
#!/bin/bash

set -e

echo "========================================"
echo "开始构建 OMaster APK"
echo "========================================"
echo ""

# 检查 Java
if ! command -v java &> /dev/null; then
    echo "错误: Java 未安装"
    exit 1
fi

# 检查 Gradle
if ! command -v gradle &> /dev/null; then
    echo "错误: Gradle 未安装"
    echo "请安装 Gradle $GRADLE_VERSION 或更高版本"
    echo "下载地址: https://gradle.org/releases/"
    exit 1
fi

# 检查 Android SDK
if [ -z "$ANDROID_HOME" ]; then
    if [ -d "$HOME/Android/Sdk" ]; then
        export ANDROID_HOME="$HOME/Android/Sdk"
    elif [ -d "/usr/local/android-sdk" ]; then
        export ANDROID_HOME="/usr/local/android-sdk"
    else
        echo "警告: ANDROID_HOME 未设置"
        echo "请设置 ANDROID_HOME 环境变量"
    fi
fi

# 构建 Debug APK
echo "执行构建..."
gradle clean assembleDebug --no-daemon

# 检查结果
if [ -f "app/build/outputs/apk/debug/OMaster-debug.apk" ]; then
    echo ""
    echo "========================================"
    echo "✓ 构建成功!"
    echo "========================================"
    echo ""
    echo "APK 位置:"
    ls -lh app/build/outputs/apk/debug/OMaster-debug.apk
    echo ""
    echo "安装命令:"
    echo "  adb install -r app/build/outputs/apk/debug/OMaster-debug.apk"
else
    echo ""
    echo "========================================"
    echo "✗ 构建失败"
    echo "========================================"
    exit 1
fi
EOF

chmod +x "$BUILD_DIR/build.sh"

# 创建 README
cat > "$BUILD_DIR/README.md" << EOF
# OMaster Android APK 构建包

## 快速开始

1. **环境要求**
   - Java 21+
   - Gradle 8.14.4+
   - Android SDK API 36

2. **构建步骤**
   \`\`\`bash
   # 进入构建目录
   cd android-build-package

   # 运行构建脚本
   ./build.sh
   \`\`\`

3. **安装 APK**
   \`\`\`bash
   # 使用 ADB 安装
   adb install -r app/build/outputs/apk/debug/OMaster-debug.apk
   \`\`\`

## APK 信息

- **应用名称**: OMaster
- **包名**: com.omaster.app
- **版本**: 1.5.0
- **目标 SDK**: 36 (Android 16)
- **最低 SDK**: 26 (Android 8.0)

## 功能特性

- AI 智能场景识别
- OPPO/Realme 相机参数管理
- 哈苏大师模式预设
- 本地图像场景分析
- 系统悬浮窗参数展示
- 水印功能

## Android 16 兼容性

✓ 支持 Android 16 (API 36)
✓ 相机2 API 参数读取
✓ 存储权限 (Photo Picker)
✓ 悬浮窗权限
✓ 通知权限

## 故障排除

### 构建失败
\`\`\`bash
# 清除缓存后重试
gradle clean --refresh-dependencies
\`\`\`

### 依赖下载失败
\`\`\`bash
# 使用阿里云镜像
# 在 gradle.properties 中添加阿里云仓库
\`\`\`

### Java 版本错误
\`\`\`bash
# 设置 JAVA_HOME
export JAVA_HOME=/path/to/jdk-21
\`\`\`

## 许可证

Apache 2.0
EOF

echo ""
echo "========================================"
echo "✓ 构建包创建成功!"
echo "========================================"
echo ""
echo "构建包位置: $BUILD_DIR"
echo ""
echo "下一步:"
echo "1. 将构建包复制到有网络的环境"
echo "2. 运行 ./build.sh"
echo "3. 等待构建完成"
echo ""
