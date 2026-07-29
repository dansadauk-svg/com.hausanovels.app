#!/usr/bin/env python3
"""Remove obsolete WebView-era Android source files before compiling the HausaNovels TWA."""
from pathlib import Path
import shutil

ROOT = Path(__file__).resolve().parents[1]
JAVA_ROOT = ROOT / "app" / "src" / "main" / "java"
RES_ROOT = ROOT / "app" / "src" / "main" / "res"

canonical_java = {
    (JAVA_ROOT / "ng/hausanovels/app/HausaNovelsLauncherActivity.java").resolve(),
    (JAVA_ROOT / "ng/hausanovels/app/OAuthReturnActivity.java").resolve(),
    (JAVA_ROOT / "ng/hausanovels/app/SplashActivity.java").resolve(),
}

removed = []
if JAVA_ROOT.exists():
    for path in JAVA_ROOT.rglob("*.java"):
        if path.resolve() not in canonical_java:
            path.unlink()
            removed.append(path.relative_to(ROOT).as_posix())

# Remove empty Java package directories left by old WebView projects.
if JAVA_ROOT.exists():
    for directory in sorted((p for p in JAVA_ROOT.rglob("*") if p.is_dir()), reverse=True):
        try:
            directory.rmdir()
        except OSError:
            pass

obsolete_resource_files = [
    RES_ROOT / "layout/activity_main.xml",
    RES_ROOT / "drawable/progress_fill.xml",
    RES_ROOT / "drawable/progress_track.xml",
    RES_ROOT / "drawable/splash_gradient.xml",
]
for path in obsolete_resource_files:
    if path.exists():
        path.unlink()
        removed.append(path.relative_to(ROOT).as_posix())

# Android 8 compatibility theme files from the old WebView wrapper are not used by the TWA.
obsolete_dir = RES_ROOT / "values-v28"
if obsolete_dir.exists():
    shutil.rmtree(obsolete_dir)
    removed.append(obsolete_dir.relative_to(ROOT).as_posix() + "/")

if removed:
    print("Removed obsolete Android files:")
    for item in removed:
        print(f"- {item}")
else:
    print("No obsolete Android files found.")
