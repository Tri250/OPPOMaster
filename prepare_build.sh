#!/bin/bash

################################################################################
# OMaster APK 构建准备脚本
# 用于在受限网络环境中准备构建环境
################################################################################

set -e

echo "=================================="
echo "OMaster APK 构建准备脚本"
echo "=================================="
echo ""

# 1. 检查 Java 环境
echo "[1/5] 检查 Java 环境..."
if command -v java &> /dev/null; then
    JAVA_VERSION=$(java -version 2>&1 | head -1 | cut -d'"' -f2)
    echo "✓ Java: $JAVA_VERSION"
    
    if [[ "$JAVA_VERSION" =~ ^21\. ]]; then
        echo "✓ Java 21 已安装，符合要求"
    else
        echo "⚠ 建议使用 Java 21，当前版本: $JAVA_VERSION"
    fi
else
    echo "✗ Java 未安装"
    exit 1
fi
echo ""

# 2. 检查 Gradle 环境
echo "[2/5] 检查 Gradle 环境..."
if command -v gradle &> /dev/null; then
    GRADLE_VERSION=$(gradle --version | head -1 | awk '{print $2}')
    echo "✓ Gradle: $GRADLE_VERSION"
    
    if [[ "$GRADLE_VERSION" == "8.14.4" ]]; then
        echo "✓ Gradle 8.14.4 已安装，符合要求"
    else
        echo "⚠ 建议使用 Gradle 8.14.4，当前版本: $GRADLE_VERSION"
    fi
else
    echo "✗ Gradle 未安装"
    exit 1
fi
echo ""

# 3. 检查 Android SDK
echo "[3/5] 检查 Android SDK..."
if [ -n "$ANDROID_HOME" ]; then
    echo "✓ ANDROID_HOME: $ANDROID_HOME"
    
    if [ -d "$ANDROID_HOME/platforms/android-36" ]; then
        echo "✓ Android 36 platform 已安装"
    else
        echo "⚠ Android 36 platform 未安装"
        echo "  运行: sdkmanager platforms;android-36"
    fi
    
    if [ -d "$ANDROID_HOME/build-tools/36.0.0" ]; then
        echo "✓ Build tools 36.0.0 已安装"
    else
        echo "⚠ Build tools 36.0.0 未安装"
        echo "  运行: sdkmanager build-tools;36.0.0"
    fi
else
    echo "⚠ ANDROID_HOME 未设置"
    
    if [ -d "$HOME/Android/Sdk" ]; then
        echo "  发现 Android SDK: $HOME/Android/Sdk"
        echo "  运行: export ANDROID_HOME=\$HOME/Android/Sdk"
    else
        echo "✗ Android SDK 未找到"
    fi
fi
echo ""

# 4. 检查 Gradle Wrapper
echo "[4/5] 检查 Gradle Wrapper..."
if [ -f "gradle/wrapper/gradle-wrapper.jar" ]; then
    echo "✓ gradle-wrapper.jar 已存在"
    
    if [ -f "gradle/wrapper/gradle-8.14.4-bin.zip" ]; then
        SIZE=$(stat -f%z "gradle/wrapper/gradle-8.14.4-bin.zip" 2>/dev/null || stat -c%s "gradle/wrapper/gradle-8.14.4-bin.zip" 2>/dev/null)
        if [ "$SIZE" -gt 10000 ]; then
            echo "✓ gradle-8.14.4-bin.zip 已下载 (${SIZE} bytes)"
        else
            echo "⚠ gradle-8.14.4-bin.zip 文件过小，需要重新下载"
        fi
    else
        echo "⚠ gradle-8.14.4-bin.zip 不存在"
        echo "  运行: curl -L -o gradle/wrapper/gradle-8.14.4-bin.zip \\"
        echo "           https://services.gradle.org/distributions/gradle-8.14.4-bin.zip"
    fi
else
    echo "⚠ gradle-wrapper.jar 不存在"
fi
echo ""

# 5. 检查项目结构
echo "[5/5] 检查项目结构..."
if [ -f "settings.gradle.kts" ]; then
    echo "✓ settings.gradle.kts 存在"
else
    echo "✗ settings.gradle.kts 不存在"
    exit 1
fi

if [ -f "app/build.gradle.kts" ]; then
    echo "✓ app/build.gradle.kts 存在"
else
    echo "✗ app/build.gradle.kts 不存在"
    exit 1
fi

if [ -f "gradlew" ]; then
    echo "✓ gradlew 存在"
    chmod +x gradlew
else
    echo "⚠ gradlew 不存在"
fi
echo ""

# 生成构建命令
echo "=================================="
echo "环境检查完成"
echo "=================================="
echo ""
echo "构建命令："
echo ""
echo "  Debug 构建:"
echo "    ./gradlew clean assembleDebug --no-daemon"
echo ""
echo "  Release 构建:"
echo "    ./gradlew clean assembleRelease --no-daemon"
echo ""
echo "  快速构建（跳过测试）:"
echo "    ./gradlew assembleDebug -x test -x lint --no-daemon"
echo ""
echo "=================================="
echo ""

# 检查网络连接
echo "网络连接检查..."
if ping -c 1 services.gradle.org &> /dev/null; then
    echo "✓ 可以连接到 services.gradle.org"
elif curl -sf https://services.gradle.org &> /dev/null; then
    echo "✓ 可以通过 HTTPS 连接到 services.gradle.org"
else
    echo "⚠ 网络连接受限，无法下载依赖"
    echo ""
    echo "解决方案："
    echo "1. 在具有网络连接的环境中运行构建"
    echo "2. 配置 VPN 或代理"
    echo "3. 使用国内镜像源"
    echo ""
fi
echo ""

# 生成本地缓存说明
echo "=================================="
echo "依赖缓存说明"
echo "=================================="
echo ""
echo "如果需要在多台机器上构建，可以导出依赖缓存："
echo ""
echo "  导出:"
echo "    tar -czf gradle-cache.tar.gz ~/.gradle/caches/"
echo ""
echo "  导入:"
echo "    tar -xzf gradle-cache.tar.gz -C ~/.gradle/"
echo ""
echo "=================================="
