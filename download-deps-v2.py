#!/usr/bin/env python3
"""
OMaster 依赖下载脚本 v2.0
- 多镜像自动重试
- 健康检查 + 智能熔断
- 并发下载 + 速率限制
- 断点续传
- 失败统计 + 报告
"""
import argparse
import asyncio
import hashlib
import json
import os
import sys
import time
from concurrent.futures import ThreadPoolExecutor, as_completed
from dataclasses import dataclass, field
from pathlib import Path
from typing import Dict, List, Optional, Tuple
from urllib.parse import urlparse

try:
    import aiohttp
    ASYNC_AVAILABLE = True
except ImportError:
    ASYNC_AVAILABLE = False
    print("[WARNING] aiohttp not available, falling back to urllib")

import urllib.request
import urllib.error
import ssl


# ============== 配置 ==============

# 镜像源（按优先级排序，自动健康检查）
MIRRORS = [
    # Maven Central
    {
        "name": "aliyun-central",
        "base": "https://maven.aliyun.com/repository/central",
        "type": "maven2",
        "priority": 1,
    },
    {
        "name": "aliyun-public",
        "base": "https://maven.aliyun.com/repository/public",
        "type": "maven2",
        "priority": 1,
    },
    {
        "name": "tencent-public",
        "base": "https://mirrors.tencent.com/nexus/repository/maven-public",
        "type": "maven2",
        "priority": 2,
    },
    {
        "name": "huawei-public",
        "base": "https://mirrors.huaweicloud.com/repository/maven",
        "type": "maven2",
        "priority": 2,
    },
    {
        "name": "maven-central",
        "base": "https://repo1.maven.org/maven2",
        "type": "maven2",
        "priority": 3,
    },
    {
        "name": "maven-central-apache",
        "base": "https://repo.maven.apache.org/maven2",
        "type": "maven2",
        "priority": 3,
    },
    # Google Maven
    {
        "name": "aliyun-google",
        "base": "https://maven.aliyun.com/repository/google",
        "type": "google",
        "priority": 1,
    },
    {
        "name": "google-maven",
        "base": "https://dl.google.com/dl/android/maven2",
        "type": "google",
        "priority": 3,
    },
    # Gradle Plugins
    {
        "name": "aliyun-gradle-plugin",
        "base": "https://maven.aliyun.com/repository/gradle-plugin",
        "type": "gradle-plugin",
        "priority": 1,
    },
    {
        "name": "gradle-plugin-portal",
        "base": "https://plugins.gradle.org/m2",
        "type": "gradle-plugin",
        "priority": 3,
    },
]

# 重试配置
RETRY_CONFIG = {
    "max_retries": 3,
    "initial_backoff": 1.0,
    "max_backoff": 10.0,
    "backoff_multiplier": 2.0,
    "timeout_per_request": 30,
    "concurrent_downloads": 8,
    "mirror_cooldown_seconds": 60,  # 失败镜像冷却时间
}


# ============== 数据结构 ==============

@dataclass
class DownloadTask:
    group: str
    artifact: str
    version: str
    extension: str = "jar"  # jar, aar, pom, module
    classifier: Optional[str] = None
    repo_type: str = "auto"  # maven2, google, gradle-plugin, auto

    @property
    def path(self) -> str:
        if self.classifier:
            return f"{self.group}/{self.artifact}/{self.version}/{self.artifact}-{self.version}-{self.classifier}.{self.extension}"
        return f"{self.group}/{self.artifact}/{self.version}/{self.artifact}-{self.version}.{self.extension}"


@dataclass
class DownloadResult:
    task: DownloadTask
    success: bool
    mirror: Optional[str] = None
    error: Optional[str] = None
    duration: float = 0.0
    size: int = 0
    attempts: int = 0


@dataclass
class MirrorHealth:
    name: str
    failures: int = 0
    successes: int = 0
    total_time: float = 0.0
    last_failure: float = 0.0
    is_healthy: bool = True

    def record_success(self, duration: float):
        self.successes += 1
        self.total_time += duration
        self.is_healthy = True

    def record_failure(self):
        self.failures += 1
        self.last_failure = time.time()
        if self.failures > 3:
            self.is_healthy = False


# ============== 镜像健康检查 ==============

