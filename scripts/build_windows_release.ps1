#requires -Version 5.1
<#
.SYNOPSIS
    All-in-one Windows release build & package script for Alcedo Studio.
.DESCRIPTION
    完整 Windows Release 流水线：
      1. 自动检测环境（MSVC / Qt6 / vcpkg / CUDA / OpenCL / Rust / NSIS / WiX）
      2. vcpkg 自动 bootstrap + install
      3. CMake 使用 win_release preset 配置（含前缀路径）
      4. 多核并行 Release 编译
      5. CTest 并行执行测试（默认使用 win_release_test preset）
      6. 收集运行时文件并安装到 install tree
      7. install tree 完整性检查
      8. CPack 输出 NSIS EXE / WiX MSI / ZIP
    请在仓库根目录下的 Windows 环境中运行。
.PARAMETER BuildDir
    Release 构建目录，默认 scripts\..\build\release。
.PARAMETER TestBuildDir
    Release 测试构建目录，默认 scripts\..\build\release-test。
.PARAMETER InstallDir
    安装目录，默认 scripts\..\build\install。
.PARAMETER PackageOutDir
    包输出目录，默认 scripts\..\build\release\package。
.PARAMETER Preset
    主构建 CMake preset，默认 win_release。
.PARAMETER TestPreset
    测试 CMake preset，默认 win_release_test。
.PARAMETER QtPrefix
    Qt6 安装根目录（包含 bin/、lib/cmake/Qt6、plugins/、qml/）。
.PARAMETER CudaPath
    CUDA Toolkit 根目录，留空则自动检测。
.PARAMETER VcpkgRoot
    vcpkg 根目录，留空则优先使用仓库根目录下的 vcpkg/。
.PARAMETER OpenCLRoot
    OpenCL SDK 根目录，留空则自动检测。
.PARAMETER ParallelJobs
    编译并行任务数，默认 CPU 核心数。
.PARAMETER SkipTests
    跳过测试。
.PARAMETER SkipVcpkg
    跳过 vcpkg bootstrap/install。
.PARAMETER SkipPackage
    跳过打包。
.PARAMETER SkipVerify
    跳过 install tree 验证。
.EXAMPLE
    .\scripts\build_windows_release.ps1
    .\scripts\build_windows_release.ps1 -SkipTests -SkipVcpkg -ParallelJobs 16
#>
param(
    [string]$BuildDir       = "$PSScriptRoot\..\build\release",
    [string]$TestBuildDir   = "$PSScriptRoot\..\build\release-test",
    [string]$InstallDir     = "$PSScriptRoot\..\build\install",
    [string]$PackageOutDir  = "$PSScriptRoot\..\build\release\package",
    [string]$Preset         = "win_release",
    [string]$TestPreset     = "win_release_test",
    [string]$QtPrefix       = "",
    [string]$CudaPath       = "",
    [string]$VcpkgRoot      = "",
    [string]$OpenCLRoot     = "",
    [int]$ParallelJobs      = [System.Environment]::ProcessorCount,
    [switch]$SkipTests      = $false,
    [switch]$SkipVcpkg      = $false,
    [switch]$SkipPackage    = $false,
    [switch]$SkipVerify     = $false
)

$ErrorActionPreference = 'Stop'
$repoRoot = Resolve-Path "$PSScriptRoot\.."
$ProgressPreference = 'SilentlyContinue'

# =====================================================================
# Helper functions
# =====================================================================
function Write-Section([string]$Title) {
    Write-Host ""
    Write-Host "========================================" -ForegroundColor Cyan
    Write-Host "  $Title" -ForegroundColor Cyan
    Write-Host "========================================" -ForegroundColor Cyan
    Write-Host ""
}

function Test-CommandAvailable([string]$Name) {
    return ($null -ne (Get-Command $Name -ErrorAction SilentlyContinue))
}

function Get-RegistryValue([string]$Path, [string]$Name) {
    try {
        $value = Get-ItemProperty -Path $Path -Name $Name -ErrorAction SilentlyContinue | Select-Object -ExpandProperty $Name
        return $value
    } catch {
        return $null
    }
}

