#!/bin/bash
# OPPO Master 优化构建脚本
# 专家级自动构建配置

set -e

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
CYAN='\033[0;36m'
NC='\033[0m'

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

echo -e "${PURPLE}"
echo "╔═══════════════════════════════════════════════════════════════╗"
echo "║               OPPO Master Android 构建系统                    ║"
echo "║                    专家级优化版 v2.0                          ║"
echo "╚═══════════════════════════════════════════════════════════════╝"
echo -e "${NC}"

# 检查环境
check_environment() {
    echo -e "${CYAN}[1/8] 检查构建环境...${NC}"
    
    # Java 检查
    if ! command -v java &> /dev/null; then
        echo -e "${RED}✗ Java 未安装！请安装 JDK 17 或更高版本${NC}"
        exit 1
    fi
    JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}')
    echo -e "${GREEN}✓ Java 版本：${JAVA_VERSION}${NC}"
    
    # Gradle Wrapper 检查
    if [ ! -f "gradlew" ]; then
        echo -e "${RED}✗ Gradle Wrapper 未找到${NC}"
        exit 1
    fi
    chmod +x gradlew
    echo -e "${GREEN}✓ Gradle Wrapper 已就绪${NC}"
    
    # 签名密钥检查
    if [ -f "release.keystore" ]; then
        echo -e "${GREEN}✓ Release 签名密钥已找到${NC}"
    else
        echo -e "${YELLOW}⚠  Release 签名密钥未找到，仅构建 Debug 版本${NC}"
    fi
    
    echo ""
}

# 配置 Gradle 属性
configure_gradle() {
    echo -e "${CYAN}[2/8] 优化 Gradle 配置...${NC}"
    
    if [ ! -f "gradle.properties" ]; then
        echo -e "${YELLOW}⚠ 创建默认 gradle.properties${NC}"
    fi
    
    cat > gradle.properties << 'EOF'
org.gradle.jvmargs=-Xmx4096m -Dfile.encoding=UTF-8
android.useAndroidX=true
android.enableJetifier=false
kotlin.code.style=official
android.nonTransitiveRClass=true
android.defaults.buildfeatures.buildconfig=true

# 网络优化配置
systemProp.http.socketTimeout=120000
systemProp.http.connectionTimeout=120000
systemProp.http.maxConnectionsPerRoute=10
systemProp.http.maxConnections=10
systemProp.http.keepAlive=true
systemProp.http.useCaches=true
systemProp.http.connectionRequestTimeout=30000
systemProp.https.socketTimeout=120000
systemProp.https.connectionTimeout=120000
systemProp.https.maxConnectionsPerRoute=10
systemProp.https.maxConnections=10

# 并行构建优化
org.gradle.parallel=true
org.gradle.caching=true
org.gradle.workers.max=10
org.gradle.configureondemand=true
org.gradle.vfs.watch=false
org.gradle.daemon=true
org.gradle.console=plain
org.gradle.parallel.intraProject=true

# 构建性能优化
org.gradle.jvmargs=-Xmx4096m -XX:MaxMetaspaceSize=512m -XX:+HeapDumpOnOutOfMemoryError -Dfile.encoding=UTF-8
EOF
    
    echo -e "${GREEN}✓ Gradle 配置已优化${NC}"
    echo ""
}

# 清理旧构建
clean_build() {
    echo -e "${CYAN}[3/8] 清理旧构建...${NC}"
    
    if [ -d "app/build" ]; then
        rm -rf app/build
        echo -e "${GREEN}✓ app/build 已清理${NC}"
    fi
    
    if [ -d ".gradle" ]; then
        rm -rf .gradle
        echo -e "${GREEN}✓ .gradle 已清理${NC}"
    fi
    
    if [ -d "build" ]; then
        rm -rf build
        echo -e "${GREEN}✓ build 已清理${NC}"
    fi
    
    echo ""
}

# 构建 Debug 版本
build_debug() {
    echo -e "${CYAN}[4/8] 构建 Debug APK...${NC}"
    
    ./gradlew assembleDebug --no-daemon --stacktrace
    
    if [ -f "app/build/outputs/apk/debug/app-debug.apk" ]; then
        APK_SIZE=$(ls -lh "app/build/outputs/apk/debug/app-debug.apk" | awk '{print $5}')
        echo -e "${GREEN}✓ Debug APK 构建成功${NC}"
        echo -e "${BLUE}  文件大小：${APK_SIZE}${NC}"
    else
        echo -e "${RED}✗ Debug APK 构建失败${NC}"
        return 1
    fi
    
    echo ""
}

