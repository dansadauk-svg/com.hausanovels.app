#!/usr/bin/env python3
"""Validate required Android resources and Java string references before Gradle runs."""
from pathlib import Path
import re
import sys
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
MAIN = ROOT / "app" / "src" / "main"
VALUES = MAIN / "res" / "values"

required_colors = {
    "hn_background", "hn_green", "hn_gold", "hn_white", "hn_text",
    "app_background", "brand_green", "brand_gold", "progress_track",
    "splash_background", "splash_background_end", "text_primary",
    "text_secondary", "brand_green_soft", "colorPrimary",
    "colorPrimaryDark", "colorAccent",
}
required_strings = {
    "app_name", "splash_title", "web_app_url", "splash_tagline",
    "loading_preparing", "asset_statements",
}

def resource_names(path: Path, tag: str) -> set[str]:
    try:
        tree = ET.parse(path)
    except (ET.ParseError, OSError) as exc:
        raise SystemExit(f"Invalid or missing {path}: {exc}")
    return {node.attrib.get("name", "") for node in tree.getroot().findall(tag)}

colors = resource_names(VALUES / "colors.xml", "color")
strings = resource_names(VALUES / "strings.xml", "string")
missing_colors = sorted(required_colors - colors)
missing_strings = sorted(required_strings - strings)

# Catch Java references such as R.string.web_app_url before javac does.
java_string_refs = set()
for java_file in (MAIN / "java").rglob("*.java"):
    java_string_refs.update(re.findall(r"\bR\.string\.([A-Za-z0-9_]+)", java_file.read_text(errors="replace")))
missing_java_strings = sorted(java_string_refs - strings)

# Catch local XML @string and @color references before AAPT does.
xml_string_refs = set()
xml_color_refs = set()
for xml_file in (MAIN / "res").rglob("*.xml"):
    text = xml_file.read_text(errors="replace")
    xml_string_refs.update(re.findall(r"@string/([A-Za-z0-9_]+)", text))
    xml_color_refs.update(re.findall(r"@color/([A-Za-z0-9_]+)", text))
missing_xml_strings = sorted(xml_string_refs - strings)
missing_xml_colors = sorted(xml_color_refs - colors)

errors = False
for label, values in (
    ("Missing required colors", missing_colors),
    ("Missing required strings", missing_strings),
    ("Missing Java string resources", missing_java_strings),
    ("Missing XML string resources", missing_xml_strings),
    ("Missing XML color resources", missing_xml_colors),
):
    if values:
        errors = True
        print(f"{label}: {', '.join(values)}", file=sys.stderr)

if errors:
    raise SystemExit(1)

print("Android resources and Java string references verified.")