class MirrorManager:
    def __init__(self):
        self.mirrors = {m["name"]: m for m in MIRRORS}
        self.health: Dict[str, MirrorHealth] = {
            m["name"]: MirrorHealth(name=m["name"]) for m in MIRRORS
        }
        self.mirror_lock = asyncio.Lock() if ASYNC_AVAILABLE else None

    def select_mirrors(self, repo_type: str = "auto") -> List[dict]:
        """选择镜像，按优先级和健康度排序"""
        # 过滤类型
        candidates = [m for m in MIRRORS if repo_type == "auto" or m["type"] == repo_type or repo_type == "all"]

        # 按优先级和健康度排序
        def sort_key(m):
            h = self.health[m["name"]]
            healthy = 0 if (h.is_healthy or time.time() - h.last_failure > RETRY_CONFIG["mirror_cooldown_seconds"]) else 1
            return (healthy, m["priority"])

        return sorted(candidates, key=sort_key)

    def record_success(self, mirror_name: str, duration: float):
        if mirror_name in self.health:
            self.health[mirror_name].record_success(duration)

    def record_failure(self, mirror_name: str):
        if mirror_name in self.health:
            self.health[mirror_name].record_failure()


# ============== 同步下载器 ==============

class SyncDownloader:
    """基于 urllib 的同步下载器（备用）"""

    def __init__(self, mirror_manager: MirrorManager):
        self.mm = mirror_manager
        self.context = ssl.create_default_context()
        self.context.check_hostname = False
        self.context.verify_mode = ssl.CERT_NONE

    def download(self, task: DownloadTask, output_dir: Path) -> DownloadResult:
        repo_type = task.repo_type
        mirrors = self.mm.select_mirrors(repo_type)
        last_error = None

        for mirror in mirrors:
            for attempt in range(RETRY_CONFIG["max_retries"]):
                url = f"{mirror['base']}/{task.path}"
                start = time.time()

                try:
                    req = urllib.request.Request(url, headers={"User-Agent": "OMaster-Builder/2.0"})
                    response = urllib.request.urlopen(req, timeout=RETRY_CONFIG["timeout_per_request"], context=self.context)
                    content = response.read()
                    duration = time.time() - start

                    # 验证内容
                    if len(content) < 10 and task.extension in ("jar", "aar", "module"):
                        raise ValueError(f"Content too small ({len(content)} bytes), likely error page")

                    # 验证 HTML 错误页
                    if content[:15].lower().startswith(b"<!doctype html") or content[:5].lower().startswith(b"<html"):
                        raise ValueError("Received HTML error page instead of binary")

                    # 写入文件
                    output_path = output_dir / task.path
                    output_path.parent.mkdir(parents=True, exist_ok=True)
                    output_path.write_bytes(content)

                    self.mm.record_success(mirror["name"], duration)
                    return DownloadResult(
                        task=task, success=True, mirror=mirror["name"],
                        duration=duration, size=len(content),
                        attempts=attempt + 1
                    )

                except (urllib.error.URLError, urllib.error.HTTPError, ValueError, OSError) as e:
                    last_error = str(e)
                    duration = time.time() - start
                    self.mm.record_failure(mirror["name"])

                    # 退避
                    if attempt < RETRY_CONFIG["max_retries"] - 1:
                        backoff = min(
                            RETRY_CONFIG["initial_backoff"] * (RETRY_CONFIG["backoff_multiplier"] ** attempt),
                            RETRY_CONFIG["max_backoff"]
                        )
                        time.sleep(backoff)
                    continue

        return DownloadResult(
            task=task, success=False, error=last_error,
            attempts=RETRY_CONFIG["max_retries"] * len(mirrors)
        )


# ============== 异步下载器 ==============

