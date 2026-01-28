# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

vim-flash is an IntelliJ Platform plugin that brings flash.nvim-style navigation to IdeaVim. It provides enhanced vim motions with visual labels for quick navigation within the editor.

**Key Features:**
- Flash search: Search for strings in the visible area with labeled jumps
- Enhanced vim f/F/t/T/;/, motions with visual highlighting
- Treesitter-like syntax node selection using IntelliJ PSI
- Remote operator mode for operations at a distance

## Build Commands

```bash
# Build the plugin (creates a distributable ZIP in build/distributions/)
./gradlew buildPlugin

# Run IDE with the plugin installed for testing
./gradlew runIde

# Run tests
./gradlew test

# Verify plugin (validates plugin.xml and structure)
./gradlew verifyPlugin

# Run Plugin Verifier (checks binary compatibility with specified IDE builds)
./gradlew runPluginVerifier

# Build searchable options index (for Settings search)
./gradlew buildSearchableOptions

# Clean build artifacts
./gradlew clean

# Check all verifications (tests + checks)
./gradlew check
```

## Development Commands

```bash
# Run IDE for UI testing (with robot-server plugin)
./gradlew runIdeForUiTests

# Prepare sandbox with plugin installed
./gradlew prepareSandbox

# Run performance tests
./gradlew runIdePerformanceTest

# List bundled plugins in the target IDE
./gradlew listBundledPlugins

# Generate coverage reports (Kover)
./gradlew koverHtmlReport  # HTML report
./gradlew koverXmlReport   # XML report
```

## Plugin Configuration

- **Target Platform**: IntelliJ Community 2022.3.3 (IC)
- **Minimum Build**: 231 (2023.1)
- **Java Version**: 17 (JetBrains JVM)
- **Dependencies**: IdeaVIM 2.1.0 (specified in build.gradle.kts)

## Architecture

### Core Components

**JumpHandler (singleton)**
- Central orchestrator for all flash modes
- Implements TypedActionHandler to intercept keyboard input when modes are active
- Manages editor state: gray overlays, canvas painting, keyboard handlers
- Handles multi-split editor support for search mode
- Coordinates timeout-based auto-cancellation for vim modes

**Mode System (enum)**
- Defines all flash operation modes: SEARCH, VIM_F, VIM_F_BACKWARD, VIM_T, VIM_T_BACKWARD, VIM_REPEAT, VIM_REPEAT_BACKWARD, TREESITTER, REMOTE
- Each mode has distinct behaviors for finding, highlighting, and jumping
- Modes support "ALL" variants (VIM_F_ALL, etc.) that expand search range beyond initial cursor direction

**Finder Interface**
- Implemented by: Search, VimF, TreesitterFinder
- Methods:
  - `start(editor, mode)`: Initialize the finder for the given mode
  - `input(editor, char, lastMarks, searchString)`: Process user input and return updated marks
  - `cleanup(editor)`: Clean up editor state (highlighters, etc.)

**MarksCanvas (JComponent)**
- Custom Swing component overlaid on the editor for drawing labels
- Renders label tags, match highlights, and syntax range indicators
- Supports split-screen with separate canvas per editor
- Handles both character-based matches and PSI range selections

**KeyTagsGenerator**
- Generates optimal label sequences based on available keys and match count
- Creates a tree structure to minimize keystrokes needed to reach any match
- Ensures labels are unique and prioritized by proximity to cursor

### Action Flow

1. **User triggers action** (e.g., `<Action>(flash.search)` from .ideavimrc)
2. **BaseAction.actionPerformed()** → **JumpHandler.start(mode, event)**
3. **JumpHandler** initializes:
   - Saves original typed handlers
   - Installs custom handlers for typed input, Escape, Backspace, Enter
   - Applies gray overlay to search area
   - Creates Finder instance for the mode
   - Calls `finder.start()` to get initial marks (if any)
4. **User types characters**:
   - **JumpHandler.execute()** intercepts each character
   - Passes to **finder.input()** which returns updated marks
   - **jumpOrShowCanvas()** either jumps immediately (1 match) or displays MarksCanvas with labels
5. **User selects a label** or **presses Enter**:
   - Jump to target offset
   - Restore editor state (remove handlers, canvas, gray overlay)
   - Execute optional onJump callback (for remote mode)

### Finder Implementations

