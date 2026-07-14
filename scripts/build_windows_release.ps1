#requires -Version 5.1
<#
.SYNOPSIS
    All-in-one Windows build & package script for Alcedo Studio.
.DESCRIPTION
    This script performs the complete Windows release pipeline:
      1. Detect & validate prerequisites (MSVC, Qt6, CUDA, vcpkg, etc.)
      2. Bootstrap vcpkg dependencies
      3. Configure CMake with win_release preset
      4. Build all targets (alcedo_main, alcedo_mind, operators, etc.)
      5. Run tests
      6. Install runtime tree
      7. Verify install tree
      8. Package (NSIS EXE / WiX MSI / ZIP)
    Run from the repository root on a Windows machine.
.EXAMPLE
    .\scripts\build_windows_release.ps1
    .\scripts\build_windows_release.ps1 -SkipTests -SkipVcpkg
#>
param(
    [string]$BuildDir       = "$PSScriptRoot\..\build\release",
    [string]$InstallDir     = "$PSScriptRoot\..\build\install",
    [string]$PackageOutDir  = "$PSScriptRoot\..\build\release\package",
    [string]$Preset         = "win_release",
    [string]$QtPrefix       = "",
    [string]$CudaPath       = "",
    [string]$VcpkgRoot      = "",
    [string]$OpenCLRoot     = "",
    [switch]$SkipTests      = $false,
    [switch]$SkipVcpkg      = $false,
    [switch]$SkipPackage    = $false,
    [switch]$SkipVerify     = $false,
    [int]$ParallelJobs      = [System.Environment]::ProcessorCount
)

$ErrorActionPreference = 'Stop'
$repoRoot = Resolve-Path "$PSScriptRoot\.."

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

function Find-Qt6 {
    if (-not [string]::IsNullOrWhiteSpace($QtPrefix)) { return $QtPrefix }
    if (-not [string]::IsNullOrWhiteSpace($env:ALCEDO_QT_PREFIX)) { return $env:ALCEDO_QT_PREFIX }
    if (-not [string]::IsNullOrWhiteSpace($env:Qt6_Dir)) { return $env:Qt6_Dir }

    # Search common install paths
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
    if (-not [string]::IsNullOrWhiteSpace($env:VCPKG_ROOT)) { return $env:VCPKG_ROOT }

    # Check for local vcpkg in repo
    $localVcpkg = Join-Path $repoRoot "vcpkg"
    if (Test-Path (Join-Path $localVcpkg "vcpkg.exe")) { return $localVcpkg }

    # Check standard location
    $stdVcpkg = "C:\vcpkg"
    if (Test-Path (Join-Path $stdVcpkg "vcpkg.exe")) { return $stdVcpkg }

    return ""
}

function Find-CUDA {
    if (-not [string]::IsNullOrWhiteSpace($CudaPath)) { return $CudaPath }
    if (-not [string]::IsNullOrWhiteSpace($env:CUDA_PATH)) { return $env:CUDA_PATH }

    $cudaRoot = "${env:ProgramFiles}\NVIDIA GPU Computing Toolkit\CUDA\v12.4"
    if (Test-Path $cudaRoot) { return $cudaRoot }
    $cudaRoot = "${env:ProgramFiles}\NVIDIA GPU Computing Toolkit\CUDA\v12.5"
    if (Test-Path $cudaRoot) { return $cudaRoot }
    $cudaRoot = "${env:ProgramFiles}\NVIDIA GPU Computing Toolkit\CUDA\v12.6"
    if (Test-Path $cudaRoot) { return $cudaRoot }
    return ""
}

function Find-OpenCL {
    if (-not [string]::IsNullOrWhiteSpace($OpenCLRoot)) { return $OpenCLRoot }
    if (-not [string]::IsNullOrWhiteSpace($env:OPENCL_ROOT)) { return $env:OPENCL_ROOT }

    # Check for Intel/OpenCL SDK
    $oclSdk = "${env:ProgramFiles(x86)}\Intel\OpenCL SDK"
    if (Test-Path $oclSdk) { return $oclSdk }
    $oclSdk = "${env:ProgramFiles}\Intel\OpenCL SDK"
    if (Test-Path $oclSdk) { return $oclSdk }

    # Check for Khronos OpenCL SDK from CMake-installed location
    $oclCmake = "${env:ProgramFiles}\OpenCL-SDK"
    if (Test-Path $oclCmake) { return $oclCmake }
    return ""
}

# =====================================================================
# Step 1: Prerequisite detection & validation
# =====================================================================
Write-Section "Step 1/8: Prerequisites Check"

$qtDir = Find-Qt6
if ([string]::IsNullOrWhiteSpace($qtDir)) {
    Write-Host "ERROR: Qt6 not found!" -ForegroundColor Red
    Write-Host "Install Qt6 (MSVC 2019/2022 64-bit) and set -QtPrefix or ALCEDO_QT_PREFIX env var." -ForegroundColor Yellow
    exit 1
}
Write-Host "Qt6          : $qtDir" -ForegroundColor Green