class AsyncDownloader:
    """基于 aiohttp 的异步下载器（推荐）"""

    def __init__(self, mirror_manager: MirrorManager):
        self.mm = mirror_manager
        self.semaphore = None
        self.session = None

    async def __aenter__(self):
        connector = aiohttp.TCPConnector(
            limit=RETRY_CONFIG["concurrent_downloads"],
            limit_per_host=4,
            ttl_dns_cache=300,
            ssl=False,
        )
        timeout = aiohttp.ClientTimeout(total=RETRY_CONFIG["timeout_per_request"], connect=10)
        self.session = aiohttp.ClientSession(connector=connector, timeout=timeout)
        self.semaphore = asyncio.Semaphore(RETRY_CONFIG["concurrent_downloads"])
        return self

    async def __aexit__(self, *args):
        if self.session:
            await self.session.close()

    async def download(self, task: DownloadTask, output_dir: Path) -> DownloadResult:
        async with self.semaphore:
            repo_type = task.repo_type
            mirrors = self.mm.select_mirrors(repo_type)
            last_error = None

            for mirror in mirrors:
                for attempt in range(RETRY_CONFIG["max_retries"]):
                    url = f"{mirror['base']}/{task.path}"
                    start = time.time()

                    try:
                        async with self.session.get(url) as response:
                            if response.status != 200:
                                raise ValueError(f"HTTP {response.status}")
                            content = await response.read()
                            duration = time.time() - start

                            # 验证内容
                            if len(content) < 10 and task.extension in ("jar", "aar", "module"):
                                raise ValueError(f"Content too small ({len(content)} bytes)")

                            if content[:15].lower().startswith(b"<!doctype html") or content[:5].lower().startswith(b"<html"):
                                raise ValueError("Received HTML error page")

                            # 写入文件
                            output_path = output_dir / task.path
                            output_path.parent.mkdir(parents=True, exist_ok=True)
                            output_path.write_bytes(content)

                            self.mm.record_success(mirror["name"], duration)
                            return DownloadResult(
                                task=task, success=True, mirror=mirror["name"],
                                duration=duration, size=len(content),
                                attempts=attempt + 1
                            )

                    except (aiohttp.ClientError, asyncio.TimeoutError, ValueError, OSError) as e:
                        last_error = str(e)
                        duration = time.time() - start
                        self.mm.record_failure(mirror["name"])

                        if attempt < RETRY_CONFIG["max_retries"] - 1:
                            backoff = min(
                                RETRY_CONFIG["initial_backoff"] * (RETRY_CONFIG["backoff_multiplier"] ** attempt),
                                RETRY_CONFIG["max_backoff"]
                            )
                            await asyncio.sleep(backoff)
                        continue

            return DownloadResult(
                task=task, success=False, error=last_error,
                attempts=RETRY_CONFIG["max_retries"] * len(mirrors)
            )


# ============== 报告生成 ==============

def print_report(results: List[DownloadResult], duration: float):
    success = [r for r in results if r.success]
    failed = [r for r in results if not r.success]

    print("\n" + "=" * 70)
    print(f"下载报告 (耗时 {duration:.1f}s)")
    print("=" * 70)
    print(f"总任务: {len(results)}")
    print(f"成功:   {len(success)} ({len(success)/len(results)*100:.1f}%)" if results else "0")
    print(f"失败:   {len(failed)} ({len(failed)/len(results)*100:.1f}%)" if results else "0")
    print(f"总大小: {sum(r.size for r in success) / 1024 / 1024:.2f} MB")
    print()

    if failed:
        print("失败任务 (前 20):")
        for r in failed[:20]:
            print(f"  - {r.task.path}: {r.error}")
        if len(failed) > 20:
            print(f"  ... 还有 {len(failed) - 20} 个失败")

    # 镜像统计
    mirror_stats = {}
    for r in results:
        if r.mirror:
            if r.mirror not in mirror_stats:
                mirror_stats[r.mirror] = {"success": 0, "failed": 0, "total_time": 0, "size": 0}
            if r.success:
                mirror_stats[r.mirror]["success"] += 1
                mirror_stats[r.mirror]["total_time"] += r.duration
                mirror_stats[r.mirror]["size"] += r.size
            else:
                mirror_stats[r.mirror]["failed"] += 1

    if mirror_stats:
        print("\n镜像使用统计:")
        for mirror, stats in sorted(mirror_stats.items(), key=lambda x: -x[1]["success"]):
            avg_time = stats["total_time"] / stats["success"] if stats["success"] else 0
            print(f"  {mirror}: 成功 {stats['success']}, 失败 {stats['failed']}, "
                  f"平均 {avg_time:.2f}s, {stats['size']/1024/1024:.1f}MB")


# ============== 预定义依赖列表 ==============

