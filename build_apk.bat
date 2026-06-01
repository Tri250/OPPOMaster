@echo off
REM OPPO Master Android 项目自动构建脚本 (Windows 版本)
REM 适用于 https://github.com/Tri250/OPPOMaster

echo ======================================
echo OPPO Master Android APK 构建脚本
echo ======================================
echo.

REM 检查 Java
echo 检查 Java 环境...
java -version >nul 2>&1
if errorlevel 1 (
    echo [错误] 未找到 Java
    echo 请安装 JDK 17 或更高版本
    pause
    exit /b 1
)
echo [成功] Java 已安装
for /f "tokens=3" %%g in ('java -version 2^>^&1 ^| findstr /i "version"') do (
    set JAVA_VERSION=%%g
)
echo Java 版本：%JAVA_VERSION%
echo.

REM 检查 Android SDK
echo 检查 Android SDK...
if defined ANDROID_HOME (
    echo [成功] ANDROID_HOME: %ANDROID_HOME%
) else if exist "%USERPROFILE%\Android\Sdk" (
    set ANDROID_HOME=%USERPROFILE%\Android\Sdk
    echo [成功] 找到 Android SDK: %ANDROID_HOME%
) else (
    echo [警告] 未找到 Android SDK
    echo 请设置 ANDROID_HOME 环境变量或在 local.properties 中配置
    echo.
    set /p SDK_PATH="请输入 Android SDK 路径："
    echo sdk.dir=%SDK_PATH%> local.properties
    set ANDROID_HOME=%SDK_PATH%
)
echo.

REM 检查 Gradle
echo 检查 Gradle...
where gradle >nul 2>&1
if not errorlevel 1 (
    echo [成功] 使用系统 Gradle
    set USE_SYSTEM_GRADLE=true
) else if exist "gradlew.bat" (
    echo [成功] 使用 Gradle Wrapper
    set USE_SYSTEM_GRADLE=false
) else (
    echo [错误] 未找到 Gradle
    pause
    exit /b 1
)
echo.

REM 清理之前的构建
echo 清理之前的构建...
if exist "app\build" (
    rmdir /s /q app\build
    echo [成功] 已清理
) else (
    echo 无需清理
)
echo.

REM 选择构建类型
echo ======================================
echo 选择构建类型
echo ======================================
echo 1. Debug (调试版，适合开发测试)
echo 2. Release (发布版，需要签名配置)
echo 3. 两者都构建
echo.

set /p BUILD_CHOICE="请选择 [1/2/3]:"

if "%BUILD_CHOICE%"=="1" (
    call :build_apk debug
    call :show_apk_info Debug
) else if "%BUILD_CHOICE%"=="2" (
    call :build_apk release
    call :show_apk_info Release
) else if "%BUILD_CHOICE%"=="3" (
    call :build_apk debug
    call :show_apk_info Debug
    echo.
    call :build_apk release
    call :show_apk_info Release
) else (
    echo [错误] 无效的选择
    pause
    exit /b 1
)

echo.
echo ======================================
echo 构建成功!
echo ======================================
echo.
echo 下一步:
echo 1. 将 APK 传输到 Android 设备
echo 2. 使用 ADB 安装：adb install -r app\build\outputs\apk\debug\app-debug.apk
echo 3. 在设备上直接点击 APK 文件安装
echo.
pause
goto :eof

:build_apk
set BUILD_TYPE=%1
echo.
echo ======================================
echo 开始构建 %BUILD_TYPE% APK...
echo ======================================

if "%USE_SYSTEM_GRADLE%"=="true" (
    echo 使用系统 Gradle 构建...
    gradle clean assemble%BUILD_TYPE% --no-daemon
) else (
    echo 使用 Gradle Wrapper 构建...
    call gradlew.bat clean assemble%BUILD_TYPE% --no-daemon
)

echo.
echo [成功] 构建完成!
goto :eof

:show_apk_info
set BUILD_TYPE=%1
set APK_PATH=app\build\outputs\apk\%BUILD_TYPE%\app-%BUILD_TYPE%.apk

echo.
echo ======================================
echo APK 文件信息
echo ======================================

if exist "%APK_PATH%" (
    echo [成功] APK 位置：%CD%\%APK_PATH%
    for %%I in ("%APK_PATH%") do echo 文件大小：%%~zI bytes
    echo.
    echo 安装命令:
    echo   adb install -r %APK_PATH%
) else (
    echo [警告] 未找到 APK 文件
)
goto :eof
