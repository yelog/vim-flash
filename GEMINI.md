# GEMINI.md - vim-flash

This file provides instructional context for Gemini CLI when working in the `vim-flash` repository.

## Project Overview

`vim-flash` is an IntelliJ Platform plugin that implements `flash.nvim`-style navigation for IdeaVim. It allows users to jump to any location in the visible editor area using labels, provides enhanced character motions (f, F, t, T), and supports Treesitter-like syntax node selection.

### Key Technologies
- **Language**: Kotlin
- **Build System**: Gradle (Kotlin DSL)
- **Target Platform**: IntelliJ IDEA (IC/IU)
- **Primary Dependency**: IdeaVIM (must be installed in the IDE)
- **Minimum Build**: 231 (2023.1)
- **Java Version**: 17

## Architecture & Core Components

### 1. Central Orchestration: `JumpHandler`
The `JumpHandler` (singleton) is the brain of the plugin. It:
- Manages the active state and intercepts keyboard input by replacing `TypedActionHandler`.
- Installs/uninstalls editor action handlers for `Escape`, `Backspace`, and `Enter`.
- Coordinates the gray overlay and `MarksCanvas` rendering.
- Supports cross-split search (searching across all visible editors).
- Handles timeouts for automatic cancellation of vim-style motions.

### 2. Search Strategies: `Finder` Interface
Each navigation mode has a corresponding `Finder` implementation:
- **`Search`**: Multi-character search with label generation via `KeyTagsGenerator`.
- **`VimF`**: Single-character motion (f/F/t/T) with auto-jump and repeat capabilities.
- **`TreesitterFinder`**: Uses IntelliJ PSI (Program Structure Interface) to select semantic code blocks.

### 3. Visual Feedback: `MarksCanvas`
A custom Swing `JComponent` overlaid on the editor's content component. It renders:
- Jump labels (tags) with configurable colors.
- Match highlights.
- PSI range indicators for Treesitter mode.

### 4. Configuration: `UserConfig`
An `ApplicationService` that persists settings such as colors, label characters, and behavior (e.g., `autoJumpWhenSingle`).

## Development Guidelines

### Building and Running
- **Build Plugin**: `./gradlew buildPlugin` (artifacts in `build/distributions/`)
- **Run IDE**: `./gradlew runIde` (runs a sandbox IDE with the plugin installed)
- **Run Tests**: `./gradlew test`
- **Verify Plugin**: `./gradlew verifyPlugin`
- **Clean**: `./gradlew clean`

### Coding Conventions
- **Compatibility**: Use the `JumpHandler.nonModalModalityState()` and reflection-based registration for document/command listeners to maintain compatibility across different IntelliJ versions.
- **Performance**: All UI-related updates (graying, canvas painting) must happen on the Event Dispatch Thread (EDT).
- **Surgical Changes**: When modifying jumping logic, ensure you handle multi-split scenarios and carets correctly (see `JumpHandler.moveToOffset`).

### Action Registration
New actions must be registered in `src/main/resources/META-INF/plugin.xml` and typically extend `BaseAction` or specific action classes in the `action` package.

## Key Files
- `src/main/kotlin/org/yelog/ideavim/flash/JumpHandler.kt`: Core logic and state management.
- `src/main/kotlin/org/yelog/ideavim/flash/action/Finder.kt`: Interface for search strategies.
- `src/main/kotlin/org/yelog/ideavim/flash/MarksCanvas.kt`: UI rendering logic.
- `src/main/resources/META-INF/plugin.xml`: Plugin metadata and action definitions.
- `CLAUDE.md`: Additional development and build command references.
