#!/bin/bash

# 完整的 Android Gradle Plugin 下载脚本
# 支持 AGP 8.5.0

set -e

AGP_VERSION="8.5.0"
LOCAL_REPO="$HOME/.m2/repository"

echo "========================================"
echo "Android Gradle Plugin $AGP_VERSION 下载器"
echo "========================================"
echo ""

# Android Gradle Plugin (正确的 Maven 坐标)
AGP_ARTIFACTS=(
    "com/android/application/gradle/${AGP_VERSION}/gradle-${AGP_VERSION}.jar"
    "com/android/application/gradle/${AGP_VERSION}/gradle-${AGP_VERSION}.pom"
)

# 核心依赖
CORE_ARTIFACTS=(
    "com/android/tools/build/gradle/${AGP_VERSION}/gradle-${AGP_VERSION}.jar"
    "com/android/tools/build/gradle/${AGP_VERSION}/gradle-${AGP_VERSION}.pom"
    "com/android/tools/build/gradle-core/${AGP_VERSION}/gradle-core-${AGP_VERSION}.jar"
    "com/android/tools/build/gradle-core/${AGP_VERSION}/gradle-core-${AGP_VERSION}.pom"
    "com/android/tools/build/builder-model/${AGP_VERSION}/builder-model-${AGP_VERSION}.jar"
    "com/android/tools/build/builder-model/${AGP_VERSION}/builder-model-${AGP_VERSION}.pom"
    "com/android/tools/build/builder-test-api/${AGP_VERSION}/builder-test-api-${AGP_VERSION}.jar"
    "com/android/tools/build/builder-test-api/${AGP_VERSION}/builder-test-api-${AGP_VERSION}.pom"
    "com/android/tools/build/manifest-merger/${AGP_VERSION}/manifest-merger-${AGP_VERSION}.jar"
    "com/android/tools/build/manifest-merger/${AGP_VERSION}/manifest-merger-${AGP_VERSION}.pom"
    "com/android/tools/build/aapt2-proto/${AGP_VERSION}/aapt2-proto-${AGP_VERSION}.jar"
    "com/android/tools/build/aapt2-proto/${AGP_VERSION}/aapt2-proto-${AGP_VERSION}.pom"
    "com/android/tools/build/apksig/${AGP_VERSION}/apksig-${AGP_VERSION}.jar"
    "com/android/tools/build/apksig/${AGP_VERSION}/apksig-${AGP_VERSION}.pom"
    "com/android/tools/build/apkzlib/${AGP_VERSION}/apkzlib-${AGP_VERSION}.jar"
    "com/android/tools/build/apkzlib/${AGP_VERSION}/apkzlib-${AGP_VERSION}.pom"
    "com/android/tools/build/transform-api/2.0.0-deprecated-use-new-api/transform-api-2.0.0-deprecated-use-new-api.jar"
    "com/android/tools/build/transform-api/2.0.0-deprecated-use-new-api/transform-api-2.0.0-deprecated-use-new-api.pom"
)

# Google Maven 仓库
GOOGLE_MAVEN="https://dl.google.com/dl/android/maven2"

download_artifact() {
    local artifact="$1"
    local filename=$(basename "$artifact")
    local dir="$LOCAL_REPO/$(dirname "$artifact")"

    mkdir -p "$dir"

    if [ ! -f "$dir/$filename" ]; then
        echo "  下载: $artifact"
        curl -sL "${GOOGLE_MAVEN}/${artifact}" -o "$dir/$filename" --connect-timeout 10 --max-time 60
        if [ $? -eq 0 ]; then
            echo "    ✓ 完成"
        else
            echo "    ✗ 失败"
        fi
    else
        echo "  已存在: $filename"
    fi
}

echo "开始下载 Android Gradle Plugin $AGP_VERSION..."
echo ""

echo "1. 下载 Android Gradle Plugin..."
for artifact in "${AGP_ARTIFACTS[@]}"; do
    download_artifact "$artifact"
done

echo ""
echo "2. 下载核心依赖..."
for artifact in "${CORE_ARTIFACTS[@]}"; do
    download_artifact "$artifact"
done

echo ""
echo "========================================"
echo "下载完成！"
echo "========================================"
echo ""
echo "本地 Maven 仓库: $LOCAL_REPO"
echo ""
echo "注意: 如果构建仍然失败，可能需要下载更多传递依赖。"
echo "可以使用 --online 模式让 Gradle 自动下载缺失的依赖。"
