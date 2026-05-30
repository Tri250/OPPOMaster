#!/bin/bash

# OPPOMaster 同步脚本
# 用于同步 Web 端和 Android 端代码

set -e

echo "=========================================="
echo "OPPOMaster 同步脚本"
echo "基准版本：25e32e6"
echo "=========================================="
echo ""

# 颜色定义
GREEN='\033[0;32m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# 检查当前分支
current_branch=$(git branch --show-current)
echo -e "${BLUE}当前分支：${NC} $current_branch"
echo ""

# 检查是否有未提交的更改
if [ -n "$(git status --porcelain)" ]; then
    echo -e "${RED}错误：存在未提交的更改${NC}"
    git status
    exit 1
fi

# 确认操作
echo -e "${YELLOW}即将执行以下操作：${NC}"
echo "1. 确保代码版本：25e32e6"
echo "2. 推送到 develop 分支"
echo "3. 合并到 main 分支"
echo ""

read -p "确认继续? (y/n) " -n 1 -r
echo ""
if [[ ! $REPLY =~ ^[Yy]$ ]]
then
    echo "操作已取消"
    exit 1
fi

# 确保在正确的版本
echo -e "${BLUE}检查版本...${NC}"
current_commit=$(git rev-parse --short HEAD)
if [ "$current_commit" != "25e32e6" ]; then
    echo -e "${RED}警告：当前版本不是 25e32e6${NC}"
    read -p "是否强制重置到 25e32e6? (y/n) " -n 1 -r
    echo ""
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        git reset --hard 25e32e6
        echo -e "${GREEN}已重置到 25e32e6${NC}"
    else
        echo "操作已取消"
        exit 1
    fi
else
    echo -e "${GREEN}版本检查通过${NC}"
fi

echo ""

# 切换到 develop 分支并推送
echo -e "${BLUE}切换到 develop 分支...${NC}"
git checkout develop || git checkout -b develop
git pull origin develop || echo "develop 分支不存在，将创建新分支"
git push -u origin develop
echo -e "${GREEN}develop 分支已更新${NC}"

echo ""

# 合并到 main 分支
echo -e "${BLUE}合并到 main 分支...${NC}"
git checkout main
git merge develop --no-edit
git push origin main
echo -e "${GREEN}main 分支已更新${NC}"

echo ""
echo -e "${GREEN}=========================================="
echo "同步完成！"
echo "==========================================${NC}"
echo ""
echo "当前状态："
echo "- 本地 main: $(git rev-parse --short HEAD)"
echo "- 远程 main: 已推送"
echo "- develop: 已推送"
echo ""
echo "下一步："
echo "1. 在 GitHub 上创建 Pull Request (可选)"
echo "2. 在 Android Studio 中同步项目"
echo "3. 在 VS Code 中重启开发服务器"
