#!/usr/bin/env python3
"""Validate the clean HausaNovels Android source before Gradle starts."""
from pathlib import Path
import re
import sys
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
MAIN = ROOT / "app" / "src" / "main"
RES = MAIN / "res"
JAVA = MAIN / "java"

EXPECTED_JAVA = {
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

# Explicit local references in XML and Java. Framework/dependency references are excluded.
refs: set[tuple[str, str, str]] = set()
xml_pattern = re.compile(r"(?<!android:)@(?P<type>[A-Za-z0-9_]+)/(?P<name>[A-Za-z0-9_.]+)")
java_pattern = re.compile(r"(?<!android\.)\bR\.(?P<type>[A-Za-z0-9_]+)\.(?P<name>[A-Za-z0-9_]+)")

for xml_file in RES.rglob("*.xml"):
    text = xml_file.read_text(errors="replace")
    for match in xml_pattern.finditer(text):
        refs.add((match.group("type"), match.group("name"), xml_file.relative_to(ROOT).as_posix()))

for java_file in JAVA.rglob("*.java"):
    text = java_file.read_text(errors="replace")
    for match in java_pattern.finditer(text):
        refs.add((match.group("type"), match.group("name"), java_file.relative_to(ROOT).as_posix()))

for resource_type, name, source in sorted(refs):
    # IDs declared with @+id are gathered as references, but Android creates them automatically.
    if resource_type == "id":
        continue
    if name not in definitions.get(resource_type, set()):
        errors.append(f"Missing @{resource_type}/{name}, referenced by {source}")

# Guard against the stale files that caused the earlier failures.
for stale in (
    RES / "layout" / "activity_main.xml",
    RES / "drawable" / "splash_progress.xml",
    RES / "drawable" / "splash_screen.xml",
    JAVA / "ng" / "hausanovels" / "app" / "MainActivity.java",
):
    if stale.exists():
        errors.append(f"Stale file must be deleted: {stale.relative_to(ROOT)}")

if errors:
    for error in errors:
        print(error, file=sys.stderr)
    raise SystemExit(1)

print("Clean Android source and all local resource references verified.")
