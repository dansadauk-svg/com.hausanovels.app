#!/usr/bin/env python3
"""Remove obsolete files left by older HausaNovels WebView/TWA uploads.

GitHub's browser uploader replaces files that share the same path but does not delete
files that are absent from a newer ZIP. This normalizer keeps the v2.1.7 seamless
WebView build repeatable even when previous TWA files remain in the repository.
"""
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
MAIN = ROOT / "app" / "src" / "main"

LEGACY_PATHS = (
    MAIN / "java" / "ng" / "hausanovels" / "app" / "MainActivity.java",
    MAIN / "java" / "ng" / "hausanovels" / "app" / "HausaNovelsLauncherActivity.java",
    MAIN / "java" / "ng" / "hausanovels" / "app" / "BrowserFallback.java",
    MAIN / "java" / "ng" / "hausanovels" / "app" / "OAuthReturnActivity.java",
    MAIN / "java" / "com" / "hausanovels" / "app" / "MainActivity.java",
    MAIN / "java" / "com" / "hausanovels" / "app" / "HausaNovelsLauncherActivity.java",
    MAIN / "res" / "layout" / "activity_main.xml",
    MAIN / "res" / "drawable" / "progress_fill.xml",
    MAIN / "res" / "drawable" / "progress_track.xml",
    MAIN / "res" / "drawable" / "splash_gradient.xml",
    MAIN / "res" / "drawable" / "splash_progress.xml",
    MAIN / "res" / "drawable" / "splash_screen.xml",
    MAIN / "res" / "xml" / "filepaths.xml",
    MAIN / "res" / "xml" / "provider_paths.xml",
)

removed: list[str] = []
for path in LEGACY_PATHS:
    if path.is_file() or path.is_symlink():
        path.unlink()
        removed.append(path.relative_to(ROOT).as_posix())

protected = {
    MAIN,
    MAIN / "java",
    MAIN / "java" / "ng",
    MAIN / "java" / "ng" / "hausanovels",
    MAIN / "java" / "ng" / "hausanovels" / "app",
    MAIN / "res",
}
for directory in sorted((p for p in MAIN.rglob("*") if p.is_dir()), key=lambda p: len(p.parts), reverse=True):
    if directory in protected:
        continue
    try:
        directory.rmdir()
    except OSError:
        pass

if removed:
    print("Removed legacy HausaNovels files:")
    for item in removed:
        print(f"- {item}")
else:
    print("No legacy HausaNovels files were present.")