**Search**
- Searches for multi-character strings in visible area
- Supports cross-split search (searches all visible editors in project)
- Generates labels using KeyTagsGenerator
- Incremental: each character typed narrows matches or advances label selection

**VimF**
- Single-character search in a specific direction (forward/backward)
- Auto-jumps to nearest match, then allows repeated keypresses to cycle through matches
- Supports 'till' variants (t/T) that stop before/after the character
- Remembers last search for repeat commands (;/,)
- Custom highlighters rendered directly in editor markup (not canvas)
- Timeout-based auto-cancellation (configurable)

**TreesitterFinder**
- Uses IntelliJ PSI (Program Structure Interface) instead of tree-sitter parser
- Walks PSI tree from cursor outward, collecting syntax ranges
- Filters out common operator keys (y/c/d/x/s) from labels to avoid conflicts
- Auto-selects innermost range on activation
- Labels shown at both start and end of each range

### Remote Mode

Remote mode allows operators (like delete) to be applied at a distant location while preserving the original cursor position:

1. User presses operator (e.g., `d`) then `r` to activate remote mode
2. Search UI appears (same as flash.search)
3. User jumps to target line
4. Operator executes at target
5. **remoteCommandListener** restores cursor to original position after command completes
6. **remoteDocListener** tracks document changes to adjust the restored offset correctly

### Configuration System

**UserConfig** (ApplicationService)
- Persists user settings across IDE restarts
- Settings:
  - `characters`: Label sequence (default: proximity-ordered alphabet)
  - `labelFg/labelBg`: Label colors (foreground/background)
  - `labelHitFg/labelHitBg`: Already-typed portion colors
  - `matchFg/matchBg`: Match highlight colors
  - `matchNearestFg/matchNearestBg`: Nearest match colors
  - `labelBeforeMatch`: Position labels before vs after match
  - `autoJumpWhenSingle`: Auto-jump when only one match
  - `scrolloff`: Lines to keep above/below cursor on jump (vim f/t modes)
  - `vimModeTimeoutMillis`: Auto-cancel timeout for vim modes (-1 = disabled)
  - `searchAcrossSplits`: Enable cross-split search

**Configurable** (ApplicationConfigurable)
- UI for Settings panel (Settings → Other Settings → vim-flash)

**ConfigUI**
- Swing form for configuration with color pickers and text fields

## Key Design Patterns

**Singleton Pattern**: JumpHandler is the single global state manager for all flash operations.

**Strategy Pattern**: Finder interface with multiple implementations (Search, VimF, TreesitterFinder) allows different search strategies.

**State Machine**: JumpHandler tracks `isStart` flag and manages transitions between active/inactive states.

**Observer Pattern**: Document and command listeners track changes for remote mode offset correction.

**Compatibility Layer**: Reflection-based API calls to support multiple IntelliJ Platform versions:
- `ModalityState` resolution (NON_MODAL vs nonModal method)
- Document listener registration (with/without Disposable)
- Command listener registration (with/without Disposable)

## Testing

- Tests located in `src/test/kotlin/com/github/yelog/ideavimflash/`
- Run with `./gradlew test`
- UI tests available via `./gradlew runIdeForUiTests`

## Plugin Registration

Plugin actions are registered in `src/main/resources/META-INF/plugin.xml`:
- `flash.search` → SearchAction
- `flash.find` → FindAction (vim f)
- `flash.find_backward` → FindBackwardAction (vim F)
- `flash.till` → TillAction (vim t)
- `flash.till_backward` → TillBackwardAction (vim T)
- `flash.repeat` → RepeatAction (vim ;)
- `flash.repeat_backward` → RepeatBackwardAction (vim ,)
- `flash.treesitter` → TreesitterAction
- `flash.remote` → RemoteAction

Users bind these actions in `.ideavimrc` with `map <key> <Action>(flash.*)`

## Important Notes

- The plugin depends on IdeaVIM being installed
- Gray overlay and canvas painting happen in the EDT (Event Dispatch Thread)
- Cross-split search only works in SEARCH mode, not vim F/T modes
- PSI-based treesitter is not true tree-sitter but mimics the UX using IntelliJ's semantic understanding
- Remote mode currently only supports delete-line operations (planned: yank, change)
- Compatibility with IntelliJ Platform 231+ (2023.1 and newer)
