#!/bin/bash
# Gradle 8.5 修复脚本
# 用于下载完整的 Gradle 8.5 分发文件

set -e

echo "======================================"
echo "Gradle 8.5 修复脚本"
echo "======================================"
echo ""

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 工作目录
WORKDIR="/workspace"
GRADLE_FILE="$WORKDIR/gradle-8.5-bin.zip"
GRADLE_CACHE="/root/.gradle/wrapper/dists/gradle-8.5-bin"

echo "步骤 1/5: 清理旧文件..."
if [ -f "$GRADLE_FILE" ]; then
    rm -f "$GRADLE_FILE"
    echo -e "${YELLOW}已删除损坏的 gradle-8.5-bin.zip${NC}"
fi

if [ -d "$GRADLE_CACHE" ]; then
    rm -rf "$GRADLE_CACHE"
    echo -e "${YELLOW}已清理 Gradle 缓存${NC}"
fi

echo ""
echo "步骤 2/5: 选择下载源..."
echo "1. 腾讯云镜像 (推荐)"
echo "2. 阿里云镜像"
echo "3. 官方源"
echo ""
read -p "请选择下载源 [1-3]: " choice

case $choice in
    1)
        GRADLE_URL="https://mirrors.cloud.tencent.com/gradle/gradle-8.5-bin.zip"
        echo -e "${GREEN}使用腾讯云镜像${NC}"
        ;;
    2)
        GRADLE_URL="https://maven.aliyun.com/repository/central/org/gradle/gradle/8.5/gradle-8.5-bin.zip"
        echo -e "${GREEN}使用阿里云镜像${NC}"
        ;;
    3)
        GRADLE_URL="https://services.gradle.org/distributions/gradle-8.5-bin.zip"
        echo -e "${YELLOW}使用官方源（可能较慢）${NC}"
        ;;
    *)
        echo -e "${RED}无效选择，使用腾讯云镜像${NC}"
        GRADLE_URL="https://mirrors.cloud.tencent.com/gradle/gradle-8.5-bin.zip"
        ;;
esac

echo ""
echo "步骤 3/5: 下载 Gradle 8.5..."
echo "下载链接: $GRADLE_URL"
echo ""

# 使用 wget 下载
if command -v wget &> /dev/null; then
    wget --show-progress --progress=bar:force -c "$GRADLE_URL" -O "$GRADLE_FILE"
else
    # 使用 curl 下载
    curl -L -# -C - "$GRADLE_URL" -o "$GRADLE_FILE"
fi

echo ""
echo "步骤 4/5: 验证文件完整性..."

# 检查文件大小（Gradle 8.5 应该约 130MB）
FILE_SIZE=$(stat -c%s "$GRADLE_FILE" 2>/dev/null || stat -f%z "$GRADLE_FILE")
EXPECTED_SIZE=130000000  # 约 130MB

if [ "$FILE_SIZE" -lt "$EXPECTED_SIZE" ]; then
    echo -e "${RED}文件大小异常：$(($FILE_SIZE / 1024 / 1024))MB (预期约 130MB)${NC}"
    echo -e "${RED}下载可能不完整，请检查网络连接${NC}"
    exit 1
else
    echo -e "${GREEN}文件大小正常：$(($FILE_SIZE / 1024 / 1024))MB${NC}"
fi

# 验证 ZIP 文件
if unzip -t "$GRADLE_FILE" &> /dev/null; then
    echo -e "${GREEN}ZIP 文件验证通过${NC}"
else
    echo -e "${RED}ZIP 文件损坏${NC}"
    exit 1
fi

echo ""
echo "步骤 5/5: 验证 Gradle..."

cd "$WORKDIR"
./gradlew --version

echo ""
echo "======================================"
echo -e "${GREEN}✅ Gradle 8.5 修复成功！${NC}"
echo "======================================"
echo ""
echo "下一步操作:"
echo "1. 构建 Debug 版本：./gradlew assembleDebug"
echo "2. 构建 Release 版本：./gradlew assembleRelease"
echo "3. 运行测试：./gradlew test"
echo ""