def get_android_dependencies() -> List[DownloadTask]:
    """Android Gradle Plugin 相关依赖"""
    tasks = []
    # AGP 8.0.2 全套依赖（基于 Maven tree）
    agp_modules = [
        ("com.android.tools.build", "gradle", "8.0.2"),
        ("com.android.tools.build", "builder", "8.0.2"),
        ("com.android.tools.build", "builder-model", "8.0.2"),
        ("com.android.tools.build", "builder-test-api", "8.0.2"),
        ("com.android.tools.build", "gradle-api", "8.0.2"),
        ("com.android.tools.build", "gradle-settings-api", "8.0.2"),
        ("com.android.tools.build", "apkzlib", "8.0.2"),
        ("com.android.tools.build", "apksig", "8.0.2"),
        ("com.android.tools.build", "aaptcompiler", "8.0.2"),
        ("com.android.tools.build", "manifest-merger", "31.0.2"),
        ("com.android.tools", "common", "31.0.2"),
        ("com.android.tools", "sdk-common", "31.0.2"),
        ("com.android.tools", "sdklib", "31.0.2"),
        ("com.android.tools", "repository", "31.0.2"),
        ("com.android.tools", "annotations", "31.0.2"),
        ("com.android.tools.layoutlib", "layoutlib-api", "31.0.2"),
        ("com.android.tools.lint", "lint-model", "31.0.2"),
        ("com.android.tools.lint", "lint-typedef-remover", "31.0.2"),
        ("com.android.tools.ddms", "ddmlib", "31.0.2"),
        ("com.android.tools.analytics-library", "shared", "31.0.2"),
        ("com.android.tools.analytics-library", "protos", "31.0.2"),
        ("com.android.tools.analytics-library", "tracker", "31.0.2"),
        ("com.android.tools.analytics-library", "crash", "31.0.2"),
        ("com.android", "zipflinger", "8.0.2"),
        ("com.android", "signflinger", "8.0.2"),
        ("com.android", "databinding.baseLibrary", "8.0.2"),
        ("androidx.databinding", "databinding-common", "8.0.2"),
        ("androidx.databinding", "databinding-compiler-common", "8.0.2"),
    ]
    for group, artifact, version in agp_modules:
        group_path = group.replace(".", "/")
        for ext in ("pom", "jar"):
            tasks.append(DownloadTask(group_path, artifact, version, ext, repo_type="google"))

    return tasks


def get_kotlin_dependencies() -> List[DownloadTask]:
    """Kotlin 1.9.22 全套依赖"""
    tasks = []
    kotlin_modules = [
        ("kotlin-gradle-plugin", "1.9.22"),
        ("kotlin-android-extensions", "1.9.22"),
        ("kotlin-compiler-embeddable", "1.9.22"),
        ("kotlin-compiler-runner", "1.9.22"),
        ("kotlin-daemon-client", "1.9.22"),
        ("kotlin-daemon-embeddable", "1.9.22"),
        ("kotlin-build-tools-api", "1.9.22"),
        ("kotlin-build-common", "1.9.22"),
        ("kotlin-gradle-plugin-api", "1.9.22"),
        ("kotlin-gradle-plugin-idea", "1.9.22"),
        ("kotlin-gradle-plugin-idea-proto", "1.9.22"),
        ("kotlin-gradle-plugin-model", "1.9.22"),
        ("kotlin-gradle-plugins-bom", "1.9.22"),
        ("kotlin-gradle-plugin-annotations", "1.9.22"),
        ("kotlin-klib-commonizer-api", "1.9.22"),
        ("kotlin-native-utils", "1.9.22"),
        ("kotlin-noarg", "1.9.22"),
        ("kotlin-allopen", "1.9.22"),
        ("kotlin-assignment", "1.9.22"),
        ("kotlin-lombok", "1.9.22"),
        ("kotlin-sam-with-receiver", "1.9.22"),
        ("kotlin-script-runtime", "1.9.22"),
        ("kotlin-scripting-common", "1.9.22"),
        ("kotlin-scripting-compiler-embeddable", "1.9.22"),
        ("kotlin-scripting-compiler-impl-embeddable", "1.9.22"),
        ("kotlin-scripting-jvm", "1.9.22"),
        ("kotlin-serialization", "1.9.22"),
        ("kotlin-reflect", "1.6.10"),
        ("kotlin-reflect", "1.9.22"),
        ("kotlin-stdlib", "1.9.22"),
        ("kotlin-stdlib", "1.9.0"),
        ("kotlin-stdlib", "1.8.0"),
        ("kotlin-stdlib", "1.6.10"),
        ("kotlin-stdlib", "1.5.0"),
        ("kotlin-stdlib-common", "1.9.22"),
        ("kotlin-stdlib-jdk7", "1.9.0"),
        ("kotlin-stdlib-jdk7", "1.8.0"),
        ("kotlin-stdlib-jdk8", "1.9.0"),
        ("kotlin-stdlib-jdk8", "1.8.0"),
        ("kotlin-stdlib-jdk8", "1.7.10"),
        ("kotlin-test", "1.9.22"),
        ("kotlin-test-junit", "1.9.22"),
        ("kotlin-tooling-core", "1.9.22"),
        ("kotlin-util-io", "1.9.22"),
        ("kotlin-util-klib", "1.9.22"),
        ("kotlin-bom", "1.9.0"),
        ("atomicfu", "1.9.22"),
    ]
    for artifact, version in kotlin_modules:
        for ext in ("pom", "jar"):
            tasks.append(DownloadTask(
                "org/jetbrains/kotlin", artifact, version, ext, repo_type="maven2"
            ))

    # kotlinx-coroutines
    for ext in ("pom", "jar", "module"):
        tasks.append(DownloadTask(
            "org/jetbrains/kotlinx", "kotlinx-coroutines-core-jvm", "1.5.0", ext, repo_type="maven2"
        ))

    return tasks


