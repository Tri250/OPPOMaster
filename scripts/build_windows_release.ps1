#requires -Version 5.1
<#
.SYNOPSIS
    Alcedo Studio Windows 一键构建脚本（Release + 打包）
.DESCRIPTION
    自动化 Windows Release 构建的完整流程：
      1. 检查前置依赖（VS2022、Qt6、CMake、Ninja、Git）
      2. 初始化 Git 子模块
      3. 初始化 vcpkg
      4. CMake 配置（win_release preset）
      5. 编译
      6. 安装
      7. 验证安装树
      8. CPack 打包（ZIP / NSIS / WiX）
.EXAMPLE
    # 最简用法（使用默认 Qt 路径）
    .\scripts\build_windows_release.ps1

    # 指定 Qt 路径
    .\scripts\build_windows_release.ps1 -QtPrefix "D:/Qt/6.9.3/msvc2022_64/lib/cmake"

    # 跳过打包，只构建
    .\scripts\build_windows_release.ps1 -SkipPackage

    # 指定并行数
    .\scripts\build_windows_release.ps1 -Parallel 16
#>
param(
    [string]$QtPrefix = "D:/Qt/6.9.3/msvc2022_64/lib/cmake",
    [string]$BuildDir = "$PSScriptRoot\..\build\release",
    [string]$InstallDir = "$PSScriptRoot\..\build\install",
    [string]$PackageOutDir = "$PSScriptRoot\..\build\release\package",
    [int]$Parallel = 8,
    [switch]$SkipPackage = $false,
    [switch]$SkipSubmodules = $false,
    [switch]$SkipVcpkg = $false,
    [switch]$SkipVerify = $false,
    [string]$DuckDbVssExtension = $env:ALCEDO_DUCKDB_VSS_EXTENSION,
    [string]$DuckDbFtsExtension = $env:ALCEDO_DUCKDB_FTS_EXTENSION,
    [string]$ExtraCMakeArgs = ""
)

$ErrorActionPreference = 'Stop'
$repoRoot = Resolve-Path "$PSScriptRoot\.."

# ======================================================================
# 工具函数
# ======================================================================
function Write-Step {
    param([string]$Message)
    Write-Host ""
    Write-Host "========================================" -ForegroundColor Cyan
    Write-Host "  $Message" -ForegroundColor Cyan
    Write-Host "========================================" -ForegroundColor Cyan
    Write-Host ""
}

function Test-CommandAvailable {
    param([string]$Name)
    return ($null -ne (Get-Command $Name -ErrorAction SilentlyContinue))
}

function Test-VsInstallation {
    $vswhere = "${env:ProgramFiles(x86)}\Microsoft Visual Studio\Installer\vswhere.exe"
    if (-not (Test-Path $vswhere)) {
        return $false
    }
    $vsPath = & $vswhere -latest -products * -requires Microsoft.VisualStudio.Component.VC.Tools.x86.x64 -property installationPath 2>$null
    return (-not [string]::IsNullOrWhiteSpace($vsPath))
}

# ======================================================================
# 步骤 1: 环境检查
# ======================================================================
Write-Step "步骤 1/8: 检查构建环境"

$envErrors = @()

if (-not (Test-VsInstallation)) {
    $envErrors += "Visual Studio 2022 未安装或缺少 VC Tools x64 组件。请安装 VS2022 并勾选 '使用 C++ 的桌面开发' 工作负载。"
} else {
    Write-Host "  [OK] Visual Studio 2022" -ForegroundColor Green
}

if (-not (Test-CommandAvailable "cmake")) {
    $envErrors += "CMake 未找到。请安装 CMake 3.21+ 并添加到 PATH。"
} else {
    $cmakeVer = & cmake --version | Select-Object -First 1
    Write-Host "  [OK] $cmakeVer" -ForegroundColor Green
}

if (-not (Test-CommandAvailable "ninja")) {
    $envErrors += "Ninja 未找到。请安装 Ninja 并添加到 PATH。"
} else {
    Write-Host "  [OK] Ninja $(ninja --version)" -ForegroundColor Green
}

if (-not (Test-CommandAvailable "git")) {
    $envErrors += "Git 未找到。请安装 Git 并添加到 PATH。"
} else {
    Write-Host "  [OK] Git" -ForegroundColor Green
}

# 检查 Qt
$qtCmakeDir = $QtPrefix
if (-not (Test-Path "$qtCmakeDir/Qt6")) {
    $envErrors += "Qt6 CMake 目录未找到: $qtCmakeDir`n  请通过 -QtPrefix 参数指定正确的 Qt 路径，例如: D:/Qt/6.9.3/msvc2022_64/lib/cmake"
} else {
    Write-Host "  [OK] Qt6 CMake: $qtCmakeDir" -ForegroundColor Green
}

