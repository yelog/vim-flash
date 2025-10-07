# Repository Guidelines

## Project Structure & Module Organization
The IntelliJ plugin code lives in `src/main/kotlin/org/yelog/ideavim/flash`. Actions, key generators, and configuration UI each sit in a dedicated Kotlin file, while `ConfigUI.form` holds the Swing layout metadata. Plugin descriptors and icons reside in `src/main/resources/META-INF` (`plugin.xml`). Test fixtures live in `src/test/testData`, and Kotlin-based platform tests sit under `src/test/kotlin/com/github/yelog/ideavimflash`. Gradle configuration stays in `build.gradle.kts` and `gradle.properties`; generated artifacts land in `build` or `out`.

## Build, Test, and Development Commands
Run `./gradlew build` for compilation and static checks. `./gradlew test` executes the `BasePlatformTestCase` suites. `./gradlew runIde` launches a sandbox IDE with the plugin for manual verification. Use `./gradlew buildPlugin` to package a distributable ZIP. `./gradlew publishPlugin` pushes to JetBrains Marketplace once environment variables are configured. Qodana inspection is available with `./gradlew qodanaScan`.

## Coding Style & Naming Conventions
Kotlin sources follow JetBrains defaults: four-space indents, PascalCase classes, and lowerCamelCase members. Keep IntelliJ action IDs and extension points aligned with `plugin.xml` naming (`flash.*`). Prefer one top-level Kotlin file per feature (for example, `JumpHandler.kt`). UI bundle strings belong in `MyBundle.kt`; add new keys using uppercase snake case. Where IntelliJ APIs require `@Suppress`, explain the intent in a one-line comment.

## Testing Guidelines
Tests extend `BasePlatformTestCase` alongside their fixtures. Name files with the `*Test.kt` suffix and mirror fixtures in `src/test/testData` directories. Use `myFixture` helpers for editor scenarios and keep assertions deterministic. Before submitting a change, run `./gradlew test`; for coverage spot-checks, enable Kover locally via `./gradlew koverHtmlReport`.

## Commit & Pull Request Guidelines
Follow the conventional prefixes seen in history (`feat:`, `fix:`, `build(deps):`). Write imperative subjects, adding a scope when it clarifies the change (for example, `feat(remote): add yank support`). Pull requests should summarize user-visible behavior, link related issues, and include GIFs or screenshots for UI updates. Call out compatibility considerations with IntelliJ platform versions 231–252.x.

## Security & Configuration Tips
Secrets for signing and publishing (`CERTIFICATE_CHAIN`, `PRIVATE_KEY`, `PUBLISH_TOKEN`) must remain in the environment; never commit them. The bundled IdeaVim dependency is pinned in `build.gradle.kts`; update `platformPlugins` thoughtfully and document upgrades in `CHANGELOG.md`. When adding new actions, verify keybindings in the sandbox IDE to avoid collisions with IdeaVim defaults.