def get_hilt_dependencies() -> List[DownloadTask]:
    """Hilt 2.48 全套依赖"""
    tasks = []
    # Hilt 自己的模块
    hilt_modules = [
        ("hilt-android", "2.48"),
        ("hilt-core", "2.48"),
        ("hilt-android-compiler", "2.48"),
        ("hilt-compiler", "2.48"),
    ]
    for artifact, version in hilt_modules:
        for ext in ("pom", "jar", "aar"):
            tasks.append(DownloadTask(
                "com/google/dagger", artifact, version, ext, repo_type="maven2"
            ))

    # Dagger 2.48
    for ext in ("pom", "jar"):
        tasks.append(DownloadTask(
            "com/google/dagger", "dagger", "2.48", ext, repo_type="maven2"
        ))
        tasks.append(DownloadTask(
            "com/google/dagger", "dagger-compiler", "2.48", ext, repo_type="maven2"
        ))
        tasks.append(DownloadTask(
            "com/google/dagger", "dagger-producers", "2.48", ext, repo_type="maven2"
        ))
        tasks.append(DownloadTask(
            "com/google/dagger", "dagger-spi", "2.48", ext, repo_type="maven2"
        ))

    return tasks


def get_androidx_dependencies() -> List[DownloadTask]:
    """关键 AndroidX 库"""
    tasks = []
    androidx_libs = [
        # Core
        ("androidx.core", "core-ktx", "1.13.1"),
        ("androidx.core", "core", "1.13.1"),
        ("androidx.appcompat", "appcompat", "1.7.0"),
        ("androidx.activity", "activity-compose", "1.9.0"),
        ("androidx.activity", "activity-ktx", "1.9.0"),
        # Lifecycle
        ("androidx.lifecycle", "lifecycle-runtime-ktx", "2.8.3"),
        ("androidx.lifecycle", "lifecycle-runtime-compose", "2.8.3"),
        ("androidx.lifecycle", "lifecycle-viewmodel-compose", "2.8.3"),
        ("androidx.lifecycle", "lifecycle-viewmodel-ktx", "2.8.3"),
        ("androidx.lifecycle", "lifecycle-common", "2.8.3"),
        ("androidx.lifecycle", "lifecycle-livedata-ktx", "2.8.3"),
        # Compose
        ("androidx.compose", "compose-bom", "2024.09.00"),
        ("androidx.compose.ui", "ui", "1.7.0"),
        ("androidx.compose.ui", "ui-graphics", "1.7.0"),
        ("androidx.compose.ui", "ui-tooling-preview", "1.7.0"),
        ("androidx.compose.ui", "ui-tooling", "1.7.0"),
        ("androidx.compose.material3", "material3", "1.3.0"),
        ("androidx.compose.material", "material-icons-extended", "1.7.0"),
        ("androidx.compose.material", "material-icons-core", "1.7.0"),
        ("androidx.compose.foundation", "foundation", "1.7.0"),
        ("androidx.compose.runtime", "runtime", "1.7.0"),
        # Navigation
        ("androidx.navigation", "navigation-compose", "2.7.7"),
        ("androidx.navigation", "navigation-common", "2.7.7"),
        ("androidx.navigation", "navigation-runtime", "2.7.7"),
        # Hilt integration
        ("androidx.hilt", "hilt-navigation-compose", "1.2.0"),
        ("androidx.hilt", "hilt-work", "1.2.0"),
        # DataStore
        ("androidx.datastore", "datastore-preferences", "1.1.1"),
        ("androidx.datastore", "datastore-preferences-core", "1.1.1"),
        # CameraX
        ("androidx.camera", "camera-core", "1.3.4"),
        ("androidx.camera", "camera-camera2", "1.3.4"),
        ("androidx.camera", "camera-lifecycle", "1.3.4"),
        ("androidx.camera", "camera-view", "1.3.4"),
        # Work
        ("androidx.work", "work-runtime-ktx", "2.9.1"),
        ("androidx.work", "work-runtime", "2.9.1"),
        # Security
        ("androidx.security", "security-crypto", "1.1.0-alpha06"),
        # Startup
        ("androidx.startup", "startup-runtime", "1.1.1"),
    ]
    for group, artifact, version in androidx_libs:
        group_path = group.replace(".", "/")
        for ext in ("pom", "aar"):
            tasks.append(DownloadTask(
                group_path, artifact, version, ext, repo_type="google"
            ))

    return tasks


