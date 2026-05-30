#!/bin/bash

################################################################################
# Android Gradle 依赖下载脚本
# 下载所有必要的依赖到本地缓存
################################################################################

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

AGP_VERSION="8.5.0"
KOTLIN_VERSION="2.0.0"
COMPOSE_BOM="2024.06.00"

echo -e "${BLUE}================================================================${NC}"
echo -e "${BLUE}  Android Gradle 依赖下载器${NC}"
echo -e "${BLUE}================================================================${NC}\n"

# 镜像源
MIRRORS=(
    "https://maven.aliyun.com/repository/google"
    "https://maven.aliyun.com/repository/central"
    "https://dl.google.com/dl/android/maven2"
)

download_file() {
    local url="$1"
    local dest="$2"
    
    mkdir -p "$(dirname "$dest")"
    
    if [ -f "$dest" ] && [ $(stat -c%s "$dest" 2>/dev/null || echo 0) -gt 10000 ]; then
        echo -e "${GREEN}✓${NC} 已存在: $(basename $dest)"
        return 0
    fi
    
    echo -n "下载: $(basename $dest) ... "
    
    for mirror in "${MIRRORS[@]}"; do
        if curl -sfL "${mirror}${url}" -o "$dest" 2>/dev/null; then
            local size=$(stat -c%s "$dest" 2>/dev/null || echo 0)
            if [ "$size" -gt 10000 ]; then
                echo -e "${GREEN}✓${NC} (${size} bytes)"
                return 0
            fi
        fi
    done
    
    echo -e "${RED}✗${NC}"
    rm -f "$dest"
    return 1
}

# 本地缓存目录
CACHE_DIR="$HOME/.gradle/caches/modules-2/files-2.1"

echo "开始下载 Android Gradle Plugin ${AGP_VERSION}..."
echo ""

# Android Gradle Plugin
download_file "/com/android/application/gradle/${AGP_VERSION}/gradle-${AGP_VERSION}.jar" \
    "$CACHE_DIR/com.android.application.gradle.plugin/${AGP_VERSION}/gradle-${AGP_VERSION}.jar"

download_file "/com/android/application/gradle/${AGP_VERSION}/gradle-${AGP_VERSION}.pom" \
    "$CACHE_DIR/com.android.application.gradle.plugin/${AGP_VERSION}/gradle-${AGP_VERSION}.pom"

# Android Build Tools
BUILD_TOOLS_VERSION="36.0.0"
download_file "/com/android/tools/build/gradle/${AGP_VERSION}/gradle-${AGP_VERSION}.jar" \
    "$CACHE_DIR/com.android.tools.build.gradle/${AGP_VERSION}/gradle-${AGP_VERSION}.jar"

download_file "/com/android/tools/build/builder-model/${AGP_VERSION}/builder-model-${AGP_VERSION}.jar" \
    "$CACHE_DIR/com.android.tools.build.builder-model/${AGP_VERSION}/builder-model-${AGP_VERSION}.jar"

download_file "/com/android/tools/build/builder-test-api/${AGP_VERSION}/builder-test-api-${AGP_VERSION}.jar" \
    "$CACHE_DIR/com.android.tools.build.builder-test-api/${AGP_VERSION}/builder-test-api-${AGP_VERSION}.jar"

download_file "/com/android/tools/build/manifest-merger/${AGP_VERSION}/manifest-merger-${AGP_VERSION}.jar" \
    "$CACHE_DIR/com.android.tools.build.manifest-merger/${AGP_VERSION}/manifest-merger-${AGP_VERSION}.jar"

# Kotlin
download_file "/org/jetbrains/kotlin/kotlin-stdlib/${KOTLIN_VERSION}/kotlin-stdlib-${KOTLIN_VERSION}.jar" \
    "$CACHE_DIR/org.jetbrains.kotlin/kotlin-stdlib/${KOTLIN_VERSION}/kotlin-stdlib-${KOTLIN_VERSION}.jar"

download_file "/org/jetbrains/kotlin/kotlin-gradle-plugin/${KOTLIN_VERSION}/kotlin-gradle-plugin-${KOTLIN_VERSION}.jar" \
    "$CACHE_DIR/org.jetbrains.kotlin/kotlin-gradle-plugin/${KOTLIN_VERSION}/kotlin-gradle-plugin-${KOTLIN_VERSION}.jar"

echo ""
echo -e "${GREEN}================================================================${NC}"
echo -e "${GREEN}  下载完成!${NC}"
echo -e "${GREEN}================================================================${NC}\n"
echo "依赖已缓存到: $CACHE_DIR"
echo ""
echo "现在可以运行构建:"
echo "  ./build.sh debug"
echo ""
