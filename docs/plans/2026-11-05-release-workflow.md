# vim-flash Release Workflow Implementation Plan

**Goal:** Prepare a version through a reusable Skill and publish an installable ZIP when its tag is pushed.

**Architecture:** Keep `gradle.properties` and `CHANGELOG.md` as the version and release-note sources. Let Gradle generate the packaged plugin descriptor. Use one tag-triggered workflow to validate, test, package, and publish a GitHub Release; upload to Marketplace manually.

**Tech Stack:** GitHub Actions, Gradle, Python standard library, GitHub CLI, Codex Skill.

## Steps

1. Add `scripts/release.py` and focused tests for tag/version agreement, exact changelog extraction, and ZIP metadata. Run `python3 -m unittest discover -s scripts -p 'test_*.py'`.
2. Update `.github/workflows/auto-release.yml` to build the requested tag, validate its package, and publish assets before making the release public. Handle existing drafts and completed-release retries.
3. Remove draft creation from `.github/workflows/build.yml` and remove the automatic Marketplace workflow. Preserve ordinary build, tests, inspection, and compatibility checks.
4. Remove duplicated source change notes from `plugin.xml`; retain Gradle generation from `CHANGELOG.md`. Document release and download steps in `README.md`.
5. Create the repository-local `vim-flash-release` Skill under `.agents/skills`. Include preparation, scoped commit/tag/push when requested, workflow monitoring, and download verification.
6. Validate the Skill, workflows, helper, and a real plugin build with `./gradlew test buildPlugin`. Inspect the packaged version and change notes. Do not create a release commit, tag, push, or Marketplace upload during implementation.

## Validation completed

- Java 17: `./gradlew test buildPlugin` succeeded; all 7 plugin tests passed.
- All 10 Python release-validation tests passed.
- The generated `vim-flash-1.0.8.zip` contains version `1.0.8` and the HTML change notes from that changelog entry.
- actionlint 1.7.12 accepted both modified workflows; the Skill validator and `git diff --check` passed.
- Local shell simulations passed for a new release, existing draft, published-release retry, published release without its ZIP, failed upload, and prerelease. No GitHub mutation was used in these simulations.
- Existing platform mismatch remains: build IDE 2022.3.3 versus minimum supported build 231. The build succeeds but logs plugin loading warnings while generating searchable options. Platform dependency changes are outside this release-workflow change.
- GitHub execution awaits pushing the reviewed workflow changes. No release commit, tag, push, or Marketplace upload was performed.