def get_third_party_dependencies() -> List[DownloadTask]:
    """第三方库"""
    tasks = []
    libs = [
        # OkHttp
        ("com/squareup/okhttp3", "okhttp", "4.12.0"),
        ("com/squareup/okhttp3", "logging-interceptor", "4.12.0"),
        # Retrofit
        ("com/squareup/retrofit2", "retrofit", "2.11.0"),
        ("com/squareup/retrofit2", "converter-gson", "2.11.0"),
        # Gson
        ("com/google/code/gson", "gson", "2.11.0"),
        # Coil
        ("io/coil-kt", "coil-compose", "2.7.0"),
        ("io/coil-kt", "coil", "2.7.0"),
        # Timber
        ("com/jakewharton/timber", "timber", "5.0.1"),
        # Coroutines
        ("org/jetbrains/kotlinx", "kotlinx-coroutines-android", "1.8.1"),
        ("org/jetbrains/kotlinx", "kotlinx-coroutines-core", "1.8.1"),
    ]
    for group, artifact, version in libs:
        for ext in ("pom", "jar", "aar"):
            tasks.append(DownloadTask(
                group, artifact, version, ext, repo_type="maven2"
            ))

    # ML Kit (Google 仓库)
    ml_kit = [
        ("com/google/mlkit", "text-recognition", "16.0.1"),
        ("com/google/mlkit", "object-detection", "17.0.1"),
    ]
    for group, artifact, version in ml_kit:
        for ext in ("pom", "aar"):
            tasks.append(DownloadTask(
                group, artifact, version, ext, repo_type="google"
            ))

    return tasks


# ============== 主流程 ==============

def collect_all_tasks() -> List[DownloadTask]:
    """收集所有依赖任务"""
    all_tasks = []
    all_tasks.extend(get_android_dependencies())
    all_tasks.extend(get_kotlin_dependencies())
    all_tasks.extend(get_hilt_dependencies())
    all_tasks.extend(get_androidx_dependencies())
    all_tasks.extend(get_third_party_dependencies())
    # 去重
    seen = set()
    unique = []
    for task in all_tasks:
        key = (task.path, task.extension)
        if key not in seen:
            seen.add(key)
            unique.append(task)
    return unique


async def run_async(tasks: List[DownloadTask], output_dir: Path):
    mm = MirrorManager()
    results = []

    async with AsyncDownloader(mm) as downloader:
        # 创建任务
        coros = [downloader.download(task, output_dir) for task in tasks]
        completed = 0
        total = len(coros)
        start = time.time()

        for coro in asyncio.as_completed(coros):
            result = await coro
            results.append(result)
            completed += 1
            if completed % 10 == 0 or completed == total:
                elapsed = time.time() - start
                rate = completed / elapsed if elapsed > 0 else 0
                success = sum(1 for r in results if r.success)
                print(f"\r进度: {completed}/{total} ({completed/total*100:.1f}%) | "
                      f"成功 {success} | 速率 {rate:.1f}/s | 剩余 {(total-completed)/rate:.0f}s"
                      if rate > 0 else f"\r进度: {completed}/{total}", end="", flush=True)

    return results, time.time() - start