function Find-MSVC {
    # 优先使用仓库自带的 msvc_env.cmd 包装器
    $msvcEnv = Join-Path $repoRoot "scripts\msvc_env.cmd"
    if (Test-Path $msvcEnv) { return $msvcEnv }

    # 否则尝试定位 vcvarsall.bat
    $vsWhere = "${env:ProgramFiles(x86)}\Microsoft Visual Studio\Installer\vswhere.exe"
    if (Test-Path $vsWhere) {
        $vsInstallPath = & $vsWhere -latest -products * -requires Microsoft.VisualStudio.Component.VC.Tools.x86.x64 -property installationPath 2>$null
        if ($vsInstallPath) {
            $vcvarsall = Join-Path $vsInstallPath "VC\Auxiliary\Build\vcvarsall.bat"
            if (Test-Path $vcvarsall) { return $vcvarsall }
        }
    }
    return ""
}

function Find-Qt6 {
    if (-not [string]::IsNullOrWhiteSpace($QtPrefix)) { return $QtPrefix }
    if (-not [string]::IsNullOrWhiteSpace($env:ALCEDO_QT_PREFIX)) { return $env:ALCEDO_QT_PREFIX }
    if (-not [string]::IsNullOrWhiteSpace($env:Qt6_Dir)) { return $env:Qt6_Dir }

    $candidates = @(
        "${env:ProgramFiles}\Qt\6.*\msvc2019_64",
        "${env:ProgramFiles}\Qt\6.*\msvc2022_64",
        "C:\Qt\6.*\msvc2019_64",
        "C:\Qt\6.*\msvc2022_64"
    )
    foreach ($pattern in $candidates) {
        $found = Get-Item -Path $pattern -ErrorAction SilentlyContinue | Sort-Object Name -Descending | Select-Object -First 1
        if ($found) { return $found.FullName }
    }
    return ""
}

function Find-Vcpkg {
    if (-not [string]::IsNullOrWhiteSpace($VcpkgRoot)) { return $VcpkgRoot }

    $localVcpkg = Join-Path $repoRoot "vcpkg"
    if (Test-Path (Join-Path $localVcpkg "bootstrap-vcpkg.bat")) { return $localVcpkg }

    if (-not [string]::IsNullOrWhiteSpace($env:VCPKG_ROOT)) { return $env:VCPKG_ROOT }

    $stdVcpkg = "C:\vcpkg"
    if (Test-Path (Join-Path $stdVcpkg "bootstrap-vcpkg.bat")) { return $stdVcpkg }

    return ""
}

function Find-CUDA {
    if (-not [string]::IsNullOrWhiteSpace($CudaPath)) { return $CudaPath }
    if (-not [string]::IsNullOrWhiteSpace($env:CUDA_PATH)) { return $env:CUDA_PATH }

    # 从注册表读取 CUDA 路径
    $cudaReg = Get-RegistryValue 'HKLM:\SOFTWARE\NVIDIA Corporation\GPU Computing Toolkit\CUDA' 'FirstVersionInstalled'
    if ($cudaReg) {
        $cudaRoot = "${env:ProgramFiles}\NVIDIA GPU Computing Toolkit\CUDA\${cudaReg}"
        if (Test-Path $cudaRoot) { return $cudaRoot }
    }

    foreach ($ver in @('v12.8', 'v12.6', 'v12.5', 'v12.4', 'v12.3', 'v12.2', 'v12.1', 'v12.0', 'v11.8')) {
        $cudaRoot = "${env:ProgramFiles}\NVIDIA GPU Computing Toolkit\CUDA\${ver}"
        if (Test-Path $cudaRoot) { return $cudaRoot }
    }
    return ""
}

function Find-OpenCL {
    if (-not [string]::IsNullOrWhiteSpace($OpenCLRoot)) { return $OpenCLRoot }
    if (-not [string]::IsNullOrWhiteSpace($env:OPENCL_ROOT)) { return $env:OPENCL_ROOT }

    # Khronos OpenCL SDK
    $oclSdk = "${env:ProgramFiles}\OpenCL-SDK"
    if (Test-Path $oclSdk) { return $oclSdk }

    # Intel OpenCL SDK
    $oclSdk = "${env:ProgramFiles(x86)}\Intel\OpenCL SDK"
    if (Test-Path $oclSdk) { return $oclSdk }
    $oclSdk = "${env:ProgramFiles}\Intel\OpenCL SDK"
    if (Test-Path $oclSdk) { return $oclSdk }

    # NVIDIA CUDA 自带 OpenCL
    $cudaDir = Find-CUDA
    if ($cudaDir -and (Test-Path (Join-Path $cudaDir "include\CL\opencl.h"))) { return $cudaDir }

    return ""
}

