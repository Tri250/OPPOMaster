#requires -Version 5.1
<#
.SYNOPSIS
    Build Windows installer packages (WiX MSI / NSIS EXE / ZIP) for Alcedo Studio.
.DESCRIPTION
    该脚本在 Windows 上执行 CMake install + CPack，输出 MSI/EXE/ZIP。
    会自动检测 Qt6、MSVC、WiX、NSIS；缺少包工具时仅生成 ZIP 并给出安装提示。
    请在仓库根目录下运行。
.PARAMETER BuildDir
    Release 构建目录，默认 scripts\..\build\release。
.PARAMETER Preset
    CMake preset，默认 win_release。
.PARAMETER QtPrefix
    Qt6 安装根目录；留空则自动检测 ALCEDO_QT_PREFIX / Qt6_Dir / 常见路径。
.PARAMETER PackageOutDir
    包输出目录，默认 scripts\..\build\release\package。
.PARAMETER DuckDbVssExtension
    DuckDB VSS 扩展文件路径；留空则使用仓库 bundled 或通过 duckdb CLI 安装。
.PARAMETER DuckDbFtsExtension
    DuckDB FTS 扩展文件路径；留空则使用仓库 bundled 或通过 duckdb CLI 安装。
.PARAMETER RequireOpenCLAssets
    是否在验证时要求 OpenCL shader 资源存在，默认 true。
.EXAMPLE
    .\scripts\package_windows.ps1
    .\scripts\package_windows.ps1 -QtPrefix D:\Qt\6.9.3\msvc2022_64
