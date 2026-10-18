// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
}
// Pinned formatter shared by local development and automated checks.
val ktlint = configurations.create("ktlint") {
    attributes {
        attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_RUNTIME))
        attribute(Category.CATEGORY_ATTRIBUTE, objects.named(Category.LIBRARY))
        attribute(Bundling.BUNDLING_ATTRIBUTE, objects.named(Bundling.EXTERNAL))
        attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, objects.named(LibraryElements.JAR))
    }
}

dependencies {
    ktlint("com.pinterest.ktlint:ktlint-cli:1.8.0")
}

val kotlinSourcePatterns = listOf("**/*.kt", "**/*.kts", "!**/build/**", "!**/.gradle/**", "!**/.idea/**", "!**/.kotlin/**")

tasks.register<JavaExec>("formatKotlin") {
    group = "formatting"
    description = "Reformat Kotlin and Gradle Kotlin files, remove unused imports and order imports."
    classpath = ktlint
    mainClass.set("com.pinterest.ktlint.Main")
    workingDir = rootDir
    args("--format")
    args(kotlinSourcePatterns)
}

tasks.register<JavaExec>("checkKotlinFormat") {
    group = "verification"
    description = "Check Kotlin formatting and imports without changing source files."
    classpath = ktlint
    mainClass.set("com.pinterest.ktlint.Main")
    workingDir = rootDir
    args(kotlinSourcePatterns)
}
