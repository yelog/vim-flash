# Remote Development Support Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Make vim-flash load on the JetBrains Client side in Remote Development so IdeaVim action mappings can access the active editor UI.

**Architecture:** vim-flash is an editor interaction plugin: actions read `CommonDataKeys.EDITOR`, intercept typed editor input, install editor action handlers, draw Swing overlays via `MarksCanvas`, and use editor highlighters. These APIs belong on the frontend side of Split Mode, so the first safe fix is to declare a frontend-side platform dependency and document that Marketplace installation/sync should install the plugin on JetBrains Client. A full frontend/backend/shared modular plugin migration is deferred because the project still targets `sinceBuild=231` and uses Gradle IntelliJ Plugin 1.x.

**Tech Stack:** IntelliJ Platform plugin XML, Kotlin, Gradle IntelliJ Plugin 1.x, IdeaVim action mappings.

---

### Task 1: Add Remote Development Frontend Dependency

**Files:**
- Modify: `src/main/resources/META-INF/plugin.xml`
- Create: `src/main/resources/META-INF/vim-flash-withFrontend.xml`

**Step 1: Add frontend dependency**

Add an optional dependency on `intellij.platform.frontend` near the platform dependency:

```xml
<depends>com.intellij.modules.platform</depends>
<depends optional="true" config-file="vim-flash-withFrontend.xml">intellij.platform.frontend</depends>
```

**Step 2: Document why it exists**

Add a short XML comment explaining that vim-flash uses editor UI and key event APIs, so it must be available to the JetBrains Client in Split Mode.

**Step 3: Verify descriptor parsing**

Run: `./gradlew buildPlugin`

Expected: plugin package builds and `patchPluginXml` accepts the dependency declaration.

### Task 2: Update User-Facing Documentation

**Files:**
- Modify: `README.md`
- Modify: `CHANGELOG.md`
- Modify: `src/main/resources/META-INF/plugin.xml`

**Step 1: Add README section**

Add a short Remote Development section explaining:

- Install vim-flash through JetBrains Marketplace when possible, so plugin sync can install it on JetBrains Client.
- The plugin is frontend/UI-heavy and the `.ideavimrc` action mappings must be available in the remote session.
- Local ZIP/JAR installs may not sync automatically between Host and Client.

**Step 2: Add changelog entry**

Add an Unreleased entry mentioning remote development frontend compatibility.

**Step 3: Add plugin change note**

Add the same user-visible change note under the `Unreleased` section in `plugin.xml`.

### Task 3: Verify Build and Tests

**Files:**
- No new source files.

**Step 1: Run tests**

Run: `./gradlew test`

Expected: existing platform tests pass.

**Step 2: Build plugin**

Run: `./gradlew buildPlugin`

Expected: distributable ZIP is produced without plugin descriptor validation errors.

### Task 4: Add Editor Context Fallback

**Files:**
- Create: `src/main/kotlin/org/yelog/ideavim/flash/EditorContext.kt`
- Modify: `src/main/kotlin/org/yelog/ideavim/flash/Actions.kt`
- Modify: `src/main/kotlin/org/yelog/ideavim/flash/JumpHandler.kt`

**Step 1: Centralize editor lookup**

Read `CommonDataKeys.EDITOR`, then fall back to `CommonDataKeys.EDITOR_EVEN_IF_INACTIVE`.

**Step 2: Use fallback in action enablement and startup**

Keep actions enabled when an inactive editor is available and start `JumpHandler` from that editor.

**Step 3: Notify when no editor is available**

If the action fires without any editor context, show a short Remote Development hint instead of silently returning.

### Task 5: Follow-Up Architecture Recommendation

**Files:**
- No code change required now.

**Step 1: Document future split-plugin work**

Record in final response that a future major architecture improvement should migrate to IntelliJ Platform Gradle Plugin 2.x and Plugin Model Version 2 if vim-flash adds backend-side PSI/indexing/RPC needs.

**Step 2: Keep current scope minimal**

Do not introduce RPC, Gradle submodules, or backend services in this issue fix.
