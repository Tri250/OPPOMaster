#!/bin/bash
# OMaster 完整构建脚本 - 带详细日志

set -e

PROJECT_DIR=$(pwd)
BUILD_LOG="$PROJECT_DIR/build.log"

echo "=========================================="
echo "  OMaster Android APK 本地构建脚本"
echo "=========================================="
echo "构建日志将保存到: $BUILD_LOG"
echo ""

# 记录日志函数
log() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] $1" | tee -a "$BUILD_LOG"
}

# 1. 检查签名密钥
log "[1/7] 检查签名密钥..."
if [ ! -f "$PROJECT_DIR/app/debug.keystore" ]; then
    log "生成Debug签名密钥..."
    cd "$PROJECT_DIR/app"
    keytool -genkey -v -keystore debug.keystore \
        -storepass android \
        -alias androiddebugkey \
        -keypass android \
        -keyalg RSA -keysize 2048 -validity 10000 \
        -dname "CN=Android Debug,O=Android,C=US" >> "$BUILD_LOG" 2>&1 || true
    cd "$PROJECT_DIR"
    log "✅ 签名密钥已生成"
else
    log "✅ 签名密钥已存在"
fi

# 2. 检查Gradle版本
log "[2/7] 检查Gradle版本..."
if command -v gradle &> /dev/null; then
    GRADLE_VERSION=$(gradle --version | head -n 1 | awk '{print $2}')
    log "✅ Gradle已安装: $GRADLE_VERSION"
else
    log "⚠️ Gradle未安装，将使用wrapper"
fi

# 3. 检查Android SDK
log "[3/7] 检查Android SDK..."
if [ -n "$ANDROID_HOME" ]; then
    log "✅ ANDROID_HOME已设置: $ANDROID_HOME"
elif [ -n "$ANDROID_SDK_ROOT" ]; then
    log "✅ ANDROID_SDK_ROOT已设置: $ANDROID_SDK_ROOT"
    export ANDROID_HOME=$ANDROID_SDK_ROOT
else
    log "⚠️ 未检测到Android SDK环境变量"
    log "请确保已安装Android SDK并设置ANDROID_HOME"
fi

# 4. 检查构建配置
log "[4/7] 检查构建配置..."
if [ -f "$PROJECT_DIR/app/build.gradle.kts" ]; then
    log "✅ app/build.gradle.kts存在"
    if grep -q "versionCode = 121" "$PROJECT_DIR/app/build.gradle.kts"; then
        log "✅ 版本配置正确: 1.2.1 (121)"
    fi
else
    log "❌ app/build.gradle.kts不存在"
    exit 1
fi

# 5. 准备构建输出目录
log "[5/7] 准备构建输出目录..."
mkdir -p "$PROJECT_DIR/app/build/outputs/apk/debug"
mkdir -p "$PROJECT_DIR/app/build/outputs/apk/release"
log "✅ 构建输出目录已准备"

# 6. 尝试构建
log "[6/7] 尝试构建APK..."
BUILD_SUCCESS=false

# 方法1: 使用本地Gradle
if command -v gradle &> /dev/null; then
    log "使用本地Gradle构建..."
    if gradle assembleDebug --no-daemon >> "$BUILD_LOG" 2>&1; then
        BUILD_SUCCESS=true
        log "✅ Gradle构建成功"
    else
        log "⚠️ Gradle构建失败，尝试其他方法..."
    fi
fi

# 方法2: 使用Gradle Wrapper
if [ "$BUILD_SUCCESS" = false ]; then
    log "尝试使用Gradle Wrapper..."
    if ./gradlew assembleDebug --no-daemon >> "$BUILD_LOG" 2>&1; then
        BUILD_SUCCESS=true
        log "✅ Gradle Wrapper构建成功"
    else
        log "⚠️ Gradle Wrapper构建失败"
    fi
fi

# 7. 验证APK
log "[7/7] 验证APK..."
APK_PATH="$PROJECT_DIR/app/build/outputs/apk/debug/app-debug.apk"
if [ -f "$APK_PATH" ]; then
    APK_SIZE=$(du -h "$APK_PATH" | cut -f1)
    log "✅ APK构建成功！"
    log "文件位置: $APK_PATH"
    log "文件大小: $APK_SIZE"
    
    # 生成APK信息
    cat > "$PROJECT_DIR/app/build/outputs/apk/debug/APK_INFO.txt" << EOF
==========================================
OMaster APK 构建信息
==========================================

应用名称: OMaster (小O帮帮)
包名: com.omaster.app
版本: 1.2.1 (121)
构建类型: Debug
构建日期: $(date '+%Y-%m-%d %H:%M:%S')

文件信息:
- 文件路径: $APK_PATH
- 文件大小: $APK_SIZE

Android配置:
- compileSdk: 35 (Android 16)
- targetSdk: 35 (Android 16)
- minSdk: 26 (Android 8.0+)

签名信息:
- 签名类型: Debug
- 算法: RSA 2048
- 有效期: 10000天

功能特性:
- 哈苏影像预设库
- AI场景识别
- 水印编辑器
- 云同步系统

兼容性:
- 最低Android: 8.0 (API 26)
- 推荐Android: 11+ (API 30+)
- 最佳体验: Android 16 (API 35)

==========================================
EOF
    log "✅ APK信息文件已生成"
    
else
    log "❌ APK构建失败"
    log ""
    log "=========================================="
    log "  构建日志位置: $BUILD_LOG"
    log "=========================================="
    log ""
    log "可能的原因:"
    log "1. 网络连接问题 - 无法下载Gradle依赖"
    log "2. Android SDK未安装或未配置"
    log "3. Java版本不兼容"
    log ""
    log "建议解决方案:"
    log "1. 使用Android Studio打开项目进行构建"
    log "2. 配置Android SDK环境"
    log "3. 检查网络连接"
    exit 1
fi

# 总结
echo ""
echo "=========================================="
echo "  🎉 构建完成！"
echo "=========================================="
echo ""
echo "APK文件位置:"
echo "$APK_PATH"
echo ""
echo "APK大小: $APK_SIZE"
echo ""
echo "下一步:"
echo "1. 将APK传输到Android设备"
echo "2. 在设备上安装APK"
echo "3. 启用开发者选项和USB调试"
echo "4. 享受哈苏影像体验！"
echo ""
echo "详细构建日志: $BUILD_LOG"
echo "=========================================="
