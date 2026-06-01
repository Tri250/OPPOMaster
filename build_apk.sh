#!/bin/bash

# OPPO Master Android 项目自动构建脚本
# 适用于 https://github.com/Tri250/OPPOMaster

set -e

echo "======================================"
echo "OPPO Master Android APK 构建脚本"
echo "======================================"
echo ""

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 检查 Java 版本
check_java() {
    echo "检查 Java 环境..."
    if ! command -v java &> /dev/null; then
        echo -e "${RED}错误：未找到 Java${NC}"
        echo "请安装 JDK 17 或更高版本"
        exit 1
    fi
    
    JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}')
    echo -e "${GREEN}✓ Java 版本：$JAVA_VERSION${NC}"
}

# 检查 Android SDK
check_android_sdk() {
    echo ""
    echo "检查 Android SDK..."
    
    if [ -n "$ANDROID_HOME" ]; then
        echo -e "${GREEN}✓ ANDROID_HOME: $ANDROID_HOME${NC}"
    elif [ -d "$HOME/Android/Sdk" ]; then
        ANDROID_HOME="$HOME/Android/Sdk"
        echo -e "${GREEN}✓ 找到 Android SDK: $ANDROID_HOME${NC}"
    elif [ -d "$HOME/Library/Android/sdk" ]; then
        ANDROID_HOME="$HOME/Library/Android/sdk"
        echo -e "${GREEN}✓ 找到 Android SDK: $ANDROID_HOME${NC}"
    else
        echo -e "${YELLOW}⚠ 未找到 Android SDK${NC}"
        echo "请设置 ANDROID_HOME 环境变量或在 local.properties 中配置"
        echo ""
        echo "创建 local.properties 文件..."
        read -p "请输入 Android SDK 路径：" SDK_PATH
        echo "sdk.dir=$SDK_PATH" > local.properties
        ANDROID_HOME="$SDK_PATH"
    fi
}

# 检查 Gradle
check_gradle() {
    echo ""
    echo "检查 Gradle..."
    if command -v gradle &> /dev/null; then
        GRADLE_VERSION=$(gradle --version 2>&1 | grep "Gradle" | awk '{print $2}')
        echo -e "${GREEN}✓ 系统 Gradle 版本：$GRADLE_VERSION${NC}"
        USE_SYSTEM_GRADLE=true
    elif [ -f "./gradlew" ]; then
        echo -e "${GREEN}✓ 使用项目 Gradle Wrapper${NC}"
        USE_SYSTEM_GRADLE=false
    else
        echo -e "${RED}错误：未找到 Gradle${NC}"
        exit 1
    fi
}

# 清理之前的构建
clean_build() {
    echo ""
    echo "清理之前的构建..."
    if [ -d "app/build" ]; then
        rm -rf app/build
        echo -e "${GREEN}✓ 已清理${NC}"
    else
        echo "无需清理"
    fi
}

# 构建 APK
build_apk() {
    echo ""
    echo "======================================"
    echo "开始构建 APK..."
    echo "======================================"
    
    BUILD_TYPE=${1:-debug}
    
    if [ "$USE_SYSTEM_GRADLE" = true ]; then
        echo "使用系统 Gradle 构建..."
        gradle clean assemble${BUILD_TYPE^} --no-daemon
    else
        echo "使用 Gradle Wrapper 构建..."
        chmod +x gradlew
        ./gradlew clean assemble${BUILD_TYPE^} --no-daemon
    fi
    
    echo ""
    echo -e "${GREEN}✓ 构建完成！${NC}"
}

# 显示 APK 信息
show_apk_info() {
    echo ""
    echo "======================================"
    echo "APK 文件信息"
    echo "======================================"
    
    BUILD_TYPE=${1:-Debug}
    APK_PATH="app/build/outputs/apk/${BUILD_TYPE,,}/app-${BUILD_TYPE,,}.apk"
    
    if [ -f "$APK_PATH" ]; then
        APK_SIZE=$(ls -lh "$APK_PATH" | awk '{print $5}')
        echo -e "${GREEN}✓ APK 位置：$(pwd)/$APK_PATH${NC}"
        echo "  文件大小：$APK_SIZE"
        echo ""
        echo "安装命令:"
        echo "  adb install -r $APK_PATH"
    else
        echo -e "${RED}⚠ 未找到 APK 文件${NC}"
    fi
}

# 主函数
main() {
    echo ""
    echo "项目目录：$(pwd)"
    echo "当前分支：$(git branch --show-current 2>/dev/null || echo 'unknown')"
    echo ""
    
    check_java
    check_android_sdk
    check_gradle
    
    echo ""
    echo "======================================"
    echo "选择构建类型"
    echo "======================================"
    echo "1. Debug (调试版，适合开发测试)"
    echo "2. Release (发布版，需要签名配置)"
    echo "3. 两者都构建"
    echo ""
    
    read -p "请选择 [1/2/3]:" BUILD_CHOICE
    
    case $BUILD_CHOICE in
        1)
            clean_build
            build_apk "debug"
            show_apk_info "Debug"
            ;;
        2)
            clean_build
            build_apk "release"
            show_apk_info "Release"
            ;;
        3)
            clean_build
            echo ""
            echo "构建 Debug 版本..."
            build_apk "debug"
            show_apk_info "Debug"
            echo ""
            echo "构建 Release 版本..."
            build_apk "release"
            show_apk_info "Release"
            ;;
        *)
            echo -e "${RED}无效的选择${NC}"
            exit 1
            ;;
    esac
    
    echo ""
    echo "======================================"
    echo "构建成功！"
    echo "======================================"
    echo ""
    echo "下一步:"
    echo "1. 将 APK 传输到 Android 设备"
    echo "2. 使用 ADB 安装：adb install -r app/build/outputs/apk/debug/app-debug.apk"
    echo "3. 在设备上直接点击 APK 文件安装"
    echo ""
}

# 运行主函数
main
