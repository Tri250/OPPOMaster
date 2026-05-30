#!/bin/bash

# OMaster Android APK 构建脚本
# 支持 Android 16 (API 36) 系统

set -e

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo -e "${GREEN}======================================${NC}"
echo -e "${GREEN}  OMaster Android APK 构建脚本${NC}"
echo -e "${GREEN}======================================${NC}"
echo ""

# 检查 Java 版本
JAVA_VERSION=$(java -version 2>&1 | head -1 | cut -d'"' -f2)
echo -e "${YELLOW}检测到 Java 版本: $JAVA_VERSION${NC}"

if [[ ! "$JAVA_VERSION" =~ ^21\. ]]; then
    echo -e "${RED}错误: 需要 Java 21，当前版本为 $JAVA_VERSION${NC}"
    exit 1
fi

# 检查 Gradle
if ! command -v gradle &> /dev/null; then
    echo -e "${YELLOW}Gradle 未安装，使用 gradlew...${NC}"
    GRADLE_CMD="./gradlew"
    if [ ! -f "./gradlew" ]; then
        echo -e "${RED}错误: gradlew 不存在${NC}"
        exit 1
    fi
    chmod +x ./gradlew
else
    GRADLE_CMD="gradle"
    echo -e "${GREEN}使用系统 Gradle${NC}"
fi

# 检查 Android SDK
if [ -z "$ANDROID_HOME" ]; then
    if [ -d "$HOME/Android/Sdk" ]; then
        export ANDROID_HOME="$HOME/Android/Sdk"
    elif [ -d "/usr/local/android-sdk" ]; then
        export ANDROID_HOME="/usr/local/android-sdk"
    fi
fi

if [ -n "$ANDROID_HOME" ]; then
    echo -e "${GREEN}Android SDK: $ANDROID_HOME${NC}"
else
    echo -e "${YELLOW}警告: ANDROID_HOME 未设置，尝试自动检测...${NC}"
fi

# 构建类型
BUILD_TYPE="${1:-debug}"

echo ""
echo -e "${GREEN}开始构建 $BUILD_TYPE APK...${NC}"
echo ""

# 执行构建
case $BUILD_TYPE in
    debug)
        echo -e "${YELLOW}构建 Debug 版本...${NC}"
        $GRADLE_CMD clean assembleDebug --no-daemon --stacktrace
        ;;
    release)
        echo -e "${YELLOW}构建 Release 版本...${NC}"
        if [ ! -f "app/release.keystore" ]; then
            echo -e "${RED}错误: release.keystore 不存在${NC}"
            exit 1
        fi
        $GRADLE_CMD clean assembleRelease --no-daemon --stacktrace
        ;;
    all)
        echo -e "${YELLOW}构建所有版本...${NC}"
        $GRADLE_CMD clean assemble --no-daemon --stacktrace
        ;;
    *)
        echo -e "${RED}未知构建类型: $BUILD_TYPE${NC}"
        echo "用法: $0 [debug|release|all]"
        exit 1
        ;;
esac

# 检查 APK 是否生成
APK_DIR="app/build/outputs/apk"
if [ "$BUILD_TYPE" = "debug" ]; then
    APK_PATH="$APK_DIR/debug"
elif [ "$BUILD_TYPE" = "release" ]; then
    APK_PATH="$APK_DIR/release"
else
    APK_PATH="$APK_DIR"
fi

echo ""
echo -e "${GREEN}======================================${NC}"
echo -e "${GREEN}  构建完成！${NC}"
echo -e "${GREEN}======================================${NC}"
echo ""

# 列出生成的 APK
if [ -d "$APK_PATH" ]; then
    echo -e "${YELLOW}生成的 APK 文件:${NC}"
    find "$APK_PATH" -name "*.apk" -exec ls -lh {} \;
    echo ""
    echo -e "${GREEN}APK 路径: $(realpath $APK_PATH)${NC}"
else
    echo -e "${RED}错误: APK 目录不存在${NC}"
    exit 1
fi

# 输出信息
echo ""
echo -e "${GREEN}======================================${NC}"
echo -e "${GREEN}  安装说明${NC}"
echo -e "${GREEN}======================================${NC}"
echo ""
echo "1. 将 APK 文件复制到手机:"
echo "   adb install -r <apk-path>"
echo ""
echo "2. 或者通过 USB 传输到手机后安装"
echo ""
echo "3. Android 16 权限说明:"
echo "   - 应用需要相机权限读取 Camera2 参数"
echo "   - 需要存储权限保存截图"
echo "   - 悬浮窗权限用于显示参数"
echo ""
