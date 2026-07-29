#!/usr/bin/env python3
"""Validate the repaired HausaNovels v2.1.4 source before Gradle starts."""
from pathlib import Path
import re
import sys
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
MAIN = ROOT / "app" / "src" / "main"
RES = MAIN / "res"
JAVA = MAIN / "java"
MANIFEST = MAIN / "AndroidManifest.xml"
APP_GRADLE = ROOT / "app" / "build.gradle"

EXPECTED_PACKAGE = "ng.hausanovels.app"
EXPECTED_JAVA = {
    "ng/hausanovels/app/BrowserFallback.java",
    "ng/hausanovels/app/HausaNovelsLauncherActivity.java",
    "ng/hausanovels/app/OAuthReturnActivity.java",
    "ng/hausanovels/app/SplashActivity.java",
}

errors: list[str] = []

actual_java = {p.relative_to(JAVA).as_posix() for p in JAVA.rglob("*.java")}
extra_java = sorted(actual_java - EXPECTED_JAVA)
missing_java = sorted(EXPECTED_JAVA - actual_java)
if extra_java:
    errors.append("Obsolete or unexpected Java files: " + ", ".join(extra_java))
if missing_java:
    errors.append("Missing required Java files: " + ", ".join(missing_java))

for java_path in sorted(EXPECTED_JAVA):
    source = JAVA / java_path
    if source.exists() and f"package {EXPECTED_PACKAGE};" not in source.read_text(errors="replace"):
        errors.append(f"Wrong Java package declaration: {java_path}")

app_gradle_text = APP_GRADLE.read_text(errors="replace")
for required in (
    f'namespace "{EXPECTED_PACKAGE}"',
    f'applicationId "{EXPECTED_PACKAGE}"',
    'versionCode 214',
    'versionName "2.1.4"',
):
    if required not in app_gradle_text:
        errors.append(f"Missing Gradle setting: {required}")

# Resource definitions from values XML and file-based resource directories.
definitions: dict[str, set[str]] = {}
for values_dir in sorted(p for p in RES.glob("values*") if p.is_dir()):
    for xml_file in values_dir.glob("*.xml"):
        try:
            tree = ET.parse(xml_file)
        except ET.ParseError as exc:
            errors.append(f"Invalid XML {xml_file.relative_to(ROOT)}: {exc}")
            continue
        for node in tree.getroot():
            name = node.attrib.get("name")
            if not name:
                continue
            resource_type = node.tag
            if resource_type == "item":
                resource_type = node.attrib.get("type", "")
            if resource_type:
                definitions.setdefault(resource_type, set()).add(name)

for directory in RES.iterdir():
    if not directory.is_dir() or directory.name.startswith("values"):
        continue
    resource_type = directory.name.split("-", 1)[0]
    for file in directory.iterdir():
        if file.is_file() and not file.name.startswith("."):
            definitions.setdefault(resource_type, set()).add(file.stem)

refs: set[tuple[str, str, str]] = set()
xml_pattern = re.compile(r"(?<!android:)@(?P<type>[A-Za-z0-9_]+)/(?P<name>[A-Za-z0-9_.]+)")
java_pattern = re.compile(r"(?<!android\.)\bR\.(?P<type>[A-Za-z0-9_]+)\.(?P<name>[A-Za-z0-9_]+)")

for xml_file in list(RES.rglob("*.xml")) + [MANIFEST]:
    text = xml_file.read_text(errors="replace")
    for match in xml_pattern.finditer(text):
        refs.add((match.group("type"), match.group("name"), xml_file.relative_to(ROOT).as_posix()))

for java_file in JAVA.rglob("*.java"):
    text = java_file.read_text(errors="replace")
    for match in java_pattern.finditer(text):
        refs.add((match.group("type"), match.group("name"), java_file.relative_to(ROOT).as_posix()))

for resource_type, name, source in sorted(refs):
    if resource_type == "id":
        continue
    if name not in definitions.get(resource_type, set()):
        errors.append(f"Missing @{resource_type}/{name}, referenced by {source}")

# Manifest checks that prevent the previous runtime loop/crash configuration.
ANDROID = "{http://schemas.android.com/apk/res/android}"
try:
    manifest_root = ET.parse(MANIFEST).getroot()
    application = manifest_root.find("application")
    if application is None:
        errors.append("AndroidManifest.xml has no <application> element")
    else:
        activities = {
            node.attrib.get(ANDROID + "name"): node
            for node in application.findall("activity")
        }
        launcher = activities.get(".HausaNovelsLauncherActivity")
        if launcher is None:
            errors.append("TWA launcher activity is missing")
        elif launcher.attrib.get(ANDROID + "launchMode") == "singleTask":
            errors.append("TWA launcher must not use singleTask with the custom splash trampoline")
        providers = application.findall("provider")
        if providers:
            errors.append("Unused FileProvider must not be present; this build uses a native splash activity")
except ET.ParseError as exc:
    errors.append(f"Invalid AndroidManifest.xml: {exc}")

for stale in (
    RES / "layout" / "activity_main.xml",
    RES / "drawable" / "splash_progress.xml",
    RES / "drawable" / "splash_screen.xml",
    RES / "xml" / "filepaths.xml",
    JAVA / "ng" / "hausanovels" / "app" / "MainActivity.java",
):
    if stale.exists():
        errors.append(f"Stale file must be deleted: {stale.relative_to(ROOT)}")

# Prevent accidental package drift.
for file in ROOT.rglob("*"):
    if not file.is_file() or any(part in {".git", "build"} for part in file.parts):
        continue
    if file.suffix.lower() not in {".java", ".xml", ".gradle", ".json", ".md", ".yml", ".yaml"}:
        continue
    text = file.read_text(errors="replace")
    if ("com" + ".hausanovels.app") in text:
        errors.append(f"Wrong package name found in {file.relative_to(ROOT)}")

if errors:
    for error in errors:
        print(error, file=sys.stderr)
    raise SystemExit(1)

print("HausaNovels v2.1.4 package, manifest and Android resources verified.")