function Find-Rust {
    $cargo = Get-Command cargo.exe -ErrorAction SilentlyContinue
    if ($cargo) { return $cargo.Source }

    $cargoFallback = "${env:USERPROFILE}\.cargo\bin\cargo.exe"
    if (Test-Path $cargoFallback) { return $cargoFallback }

    return ""
}

function Find-NSIS {
    $makensis = Get-Command makensis.exe -ErrorAction SilentlyContinue
    if ($makensis) { return $makensis.Source }

    $nsisReg = Get-RegistryValue 'HKLM:\SOFTWARE\NSIS' ''
    if (-not $nsisReg) { $nsisReg = Get-RegistryValue 'HKLM:\SOFTWARE\Wow6432Node\NSIS' '' }
    if ($nsisReg) {
        $candidate = Join-Path $nsisReg 'makensis.exe'
        if (Test-Path $candidate) { return $candidate }
    }

    $commonPaths = @(
        "${env:ProgramFiles(x86)}\NSIS\makensis.exe",
        "${env:ProgramFiles}\NSIS\makensis.exe"
    )
    foreach ($p in $commonPaths) { if (Test-Path $p) { return $p } }
    return ""
}

function Find-WiX {
    $candle = Get-Command candle.exe -ErrorAction SilentlyContinue
    $light  = Get-Command light.exe  -ErrorAction SilentlyContinue
    if ($candle -and $light) {
        return @{ Candle = $candle.Source; Light = $light.Source }
    }

    $wixReg = Get-RegistryValue 'HKLM:\SOFTWARE\Microsoft\Windows Installer XML' 'InstallRoot'
    if (-not $wixReg) { $wixReg = Get-RegistryValue 'HKLM:\SOFTWARE\Wow6432Node\Microsoft\Windows Installer XML' 'InstallRoot' }
    if (-not $wixReg -and $env:WIX) { $wixReg = $env:WIX }

    $searchRoots = @()
    if ($wixReg) { $searchRoots += $wixReg }
    $searchRoots += @(
        "C:\Program Files (x86)\WiX Toolset v3.11\bin",
        "C:\Program Files\WiX Toolset v3.11\bin",
        "C:\Program Files (x86)\WiX Toolset v3.14\bin",
        "C:\Program Files\WiX Toolset v3.14\bin"
    )

    foreach ($root in $searchRoots) {
        $c = Join-Path $root 'candle.exe'
        $l = Join-Path $root 'light.exe'
        if ((Test-Path $c) -and (Test-Path $l)) {
            return @{ Candle = $c; Light = $l }
        }
    }
    return $null
}

function Invoke-CMakeViaMSVC([Parameter(Mandatory=$true)][string[]]$Arguments) {
    $msvcEnv = Find-MSVC
    if (-not $msvcEnv) { throw "MSVC environment setup script not found. Install Visual Studio 2022 with C++ workload." }
    Write-Host "Using MSVC wrapper: $msvcEnv" -ForegroundColor Gray
    & $msvcEnv @Arguments
    if ($LASTEXITCODE -ne 0) { throw "CMake command failed: cmake $($Arguments -join ' ')" }
}