$vcpkgDir = Find-Vcpkg
if ([string]::IsNullOrWhiteSpace($vcpkgDir)) {
    Write-Host "WARNING: vcpkg not found. Set -VcpkgRoot or VCPKG_ROOT env var." -ForegroundColor Yellow
    Write-Host "         Dependencies will rely on system-installed libraries." -ForegroundColor Yellow
} else {
    Write-Host "vcpkg        : $vcpkgDir" -ForegroundColor Green
}

$cudaDir = Find-CUDA
if (-not [string]::IsNullOrWhiteSpace($cudaDir)) {
    Write-Host "CUDA         : $cudaDir" -ForegroundColor Green
} else {
    Write-Host "CUDA         : not found (GPU CUDA pipeline disabled)" -ForegroundColor Yellow
}

$oclDir = Find-OpenCL
if (-not [string]::IsNullOrWhiteSpace($oclDir)) {
    Write-Host "OpenCL SDK   : $oclDir" -ForegroundColor Green
} else {
    Write-Host "OpenCL SDK   : not found (OpenCL backend disabled)" -ForegroundColor Yellow
}

$hasNsis = Test-CommandAvailable "makensis.exe"
$hasWix  = (Test-CommandAvailable "candle.exe") -and (Test-CommandAvailable "light.exe")
Write-Host "NSIS         : $(if ($hasNsis) { 'detected' } else { 'not found' })" -ForegroundColor $(if ($hasNsis) { 'Green' } else { 'Yellow' })
Write-Host "WiX          : $(if ($hasWix) { 'detected' } else { 'not found' })" -ForegroundColor $(if ($hasWix) { 'Green' } else { 'Yellow' })

# Check MSVC
$msvcEnvScript = Join-Path $repoRoot "scripts\msvc_env.cmd"
if (-not (Test-Path $msvcEnvScript)) {
    # Try to find vcvarsall.bat directly
    $vsWhere = "${env:ProgramFiles(x86)}\Microsoft Visual Studio\Installer\vswhere.exe"
    if (Test-Path $vsWhere) {
        $vsInstallPath = & $vsWhere -latest -property installationPath 2>$null
        $vcvarsall = Join-Path $vsInstallPath "VC\Auxiliary\Build\vcvarsall.bat"
        if (Test-Path $vcvarsall) {
            Write-Host "MSVC         : $vcvarsall" -ForegroundColor Green
        } else {
            Write-Host "WARNING: MSVC vcvarsall.bat not found via vswhere." -ForegroundColor Yellow
        }
    } else {
        Write-Host "WARNING: Visual Studio not detected. Build may fail." -ForegroundColor Yellow
    }
} else {
    Write-Host "MSVC env     : $msvcEnvScript" -ForegroundColor Green
}

# Check Rust (for alcedo_mind sidecar)
$hasRust = Test-CommandAvailable "cargo.exe"
if ($hasRust) {
    $rustVer = & rustc --version 2>$null
    Write-Host "Rust         : $rustVer" -ForegroundColor Green
} else {
    Write-Host "Rust         : not found (alcedo_mind sidecar will be skipped)" -ForegroundColor Yellow
}

# =====================================================================
# Step 2: Bootstrap vcpkg dependencies
# =====================================================================
if (-not $SkipVcpkg -and -not [string]::IsNullOrWhiteSpace($vcpkgDir)) {
    Write-Section "Step 2/8: vcpkg Dependency Bootstrap"

    $vcpkgExe = Join-Path $vcpkgDir "vcpkg.exe"
    if (-not (Test-Path $vcpkgExe)) {
        Write-Host "Bootstrapping vcpkg ..." -ForegroundColor Yellow
        Push-Location $vcpkgDir
        & .\bootstrap-vcpkg.bat -disableMetrics
        if ($LASTEXITCODE -ne 0) { throw "vcpkg bootstrap failed." }
        Pop-Location
    }

    Write-Host "Installing vcpkg dependencies (x64-windows) ..." -ForegroundColor Yellow
    & $vcpkgExe install --triplet x64-windows --recurse
    if ($LASTEXITCODE -ne 0) {
        Write-Host "WARNING: Some vcpkg packages may have failed to install." -ForegroundColor Yellow
        Write-Host "         Continuing - CMake will report missing dependencies." -ForegroundColor Yellow
    }
} elseif ($SkipVcpkg) {
    Write-Section "Step 2/8: vcpkg (SKIPPED)"
} else {
    Write-Section "Step 2/8: vcpkg (NOT AVAILABLE - using system libraries)"
}

# =====================================================================
# Step 3: CMake Configure
# =====================================================================
Write-Section "Step 3/8: CMake Configure"

$buildDir = Resolve-Path (New-Item -ItemType Directory -Force -Path $BuildDir -PassThru).FullName

