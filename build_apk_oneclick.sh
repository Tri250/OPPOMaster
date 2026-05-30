#!/bin/bash

################################################################################
# OMaster 一键构建脚本
# 在网络畅通环境中运行此脚本即可完成 APK 构建
################################################################################

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m'

# 配置
GRADLE_VERSION="8.14.4"
AGP_VERSION="8.5.0"
BUILD_TYPE="${1:-debug}"

# 打印函数
print_header() {
    echo ""
    echo -e "${BLUE}═══════════════════════════════════════════════════════${NC}"
    echo -e "${BLUE}  $1${NC}"
    echo -e "${BLUE}═══════════════════════════════════════════════════════${NC}"
    echo ""
}

print_step() {
    echo -e "${CYAN}[步骤 $1]${NC} $2"
}

print_success() {
    echo -e "${GREEN}✓ 成功:${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}⚠ 警告:${NC} $1"
}

print_error() {
    echo -e "${RED}✗ 错误:${NC} $1"
}

print_info() {
    echo -e "${BLUE}ℹ 信息:${NC} $1"
}

# 主函数
main() {
    print_header "OMaster Android APK 一键构建"
    
    echo "项目版本: 2.0.0"
    echo "构建类型: $BUILD_TYPE"
    echo "Gradle 版本: $GRADLE_VERSION"
    echo "AGP 版本: $AGP_VERSION"
    echo ""
    
    # 步骤 1: 检查 Java 环境
    print_step "1" "检查 Java 环境"
    if command -v java &> /dev/null; then
        JAVA_VERSION=$(java -version 2>&1 | head -1 | cut -d'"' -f2)
        print_success "Java $JAVA_VERSION"
        
        if [[ "$JAVA_VERSION" =~ ^21\. ]]; then
            print_success "Java 21 符合要求 ✓"
        else
            print_warning "建议使用 Java 21，当前版本可能存在兼容性问题"
        fi
    else
        print_error "Java 未安装，请先安装 Java 21"
        exit 1
    fi
    echo ""
    
    # 步骤 2: 下载 Gradle Wrapper
    print_step "2" "下载 Gradle Wrapper"
    WRAPPER_ZIP="gradle/wrapper/gradle-${GRADLE_VERSION}-bin.zip"
    
    if [ -f "$WRAPPER_ZIP" ]; then
        SIZE=$(stat -f%z "$WRAPPER_ZIP" 2>/dev/null || stat -c%s "$WRAPPER_ZIP" 2>/dev/null)
        if [ "$SIZE" -gt 100000000 ]; then
            print_success "Gradle Wrapper 已存在 (${SIZE} bytes)"
        else
            print_warning "Gradle Wrapper 文件过小，重新下载..."
            rm -f "$WRAPPER_ZIP"
            curl -L --retry 3 -o "$WRAPPER_ZIP" \
                "https://mirrors.aliyun.com/gradle/gradle-${GRADLE_VERSION}-bin.zip"
            print_success "Gradle Wrapper 下载完成"
        fi
    else
        print_info "正在下载 Gradle Wrapper..."
        curl -L --retry 3 -o "$WRAPPER_ZIP" \
            "https://mirrors.aliyun.com/gradle/gradle-${GRADLE_VERSION}-bin.zip"
        print_success "Gradle Wrapper 下载完成"
    fi
    echo ""
    
    # 步骤 3: 配置 Android SDK
    print_step "3" "配置 Android SDK"
    
    # 设置 ANDROID_HOME
    if [ -z "$ANDROID_HOME" ]; then
        if [ -d "$HOME/Android/Sdk" ]; then
            export ANDROID_HOME="$HOME/Android/Sdk"
        elif [ -d "/opt/android-sdk" ]; then
            export ANDROID_HOME="/opt/android-sdk"
        else
            print_warning "ANDROID_HOME 未设置，尝试使用默认位置..."
            mkdir -p "$HOME/Android/Sdk"
            export ANDROID_HOME="$HOME/Android/Sdk"
        fi
    fi
    
    print_success "ANDROID_HOME: $ANDROID_HOME"
    
    # 创建 local.properties
    echo "sdk.dir=$ANDROID_HOME" > local.properties
    print_success "local.properties 已创建"
    
    # 检查必要的 SDK 组件
    SDK_MANAGER="$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager"
    
    if [ ! -d "$ANDROID_HOME/platforms/android-36" ]; then
        print_info "安装 Android 36 platform..."
        
        if [ ! -f "$SDK_MANAGER" ]; then
            print_info "安装 Android command line tools..."
            mkdir -p "$ANDROID_HOME/cmdline-tools"
            cd "$ANDROID_HOME/cmdline-tools"
            curl -L -o cmdline-tools.zip \
                "https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip"
            unzip -q cmdline-tools.zip
            mv cmdline-tools latest
            rm cmdline-tools.zip
            cd - > /dev/null
        fi
        
        yes | "$SDK_MANAGER" --licenses > /dev/null 2>&1 || true
        "$SDK_MANAGER" "platforms;android-36" "build-tools;36.0.0" "platform-tools"
        print_success "Android SDK 组件安装完成"
    else
        print_success "Android 36 platform 已存在"
    fi
    echo ""
    
    # 步骤 4: 构建项目
    print_step "4" "构建 $BUILD_TYPE APK"
    print_info "这可能需要 5-15 分钟（首次构建）..."
    echo ""
    
    # 确保 gradlew 可执行
    chmod +x ./gradlew
    
    # 执行构建
    case $BUILD_TYPE in
        debug)
            ./gradlew clean assembleDebug --no-daemon --stacktrace
            ;;
        release)
            if [ ! -f "app/release.keystore" ]; then
                print_warning "Release 签名文件不存在，使用默认配置"
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
    
    # 步骤 5: 验证构建结果
    print_step "5" "验证构建结果"
    
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
        
        print_header "🎉 构建成功!"
        echo "APK 路径: $(pwd)/$APK_PATH"
        echo "APK 大小: ${APK_SIZE_MB} MB"
        echo ""
        
        # 显示 APK 信息
        if command -v aapt &> /dev/null; then
            print_info "APK 详细信息:"
            echo ""
            aapt dump badging "$APK_PATH" | grep -E \
                "package:|application-label:|sdkVersion:|targetSdkVersion:" || true
        fi
        
        echo ""
        print_success "构建完成!"
        print_info "APK 位置: $APK_PATH"
        
        # 询问是否上传
        echo ""
        read -p "是否将 APK 上传到项目仓库? (y/n) " -n 1 -r
        echo ""
        if [[ $REPLY =~ ^[Yy]$ ]]; then
            print_info "上传 APK..."
            # 添加上传命令
            # git add $APK_PATH
            # git commit -m "Add debug APK"
            # git push
            print_success "APK 已准备好上传"
        fi
    else
        print_header "✗ 构建失败"
        print_error "APK 文件未生成"
        print_info "请检查构建日志获取详细信息"
        exit 1
    fi
}

# 运行主函数
main "$@"