#>
param(
    [string]$BuildDir = "$PSScriptRoot\..\build\release",
    [string]$Preset = "win_release",
    [string]$QtPrefix = "",
    [string]$PackageOutDir = "$PSScriptRoot\..\build\release\package",
    [string]$DuckDbVssExtension = $env:ALCEDO_DUCKDB_VSS_EXTENSION,
    [string]$DuckDbFtsExtension = $env:ALCEDO_DUCKDB_FTS_EXTENSION,
    [bool]$RequireOpenCLAssets = $true
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

function Test-CommandAvailable {
    param([string]$Name)
    return ($null -ne (Get-Command $Name -ErrorAction SilentlyContinue))
}

function Get-RegistryValue([string]$Path, [string]$Name) {
    try {
        return Get-ItemProperty -Path $Path -Name $Name -ErrorAction SilentlyContinue | Select-Object -ExpandProperty $Name
    } catch { return $null }
}

function Find-MSVC {
    $msvcEnv = Join-Path $repoRoot "scripts\msvc_env.cmd"
    if (Test-Path $msvcEnv) { return $msvcEnv }

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

function Find-NSIS {
    $makensis = Get-Command makensis.exe -ErrorAction SilentlyContinue
    if ($makensis) { return $makensis.Source }

    $nsisReg = Get-RegistryValue 'HKLM:\SOFTWARE\NSIS' ''
    if (-not $nsisReg) { $nsisReg = Get-RegistryValue 'HKLM:\SOFTWARE\Wow6432Node\NSIS' '' }
    if ($nsisReg) {
        $candidate = Join-Path $nsisReg 'makensis.exe'
        if (Test-Path $candidate) { return $candidate }
    }
    foreach ($p in @("${env:ProgramFiles(x86)}\NSIS\makensis.exe", "${env:ProgramFiles}\NSIS\makensis.exe")) {
        if (Test-Path $p) { return $p }
    }
    return ""
}

function Find-WiX {
    $candle = Get-Command candle.exe -ErrorAction SilentlyContinue
    $light  = Get-Command light.exe  -ErrorAction SilentlyContinue
    if ($candle -and $light) { return @{ Candle = $candle.Source; Light = $light.Source } }

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
        if ((Test-Path $c) -and (Test-Path $l)) { return @{ Candle = $c; Light = $l } }
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
        [Parameter(Mandatory = $true)][string]$FileName,
        [string]$ConfiguredPath,
        [string]$FallbackPath
    )
    if (-not [string]::IsNullOrWhiteSpace($ConfiguredPath)) {
        if (-not (Test-Path -LiteralPath $ConfiguredPath -PathType Leaf)) {
            throw "Configured DuckDB $ExtensionName extension not found: $ConfiguredPath"
        }
        return (Resolve-Path -LiteralPath $ConfiguredPath).Path
    }
    if (-not [string]::IsNullOrWhiteSpace($FallbackPath) -and (Test-Path -LiteralPath $FallbackPath -PathType Leaf)) {
        return (Resolve-Path -LiteralPath $FallbackPath).Path
    }
    if (-not (Test-CommandAvailable "duckdb.exe")) {
        throw "DuckDB CLI is required to prepare the $ExtensionName extension. Install DuckDB or pass the extension path."
    }
    $extensionRoot = Join-Path $BuildDir "duckdb_extensions"
    New-Item -ItemType Directory -Force -Path $extensionRoot | Out-Null
    $sqlRoot = $extensionRoot.Replace("'", "''")
    & duckdb.exe -c "SET extension_directory='$sqlRoot'; INSTALL $ExtensionName;" | Out-Null
    if ($LASTEXITCODE -ne 0) { throw "Failed to install DuckDB $ExtensionName extension." }
    $installed = Get-ChildItem -Path $extensionRoot -Filter $FileName -Recurse -File | Select-Object -First 1
    if (-not $installed) { throw "Failed to locate installed $FileName under $extensionRoot." }
    return $installed.FullName
}

# =====================================================================
# Environment detection
# =====================================================================
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Alcedo Studio Windows Packager" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

$qtDir = Find-Qt6
if ([string]::IsNullOrWhiteSpace($qtDir)) {
    Write-Host "ERROR: Qt6 not found." -ForegroundColor Red
    Write-Host "Set -QtPrefix, ALCEDO_QT_PREFIX, or Qt6_Dir to the Qt6 MSVC x64 root." -ForegroundColor Yellow
    exit 1
}
Write-Host "Qt6          : $qtDir" -ForegroundColor Green
$env:ALCEDO_QT_PREFIX = $qtDir

$msvcEnv = Find-MSVC
if ([string]::IsNullOrWhiteSpace($msvcEnv)) {
    Write-Host "ERROR: MSVC not found." -ForegroundColor Red
    exit 1
}
Write-Host "MSVC wrapper : $msvcEnv" -ForegroundColor Green

$hasWix = $null -ne (Find-WiX)
$hasNsis = -not [string]::IsNullOrWhiteSpace((Find-NSIS))
Write-Host "NSIS         : $(if ($hasNsis) { 'detected' } else { 'not detected' })" -ForegroundColor $(if ($hasNsis) { 'Green' } else { 'Yellow' })
Write-Host "WiX          : $(if ($hasWix) { 'detected' } else { 'not detected' })" -ForegroundColor $(if ($hasWix) { 'Green' } else { 'Yellow' })
Write-Host ""

# =====================================================================
# Resolve DuckDB extensions
# =====================================================================
$repoDuckDbExtensionDir = Join-Path $repoRoot "alcedo_studio\third_party\libduckdb-windows\extensions"
$repoVssExtension = Join-Path $repoDuckDbExtensionDir "vss.duckdb_extension"
$repoFtsExtension = Join-Path $repoDuckDbExtensionDir "fts.duckdb_extension"

$resolvedDuckDbVssExtension = Resolve-DuckDbExtension `
    -ExtensionName "vss" `
    -FileName "vss.duckdb_extension" `
    -ConfiguredPath $DuckDbVssExtension `
    -FallbackPath $repoVssExtension

$resolvedDuckDbFtsExtension = Resolve-DuckDbExtension `
    -ExtensionName "fts" `
    -FileName "fts.duckdb_extension" `
    -ConfiguredPath $DuckDbFtsExtension `
    -FallbackPath $repoFtsExtension

Write-Host "DuckDB VSS extension: $resolvedDuckDbVssExtension" -ForegroundColor Gray
Write-Host "DuckDB FTS extension: $resolvedDuckDbFtsExtension" -ForegroundColor Gray
Write-Host ""

# =====================================================================
# 1. Configure (re-run to pick up packaging tools like WiX/NSIS)
# =====================================================================
Write-Section "Step 1/4: Configure CMake"
$configureArgs = @(
    "--preset", $Preset,
    "-DCMAKE_PREFIX_PATH=`"$qtDir`"",
    "-DALCEDO_DUCKDB_VSS_EXTENSION=`"$resolvedDuckDbVssExtension`"",
    "-DALCEDO_DUCKDB_FTS_EXTENSION=`"$resolvedDuckDbFtsExtension`""
)
Write-Host "CMake args: $($configureArgs -join ' ')" -ForegroundColor Gray
Invoke-CMakeViaMSVC -Arguments $configureArgs

# =====================================================================
# 2. Build install target
# =====================================================================
Write-Section "Step 2/4: Build Install Target"
Invoke-CMakeViaMSVC -Arguments @("--build", $BuildDir, "--target", "install", "--config", "Release", "--parallel", "4")

# =====================================================================
# 3. Verify install tree
# =====================================================================
Write-Section "Step 3/4: Verify Install Tree"
$verifyScript = Join-Path $repoRoot "scripts\verify_windows_install_tree.ps1"
$installDir = Join-Path $repoRoot "build\install"
$verifyArgs = @(
    '-ExecutionPolicy', 'Bypass',
    '-File', $verifyScript,
    '-InstallDir', $installDir
)
if (-not $RequireOpenCLAssets) { $verifyArgs += '-SkipOpenCLAssetCheck' }
& powershell @verifyArgs
if ($LASTEXITCODE -ne 0) { throw "Install tree verification failed." }

# =====================================================================
# 4. Run CPack
# =====================================================================
Write-Section "Step 4/4: Run CPack"
New-Item -ItemType Directory -Force -Path $PackageOutDir | Out-Null

$cpackConfig = Join-Path $BuildDir "CPackConfig.cmake"
if (-not (Test-Path $cpackConfig)) { throw "CPackConfig.cmake not found at $cpackConfig" }

# CMakeLists 已经根据可用工具设置了 CPACK_GENERATOR，但这里显式逐个执行，便于单独失败时给出提示。
$generators = @()
if ($hasWix)  { $generators += "WIX" }
if ($hasNsis) { $generators += "NSIS" }
$generators += "ZIP"

Write-Host "Packaging with generators: $($generators -join ', ')" -ForegroundColor Yellow
foreach ($gen in $generators) {
    Write-Host "Running CPack ($gen) ..." -ForegroundColor Yellow
    & cpack --config $cpackConfig -B $PackageOutDir -G $gen
    if ($LASTEXITCODE -ne 0) {
        Write-Host "WARNING: CPack ($gen) failed." -ForegroundColor Yellow
    }
}

# =====================================================================
# 5. Report results
# =====================================================================
Write-Host ""
Write-Host "========================================" -ForegroundColor Green
Write-Host "  Packaging Complete" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green

$packages = Get-ChildItem -Path "$PackageOutDir\*" -Include *.msi,*.exe,*.zip -ErrorAction SilentlyContinue
if ($packages) {
    foreach ($pkg in $packages) {
        $sizeMB = [math]::Round($pkg.Length / 1MB, 2)
        Write-Host "  Generated: $($pkg.Name) ($sizeMB MB)" -ForegroundColor Green
    }
} else {
    Write-Host "  No package files found in $PackageOutDir" -ForegroundColor Red
}
Write-Host ""

# =====================================================================
# 6. Tooling hints
# =====================================================================
if (-not $hasWix -and -not $hasNsis) {
    Write-Host "Notice: Neither WiX nor NSIS was detected. Only ZIP was generated." -ForegroundColor Yellow
    Write-Host ""
    Write-Host "To generate a high-compression installer, install one of the following:" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "  WiX Toolset v3.11 (MSI):" -ForegroundColor White
    Write-Host "    https://github.com/wixtoolset/wix3/releases/tag/wix3112rtm" -ForegroundColor White
    Write-Host "    Install and ensure candle.exe / light.exe are on PATH." -ForegroundColor White
    Write-Host ""
    Write-Host "  NSIS (high-compression EXE):" -ForegroundColor White
    Write-Host "    https://nsis.sourceforge.io/Download" -ForegroundColor White
    Write-Host "    Install and ensure makensis.exe is on PATH." -ForegroundColor White
    Write-Host ""
    Write-Host "After installing, re-run this script to produce MSI or EXE installers." -ForegroundColor Cyan
} else {
    if ($hasWix) { Write-Host "WiX detected   : MSI package enabled" -ForegroundColor Green }
    if ($hasNsis) { Write-Host "NSIS detected  : EXE package enabled" -ForegroundColor Green }
}

Write-Host ""