# 可选工具
if (Test-CommandAvailable "duckdb") {
    $duckdbVer = & duckdb --version 2>$null
    Write-Host "  [OK] DuckDB CLI: $duckdbVer" -ForegroundColor Green
} else {
    Write-Host "  [INFO] DuckDB CLI 未安装（可选，用于自动下载 VSS/FTS 扩展）" -ForegroundColor Yellow
}

$cudaPath = "${env:CUDA_PATH}"
if ($cudaPath -and (Test-Path $cudaPath)) {
    Write-Host "  [OK] CUDA Toolkit: $cudaPath" -ForegroundColor Green
} else {
    Write-Host "  [INFO] CUDA Toolkit 未检测到（可选，将使用 OpenCL 后端）" -ForegroundColor Yellow
}

if ($envErrors.Count -gt 0) {
    Write-Host ""
    Write-Host "环境检查失败:" -ForegroundColor Red
    foreach ($err in $envErrors) {
        Write-Host "  - $err" -ForegroundColor Red
    }
    Write-Host ""
    Write-Host "请修复上述问题后重新运行。" -ForegroundColor Red
    exit 1
}

# ======================================================================
# 步骤 2: 初始化 Git 子模块
# ======================================================================
if (-not $SkipSubmodules) {
    Write-Step "步骤 2/8: 初始化 Git 子模块"
    Push-Location $repoRoot
    try {
        & git submodule update --init --recursive `
            alcedo_studio/src/third_party/lensfun `
            alcedo_studio/src/third_party/libultrahdr `
            alcedo_studio/src/third_party/metal-cpp
        if ($LASTEXITCODE -ne 0) {
            Write-Host "Git 子模块初始化失败，请检查网络连接和仓库访问权限。" -ForegroundColor Red
            exit 1
        }
        Write-Host "子模块初始化完成。" -ForegroundColor Green
    }
    finally {
        Pop-Location
    }
} else {
    Write-Step "步骤 2/8: 跳过 Git 子模块初始化"
}

# ======================================================================
# 步骤 3: 初始化 vcpkg
# ======================================================================
if (-not $SkipVcpkg) {
    Write-Step "步骤 3/8: 初始化 vcpkg"
    $vcpkgDir = Join-Path $repoRoot "vcpkg"
    $vcpkgBootstrap = Join-Path $vcpkgDir "bootstrap-vcpkg.bat"
    $vcpkgExe = Join-Path $vcpkgDir "vcpkg.exe"

    if (-not (Test-Path $vcpkgDir)) {
        Write-Host "vcpkg 目录不存在，正在克隆..." -ForegroundColor Yellow
        & git clone https://github.com/microsoft/vcpkg.git $vcpkgDir
        if ($LASTEXITCODE -ne 0) {
            Write-Host "vcpkg 克隆失败。" -ForegroundColor Red
            exit 1
        }
    }

    if (-not (Test-Path $vcpkgExe)) {
        Write-Host "正在初始化 vcpkg..." -ForegroundColor Yellow
        & $vcpkgBootstrap
        if ($LASTEXITCODE -ne 0) {
            Write-Host "vcpkg 初始化失败。" -ForegroundColor Red
            exit 1
        }
    }
    Write-Host "vcpkg 就绪: $vcpkgExe" -ForegroundColor Green
} else {
    Write-Step "步骤 3/8: 跳过 vcpkg 初始化"
}

# ======================================================================
# 步骤 4: CMake 配置
# ======================================================================
Write-Step "步骤 4/8: CMake 配置 (win_release)"

# 构建配置参数
$configureArgs = @(
    "--preset", "win_release",
    "-DCMAKE_PREFIX_PATH=`"$qtCmakeDir`""
)

# DuckDB 扩展路径
if (-not [string]::IsNullOrWhiteSpace($DuckDbVssExtension)) {
    $configureArgs += "-DALCEDO_DUCKDB_VSS_EXTENSION=`"$DuckDbVssExtension`""
}
if (-not [string]::IsNullOrWhiteSpace($DuckDbFtsExtension)) {
    $configureArgs += "-DALCEDO_DUCKDB_FTS_EXTENSION=`"$DuckDbFtsExtension`""
}

# 额外 CMake 参数
if (-not [string]::IsNullOrWhiteSpace($ExtraCMakeArgs)) {
    $configureArgs += $ExtraCMakeArgs.Split(" ")
}

$msvcEnvCmd = Join-Path $repoRoot "scripts\msvc_env.cmd"
$configureCmd = "cmd /c `"$msvcEnvCmd`" $configureArgs"
Write-Host "执行: $configureCmd" -ForegroundColor Gray
Invoke-Expression $configureCmd
if ($LASTEXITCODE -ne 0) {
    Write-Host "CMake 配置失败。" -ForegroundColor Red
    exit 1
}
Write-Host "CMake 配置成功。" -ForegroundColor Green

# ======================================================================
# 步骤 5: 编译
# ======================================================================
Write-Step "步骤 5/8: 编译 (并行 $Parallel 个任务)"

$buildCmd = "cmd /c `"$msvcEnvCmd`" --build `"$BuildDir`" --target alcedo_main --parallel $Parallel"
Write-Host "执行: $buildCmd" -ForegroundColor Gray
Invoke-Expression $buildCmd
if ($LASTEXITCODE -ne 0) {
    Write-Host "编译失败。" -ForegroundColor Red
    exit 1
}
Write-Host "编译成功。" -ForegroundColor Green

# ======================================================================
# 步骤 6: 安装
# ======================================================================
Write-Step "步骤 6/8: 安装到 $InstallDir"

$installCmd = "cmd /c `"$msvcEnvCmd`" --install `"$BuildDir`" --prefix `"$InstallDir`""
Write-Host "执行: $installCmd" -ForegroundColor Gray
Invoke-Expression $installCmd
if ($LASTEXITCODE -ne 0) {
    Write-Host "安装失败。" -ForegroundColor Red
    exit 1
}
Write-Host "安装成功。" -ForegroundColor Green

# ======================================================================
# 步骤 7: 验证安装树
# ======================================================================
if (-not $SkipVerify) {
    Write-Step "步骤 7/8: 验证安装树"

    $verifyScript = Join-Path $repoRoot "scripts\verify_windows_install_tree.ps1"
    if (Test-Path $verifyScript) {
        & powershell -ExecutionPolicy Bypass -File $verifyScript -InstallDir $InstallDir
        if ($LASTEXITCODE -ne 0) {
            Write-Host "安装树验证失败，请检查构建输出是否完整。" -ForegroundColor Red
            exit 1
        }
    } else {
        Write-Host "验证脚本不存在，跳过验证: $verifyScript" -ForegroundColor Yellow
    }
} else {
    Write-Step "步骤 7/8: 跳过安装树验证"
}

# ======================================================================
# 步骤 8: 打包
# ======================================================================
if (-not $SkipPackage) {
    Write-Step "步骤 8/8: 打包 (CPack)"

    # 使用项目自带的打包脚本
    $packageScript = Join-Path $repoRoot "scripts\package_windows.ps1"
    if (Test-Path $packageScript) {
        $packageArgs = @{
            BuildDir             = $BuildDir
            Preset               = "win_release"
            QtPrefix             = $qtCmakeDir
            PackageOutDir        = $PackageOutDir
        }
        if (-not [string]::IsNullOrWhiteSpace($DuckDbVssExtension)) {
            $packageArgs['DuckDbVssExtension'] = $DuckDbVssExtension
        }
        if (-not [string]::IsNullOrWhiteSpace($DuckDbFtsExtension)) {
            $packageArgs['DuckDbFtsExtension'] = $DuckDbFtsExtension
        }
        & powershell -ExecutionPolicy Bypass -File $packageScript @packageArgs
        if ($LASTEXITCODE -ne 0) {
            Write-Host "打包失败。" -ForegroundColor Red
            exit 1
        }
    } else {
        # 备用方案: 直接运行 CPack
        Write-Host "使用 CPack 直接打包..." -ForegroundColor Yellow
        New-Item -ItemType Directory -Force -Path $PackageOutDir | Out-Null
        & cpack --config "$BuildDir\CPackConfig.cmake" -B "$PackageOutDir"
        if ($LASTEXITCODE -ne 0) {
            Write-Host "CPack 打包失败。" -ForegroundColor Red
            exit 1
        }
    }

    # 列出生成的安装包
    Write-Host ""
    Write-Host "生成的安装包:" -ForegroundColor Green
    $packages = Get-ChildItem -Path "$PackageOutDir\*" -Include *.msi,*.exe,*.zip -ErrorAction SilentlyContinue
    if ($packages) {
        foreach ($pkg in $packages) {
            $sizeMB = [math]::Round($pkg.Length / 1MB, 2)
            Write-Host "  $($pkg.Name) ($sizeMB MB)" -ForegroundColor Green
        }
    } else {
        Write-Host "  未找到安装包文件，请检查 $PackageOutDir" -ForegroundColor Yellow
    }
} else {
    Write-Step "步骤 8/8: 跳过打包"
}

# ======================================================================
# 完成
# ======================================================================
Write-Host ""
Write-Host "========================================" -ForegroundColor Green
Write-Host "  构建完成!" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green
Write-Host ""
Write-Host "  安装目录: $InstallDir" -ForegroundColor White
if (-not $SkipPackage) {
    Write-Host "  安装包目录: $PackageOutDir" -ForegroundColor White
}
Write-Host ""