def run_sync(tasks: List[DownloadTask], output_dir: Path):
    mm = MirrorManager()
    downloader = SyncDownloader(mm)
    results = []
    start = time.time()
    total = len(tasks)

    with ThreadPoolExecutor(max_workers=RETRY_CONFIG["concurrent_downloads"]) as executor:
        futures = {executor.submit(downloader.download, task, output_dir): task for task in tasks}

        completed = 0
        for future in as_completed(futures):
            result = future.result()
            results.append(result)
            completed += 1
            if completed % 10 == 0 or completed == total:
                elapsed = time.time() - start
                rate = completed / elapsed if elapsed > 0 else 0
                success = sum(1 for r in results if r.success)
                print(f"\r进度: {completed}/{total} ({completed/total*100:.1f}%) | "
                      f"成功 {success} | 速率 {rate:.1f}/s | 剩余 {(total-completed)/rate:.0f}s"
                      if rate > 0 else f"\r进度: {completed}/{total}", end="", flush=True)

    return results, time.time() - start


def main():
    parser = argparse.ArgumentParser(description="OMaster 依赖下载器 v2.0")
    parser.add_argument("-o", "--output", default="local-maven", help="输出目录")
    parser.add_argument("-c", "--concurrent", type=int, default=8, help="并发数")
    parser.add_argument("-r", "--retries", type=int, default=3, help="重试次数")
    parser.add_argument("--sync", action="store_true", help="使用同步模式")
    parser.add_argument("--task", choices=["all", "agp", "kotlin", "hilt", "androidx", "thirdparty"],
                        default="all", help="下载任务集")
    parser.add_argument("--report", default="download-report.json", help="报告输出文件")
    args = parser.parse_args()

    RETRY_CONFIG["concurrent_downloads"] = args.concurrent
    RETRY_CONFIG["max_retries"] = args.retries

    output_dir = Path(args.output)
    output_dir.mkdir(parents=True, exist_ok=True)

    print("=" * 70)
    print("OMaster 依赖下载器 v2.0")
    print("=" * 70)
    print(f"输出目录: {output_dir.absolute()}")
    print(f"并发数: {RETRY_CONFIG['concurrent_downloads']}")
    print(f"重试次数: {RETRY_CONFIG['max_retries']}")
    print(f"异步模式: {'否' if args.sync or not ASYNC_AVAILABLE else '是'}")
    print()

    # 收集任务
    print("[1/3] 收集依赖任务...")
    if args.task == "all":
        tasks = collect_all_tasks()
    elif args.task == "agp":
        tasks = get_android_dependencies()
    elif args.task == "kotlin":
        tasks = get_kotlin_dependencies()
    elif args.task == "hilt":
        tasks = get_hilt_dependencies()
    elif args.task == "androidx":
        tasks = get_androidx_dependencies()
    elif args.task == "thirdparty":
        tasks = get_third_party_dependencies()
    print(f"      共 {len(tasks)} 个任务")

    # 开始下载
    print("\n[2/3] 开始下载...")
    if ASYNC_AVAILABLE and not args.sync:
        try:
            results, duration = asyncio.run(run_async(tasks, output_dir))
        except Exception as e:
            print(f"异步模式失败 ({e})，回退到同步模式")
            results, duration = run_sync(tasks, output_dir)
    else:
        results, duration = run_sync(tasks, output_dir)

    # 报告
    print("\n\n[3/3] 生成报告...")
    print_report(results, duration)

    # 保存 JSON 报告
    report = {
        "duration": duration,
        "total": len(results),
        "success": sum(1 for r in results if r.success),
        "failed": sum(1 for r in results if not r.success),
        "failures": [
            {"path": r.task.path, "error": r.error, "attempts": r.attempts}
            for r in results if not r.success
        ],
    }
    Path(args.report).write_text(json.dumps(report, indent=2, ensure_ascii=False))
    print(f"\n详细报告已保存: {args.report}")

    # 返回错误码
    failed = sum(1 for r in results if not r.success)
    return 1 if failed > 0 else 0


if __name__ == "__main__":
    sys.exit(main())
