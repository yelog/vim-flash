# Label Generation Freeze Fix Implementation Plan

**Goal:** Make label generation terminate for empty, single-character, and duplicate alphabets without losing search matches.

**Architecture:** Normalize the label alphabet once at generation and settings boundaries. Preserve the existing label ordering for valid inputs. Represent temporarily unavailable labels as empty strings while retaining every match, its highlight, and Enter confirmation. Keep Vim character motions on their existing dedicated highlighters.

**Tech Stack:** Kotlin, IntelliJ Platform APIs, BasePlatformTestCase, Gradle.

## Implementation

1. Guard and normalize inputs in `KeyTagsGenerator.kt`; preserve one result per positive target count, using empty labels when distinct labels cannot be generated.
2. Validate and normalize saved characters in `Configurable.kt`, with the validation text in `MyBundle.kt`.
3. Draw search match highlights before skipping empty labels in `MarksCanvas.kt`. Avoid redundant canvas painting for Vim motions in `JumpHandler.kt`.
4. Add regression tests for generator termination and label invariants; search fallback, continued typing and selection; empty-label rendering; and settings validation.
5. Run focused tests, then the complete test suite and plugin packaging. Recheck the packaged generator in an isolated process with a timeout, and inspect the final diff.
6. Keep support for IDE 2023.1/2023.2 unless the user chooses a higher baseline. Marketplace's built-in error reporter requires 2023.3+, so do not register an unavailable class on older IDEs.
7. Align the build/test SDK and Plugin Verifier matrix with the supported 2023.1 baseline. Search tests initially failed to initialize `Mode` on the old, unsupported 2022.3 test SDK. The released 1.0.9 enum loads successfully with the Kotlin 1.8.0 runtime bundled in 2023.1, so no compiler downgrade is needed.

## Acceptance

- Empty/one-character alphabets cannot busy-loop; duplicate input cannot recurse indefinitely.
- Normal labels keep their existing ordering, are unique and prefix-free, and cover all matches.
- Exhausting the default alphabet retains all search matches and the nearest highlight; further typing can restore labels.
- Empty/one-character settings are rejected before any configuration is saved; duplicate and whitespace characters are normalized.
- Existing search confirmation and Vim character-motion behavior remain intact.

## Validation

- Prepared the local 1.0.10 candidate, preserving `since-build=231`. The user chose to retain 2023.1/2023.2 support and defer Marketplace error reporting.
- `./gradlew test buildPlugin verifyPlugin`: passed on Java 17 and IntelliJ 2023.1.7; 26 tests passed, including 19 new regression tests. Enter confirmation and repeated Vim `f` motions are exercised through editor action handlers.
- Release helper: all 10 Python tests passed; version, change notes, and the packaged ZIP validated for `v1.0.10`.
- Isolated final-JAR probes, each with a three-second timeout: empty, singleton, and duplicate-only alphabets returned without hanging; 20,000 targets with a two-character alphabet produced distinct labels. `Mode` loaded with the 2023.1 bundled runtime.
- Plugin Verifier now includes 2023.1.7 alongside 2024.3.7 in CI. The full cross-version verifier has not been run locally; local verification covered the test suite and plugin structure.
- Git diff whitespace checks passed. The user approved publishing 1.0.10 to GitHub on 2026-11-06; Marketplace upload remains manual.