# Build CMake prefix path
$cmakePrefixPaths = @()
if (-not [string]::IsNullOrWhiteSpace($qtDir))    { $cmakePrefixPaths += $qtDir }
if (-not [string]::IsNullOrWhiteSpace($vcpkgDir)) { $cmakePrefixPaths += "$vcpkgDir\installed\x64-windows" }
if (-not [string]::IsNullOrWhiteSpace($oclDir))   { $cmakePrefixPaths += $oclDir }
$cmakePrefixPath = $cmakePrefixPaths -join ";"

# Build CMake configure arguments
$cmakeArgs = @(
    "--preset", $Preset,
    "-DCMAKE_PREFIX_PATH=`"$cmakePrefixPath`""
)

if (-not [string]::IsNullOrWhiteSpace($cudaDir)) {
    $cmakeArgs += "-DALCEDO_CUDA_ROOT=`"$cudaDir`""
}

Write-Host "CMake args: $($cmakeArgs -join ' ')" -ForegroundColor Gray
& cmake @cmakeArgs
if ($LASTEXITCODE -ne 0) { throw "CMake configure failed." }

# =====================================================================
# Step 4: Build
# =====================================================================
Write-Section "Step 4/8: Build"

Write-Host "Building with $ParallelJobs parallel jobs ..." -ForegroundColor Yellow
& cmake --build $buildDir --parallel $ParallelJobs --config Release
if ($LASTEXITCODE -ne 0) { throw "Build failed." }

Write-Host "Build succeeded." -ForegroundColor Green

# =====================================================================
# Step 5: Tests
# =====================================================================
if (-not $SkipTests) {
    Write-Section "Step 5/8: Tests"
    & ctest --test-dir $buildDir -C Release --output-on-failure -j $ParallelJobs
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

# Resolve DuckDB extensions
$duckDbExtensionDir = Join-Path $repoRoot "alcedo_studio\third_party\libduckdb-windows\extensions"
$vssExt = if ($env:ALCEDO_DUCKDB_VSS_EXTENSION) { $env:ALCEDO_DUCKDB_VSS_EXTENSION } elseif (Test-Path (Join-Path $duckDbExtensionDir "vss.duckdb_extension")) { Join-Path $duckDbExtensionDir "vss.duckdb_extension" } else { "" }
$ftsExt = if ($env:ALCEDO_DUCKDB_FTS_EXTENSION) { $env:ALCEDO_DUCKDB_FTS_EXTENSION } elseif (Test-Path (Join-Path $duckDbExtensionDir "fts.duckdb_extension")) { Join-Path $duckDbExtensionDir "fts.duckdb_extension" } else { "" }

$installArgs = @(
    "--install", $buildDir,
    "--prefix", (Resolve-Path (New-Item -ItemType Directory -Force -Path $InstallDir -PassThru).FullName),
    "--config", "Release"
)
if (-not [string]::IsNullOrWhiteSpace($vssExt)) {
    $installArgs += "-DALCEDO_DUCKDB_VSS_EXTENSION=$vssExt"
}
if (-not [string]::IsNullOrWhiteSpace($ftsExt)) {
    $installArgs += "-DALCEDO_DUCKDB_FTS_EXTENSION=$ftsExt"
}

& cmake @installArgs
if ($LASTEXITCODE -ne 0) { throw "Install failed." }

Write-Host "Install tree ready at: $InstallDir" -ForegroundColor Green

# =====================================================================
# Step 7: Verify install tree
# =====================================================================
if (-not $SkipVerify) {
    Write-Section "Step 7/8: Verify Install Tree"

    $verifyScript = Join-Path $repoRoot "scripts\verify_windows_install_tree.ps1"
    if (Test-Path $verifyScript) {
        & powershell -ExecutionPolicy Bypass -File $verifyScript -InstallDir $InstallDir
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
# Step 8: Package (NSIS EXE / WiX MSI / ZIP)
# =====================================================================
if (-not $SkipPackage) {
    Write-Section "Step 8/8: Package"

    New-Item -ItemType Directory -Force -Path $PackageOutDir | Out-Null

    # Determine available generators
    $generators = @("ZIP")
    if ($hasNsis) { $generators += "NSIS" }
    if ($hasWix)  { $generators += "WIX" }

    Write-Host "Packaging with generators: $($generators -join ', ')" -ForegroundColor Yellow

    foreach ($gen in $generators) {
        Write-Host "Running CPack ($gen) ..." -ForegroundColor Yellow
        & ctest --build-dir $buildDir -T Package -C Release -- -G $gen -B $PackageOutDir
        if ($LASTEXITCODE -ne 0) {
            Write-Host "WARNING: CPack ($gen) failed." -ForegroundColor Yellow
        }
    }

    # Report results
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
Write-Host "  Build dir   : $buildDir" -ForegroundColor White
Write-Host "  Install dir : $InstallDir" -ForegroundColor White
Write-Host "  Package dir : $PackageOutDir" -ForegroundColor White
Write-Host ""
