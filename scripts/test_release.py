import io
import tempfile
import unittest
from pathlib import Path
from zipfile import ZipFile

from release import release_metadata, validate_archive


class ReleaseMetadataTest(unittest.TestCase):
    def setUp(self):
        self.directory = tempfile.TemporaryDirectory()
        self.addCleanup(self.directory.cleanup)
        self.root = Path(self.directory.name)
        self.write("gradle.properties", "pluginVersion = 1.2.3\n")

    def write(self, name, content):
        (self.root / name).write_text(content, encoding="utf-8")

    def test_extracts_exact_section_preserving_markdown_and_shell_characters(self):
        notes = '- Fix `Enter` and "quoted" text.\n\n- Keep $(literal) and $VARIABLE.\n'
        self.write("CHANGELOG.md", f"# Changelog\n\n## 1.2.3\n\n{notes}\n## 1.2.2\n\n- Old\n")
        self.assertEqual(("1.2.3", notes), release_metadata(self.root, "v1.2.3"))

    def test_preserves_last_line_at_end_of_file_and_accepts_legacy_tag(self):
        self.write("CHANGELOG.md", "## 1.2.3\n\n- First\n- Last")
        self.assertEqual(("1.2.3", "- First\n- Last\n"), release_metadata(self.root, "1.2.3"))

    def test_rejects_version_mismatch(self):
        self.write("CHANGELOG.md", "## 1.2.4\n\n- New\n")
        with self.assertRaisesRegex(ValueError, "pluginVersion"):
            release_metadata(self.root, "v1.2.4")

    def test_rejects_missing_empty_or_duplicate_sections(self):
        for changelog in ("## 1.2.30\n- Wrong\n", "## 1.2.3\n\n", "## 1.2.3\n- A\n## 1.2.3\n- B\n"):
            with self.subTest(changelog=changelog):
                self.write("CHANGELOG.md", changelog)
                with self.assertRaisesRegex(ValueError, "nonempty"):
                    release_metadata(self.root, "v1.2.3")

    def test_accepts_prerelease(self):
        self.write("gradle.properties", "pluginVersion = 1.2.3-rc.1\n")
        self.write("CHANGELOG.md", "## 1.2.3-rc.1\n\n- Preview\n")
        self.assertEqual("1.2.3-rc.1", release_metadata(self.root, "v1.2.3-rc.1")[0])

    def test_rejects_invalid_tags(self):
        for tag in ("main", "v1.2", "v01.2.3", "v1.2.3/other", "v1.2.3-rc.01", "v1.2.3-$(command)"):
            with self.subTest(tag=tag), self.assertRaises(ValueError):
                release_metadata(self.root, tag)


class ReleaseArchiveTest(unittest.TestCase):
    def setUp(self):
        self.directory = tempfile.TemporaryDirectory()
        self.addCleanup(self.directory.cleanup)
        self.archive = Path(self.directory.name) / "vim-flash-1.2.3.zip"

    def build_archive(self, version="1.2.3", notes="<ul><li>Fixed</li></ul>", prefix="vim-flash/lib/"):
        jar_bytes = io.BytesIO()
        with ZipFile(jar_bytes, "w") as jar:
            jar.writestr("META-INF/plugin.xml", f"<idea-plugin><id>org.yelog.ideavim.flash</id><version>{version}</version><change-notes><![CDATA[{notes}]]></change-notes></idea-plugin>")
        with ZipFile(self.archive, "w") as distribution:
            distribution.writestr(f"{prefix}vim-flash-1.2.3.jar", jar_bytes.getvalue())

    def test_accepts_installable_archive(self):
        self.build_archive()
        validate_archive(self.archive, "1.2.3")

    def test_rejects_wrong_packaged_version(self):
        self.build_archive(version="1.2.2")
        with self.assertRaisesRegex(ValueError, "version"):
            validate_archive(self.archive, "1.2.3")

    def test_rejects_empty_change_notes(self):
        self.build_archive(notes="")
        with self.assertRaisesRegex(ValueError, "change notes"):
            validate_archive(self.archive, "1.2.3")

    def test_rejects_unpacked_artifact_layout(self):
        self.build_archive(prefix="lib/")
        with self.assertRaisesRegex(ValueError, "descriptor"):
            validate_archive(self.archive, "1.2.3")


if __name__ == "__main__":
    unittest.main()
