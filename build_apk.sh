#!/bin/bash

# OMaster APK 一键构建脚本
# 适用于 Android 14-16 系统

set -e

echo "======================================"
echo "OMaster APK 构建脚本"
echo "======================================"
echo

# 检查 Java
if ! command -v java &> /dev/null; then
    echo "❌ 错误：未找到 Java"
    echo "请先安装 JDK 17 或更高版本"
    exit 1
fi

echo "✅ Java 版本:"
java -version
echo

# 检查 Android SDK
if [ -z "$ANDROID_HOME" ] && [ -z "$ANDROID_SDK_ROOT" ]; then
    echo "⚠️  警告：未设置 ANDROID_HOME 或 ANDROID_SDK_ROOT"
    echo
    echo "请先安装 Android SDK，然后设置环境变量："
    echo
    echo "  Linux/macOS:"
    echo "  export ANDROID_HOME=\$HOME/Android/Sdk"
    echo "  export PATH=\$PATH:\$ANDROID_HOME/platform-tools:\$ANDROID_HOME/cmdline-tools/latest/bin"
    echo
    echo "  Windows:"
    echo "  set ANDROID_HOME=C:\\Users\\YourName\\AppData\\Local\\Android\\Sdk"
    echo "  set PATH=%PATH%;%ANDROID_HOME%\\platform-tools"
    echo
    echo "或者使用 Android Studio，它会自动配置 SDK"
    echo
    exit 1
fi

echo "✅ Android SDK 路径：${ANDROID_HOME:-$ANDROID_SDK_ROOT}"
echo

# 进入项目目录
PROJECT_DIR="$(dirname "$0")/omaster_final_build"
if [ ! -d "$PROJECT_DIR" ]; then
    echo "❌ 错误：项目目录不存在"
    echo "请确保 omaster_final_build 目录存在"
    exit 1
fi

cd "$PROJECT_DIR"
echo "✅ 项目目录：$PROJECT_DIR"
echo

# 检查 gradlew
if [ ! -f "gradlew" ]; then
    echo "❌ 错误：gradlew 不存在"
    exit 1
fi

chmod +x gradlew

# 构建 APK
echo "======================================"
echo "开始构建 APK..."
echo "======================================"
echo

./gradlew assembleDebug

echo
echo "======================================"
echo "✅ 构建完成！"
echo "======================================"
echo
echo "APK 位置：app/build/outputs/apk/debug/app-debug.apk"
echo

# 检查 APK 是否生成
if [ -f "app/build/outputs/apk/debug/app-debug.apk" ]; then
    APK_SIZE=$(ls -lh app/build/outputs/apk/debug/app-debug.apk | awk '{print $5}')
    echo "📦 APK 大小：$APK_SIZE"
    echo
    echo "📱 安装说明："
    echo "  1. 将 APK 传输到 Android 设备"
    echo "  2. 在设备上启用'未知来源'安装"
    echo "  3. 点击 APK 文件进行安装"
    echo
    echo "✅ 支持系统：Android 14, 15, 16"
else
    echo "❌ 错误：APK 未生成"
    exit 1
fi
