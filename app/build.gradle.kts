plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

// Release signing is only configured when these are supplied (e.g. by the release CI workflow,
// see .github/workflows/release.yml) - a local `assembleRelease` with none of them set still
// produces an unsigned APK exactly as before, so this doesn't require every contributor to have
// a keystore.
val releaseKeystorePath: String? = System.getenv("KEYSTORE_PATH")

// versionName tracks the pushed git tag (e.g. v1.2.0 -> "1.2.0") so the release workflow never
// needs a manual bump here before tagging, and the in-app "About" version row stays truthful.
// GITHUB_REF_NAME is set by the tag-triggered release workflow; `git describe` covers local
// builds. Falls back to "1.0" if neither is available (e.g. a shallow clone with no tags yet).
fun resolveVersionName(): String {
    val envTag = System.getenv("GITHUB_REF_NAME")?.takeIf { it.startsWith("v") }
    val tag = envTag ?: runCatching {
        val process = ProcessBuilder("git", "describe", "--tags", "--abbrev=0")
            .redirectErrorStream(true)
            .start()
        val output = process.inputStream.bufferedReader().readText().trim()
        process.waitFor()
        output
    }.getOrNull()?.takeIf { it.isNotBlank() }
    return tag?.removePrefix("v") ?: "1.0"
}

// Derived purely from the version name (major*10_000 + minor*100 + patch) so it doesn't depend
// on git history depth (CI checkouts are often shallow) and only ever increases with real releases.
fun versionCodeFromName(name: String): Int {
    val parts = name.split(".", "-").mapNotNull { it.toIntOrNull() }
    val major = parts.getOrElse(0) { 1 }
    val minor = parts.getOrElse(1) { 0 }
    val patch = parts.getOrElse(2) { 0 }
    return major * 10_000 + minor * 100 + patch
}

val resolvedVersionName = resolveVersionName()
val resolvedVersionCode = versionCodeFromName(resolvedVersionName)

android {
    namespace = "com.jaguar.gearbox"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.jaguar.gearbox"
        minSdk = 30
        targetSdk = 35
        versionCode = resolvedVersionCode
        versionName = resolvedVersionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (releaseKeystorePath != null) {
            create("release") {
                storeFile = file(releaseKeystorePath)
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                keyAlias = System.getenv("KEY_ALIAS")
                keyPassword = System.getenv("KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            if (releaseKeystorePath != null) {
                signingConfig = signingConfigs.getByName("release")
            }
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        getByName("debug") {
            applicationIdSuffix = ".beta"
            versionNameSuffix = "-b"
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.runtime.ktx)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.ui.text.google.fonts)
    implementation(libs.androidx.glance.appwidget)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}