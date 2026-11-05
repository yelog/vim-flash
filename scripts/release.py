#!/usr/bin/env python3
"""Validate release metadata and an installable plugin ZIP without changing sources."""

import argparse
import io
import re
import sys
import xml.etree.ElementTree as ET
from pathlib import Path
from zipfile import BadZipFile, ZipFile


PLUGIN_ID = "org.yelog.ideavim.flash"
VERSION_PATTERN = re.compile(
    r"(?:0|[1-9][0-9]*)\.(?:0|[1-9][0-9]*)\.(?:0|[1-9][0-9]*)"
    r"(?:-[0-9A-Za-z-]+(?:\.[0-9A-Za-z-]+)*)?"
)


def release_metadata(root: Path, tag: str) -> tuple[str, str]:
    # Unprefixed tags are accepted for manually rebuilding historical releases.
    version = tag.removeprefix("v")
    if not VERSION_PATTERN.fullmatch(version):
        raise ValueError("Use X.Y.Z or vX.Y.Z, optionally followed by a prerelease such as -rc.1")
    for identifier in version.partition("-")[2].split("."):
        if identifier.isdigit() and len(identifier) > 1 and identifier.startswith("0"):
            raise ValueError("Numeric prerelease identifiers must not have leading zeros")

    properties = (root / "gradle.properties").read_text(encoding="utf-8")
    versions = re.findall(r"^pluginVersion\s*=\s*(\S+)\s*$", properties, re.MULTILINE)
    if versions != [version]:
        raise ValueError(f"Tag {tag} must match the single pluginVersion in gradle.properties: {versions}")

    changelog = (root / "CHANGELOG.md").read_text(encoding="utf-8")
    sections = re.findall(
        rf"^## {re.escape(version)}[ \t]*\n(.*?)(?=^## |\Z)",
        changelog,
        re.MULTILINE | re.DOTALL,
    )
    if len(sections) != 1 or not sections[0].strip():
        raise ValueError(f"CHANGELOG.md must contain exactly one nonempty '## {version}' section")
    return version, sections[0].strip() + "\n"


def validate_archive(archive: Path, version: str) -> None:
    expected_name = f"vim-flash-{version}.zip"
    if archive.name != expected_name:
        raise ValueError(f"Expected archive {expected_name}, got {archive.name}")

    descriptors = []
    with ZipFile(archive) as distribution:
        for entry in distribution.namelist():
            if not entry.startswith("vim-flash/lib/") or not entry.endswith(".jar"):
                continue
            with ZipFile(io.BytesIO(distribution.read(entry))) as jar:
                if "META-INF/plugin.xml" in jar.namelist():
                    descriptor = ET.fromstring(jar.read("META-INF/plugin.xml"))
                    if descriptor.findtext("id") == PLUGIN_ID:
                        descriptors.append(descriptor)
    if len(descriptors) != 1:
        raise ValueError("ZIP must contain exactly one vim-flash plugin descriptor under vim-flash/lib/")
    descriptor = descriptors[0]
    if descriptor.findtext("version") != version:
        raise ValueError(f"Packaged plugin version does not match {version}")
    if not (descriptor.findtext("change-notes") or "").strip():
        raise ValueError("Packaged plugin is missing change notes")


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--root", type=Path, default=Path(__file__).resolve().parents[1], help="Repository to validate")
    parser.add_argument("--tag", required=True, help="Tag to validate against pluginVersion")
    parser.add_argument("--notes-file", type=Path, help="Write this version's Markdown release notes")
    parser.add_argument("--archive", type=Path, help="Also validate the built plugin ZIP")
    args = parser.parse_args()
    try:
        version, notes = release_metadata(args.root, args.tag)
        if args.archive:
            validate_archive(args.archive, version)
        if args.notes_file:
            args.notes_file.parent.mkdir(parents=True, exist_ok=True)
            args.notes_file.write_text(notes, encoding="utf-8")
    except (ValueError, OSError, BadZipFile, ET.ParseError) as error:
        print(f"Release validation failed: {error}", file=sys.stderr)
        return 1
    print(f"Release metadata validated: {args.tag} (pluginVersion {version})")
    return 0


if __name__ == "__main__":
    sys.exit(main())
