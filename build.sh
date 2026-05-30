#!/bin/bash

################################################################################
# OMaster Android APK 构建脚本
# 目标：Android 16 (API 36)
# 版本：1.5.0
################################################################################

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 构建配置
BUILD_TYPE="${1:-debug}"
GRADLE_VERSION="8.14.4"
AGP_VERSION="8.5.0"
KOTLIN_VERSION="2.0.0"
COMPOSE_BOM="2024.06.00"

# 打印带颜色的消息
print_header() {
    echo -e "\n${BLUE}================================================================${NC}"
    echo -e "${BLUE}  $1${NC}"
    echo -e "${BLUE}================================================================${NC}\n"
}

print_success() {
    echo -e "${GREEN}✓${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}⚠${NC} $1"
}

print_error() {
    echo -e "${RED}✗${NC} $1"
}

# 检查环境
check_environment() {
    print_header "检查构建环境"

    # 检查 Java
    if command -v java &> /dev/null; then
        JAVA_VERSION=$(java -version 2>&1 | head -1 | cut -d'"' -f2)
        print_success "Java: $JAVA_VERSION"
        
        # 检查 Java 版本
        if [[ ! "$JAVA_VERSION" =~ ^21\. ]]; then
            print_warning "建议使用 Java 21，当前版本: $JAVA_VERSION"
        fi
    else
        print_error "Java 未安装"
        exit 1
    fi

    # 检查 Gradle
    if command -v gradle &> /dev/null; then
        GRADLE_INSTALLED=$(gradle --version | head -1 | awk '{print $2}')
        print_success "Gradle: $GRADLE_INSTALLED"
    else
        print_warning "Gradle 未安装，请安装 Gradle $GRADLE_VERSION+"
    fi

    # 检查 Android SDK
    if [ -n "$ANDROID_HOME" ]; then
        print_success "ANDROID_HOME: $ANDROID_HOME"
    elif [ -d "$HOME/Android/Sdk" ]; then
        export ANDROID_HOME="$HOME/Android/Sdk"
        print_success "ANDROID_HOME: $ANDROID_HOME"
    else
        print_warning "ANDROID_HOME 未设置"
    fi

    echo ""
}

# 下载依赖
download_dependencies() {
    print_header "下载构建依赖"

    local deps_dir="$HOME/.gradle/caches/modules-2/files-2.1/com.android.tools.build"
    mkdir -p "$deps_dir"

    echo "Android Gradle Plugin $AGP_VERSION 下载..."
    
    # 尝试从多个源下载
    local sources=(
        "https://dl.google.com/dl/android/maven2"
        "https://maven.aliyun.com/repository/google"
        "https://maven.aliyun.com/repository/central"
    )

    for source in "${sources[@]}"; do
        echo "尝试从: $source"
        
        # 下载 AGP
        local agp_url="${source}/com/android/application/gradle/${AGP_VERSION}/gradle-${AGP_VERSION}.jar"
        local agp_path="$deps_dir/gradle/${AGP_VERSION}"
        mkdir -p "$agp_path"
        
        if curl -sfL "$agp_url" -o "$agp_path/gradle-${AGP_VERSION}.jar" 2>/dev/null; then
            local size=$(stat -f%z "$agp_path/gradle-${AGP_VERSION}.jar" 2>/dev/null || stat -c%s "$agp_path/gradle-${AGP_VERSION}.jar" 2>/dev/null)
            if [ "$size" -gt 10000 ]; then
                print_success "AGP 下载成功 (${size} bytes)"
                break
            fi
        fi
    done

    echo ""
}

# 构建项目
build_project() {
    print_header "构建 OMaster APK"

    local gradle_cmd="./gradlew"
    
    if [ ! -f "./gradlew" ]; then
        print_error "gradlew 不存在"
        exit 1
    fi
    
    chmod +x ./gradlew

    print_warning "开始构建 $BUILD_TYPE APK..."
    print_warning "这可能需要 5-15 分钟（首次构建）"
    echo ""

    # 执行构建
    case $BUILD_TYPE in
        debug)
            ./gradlew clean assembleDebug --no-daemon --stacktrace
            ;;
        release)
            if [ ! -f "app/release.keystore" ]; then
                print_error "Release 构建需要签名文件: app/release.keystore"
                exit 1
            fi
            ./gradlew clean assembleRelease --no-daemon --stacktrace
            ;;
        *)
            print_error "未知的构建类型: $BUILD_TYPE"
            echo "可用选项: debug, release"
            exit 1
            ;;
    esac
}

# 检查构建结果
check_result() {
    print_header "检查构建结果"

    local apk_dir="app/build/outputs/apk"
    
    case $BUILD_TYPE in
        debug)
            local apk_path="$apk_dir/debug/OMaster-debug.apk"
            ;;
        release)
            local apk_path="$apk_dir/release/OMaster-release.apk"
            ;;
    esac

    if [ -f "$apk_path" ]; then
        local size=$(stat -f%z "$apk_path" 2>/dev/null || stat -c%s "$apk_path" 2>/dev/null)
        local size_mb=$((size / 1024 / 1024))
        
        print_success "APK 构建成功!"
        echo ""
        echo "APK 路径: $(pwd)/$apk_path"
        echo "APK 大小: ${size_mb} MB"
        echo ""
        
        # 验证 APK
        if command -v aapt &> /dev/null; then
            echo "APK 信息:"
            aapt dump badging "$apk_path" | grep -E "package:|application-label:|sdkVersion:|targetSdkVersion:" | head -4
        fi
        
        return 0
    else
        print_error "APK 构建失败"
        return 1
    fi
}

# 打印使用说明
print_usage() {
    print_header "使用说明"
    
    echo "构建 Debug APK:"
    echo "  $0 debug"
    echo ""
    echo "构建 Release APK:"
    echo "  $0 release"
    echo ""
    echo "直接使用 Gradle:"
    echo "  ./gradlew assembleDebug"
    echo ""
    echo "跳过测试构建:"
    echo "  ./gradlew assembleDebug -x test"
    echo ""
}

# 主函数
main() {
    echo ""
    print_header "OMaster Android APK 构建工具"
    echo "项目版本: 1.5.0"
    echo "目标系统: Android 16 (API 36)"
    echo "构建类型: $BUILD_TYPE"
    echo ""

    # 检查环境
    check_environment

    # 下载依赖（如果需要）
    # download_dependencies

    # 构建项目
    build_project

    # 检查结果
    if check_result; then
        print_header "🎉 构建完成!"
        print_usage
    else
        print_error "构建失败，请检查错误信息"
        exit 1
    fi
}

# 运行主函数
main "$@"