function Resolve-DuckDbExtension {
    param(
        [Parameter(Mandatory = $true)][string]$ExtensionName,
        [Parameter(Mandatory = $true)][string]$FileName
    )
    $envVar = "ALCEDO_DUCKDB_$($ExtensionName.ToUpper())_EXTENSION"
    $configured = [Environment]::GetEnvironmentVariable($envVar)
    $repoDir = Join-Path $repoRoot "alcedo_studio\third_party\libduckdb-windows\extensions"
    $fallback = Join-Path $repoDir $FileName

    if (-not [string]::IsNullOrWhiteSpace($configured)) {
        if (-not (Test-Path -LiteralPath $configured -PathType Leaf)) {
            throw "Configured DuckDB $ExtensionName extension not found: $configured"
        }
        return (Resolve-Path -LiteralPath $configured).Path
    }
    if (Test-Path -LiteralPath $fallback -PathType Leaf) {
        return (Resolve-Path -LiteralPath $fallback).Path
    }

    # 尝试用 duckdb CLI 安装
    $duckdb = Get-Command duckdb.exe -ErrorAction SilentlyContinue
    if (-not $duckdb) {
        throw "DuckDB $ExtensionName extension missing and duckdb CLI not found. Pass -$envVar or install duckdb."
    }
    $extRoot = Join-Path $repoRoot "build\duckdb_extensions"
    New-Item -ItemType Directory -Force -Path $extRoot | Out-Null
    $sqlRoot = $extRoot.Replace("'", "''")
    & $duckdb.Source -c "SET extension_directory='$sqlRoot'; INSTALL $ExtensionName;" | Out-Null
    if ($LASTEXITCODE -ne 0) { throw "Failed to install DuckDB $ExtensionName extension via duckdb CLI." }
    $installed = Get-ChildItem -Path $extRoot -Filter $FileName -Recurse -File | Select-Object -First 1
    if (-not $installed) { throw "Installed DuckDB $ExtensionName extension not found under $extRoot." }
    return $installed.FullName
}

# =====================================================================
# Step 1: Environment detection
# =====================================================================
Write-Section "Step 1/8: Environment Detection"

$msvcEnv = Find-MSVC
if ([string]::IsNullOrWhiteSpace($msvcEnv)) {
    Write-Host "ERROR: MSVC not found." -ForegroundColor Red
    exit 1
}
Write-Host "MSVC wrapper : $msvcEnv" -ForegroundColor Green

$qtDir = Find-Qt6
if ([string]::IsNullOrWhiteSpace($qtDir)) {
    Write-Host "ERROR: Qt6 not found." -ForegroundColor Red
    Write-Host "Set -QtPrefix, ALCEDO_QT_PREFIX, or Qt6_Dir to the Qt6 MSVC x64 root." -ForegroundColor Yellow
    exit 1
}
Write-Host "Qt6          : $qtDir" -ForegroundColor Green
$env:ALCEDO_QT_PREFIX = $qtDir

$vcpkgDir = Find-Vcpkg
if ([string]::IsNullOrWhiteSpace($vcpkgDir)) {
    Write-Host "ERROR: vcpkg not found. Expected at ${repoRoot}\vcpkg or VCPKG_ROOT." -ForegroundColor Red
    exit 1
}
Write-Host "vcpkg        : $vcpkgDir" -ForegroundColor Green

$cudaDir = Find-CUDA
if (-not [string]::IsNullOrWhiteSpace($cudaDir)) {
    Write-Host "CUDA         : $cudaDir" -ForegroundColor Green
    $env:CUDA_PATH = $cudaDir
} else {
    Write-Host "CUDA         : not found (CUDA backend disabled)" -ForegroundColor Yellow
}

$oclDir = Find-OpenCL
if (-not [string]::IsNullOrWhiteSpace($oclDir)) {
    Write-Host "OpenCL SDK   : $oclDir" -ForegroundColor Green
    $env:OPENCL_ROOT = $oclDir
} else {
    Write-Host "OpenCL SDK   : not found (OpenCL backend disabled)" -ForegroundColor Yellow
}

$rustCargo = Find-Rust
if (-not [string]::IsNullOrWhiteSpace($rustCargo)) {
    $rustVer = & rustc --version 2>$null
    Write-Host "Rust         : $rustVer ($rustCargo)" -ForegroundColor Green
} else {
    Write-Host "Rust         : not found (alcedo_mind sidecar skipped)" -ForegroundColor Yellow
}

$nsisPath = Find-NSIS
$wix = Find-WiX
$hasNsis = -not [string]::IsNullOrWhiteSpace($nsisPath)
$hasWix  = $null -ne $wix
Write-Host "NSIS         : $(if ($hasNsis) { $nsisPath } else { 'not found' })" -ForegroundColor $(if ($hasNsis) { 'Green' } else { 'Yellow' })
Write-Host "WiX          : $(if ($hasWix) { "$($wix.Candle) / $($wix.Light)" } else { 'not found' })" -ForegroundColor $(if ($hasWix) { 'Green' } else { 'Yellow' })

