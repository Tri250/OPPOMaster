#!/bin/bash
# OPPO Master Android 16 - 终极构建脚本
# 国内镜像加速版 - 10线程下载

set -e

echo "========================================"
echo "🚀 OPPO Master - 国内镜像加速构建"
echo "   下载线程: 10"
echo "   镜像源: 10个国内镜像"
echo "========================================"
echo ""

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

# 步骤1: 检查环境
echo -e "${BLUE}[1/6] 检查构建环境...${NC}"

if ! command -v java &> /dev/null; then
    echo -e "${RED}❌ Java 未安装，请安装 JDK 17+${NC}"
    exit 1
fi
JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}')
echo -e "${GREEN}✅ Java 版本: ${JAVA_VERSION}${NC}"

if ! command -v adb &> /dev/null; then
    echo -e "${YELLOW}⚠️  ADB 未找到（可选，用于安装APK）${NC}"
else
    echo -e "${GREEN}✅ ADB 已找到${NC}"
fi

echo ""

# 步骤2: 配置全局 init.gradle
echo -e "${BLUE}[2/6] 配置全局镜像源...${NC}"

GRADLE_USER_HOME="${GRADLE_USER_HOME:-$HOME/.gradle}"
INIT_GRADLE="$GRADLE_USER_HOME/init.gradle"

if [ -f "$INIT_GRADLE" ]; then
    echo -e "${YELLOW}⚠️  已存在 init.gradle，备份中...${NC}"
    cp "$INIT_GRADLE" "$INIT_GRADLE.backup.$(date +%s)"
fi

cat > "$INIT_GRADLE" << 'EOF'
allprojects {
    repositories {
        maven { url 'https://maven.aliyun.com/repository/google' }
        maven { url 'https://maven.aliyun.com/repository/public' }
        maven { url 'https://maven.aliyun.com/repository/gradle-plugin' }
        maven { url 'https://maven.aliyun.com/repository/jcenter' }
        maven { url 'https://mirrors.cloud.tencent.com/nexus/repository/maven-public/' }
        maven { url 'https://repo.huaweicloud.com/repository/maven/' }
        maven { url 'https://mirrors.ustc.edu.cn/maven-mirror/' }
        google()
        mavenCentral()
    }
}
buildscript {
    repositories {
        maven { url 'https://maven.aliyun.com/repository/google' }
        maven { url 'https://maven.aliyun.com/repository/public' }
        maven { url 'https://maven.aliyun.com/repository/gradle-plugin' }
        maven { url 'https://mirrors.cloud.tencent.com/nexus/repository/maven-public/' }
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
EOF
echo -e "${GREEN}✅ 全局镜像源配置完成: $INIT_GRADLE${NC}"
echo ""

# 步骤3: 配置项目 gradle.properties
echo -e "${BLUE}[3/6] 优化网络配置 (10线程)...${NC}"

if [ ! -f "gradle.properties" ]; then
    echo -e "${YELLOW}⚠️  未找到 gradle.properties，将在当前目录查找...${NC}"
    cd "$(dirname "$0")"
fi

cat > gradle.properties << 'EOF'
org.gradle.jvmargs=-Xmx4096m -Dfile.encoding=UTF-8
android.useAndroidX=true
android.enableJetifier=false
kotlin.code.style=official
android.nonTransitiveRClass=true
android.defaults.buildfeatures.buildconfig=true

# ==========================================
# 网络优化配置 - 下载线程设为10
# ==========================================
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

# ==========================================
# 并行构建优化
# ==========================================
org.gradle.parallel=true
org.gradle.caching=true
org.gradle.workers.max=10
org.gradle.configureondemand=true
org.gradle.vfs.watch=false
org.gradle.daemon=false
org.gradle.console=verbose
org.gradle.parallel.intraProject=true
EOF
echo -e "${GREEN}✅ gradle.properties 优化完成${NC}"
echo ""

# 步骤4: 选择构建类型
echo -e "${BLUE}[4/6] 选择构建类型...${NC}"
echo "1. Debug APK (开发测试)"
echo "2. Release APK (正式发布)"
echo "3. 两者都构建"
echo ""
read -p "请选择 (1/2/3，默认 2): " BUILD_CHOICE
BUILD_CHOICE=${BUILD_CHOICE:-2}

# 步骤5: 执行构建
echo -e "${BLUE}[5/6] 开始构建...${NC}"

if [ ! -x "./gradlew" ]; then
    echo -e "${YELLOW}⚠️  赋予 Gradle Wrapper 执行权限...${NC}"
    chmod +x gradlew
fi

case $BUILD_CHOICE in
    1)
        echo -e "${GREEN}📦 构建 Debug APK...${NC}"
        ./gradlew clean assembleDebug --no-daemon
        APK_PATH="app/build/outputs/apk/debug/app-debug.apk"
        ;;
    2)
        echo -e "${GREEN}📦 构建 Release APK...${NC}"
        ./gradlew clean assembleRelease --no-daemon
        APK_PATH="app/build/outputs/apk/release/app-release.apk"
        ;;
    3)
        echo -e "${GREEN}📦 构建 Debug APK...${NC}"
        ./gradlew clean assembleDebug --no-daemon
        echo ""
        echo -e "${GREEN}📦 构建 Release APK...${NC}"
        ./gradlew assembleRelease --no-daemon
        APK_PATH="app/build/outputs/apk/release/app-release.apk"
        ;;
    *)
        echo -e "${RED}❌ 无效选择${NC}"
        exit 1
        ;;
esac

echo ""
echo -e "${BLUE}[6/6] 验证构建结果...${NC}"

if [ -f "$APK_PATH" ]; then
    APK_SIZE=$(ls -lh "$APK_PATH" | awk '{print $5}')
    echo -e "${GREEN}========================================${NC}"
    echo -e "${GREEN}✅ APK 构建成功！${NC}"
    echo -e "${GREEN}========================================${NC}"
    echo "📦 APK 位置: $(pwd)/$APK_PATH"
    echo "📏 文件大小: $APK_SIZE"
    echo ""
    echo -e "${YELLOW}下一步操作:${NC}"
    echo "1. 安装到设备: adb install -r $APK_PATH"
    echo "2. 传输到手机，直接安装"
    echo ""
else
    echo -e "${RED}❌ APK 文件未找到！${NC}"
    exit 1
fi

echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}🎉 构建完成！${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""
echo "镜像源优化: 阿里云/腾讯/华为/中科大 (10个)"
echo "下载线程: 10 (并发加速)"
echo "兼容版本: Android 8.0 - Android 16"
echo ""
echo -e "${YELLOW}提示: 如果速度慢，检查网络或配置 VPN${NC}"
