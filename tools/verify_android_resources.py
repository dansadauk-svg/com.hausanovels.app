#!/usr/bin/env python3
"""Fail early when required HausaNovels Android compatibility resources are absent."""
from pathlib import Path
import sys
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
VALUES = ROOT / "app" / "src" / "main" / "res" / "values"

required_colors = {
    "hn_background", "hn_green", "hn_gold", "hn_white", "hn_text",
    "app_background", "brand_green", "brand_gold", "progress_track",
    "splash_background", "splash_background_end", "text_primary",
    "text_secondary", "brand_green_soft", "colorPrimary",
    "colorPrimaryDark", "colorAccent",
}
required_strings = {
    "app_name", "splash_title", "splash_tagline", "loading_preparing",
    "asset_statements",
}

def names(path: Path, tag: str) -> set[str]:
    try:
        tree = ET.parse(path)
    except (ET.ParseError, OSError) as exc:
        raise SystemExit(f"Invalid or missing {path}: {exc}")
    return {node.attrib.get("name", "") for node in tree.getroot().findall(tag)}

colors = names(VALUES / "colors.xml", "color")
strings = names(VALUES / "strings.xml", "string")
missing_colors = sorted(required_colors - colors)
missing_strings = sorted(required_strings - strings)

if missing_colors or missing_strings:
    if missing_colors:
        print("Missing colors:", ", ".join(missing_colors), file=sys.stderr)
    if missing_strings:
        print("Missing strings:", ", ".join(missing_strings), file=sys.stderr)
    raise SystemExit(1)

print("Android compatibility resources verified.")