# =====================================================================
# Step 2: vcpkg bootstrap + install
# =====================================================================
if (-not $SkipVcpkg) {
    Write-Section "Step 2/8: vcpkg Bootstrap + Install"

    $vcpkgExe = Join-Path $vcpkgDir "vcpkg.exe"
    if (-not (Test-Path $vcpkgExe)) {
        Write-Host "Bootstrapping vcpkg ..." -ForegroundColor Yellow
        Push-Location $vcpkgDir
        & .\bootstrap-vcpkg.bat -disableMetrics
        if ($LASTEXITCODE -ne 0) { throw "vcpkg bootstrap failed." }
        Pop-Location
    }

    Write-Host "Installing vcpkg dependencies (x64-windows, manifest mode) ..." -ForegroundColor Yellow
    & $vcpkgExe install --triplet x64-windows
    if ($LASTEXITCODE -ne 0) { throw "vcpkg install failed." }
} else {
    Write-Section "Step 2/8: vcpkg (SKIPPED)"
}

# =====================================================================
# Step 3: CMake Configure (main release)
# =====================================================================
Write-Section "Step 3/8: CMake Configure ($Preset)"

$buildDir = (New-Item -ItemType Directory -Force -Path $BuildDir).FullName

# 构造 CMAKE_PREFIX_PATH
$cmakePrefixPaths = @($qtDir)
$vcpkgInstalled = Join-Path $vcpkgDir "installed\x64-windows"
if (Test-Path $vcpkgInstalled) { $cmakePrefixPaths += $vcpkgInstalled }
if (-not [string]::IsNullOrWhiteSpace($oclDir)) { $cmakePrefixPaths += $oclDir }
$cmakePrefixPath = $cmakePrefixPaths -join ";"

$configureArgs = @(
    "--preset", $Preset,
    "-DCMAKE_PREFIX_PATH=`"$cmakePrefixPath`""
)
if (-not [string]::IsNullOrWhiteSpace($cudaDir)) {
    $configureArgs += "-DCUDAToolkit_ROOT=`"$cudaDir`""
}

$duckDbVss = Resolve-DuckDbExtension -ExtensionName 'vss' -FileName 'vss.duckdb_extension'
$duckDbFts = Resolve-DuckDbExtension -ExtensionName 'fts' -FileName 'fts.duckdb_extension'
$configureArgs += "-DALCEDO_DUCKDB_VSS_EXTENSION=`"$duckDbVss`""
$configureArgs += "-DALCEDO_DUCKDB_FTS_EXTENSION=`"$duckDbFts`""

Write-Host "CMake args: $($configureArgs -join ' ')" -ForegroundColor Gray
Invoke-CMakeViaMSVC -Arguments $configureArgs

# =====================================================================
# Step 4: Build
# =====================================================================
Write-Section "Step 4/8: Build ($Preset)"

Write-Host "Building with $ParallelJobs parallel jobs ..." -ForegroundColor Yellow
Invoke-CMakeViaMSVC -Arguments @("--build", $buildDir, "--config", "Release", "--parallel", "$ParallelJobs")
Write-Host "Build succeeded." -ForegroundColor Green

# =====================================================================
# Step 5: Tests
# =====================================================================
if (-not $SkipTests) {
    Write-Section "Step 5/8: Tests ($TestPreset)"

    $testBuildDir = (New-Item -ItemType Directory -Force -Path $TestBuildDir).FullName

    # 复用相同的 CMAKE_PREFIX_PATH 与 DuckDB 扩展路径
    $testConfigureArgs = @(
        "--preset", $TestPreset,
        "-DCMAKE_PREFIX_PATH=`"$cmakePrefixPath`"",
        "-DALCEDO_DUCKDB_VSS_EXTENSION=`"$duckDbVss`"",
        "-DALCEDO_DUCKDB_FTS_EXTENSION=`"$duckDbFts`""
    )
    if (-not [string]::IsNullOrWhiteSpace($cudaDir)) {
        $testConfigureArgs += "-DCUDAToolkit_ROOT=`"$cudaDir`""
    }

    Write-Host "Configuring test preset ..." -ForegroundColor Yellow
    Invoke-CMakeViaMSVC -Arguments $testConfigureArgs

    Write-Host "Building test targets ..." -ForegroundColor Yellow
    Invoke-CMakeViaMSVC -Arguments @("--build", $testBuildDir, "--config", "Release", "--parallel", "$ParallelJobs")

    Write-Host "Running CTest in parallel ..." -ForegroundColor Yellow
    & ctest --preset $TestPreset --output-on-failure -j $ParallelJobs
    if ($LASTEXITCODE -ne 0) {
        Write-Host "WARNING: Some tests failed." -ForegroundColor Yellow
    } else {
        Write-Host "All tests passed." -ForegroundColor Green
    }
} else {
    Write-Section "Step 5/8: Tests (SKIPPED)"
}

