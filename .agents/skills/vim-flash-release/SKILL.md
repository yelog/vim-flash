---
name: vim-flash-release
description: Prepare or publish a release of yelog/vim-flash, including its version, CHANGELOG, generated plugin change notes, Git tag, GitHub Actions package, and downloadable GitHub Release ZIP. Use for vim-flash version bumps and releases; Marketplace upload remains manual.
---

# vim-flash 发布

为 `yelog/vim-flash` 准备或执行发布。以当前仓库的 `README.md`、`scripts/release.py` 和 `.github/workflows/auto-release.yml` 为准，不把这里的示例版本当成目标版本。

## 识别请求

- “准备 1.0.9”“整理下一版”：修改本地发布资料、验证、展示结果；不提交、不打 tag、不推送。
- “发布 1.0.9 到 GitHub”“准备并推送 1.0.9”：包含必要的发布提交、指定 tag、推送及 CI 结果跟进。执行前展示具体版本、变更摘要和推送目标；已有明确授权时直接继续，不重复确认。
- 仅分析 CI 或发布方案时先检查并说明现状，不发布实际版本。
- 没有目标版本时可以分析提交并提出候选版本、准备说明；对外发布前需要用户明确采用哪个版本。
- GitHub Release 与 Marketplace 上传分开。这个流程交付 GitHub ZIP；不执行 `publishPlugin`、不读取签名密钥、不上传 Marketplace。

## 准备

1. 定位当前 Git 仓库并读取项目指令。核实 `origin` 对应 `yelog/vim-flash`；不要在其他仓库套用本流程。查看分支、工作区、暂存区、远端状态和已有 tags，保留用户已有改动。
2. 读取 `gradle.properties`、`CHANGELOG.md`、`build.gradle.kts` 和发布工作流。查看最近的版本 tag 和 GitHub 已发布版本，检查基线以来的提交及实际 diff。历史 tag 混用 `1.0.8` 和 `v1.0.7`，不要仅靠字典排序选择基线。
3. 使用用户指定版本。检查本地和远端是否已有同一版本的带 `v` / 不带 `v` tag 和 Release。已发布版本不可重新指向另一个提交；若是在恢复已有发布，转到“重试”。
4. 修改 `gradle.properties` 中的 `pluginVersion`，在 `CHANGELOG.md` 顶部添加精确的 `## X.Y.Z` 条目。摘要基于实际变更，用仓库现有的英文风格描述用户可感知的改动，保留旧版本记录。若已存在目标条目，更新该条目而非重复添加。
5. `patchPluginXml` 会把版本及该版本的 Markdown 转为 HTML 写进最终包。无需在 `src/main/resources/META-INF/plugin.xml` 手写版本或 change-notes，也不要修改生成的 `build/` 文件。
6. 在仓库根目录运行，`VERSION` 使用已选定版本：

   ```bash
   python3 -B -m unittest discover -s scripts -p 'test_*.py'
   python3 scripts/release.py --tag "v$VERSION" --notes-file build/release-notes.md
   ./gradlew test buildPlugin
   python3 scripts/release.py --tag "v$VERSION" --archive "build/distributions/vim-flash-$VERSION.zip"
   git diff --check
   ```

   使用 Java 17 启动 Gradle。若默认 Java 不可用，检查本机已有 JDK / Gradle toolchain，并为这次命令设置 `JAVA_HOME`；不为发布顺带升级 Gradle、Kotlin 或 IntelliJ 依赖。查看 ZIP 内主插件 JAR 的 `META-INF/plugin.xml`，确认变更说明确实对应本版。构建失败时先解决范围内问题，不把未完成验证说成通过。
7. 展示版本、tag、更新说明、验证结果和本地 ZIP 路径。仅准备时到此结束。

## 发布到 GitHub

1. 发布前核对实际 diff、暂存区和所有待推送提交。默认发布目标是 `origin/main`；使用其他分支需要用户明确指定。不要把不相关的工作或暂存内容带进发布提交。只提交已审查的发布文件，使用 `chore(release): X.Y.Z` 等项目约定的消息。
2. 确认远端同名 tag 不存在后，在该提交上创建带注释的 `vX.Y.Z` tag。只推送目标分支及这个 tag，优先原子推送：

   ```bash
   git tag -a "v$VERSION" -m "Release v$VERSION"
   git push --atomic origin HEAD:refs/heads/main "refs/tags/v$VERSION"
   ```

   不使用 `--force` 或 `--tags`。推送被拒绝时检查远端差异，不自动重写历史或移动 tag。若需要先通过 PR 合并，先走仓库已有流程，tag 必须指向最终合并后的发布提交。
3. 用 GitHub connector 或 `gh run list --workflow auto-release.yml --commit COMMIT_SHA` 找到与这次 tag/提交相符的运行。持续跟进到成功或明确失败，不把“推送成功”当成“发布成功”；失败时查看失败步骤并报告原因，不自动创建另一版本或重复发布。
4. 检查 Release 已公开、预发布标志正确，并确实有 `vim-flash-X.Y.Z.zip` 附件。需要下载时用 `gh release download` 将该文件下载到 `build/release-downloads/vX.Y.Z/`，不要覆盖已有不同文件。下载后再次运行 `scripts/release.py --archive ...`；若 API 提供 SHA-256 digest，也核对下载文件哈希。
5. 返回实际 Release 链接、ZIP 下载链接、本地路径（如果下载了）以及测试结果。说明这个 ZIP 可用于 IDE 的 **Install plugin from disk…** 或手动上传 JetBrains Marketplace。GitHub 的 **Source code** ZIP 不是插件安装包。

## 重试

- 普通 main 构建不再维护草稿；唯一自动发布入口是推送 `v*` tag。
- 对已存在 tag，优先重跑失败的 Auto Release，或从包含新版工作流的分支手动运行 **Auto Release**，填写确切 tag。手动入口兼容无 `v` 的历史 tag，并使用目标 tag 的代码构建。
- 工作流先上传到草稿，再公开 Release；已有同名草稿可继续完成。已公开且有预期 ZIP 的 Release 会保持不变。已公开但缺 ZIP 的旧 Release 会报错，先说明具体状况，再按用户对该 Release 的修复要求处理，不自动覆盖公开包。
- 手动构建旧 tag 时，版本与更新说明以该 tag 的文件为准。若 tag 内 metadata 错误或构建本身不兼容，报告具体问题；不要移动旧 tag 或伪造包版本。
