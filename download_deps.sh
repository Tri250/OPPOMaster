#!/bin/bash

# Android Gradle Plugin 依赖下载脚本

set -e

GRADLE_PLUGIN_VERSION="8.5.0"
ANDROID_SDK_VERSION="36"

echo "开始下载 Android Gradle Plugin 依赖..."
echo ""

# 创建本地 Maven 仓库目录
LOCAL_REPO="$HOME/.m2/repository"
mkdir -p "$LOCAL_REPO"

# Android Gradle Plugin 工件
ARTIFACTS=(
    "com/android/tools/build/gradle/${GRADLE_PLUGIN_VERSION}/gradle-${GRADLE_PLUGIN_VERSION}.jar"
    "com/android/tools/build/gradle-core/${GRADLE_PLUGIN_VERSION}/gradle-core-${GRADLE_PLUGIN_VERSION}.jar"
    "com/android/tools/build/builder-model/${GRADLE_PLUGIN_VERSION}/builder-model-${GRADLE_PLUGIN_VERSION}.jar"
    "com/android/tools/build/builder-test-api/${GRADLE_PLUGIN_VERSION}/builder-test-api-${GRADLE_PLUGIN_VERSION}.jar"
    "com/android/tools/build/manifest-merger/${GRADLE_PLUGIN_VERSION}/manifest-merger-${GRADLE_PLUGIN_VERSION}.jar"
)

# Google Maven 仓库 URL
GOOGLE_MAVEN="https://dl.google.com/dl/android/maven2"

echo "下载 Android Gradle Plugin ${GRADLE_PLUGIN_VERSION}..."

for artifact in "${ARTIFACTS[@]}"; do
    filename=$(basename "$artifact")
    dir="$LOCAL_REPO/$(dirname "$artifact")"

    mkdir -p "$dir"

    if [ ! -f "$dir/$filename" ]; then
        echo "  下载: $artifact"
        curl -sL "${GOOGLE_MAVEN}/${artifact}" -o "$dir/$filename"
    else
        echo "  已存在: $filename"
    fi
done

echo ""
echo "下载完成!"
echo ""
echo "本地 Maven 仓库: $LOCAL_REPO"
echo ""
echo "请确保在项目中使用正确的仓库配置。"
