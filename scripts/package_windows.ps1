#requires -Version 5.1
<#
.SYNOPSIS
    Build Windows installer packages (WiX MSI / NSIS EXE / ZIP) for Alcedo Studio.
.DESCRIPTION
    This script automates the CMake install + CPack workflow on Windows.
    It detects available packaging tools and prints installation hints if they are missing.
    Run from the repository root.
.EXAMPLE
    .\scripts\package_windows.ps1 -BuildDir build\release -Preset win_release
#>
param(
    [string]$BuildDir = "$PSScriptRoot\..\build\release",
    [string]$Preset = "win_release",
    [string]$QtPrefix = "D:/Qt/6.9.3/msvc2022_64/lib/cmake",
    [string]$PackageOutDir = "$PSScriptRoot\..\build\release\package",
    [string]$DuckDbVssExtension = $env:ALCEDO_DUCKDB_VSS_EXTENSION,
    [string]$DuckDbFtsExtension = $env:ALCEDO_DUCKDB_FTS_EXTENSION,
    [bool]$RequireOpenCLAssets = $true,
    [string]$OnnxRuntimeDir = $env:ALCEDO_ORT_DIR,
    [string]$OpenCLIcdDir = $env:ALCEDO_OPENCL_ICD_DIR
)

$ErrorActionPreference = 'Stop'
$repoRoot = Resolve-Path "$PSScriptRoot\.."

function Test-CommandAvailable {
    param([string]$Name)
    return ($null -ne (Get-Command $Name -ErrorAction SilentlyContinue))
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
            throw "$ExtensionName DuckDB extension path does not exist: $ConfiguredPath"
        }
        return (Resolve-Path -LiteralPath $ConfiguredPath).Path
    }

    if (-not [string]::IsNullOrWhiteSpace($FallbackPath) -and
        (Test-Path -LiteralPath $FallbackPath -PathType Leaf)) {
        return (Resolve-Path -LiteralPath $FallbackPath).Path
    }

    if (-not (Test-CommandAvailable "duckdb")) {
        throw "DuckDB CLI is required to prepare the $ExtensionName extension. Install DuckDB or pass -DuckDb$($ExtensionName.Substring(0,1).ToUpper())$($ExtensionName.Substring(1))Extension."
    }

    $extensionRoot = Join-Path $BuildDir "duckdb_extensions"
    New-Item -ItemType Directory -Force -Path $extensionRoot | Out-Null

    $sqlExtensionRoot = $extensionRoot.Replace("'", "''")
    & duckdb -c "SET extension_directory='$sqlExtensionRoot'; INSTALL $ExtensionName;" | Out-Null
    if ($LASTEXITCODE -ne 0) {
        throw "Failed to install DuckDB $ExtensionName extension."
    }

    $installed = Get-ChildItem -Path $extensionRoot -Filter $FileName -Recurse -File |
        Select-Object -First 1
    if (-not $installed) {
        throw "Failed to locate installed $FileName under $extensionRoot."
    }

    return $installed.FullName
}

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Alcedo Studio Windows Packager" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

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

# ------------------------------------------------------------------
# 1. Configure (re-run to pick up new packaging tools like WiX/NSIS)
# ------------------------------------------------------------------
Write-Host "Configuring CMake with preset '$Preset' ..." -ForegroundColor Yellow
$configureCmd = "cmd /c `"$repoRoot\scripts\msvc_env.cmd`" --preset $Preset -DCMAKE_PREFIX_PATH=`"$QtPrefix`" -DALCEDO_DUCKDB_VSS_EXTENSION=`"$resolvedDuckDbVssExtension`" -DALCEDO_DUCKDB_FTS_EXTENSION=`"$resolvedDuckDbFtsExtension`""
Write-Host "> $configureCmd"
Invoke-Expression $configureCmd
if ($LASTEXITCODE -ne 0) {
    throw "CMake configuration failed."
}

# ------------------------------------------------------------------
# 2. Build install target
# ------------------------------------------------------------------
Write-Host "Building install target ..." -ForegroundColor Yellow
$buildCmd = "cmd /c `"$repoRoot\scripts\msvc_env.cmd`" --build $BuildDir --target install --parallel 4"
Write-Host "> $buildCmd"
Invoke-Expression $buildCmd
if ($LASTEXITCODE -ne 0) {
    throw "Build/install failed."
}

