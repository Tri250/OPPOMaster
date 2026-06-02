#!/usr/bin/env python3
"""递归下载 Gradle 构建所需的所有依赖到本地 Maven 仓库"""

import os
import sys
import time
import hashlib
import xml.etree.ElementTree as ET
import urllib.request
import urllib.error

LOCAL_REPO = "/workspace/local-maven"
MIRRORS = [
    "https://maven.aliyun.com/repository/google",
    "https://maven.aliyun.com/repository/public",
    "https://repo1.maven.org/maven2",
    "https://dl.google.com/dl/android/maven2",
]
TIMEOUT = 30
MAX_RETRIES = 3
downloaded = set()
failed = set()

def download_file(url, dest):
    for retry in range(MAX_RETRIES):
        try:
            req = urllib.request.Request(url)
            with urllib.request.urlopen(req, timeout=TIMEOUT) as resp:
                data = resp.read()
                if len(data) > 50:
                    os.makedirs(os.path.dirname(dest), exist_ok=True)
                    with open(dest, 'wb') as f:
                        f.write(data)
                    return True
        except Exception as e:
            if retry < MAX_RETRIES - 1:
                time.sleep(2)
    return False

def get_from_mirrors(group_path, artifact, version, ext):
    filename = f"{artifact}-{version}.{ext}"
    dest = os.path.join(LOCAL_REPO, group_path, artifact, version, filename)
    if os.path.exists(dest) and os.path.getsize(dest) > 50:
        return dest
    
    for mirror in MIRRORS:
        url = f"{mirror}/{group_path}/{artifact}/{version}/{filename}"
        if download_file(url, dest):
            return dest
    return None

def parse_pom_deps(pom_path):
    deps = []
    try:
        tree = ET.parse(pom_path)
        root = tree.getroot()
        ns = {'m': 'http://maven.apache.org/POM/4.0.0'}
        for dep in root.findall('.//m:dependency', ns):
            gid = dep.find('m:groupId', ns)
            aid = dep.find('m:artifactId', ns)
            ver = dep.find('m:version', ns)
            if gid is not None and aid is not None and ver is not None:
                deps.append((gid.text, aid.text, ver.text))
    except:
        pass
    return deps

def download_artifact(group_id, artifact_id, version, depth=0):
    key = f"{group_id}:{artifact_id}:{version}"
    if key in downloaded or key in failed:
        return
    if depth > 3:
        return
    
    group_path = group_id.replace('.', '/')
    prefix = "  " * depth
    print(f"{prefix}→ {key}")
    
    # Download POM first
    pom = get_from_mirrors(group_path, artifact_id, version, "pom")
    if not pom:
        print(f"{prefix}  ✗ POM not found")
        failed.add(key)
        return
    
    # Download JAR
    jar = get_from_mirrors(group_path, artifact_id, version, "jar")
    if jar:
        print(f"{prefix}  ✓ JAR downloaded")
    else:
        print(f"{prefix}  - No JAR (might be POM-only)")
    
    downloaded.add(key)
    
    # Parse and download transitive deps
    deps = parse_pom_deps(pom)
    for gid, aid, ver in deps:
        download_artifact(gid, aid, ver, depth + 1)

# ============ 核心构建插件 ============
print("=" * 60)
print("下载 Gradle 构建插件及传递依赖")
print("=" * 60)

# AGP 8.2.2
download_artifact("com.android.tools.build", "gradle", "8.2.2")
download_artifact("com.android.application", "com.android.application.gradle.plugin", "8.2.2")

# Kotlin 1.9.22
download_artifact("org.jetbrains.kotlin", "kotlin-gradle-plugin", "1.9.22")
download_artifact("org.jetbrains.kotlin.android", "org.jetbrains.kotlin.android.gradle.plugin", "1.9.22")
download_artifact("org.jetbrains.kotlin.kapt", "org.jetbrains.kotlin.kapt.gradle.plugin", "1.9.22")
download_artifact("org.jetbrains.kotlin.plugin.parcelize", "org.jetbrains.kotlin.plugin.parcelize.gradle.plugin", "1.9.22")

# Hilt 2.48
download_artifact("com.google.dagger", "hilt-android-gradle-plugin", "2.48")
download_artifact("com.google.dagger.hilt.android", "com.google.dagger.hilt.android.gradle.plugin", "2.48")

print("\n" + "=" * 60)
print(f"下载完成: {len(downloaded)} 个成功, {len(failed)} 个失败")
print("=" * 60)