# =====================================================================
# Step 6: Install
# =====================================================================
Write-Section "Step 6/8: Install"

$installDir = (New-Item -ItemType Directory -Force -Path $InstallDir).FullName
$installArgs = @(
    "--install", $buildDir,
    "--prefix", $installDir,
    "--config", "Release"
)
Invoke-CMakeViaMSVC -Arguments $installArgs
Write-Host "Install tree ready at: $installDir" -ForegroundColor Green

# =====================================================================
# Step 7: Verify install tree
# =====================================================================
if (-not $SkipVerify) {
    Write-Section "Step 7/8: Verify Install Tree"

    $verifyScript = Join-Path $repoRoot "scripts\verify_windows_install_tree.ps1"
    if (Test-Path $verifyScript) {
        & powershell -ExecutionPolicy Bypass -File $verifyScript -InstallDir $installDir
        if ($LASTEXITCODE -ne 0) {
            Write-Host "WARNING: Install tree verification found issues." -ForegroundColor Yellow
        } else {
            Write-Host "Install tree verification passed." -ForegroundColor Green
        }
    } else {
        Write-Host "Verification script not found, skipping." -ForegroundColor Yellow
    }
} else {
    Write-Section "Step 7/8: Verify (SKIPPED)"
}

# =====================================================================
# Step 8: Package
# =====================================================================
if (-not $SkipPackage) {
    Write-Section "Step 8/8: Package"

    New-Item -ItemType Directory -Force -Path $PackageOutDir | Out-Null

    # CMakeLists 已根据可用工具设置 CPACK_GENERATOR；使用 CPackConfig 即可。
    # 为了按用户要求明确输出 NSIS/WIX/ZIP，按生成器单独执行，失败时仅告警。
    $generators = @()
    if ($hasWix)  { $generators += "WIX" }
    if ($hasNsis) { $generators += "NSIS" }
    $generators += "ZIP"

    Write-Host "Packaging with generators: $($generators -join ', ')" -ForegroundColor Yellow

    $cpackConfig = Join-Path $buildDir "CPackConfig.cmake"
    if (-not (Test-Path $cpackConfig)) { throw "CPackConfig.cmake not found at $cpackConfig" }

    foreach ($gen in $generators) {
        Write-Host "Running CPack ($gen) ..." -ForegroundColor Yellow
        & cpack --config $cpackConfig -B $PackageOutDir -G $gen
        if ($LASTEXITCODE -ne 0) {
            Write-Host "WARNING: CPack ($gen) failed." -ForegroundColor Yellow
        }
    }

    Write-Host ""
    $packages = Get-ChildItem -Path "$PackageOutDir\*" -Include *.msi,*.exe,*.zip -ErrorAction SilentlyContinue
    if ($packages) {
        Write-Host "Generated packages:" -ForegroundColor Green
        foreach ($pkg in $packages) {
            $sizeMB = [math]::Round($pkg.Length / 1MB, 2)
            Write-Host "  $($pkg.Name) ($sizeMB MB)" -ForegroundColor Green
        }
    } else {
        Write-Host "No package files found in $PackageOutDir" -ForegroundColor Red
    }
} else {
    Write-Section "Step 8/8: Package (SKIPPED)"
}

# =====================================================================
# Final summary
# =====================================================================
Write-Host ""
Write-Host "========================================" -ForegroundColor Green
Write-Host "  Build & Package Complete" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green
Write-Host ""
Write-Host "  Build dir      : $buildDir" -ForegroundColor White
if (-not $SkipTests) { Write-Host "  Test build dir : $testBuildDir" -ForegroundColor White }
Write-Host "  Install dir    : $installDir" -ForegroundColor White
if (-not $SkipPackage) { Write-Host "  Package dir    : $PackageOutDir" -ForegroundColor White }
Write-Host ""