# ------------------------------------------------------------------
# 3. Verify install tree
# ------------------------------------------------------------------
Write-Host "Verifying install tree ..." -ForegroundColor Yellow
$verifyScript = Join-Path $repoRoot "scripts\verify_windows_install_tree.ps1"
$installDir = Join-Path $repoRoot "build\install"
$verifyArgs = @(
    '-ExecutionPolicy', 'Bypass',
    '-File', $verifyScript,
    '-InstallDir', $installDir
)
if (-not $RequireOpenCLAssets) {
    $verifyArgs += '-SkipOpenCLAssetCheck'
}
& powershell @verifyArgs
if ($LASTEXITCODE -ne 0) {
    throw "Install tree verification failed."
}

# ------------------------------------------------------------------
# 4. Run CPack
# ------------------------------------------------------------------
New-Item -ItemType Directory -Force -Path $PackageOutDir | Out-Null
Write-Host "Running CPack ..." -ForegroundColor Yellow
$cpackCmd = "cpack --config `"$BuildDir\CPackConfig.cmake`" -B `"$PackageOutDir`""
Write-Host "> $cpackCmd"
Invoke-Expression $cpackCmd
if ($LASTEXITCODE -ne 0) {
    throw "CPack failed."
}

# ------------------------------------------------------------------
# 4b. Deploy additional runtime files into the install tree
# ------------------------------------------------------------------
$installDir = Join-Path $repoRoot "build\install"
$binDir = Join-Path $installDir "bin"
if (-not (Test-Path $binDir)) {
    # Search for the bin directory in the CPack staging area
    $stagingDir = Get-ChildItem -Path $PackageOutDir -Filter "_CPack_Packages" -Directory -Recurse -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($stagingDir) {
        $binDir = Get-ChildItem -Path $stagingDir.FullName -Filter "alcedo_main.exe" -Recurse -ErrorAction SilentlyContinue | Select-Object -First 1 -ExpandProperty DirectoryName
    }
}

if (Test-Path $binDir) {
    # --- ONNX Runtime DLL deployment ---
    $ortDir = $OnnxRuntimeDir
    if (-not $ortDir) {
        # Try vcpkg installed location
        $vcpkgOrt = Join-Path $env:VCPKG_ROOT "installed\x64-windows\bin"
        if (Test-Path (Join-Path $vcpkgOrt "onnxruntime.dll")) {
            $ortDir = $vcpkgOrt
        } else {
            # Try third_party directory
            $tpOrt = Join-Path $repoRoot "alcedo_studio\third_party\onnxruntime-win"
            if (Test-Path $tpOrt) { $ortDir = $tpOrt }
        }
    }
    if ($ortDir -and (Test-Path $ortDir)) {
        $ortDll = Get-ChildItem -Path $ortDir -Filter "onnxruntime.dll" -Recurse -ErrorAction SilentlyContinue | Select-Object -First 1
        if ($ortDll) {
            Copy-Item -Path $ortDll.FullName -Destination $binDir -Force
            Write-Host "Deployed onnxruntime.dll from $($ortDll.FullName)" -ForegroundColor Green
            # Also copy any companion DLLs (onnxruntime_providers*.dll etc.)
            Get-ChildItem -Path $ortDll.DirectoryName -Filter "onnxruntime*.dll" | ForEach-Object {
                Copy-Item -Path $_.FullName -Destination $binDir -Force
                Write-Host "  + $($_.Name)" -ForegroundColor Gray
            }
        } else {
            Write-Host "WARNING: onnxruntime.dll not found in $ortDir" -ForegroundColor Yellow
        }
    } else {
        Write-Host "WARNING: ONNX Runtime directory not configured. Set -OnnxRuntimeDir or ALCEDO_ORT_DIR." -ForegroundColor Yellow
    }

    # --- System DLLs (icmui.dll, dwmapi.dll) ---
    # These are system DLLs that may not be present on older Windows versions.
    # Only deploy icmui.dll if it exists in the system directory; dwmapi.dll is
    # always present on Windows 10+ and should NOT be bundled.
    $sysDir = [System.Environment]::SystemDirectory
    $icmuiPath = Join-Path $sysDir "icmui.dll"
    if (Test-Path $icmuiPath) {
        Copy-Item -Path $icmuiPath -Destination $binDir -Force
        Write-Host "Deployed icmui.dll (system DLL)" -ForegroundColor Gray
    }
    # dwmapi.dll is a system DLL — do NOT redistribute it.
    # It ships with the OS. Log a note for the packaging manifest instead.
    Write-Host "NOTE: dwmapi.dll is a system DLL and will not be bundled (ships with Windows)." -ForegroundColor Cyan

    # --- Model files directory ---
    $modelsDir = Join-Path $repoRoot "alcedo_studio\src\config\models"
    $destModelsDir = Join-Path $binDir "models"
    if (Test-Path $modelsDir) {
        New-Item -ItemType Directory -Force -Path $destModelsDir | Out-Null
        Copy-Item -Path "$modelsDir\*" -Destination $destModelsDir -Recurse -Force
        Write-Host "Deployed model files to $destModelsDir" -ForegroundColor Green
    }

    # --- OpenCL ICD loader deployment ---
    $icdDir = $OpenCLIcdDir
    if (-not $icdDir) {
        # Try vcpkg installed location
        $vcpkgIcd = Join-Path $env:VCPKG_ROOT "installed\x64-windows\bin"
        if (Test-Path (Join-Path $vcpkgIcd "OpenCL.dll")) {
            $icdDir = $vcpkgIcd
        }
    }
    if ($icdDir -and (Test-Path $icdDir)) {
        $openclDll = Get-ChildItem -Path $icdDir -Filter "OpenCL.dll" -Recurse -ErrorAction SilentlyContinue | Select-Object -First 1
        if ($openclDll) {
            Copy-Item -Path $openclDll.FullName -Destination $binDir -Force
            Write-Host "Deployed OpenCL ICD loader (OpenCL.dll)" -ForegroundColor Green
        }
        # Deploy ICD loader .icd files if present
        $icdFiles = Get-ChildItem -Path $icdDir -Filter "*.icd" -Recurse -ErrorAction SilentlyContinue
        if ($icdFiles) {
            $icdDestDir = Join-Path $binDir "OpenCL\icd"
            New-Item -ItemType Directory -Force -Path $icdDestDir | Out-Null
            $icdFiles | ForEach-Object {
                Copy-Item -Path $_.FullName -Destination $icdDestDir -Force
                Write-Host "  + $($_.Name)" -ForegroundColor Gray
            }
        }
    } else {
        Write-Host "NOTE: OpenCL ICD loader not configured. Set -OpenCLIcdDir or ALCEDO_OPENCL_ICD_DIR if OpenCL is required." -ForegroundColor Cyan
    }
} else {
    Write-Host "WARNING: Install bin directory not found; skipping additional DLL deployment." -ForegroundColor Yellow
}

