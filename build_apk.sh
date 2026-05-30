#!/bin/bash

# OPPOMaster APK 构建脚本
# 使用方法：./build_apk.sh

set -e

echo "========================================"
echo "OPPOMaster APK 构建脚本 v1.0"
echo "========================================"
echo ""

# 1. 检查环境
echo "步骤1: 检查环境..."
echo "----------------------------------------"

# 检查Java
if command -v java &> /dev/null; then
    JAVA_VERSION=$(java -version 2>&1 | head -1)
    echo "✅ Java已安装: $JAVA_VERSION"
else
    echo "❌ Java未安装"
    echo "请安装 JDK 17: https://adoptium.net/temurin/releases/?version=17"
    exit 1
fi

# 检查Gradle
if command -v gradle &> /dev/null; then
    GRADLE_VERSION=$(gradle --version | head -1)
    echo "✅ Gradle已安装: $GRADLE_VERSION"
else
    echo "⚠️ Gradle未安装（将使用gradlew）"
fi

# 检查Android SDK
if [ -n "$ANDROID_HOME" ]; then
    echo "✅ Android SDK已配置: $ANDROID_HOME"
else
    echo "⚠️ ANDROID_HOME未设置"
    echo "请设置: export ANDROID_HOME=/path/to/android/sdk"
fi

echo ""

# 2. 准备构建
echo "步骤2: 准备构建..."
echo "----------------------------------------"

# 确保gradlew可执行
chmod +x gradlew

# 清理构建目录
echo "清理旧构建..."
rm -rf app/build/outputs/apk
mkdir -p app/build/outputs/apk/debug

echo ""

# 3. 检查项目文件
echo "步骤3: 检查项目文件..."
echo "----------------------------------------"

# 检查关键文件
FILES=(
    "app/build.gradle.kts"
    "build.gradle.kts"
    "gradle.properties"
    "settings.gradle.kts"
    "app/src/main/AndroidManifest.xml"
    "app/src/main/java/com/omaster/app/MainActivity.kt"
)

for file in "${FILES[@]}"; do
    if [ -f "$file" ]; then
        echo "✅ $file"
    else
        echo "❌ $file 不存在"
        exit 1
    fi
done

echo ""

# 4. 构建Debug APK
echo "步骤4: 构建Debug APK..."
echo "----------------------------------------"
echo "执行命令: ./gradlew assembleDebug"
echo ""

./gradlew assembleDebug --no-daemon --stacktrace

# 5. 验证APK
echo ""
echo "步骤5: 验证APK..."
echo "----------------------------------------"

APK_PATH="app/build/outputs/apk/debug/app-debug.apk"

if [ -f "$APK_PATH" ]; then
    APK_SIZE=$(du -h "$APK_PATH" | cut -f1)
    echo "✅ APK构建成功！"
    echo ""
    echo "========================================"
    echo "APK信息"
    echo "========================================"
    echo "路径: $APK_PATH"
    echo "大小: $APK_SIZE"
    echo ""
    echo "安装命令:"
    echo "adb install -r $APK_PATH"
    echo ""
    echo "下一步:"
    echo "1. 将APK传输到Android 16设备"
    echo "2. 启用'安装未知来源应用'"
    echo "3. 安装APK并享受AI功能！"
    echo "========================================"
else
    echo "❌ APK构建失败"
    exit 1
fi
