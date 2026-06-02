@echo off
setlocal enabledelayedexpansion

echo ==========================================
echo OMaster 快速构建脚本 (Windows)
echo ==========================================

set GRADLE_CMD=gradlew.bat

if not exist "gradlew.bat" (
    echo Gradle Wrapper 不存在，正在初始化...
    where gradle >nul 2>&1
    if %errorlevel% equ 0 (
        gradle wrapper
    ) else (
        echo 错误: 未找到 gradle 命令，请先安装 Gradle 或手动下载 Gradle Wrapper
        exit /b 1
    )
)

echo.
echo 检查 Java 版本...
java -version

echo.
echo 检查 Android SDK...
if "%ANDROID_HOME%"=="" (
    if exist "local.properties" (
        echo 使用 local.properties 中的 SDK 配置
    ) else (
        echo 警告: ANDROID_HOME 未设置
        echo 请设置 ANDROID_HOME 或创建 local.properties 文件
    )
) else (
    echo ANDROID_HOME: %ANDROID_HOME%
)

echo.
echo ==========================================
echo 选择构建选项:
echo ==========================================
echo 1) 构建 Debug APK
echo 2) 构建 Release APK
echo 3) 清理并构建 Debug APK
echo 4) 清理并构建 Release APK
echo 5) 安装到设备 (Debug)
echo 6) 运行单元测试
echo 7) 离线构建 Debug APK
echo 8) 使用 init.gradle 构建
echo ==========================================

set /p choice="请输入选项 (1-8): "

if "%choice%"=="1" (
    echo 构建 Debug APK...
    call %GRADLE_CMD% assembleDebug
    echo APK 位置: app\build\outputs\apk\debug\app-debug.apk
) else if "%choice%"=="2" (
    echo 构建 Release APK...
    call %GRADLE_CMD% assembleRelease
    echo APK 位置: app\build\outputs\apk\release\app-release.apk
) else if "%choice%"=="3" (
    echo 清理并构建 Debug APK...
    call %GRADLE_CMD% clean assembleDebug
    echo APK 位置: app\build\outputs\apk\debug\app-debug.apk
) else if "%choice%"=="4" (
    echo 清理并构建 Release APK...
    call %GRADLE_CMD% clean assembleRelease
    echo APK 位置: app\build\outputs\apk\release\app-release.apk
) else if "%choice%"=="5" (
    echo 安装到设备...
    call %GRADLE_CMD% installDebug
) else if "%choice%"=="6" (
    echo 运行单元测试...
    call %GRADLE_CMD% test
) else if "%choice%"=="7" (
    echo 离线构建 Debug APK...
    call %GRADLE_CMD% assembleDebug --offline
    echo APK 位置: app\build\outputs\apk\debug\app-debug.apk
) else if "%choice%"=="8" (
    echo 使用 init.gradle 构建...
    call %GRADLE_CMD% assembleDebug --init-script gradle\init.gradle
    echo APK 位置: app\build\outputs\apk\debug\app-debug.apk
) else (
    echo 无效选项
    exit /b 1
)

echo.
echo ==========================================
echo 构建完成!
echo ==========================================

endlocal