# ------------------------------------------------------------------
# 5. Report results
# ------------------------------------------------------------------
Write-Host ""
Write-Host "========================================" -ForegroundColor Green
Write-Host "  Packaging Complete" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green

$packages = Get-ChildItem -Path "$PackageOutDir\*" -Include *.msi,*.exe,*.zip
if ($packages) {
    foreach ($pkg in $packages) {
        $sizeMB = [math]::Round($pkg.Length / 1MB, 2)
        Write-Host "  Generated: $($pkg.Name) ($sizeMB MB)" -ForegroundColor Green
    }
} else {
    Write-Host "  No package files found in $PackageOutDir" -ForegroundColor Red
}

Write-Host ""

# ------------------------------------------------------------------
# 6. Tooling hints
# ------------------------------------------------------------------
$hasWix = (Test-CommandAvailable "candle.exe") -and (Test-CommandAvailable "light.exe")
$hasNsis = Test-CommandAvailable "makensis.exe"

if (-not $hasWix -and -not $hasNsis) {
    Write-Host "Notice: Neither WiX nor NSIS was detected. Only ZIP was generated." -ForegroundColor Yellow
    Write-Host ""
    Write-Host "To generate a high-compression installer, install one of the following:" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "  WiX Toolset v3.11 (MSI):" -ForegroundColor White
    Write-Host "    https://github.com/wixtoolset/wix3/releases/tag/wix3112rtm"
    Write-Host "    Install and ensure candle.exe / light.exe are on PATH."
    Write-Host ""
    Write-Host "  NSIS (high-compression EXE):" -ForegroundColor White
    Write-Host "    https://nsis.sourceforge.io/Download"
    Write-Host "    Install and ensure makensis.exe is on PATH."
    Write-Host ""
    Write-Host "After installing, re-run this script to produce MSI or EXE installers." -ForegroundColor Cyan
} else {
    if ($hasWix) { Write-Host "WiX detected   : MSI package enabled" -ForegroundColor Green }
    if ($hasNsis) { Write-Host "NSIS detected  : EXE package enabled" -ForegroundColor Green }
}

Write-Host ""
