plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "vim-flash"

// Include the local IdeaVIM checkout only when it exists. Gradle 9 rejects
// `include()` for directories that are not present, and the directory is only
// created when developing against a local Vim plugin build.
if (file("IdeaVIM").isDirectory) {
    include("IdeaVIM")
}