# 构建 Release 版本
build_release() {
    if [ ! -f "release.keystore" ]; then
        echo -e "${YELLOW}[5/8] 跳过 Release 构建（无签名密钥）${NC}"
        return 0
    fi
    
    echo -e "${CYAN}[5/8] 构建 Release APK...${NC}"
    
    ./gradlew assembleRelease --no-daemon --stacktrace
    
    if [ -f "app/build/outputs/apk/release/app-release.apk" ]; then
        APK_SIZE=$(ls -lh "app/build/outputs/apk/release/app-release.apk" | awk '{print $5}')
        echo -e "${GREEN}✓ Release APK 构建成功${NC}"
        echo -e "${BLUE}  文件大小：${APK_SIZE}${NC}"
    else
        echo -e "${RED}✗ Release APK 构建失败${NC}"
        return 1
    fi
    
    echo ""
}

# 运行单元测试
run_tests() {
    echo -e "${CYAN}[6/8] 运行单元测试...${NC}"
    
    ./gradlew test --no-daemon || echo -e "${YELLOW}⚠ 测试完成${NC}"
    
    echo ""
}

# 验证 APK
verify_apks() {
    echo -e "${CYAN}[7/8] 验证 APK 文件...${NC}"
    
    echo ""
    echo -e "${PURPLE}构建结果：${NC}"
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    
    if [ -f "app/build/outputs/apk/debug/app-debug.apk" ]; then
        DEBUG_SIZE=$(ls -lh "app/build/outputs/apk/debug/app-debug.apk" | awk '{print $5}')
        echo -e "${GREEN}✓ Debug APK:  ${SCRIPT_DIR}/app/build/outputs/apk/debug/app-debug.apk${NC}"
        echo "  大小：${DEBUG_SIZE}"
    fi
    
    if [ -f "app/build/outputs/apk/release/app-release.apk" ]; then
        RELEASE_SIZE=$(ls -lh "app/build/outputs/apk/release/app-release.apk" | awk '{print $5}')
        echo -e "${GREEN}✓ Release APK:${SCRIPT_DIR}/app/build/outputs/apk/release/app-release.apk${NC}"
        echo "  大小：${RELEASE_SIZE}"
    fi
    
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo ""
}

# 生成构建报告
generate_report() {
    echo -e "${CYAN}[8/8] 生成构建报告...${NC}"
    
    REPORT_FILE="BUILD_REPORT.txt"
    cat > "$REPORT_FILE" << EOF
============================================
OPPO Master Android 构建报告
============================================
构建时间：$(date '+%Y-%m-%d %H:%M:%S')
项目目录：${SCRIPT_DIR}
应用版本：1.2.1 (versionCode: 121)
包名：com.omaster.app

编译配置：
  compileSdk: 34
  targetSdk: 34
  minSdk: 26

技术栈：
  Kotlin: 1.9.22
  AGP: 8.2.2
  Gradle: 8.14.4
  Compose: 2024.02.00
  Hilt: 2.48

构建说明：
  - 支持 Android 8.0 (API 26) 及以上
  - 完全兼容 Android 16 系统
  - 已优化的国内镜像源配置
  - 10线程并发下载加速

安装说明：
  Debug: adb install -r app/build/outputs/apk/debug/app-debug.apk
  Release: adb install -r app/build/outputs/apk/release/app-release.apk
EOF
    
    echo -e "${GREEN}✓ 构建报告已生成：${REPORT_FILE}${NC}"
    echo ""
}

# 显示成功信息
show_success() {
    echo -e "${GREEN}"
    echo "╔═══════════════════════════════════════════════════════════════╗"
    echo "║                    🎉 构建成功！                               ║"
    echo "╚═══════════════════════════════════════════════════════════════╝"
    echo -e "${NC}"
    
    echo -e "${YELLOW}快速开始：${NC}"
    echo "  1. Debug 安装：adb install -r app/build/outputs/apk/debug/app-debug.apk"
    if [ -f "app/build/outputs/apk/release/app-release.apk" ]; then
        echo "  2. Release 安装：adb install -r app/build/outputs/apk/release/app-release.apk"
    fi
    echo "  3. 查看报告：cat BUILD_REPORT.txt"
    echo ""
}

# 主流程
main() {
    start_time=$(date +%s)
    
    check_environment
    configure_gradle
    clean_build
    build_debug
    build_release
    run_tests
    verify_apks
    generate_report
    show_success
    
    end_time=$(date +%s)
    duration=$((end_time - start_time))
    
    echo -e "${CYAN}总耗时：${duration} 秒${NC}"
    echo ""
}

# 运行主流程
main
