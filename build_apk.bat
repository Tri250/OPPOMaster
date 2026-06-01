@echo off
REM OMaster APK 一键构建脚本 (Windows)
REM 适用于 Android 14-16 系统

echo ======================================
echo OMaster APK 构建脚本
echo ======================================
echo.

REM 检查 Java
where java >nul 2>&1
if %errorlevel% neq 0 (
    echo 错误：未找到 Java
    echo 请先安装 JDK 17 或更高版本
    pause
    exit /b 1
)

echo ✅ Java 版本:
java -version
echo.

REM 检查 Android SDK
if "%ANDROID_HOME%"=="" (
    if "%ANDROID_SDK_ROOT%"=="" (
        echo ⚠️  警告：未设置 ANDROID_HOME 或 ANDROID_SDK_ROOT
        echo.
        echo 请先安装 Android SDK，然后设置环境变量：
        echo.
        echo set ANDROID_HOME=C:\Users\YourName\AppData\Local\Android\Sdk
        echo set PATH=%%PATH%%;%%ANDROID_HOME%%\platform-tools
        echo.
        echo 或者使用 Android Studio，它会自动配置 SDK
        echo.
        pause
        exit /b 1
    )
)

if not "%ANDROID_HOME%"=="" (
    echo ✅ Android SDK 路径：%ANDROID_HOME%
) else (
    echo ✅ Android SDK 路径：%ANDROID_SDK_ROOT%
)
echo.

REM 进入项目目录
set PROJECT_DIR=%~dp0omaster_final_build
if not exist "%PROJECT_DIR%" (
    echo 错误：项目目录不存在
    echo 请确保 omaster_final_build 目录存在
    pause
    exit /b 1
)

cd /d "%PROJECT_DIR%"
echo ✅ 项目目录：%PROJECT_DIR%
echo.

REM 检查 gradlew
if not exist "gradlew.bat" (
    echo 错误：gradlew.bat 不存在
    pause
    exit /b 1
)

REM 构建 APK
echo ======================================
echo 开始构建 APK...
echo ======================================
echo.

call gradlew.bat assembleDebug

echo.
echo ======================================
echo ✅ 构建完成！
echo ======================================
echo.
echo APK 位置：app\build\outputs\apk\debug\app-debug.apk
echo.

REM 检查 APK 是否生成
if exist "app\build\outputs\apk\debug\app-debug.apk" (
    echo 📦 APK 已生成
    echo.
    echo 📱 安装说明：
    echo   1. 将 APK 传输到 Android 设备
    echo   2. 在设备上启用"未知来源"安装
    echo   3. 点击 APK 文件进行安装
    echo.
    echo ✅ 支持系统：Android 14, 15, 16
) else (
    echo 错误：APK 未生成
    pause
    exit /b 1
)

pause
