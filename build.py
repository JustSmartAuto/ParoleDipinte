#!/usr/bin/env python3
# -*- coding: utf-8 -*-

"""
识字涂色(ParoleDipinte) 构建脚本
用于构建FatJar并复制到项目根目录和fatjar目录
"""

import os
import re
import subprocess
import sys
from pathlib import Path
from datetime import datetime


def run_command(cmd, check=True):
    """运行shell命令"""
    result = subprocess.run(cmd, shell=True, capture_output=True, text=True)
    if check and result.returncode != 0:
        print(f"命令执行失败: {cmd}")
        print(result.stderr)
        sys.exit(1)
    return result


def get_java_home():
    """获取系统 Java 8 路径"""
    result = run_command("/usr/libexec/java_home -v 1.8")
    return result.stdout.strip()


def get_version():
    """从 build.gradle 中解析版本号"""
    build_gradle = Path("build.gradle")
    if not build_gradle.exists():
        print("错误: 找不到 build.gradle 文件")
        sys.exit(1)
    
    content = build_gradle.read_text(encoding="utf-8")
    match = re.search(r"^version\s*=\s*['\"]([^'\"]+)['\"]", content, re.MULTILINE)
    if not match:
        print("错误: 无法从 build.gradle 中解析版本号")
        sys.exit(1)
    
    return match.group(1)


def get_timestamp():
    """获取时间戳"""
    return datetime.now().strftime("%Y%m%d%H%M%S")


def get_file_size(path):
    """获取人类可读的文件大小"""
    size = path.stat().st_size
    for unit in ["B", "KB", "MB", "GB"]:
        if size < 1024.0:
            return f"{size:.1f} {unit}"
        size /= 1024.0
    return f"{size:.1f} TB"


def main():
    # 设置环境变量
    java_home = get_java_home()
    os.environ["JAVA_HOME"] = java_home
    os.environ["PATH"] = f"{java_home}/bin:{os.environ.get('PATH', '')}"
    
    print(f"使用 Java: {java_home}")
    run_command("java -version", check=False)
    
    print("=" * 42)
    print("  识字涂色(ParoleDipinte) 构建脚本")
    print("=" * 42)
    print()
    
    # 获取版本号和时间戳
    version = get_version()
    timestamp = get_timestamp()
    print(f"版本号: {version}")
    print(f"时间戳: {timestamp}")
    
    # 构建FatJar
    print()
    print(">>> 开始构建FatJar...")
    run_command("./gradlew jar")
    
    # 查找生成的jar文件（最新的）
    build_libs = Path("build/libs")
    jar_files = sorted(build_libs.glob("ParoleDipinte-*.jar"), key=lambda p: p.stat().st_mtime, reverse=True)
    
    if not jar_files:
        print("错误: 找不到生成的jar文件")
        sys.exit(1)
    
    jar_file = jar_files[0]
    jar_name = jar_file.name
    
    print()
    print(f">>> 构建完成: {jar_name}")
    
    # 复制到项目根目录
    print()
    print(">>> 复制到项目根目录...")
    import shutil
    shutil.copy2(jar_file, "./")
    print(f"已复制: ./{jar_name}")
    
    # 复制到fatjar目录
    print()
    print(">>> 复制到fatjar目录...")
    fatjar_dir = Path("fatjar")
    fatjar_dir.mkdir(exist_ok=True)
    shutil.copy2(jar_file, fatjar_dir)
    print(f"已复制: fatjar/{jar_name}")
    
    print()
    print("=" * 42)
    print("  构建完成!")
    print("=" * 42)
    print()
    print("输出文件:")
    print(f"  - ./{jar_name}")
    print(f"  - fatjar/{jar_name}")
    print()
    print(f"文件大小: {get_file_size(jar_file)}")
    print()


if __name__ == "__main__":
    main()
