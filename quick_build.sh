#!/bin/bash

################################################################################
# OMaster 快速构建脚本
# 用于在网络畅通环境中快速构建 APK
################################################################################

set -e

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

print_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# 配置
GRADLE_VERSION="8.14.4"
AGP_VERSION="8.5.0"
BUILD_TYPE="${1:-debug}"

print_info "OMaster APK 快速构建脚本"
print_info "======================================"
print_info "Gradle 版本: $GRADLE_VERSION"
print_info "AGP 版本: $AGP_VERSION"
print_info "构建类型: $BUILD_TYPE"
print_info "======================================"
echo ""

# 1. 下载 Gradle Wrapper
print_info "步骤 1: 下载 Gradle Wrapper..."
if [ ! -f "gradle/wrapper/gradle-${GRADLE_VERSION}-bin.zip" ]; then
    curl -L -o "gradle/wrapper/gradle-${GRADLE_VERSION}-bin.zip" \
        "https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip"
    print_success "Gradle Wrapper 下载完成"
else
    print_warning "Gradle Wrapper 已存在，跳过下载"
fi
echo ""

# 2. 配置 Android SDK
print_info "步骤 2: 配置 Android SDK..."
if [ -z "$ANDROID_HOME" ]; then
    if [ -d "$HOME/Android/Sdk" ]; then
        export ANDROID_HOME="$HOME/Android/Sdk"
    else
        print_warning "ANDROID_HOME 未设置，请手动配置"
    fi
fi

if [ -n "$ANDROID_HOME" ]; then
    print_success "ANDROID_HOME: $ANDROID_HOME"
    
    # 检查必要的 SDK 组件
    if [ ! -d "$ANDROID_HOME/platforms/android-36" ]; then
        print_warning "缺少 Android 36 platform，正在下载..."
        mkdir -p "$ANDROID_HOME/cmdline-tools"
        cd "$ANDROID_HOME/cmdline-tools"
        if [ ! -d "latest" ]; then
            curl -L -o cmdline-tools.zip \
                "https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip"
            unzip -q cmdline-tools.zip
            mv cmdline-tools latest
            rm cmdline-tools.zip
        fi
        cd - > /dev/null
        yes | "$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager" \
            "platforms;android-36" \
            "build-tools;36.0.0" \
            "platform-tools" || print_warning "SDK 组件安装可能需要手动确认"
    fi
else
    print_warning "Android SDK 未配置"
fi
echo ""

# 3. 验证 Java 环境
print_info "步骤 3: 验证 Java 环境..."
java -version 2>&1 | head -1
print_success "Java 环境正常"
echo ""

# 4. 构建项目
print_info "步骤 4: 构建 $BUILD_TYPE APK..."
echo ""

if [ -f "./gradlew" ]; then
    chmod +x ./gradlew
    
    case $BUILD_TYPE in
        debug)
            ./gradlew clean assembleDebug --no-daemon --stacktrace
            ;;
        release)
            if [ ! -f "app/release.keystore" ]; then
                print_warning "Release 签名文件不存在，使用默认配置"
                ./gradlew clean assembleRelease --no-daemon --stacktrace || \
                ./gradlew clean assembleDebug --no-daemon --stacktrace
            else
                ./gradlew clean assembleRelease --no-daemon --stacktrace
            fi
            ;;
        *)
            print_error "未知的构建类型: $BUILD_TYPE"
            echo "用法: $0 [debug|release]"
            exit 1
            ;;
    esac
else
    print_warning "gradlew 不存在，尝试使用系统 Gradle..."
    gradle clean assembleDebug --no-daemon
fi

# 5. 检查结果
print_info "步骤 5: 验证构建结果..."
echo ""

case $BUILD_TYPE in
    debug)
        APK_PATH="app/build/outputs/apk/debug/OMaster-debug.apk"
        ;;
    release)
        APK_PATH="app/build/outputs/apk/release/OMaster-release.apk"
        ;;
esac

if [ -f "$APK_PATH" ]; then
    APK_SIZE=$(stat -f%z "$APK_PATH" 2>/dev/null || stat -c%s "$APK_PATH" 2>/dev/null)
    APK_SIZE_MB=$((APK_SIZE / 1024 / 1024))
    
    print_success "======================================"
    print_success "APK 构建成功!"
    print_success "======================================"
    print_info "APK 路径: $(pwd)/$APK_PATH"
    print_info "APK 大小: ${APK_SIZE_MB} MB"
    print_info "======================================"
    echo ""
    
    # 显示 APK 信息
    if command -v aapt &> /dev/null; then
        print_info "APK 详细信息:"
        aapt dump badging "$APK_PATH" | grep -E \
            "package:|application-label:|sdkVersion:|targetSdkVersion:" || true
    fi
    
    echo ""
    print_success "构建完成!"
else
    print_error "======================================"
    print_error "APK 构建失败"
    print_error "======================================"
    print_error "请检查构建日志以获取详细信息"
    exit 1
fi
