import java.net.URI
import java.util.Base64
import java.util.Properties

plugins {
    alias(libs.plugins.ksp)
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

// Local public client configuration is never committed. Never use service_role/secret keys.
val supabaseProperties = Properties().apply {
    val config = rootProject.file("supabase.properties")
    if (config.exists()) config.inputStream().use { load(it) }
}
fun configValue(name: String): String = providers.gradleProperty(name).orNull
    ?: System.getenv(name) ?: supabaseProperties.getProperty(name, "")
fun String.asBuildConfigString() = "\"" + replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "").replace("\r", "") + "\""
val supabaseUrl = configValue("SUPABASE_URL").trim()
val supabaseKey = configValue("SUPABASE_PUBLISHABLE_KEY").trim()
require(supabaseUrl.isBlank() || (URI(supabaseUrl).let { it.scheme == "https" && !it.host.isNullOrBlank() })) {
    "SUPABASE_URL must be HTTPS"
}
require(
    supabaseKey.isBlank() || supabaseKey.startsWith("sb_publishable_") || runCatching {
        val payload = String(Base64.getUrlDecoder().decode(supabaseKey.split('.')[1]))
        Regex("\"role\"\\s*:\\s*\"anon\"").containsMatchIn(payload)
    }.getOrDefault(false),
) { "Use a publishable/anon key, never a secret or service_role key" }

android {
    namespace = "com.llsit.sipduddy"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.llsit.sipduddy"
        minSdk = 28
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"
        buildConfigField("String", "SUPABASE_URL", supabaseUrl.asBuildConfigString())
        buildConfigField("String", "SUPABASE_PUBLISHABLE_KEY", supabaseKey.asBuildConfigString())
        buildConfigField("String", "AUTH_SCHEME", "\"sipbuddy\"")
        manifestPlaceholders["authScheme"] = "sipbuddy"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = true
                packageScope = setOf("**")
            }
        }
    }
    buildTypes.create("benchmark") {
        initWith(buildTypes.getByName("release"))
        applicationIdSuffix = ".benchmark"
        buildConfigField("String", "AUTH_SCHEME", "\"sipbuddy-benchmark\"")
        manifestPlaceholders["authScheme"] = "sipbuddy-benchmark"
        signingConfig = signingConfigs.getByName("debug")
        isDebuggable = false
        matchingFallbacks += "release"
    }
    // Instrumented tests may uninstall their target after running. Never target the personal diary.
    buildTypes.create("qa") {
        initWith(buildTypes.getByName("debug"))
        applicationIdSuffix = ".qa"
        signingConfig = signingConfigs.getByName("debug")
        matchingFallbacks += "debug"
        buildConfigField("String", "AUTH_SCHEME", "\"sipbuddy-qa\"")
        manifestPlaceholders["authScheme"] = "sipbuddy-qa"
    }
    testBuildType = "qa"
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    lint {
        abortOnError = true
        checkReleaseBuilds = true
        checkDependencies = true
        lintConfig = file("lint.xml")
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.supabase.auth)
    implementation(libs.supabase.postgrest)
    implementation(libs.work.runtime)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.coroutines.android)
    implementation(libs.room.runtime)
    ksp(libs.room.compiler)
    implementation(libs.datastore)
    implementation(libs.koin.android)
    implementation(libs.koin.compose)
    implementation(libs.lifecycle.compose)
    implementation(libs.lifecycle.viewmodel)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.runtime)
    implementation(libs.androidx.compose.runtime.saveable)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    testImplementation(libs.junit)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.coroutines.test)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    add("qaImplementation", libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
ksp { arg("room.schemaLocation", "$projectDir/schemas") }
