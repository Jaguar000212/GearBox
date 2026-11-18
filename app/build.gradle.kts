import com.jaguar.versioning.ReleaseVersion
import com.jaguar.versioning.DeveloperPreviewVersion

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

// Opt-in side-by-side test identity; never changes release or ordinary debug installs.
val isolatedPreview = providers.gradleProperty("isolatedPreview").orNull?.let {
    require(it == "true" || it == "false") { "isolatedPreview must be true or false" }
    it == "true"
} ?: false
// Dedicated rolling development release. Explicit provenance is mandatory; never infer it
// from a version tag or use a stable/beta installation identity for these artifacts.
val developerPreview = providers.gradleProperty("developerPreview").orNull?.let {
    require(it == "true" || it == "false") { "developerPreview must be true or false" }
    it == "true"
} ?: false
require(!developerPreview || !isolatedPreview) { "developerPreview and isolatedPreview cannot be combined" }
val developerPreviewVersion = if (developerPreview) DeveloperPreviewVersion.fromBuild(
    providers.gradleProperty("developerPreviewRunNumber").orElse(providers.environmentVariable("DEVELOPER_PREVIEW_RUN_NUMBER")).orNull,
    providers.gradleProperty("developerPreviewSha").orElse(providers.environmentVariable("DEVELOPER_PREVIEW_SHA")).orNull,
) else null
// Unsigned validation builds are allowed. Publication separately verifies signing; if signing
// is requested here, fail closed on incomplete credentials rather than producing a false candidate.
if (developerPreview && releaseKeystorePath != null) {
    require(releaseKeystorePath.isNotBlank() && file(releaseKeystorePath).isFile && file(releaseKeystorePath).canRead()) {
        "Developer preview signing requires a readable KEYSTORE_PATH"
    }
    listOf("KEYSTORE_PASSWORD", "KEY_ALIAS", "KEY_PASSWORD").forEach { key ->
        require(!System.getenv(key).isNullOrBlank()) { "Developer preview signing requires $key" }
    }
}
// A bounded second test install avoids replacing the previously delivered preview.
val previewCheckpoint = providers.gradleProperty("previewCheckpoint").orElse("1").get().also {
    require(it == "1" || it == "2") { "previewCheckpoint must be 1 or 2" }
}
val previewSuffix = if (previewCheckpoint == "2") ".preview2" else ".preview"
val previewLabel = if (previewCheckpoint == "2") "GearBox Preview 2" else "GearBox Preview"

// Only tag refs determine CI release identity. A branch named v... is not a release.
// Manual signed-candidate builds may supply an explicit validated release tag.
fun resolveVersionTag(): String {
    System.getenv("RELEASE_VERSION_TAG")?.let { return it }
    val refName = System.getenv("GITHUB_REF_NAME")
    if (System.getenv("GITHUB_REF_TYPE") == "tag" && refName != null) return refName
    val localTag = runCatching {
        val process = ProcessBuilder("git", "describe", "--tags", "--abbrev=0", "--match", "v[0-9]*")
            .redirectErrorStream(true)
            .start()
        val output = process.inputStream.bufferedReader().readText().trim()
        if (process.waitFor() == 0) output else null
    }.getOrNull()?.takeIf { it.isNotBlank() }
    return localTag ?: "v1.0.0"
}

// Fail loudly for invalid tags instead of silently manufacturing a version code.
val resolvedVersion = ReleaseVersion.fromTag(resolveVersionTag())
val resolvedVersionName = resolvedVersion.name
val resolvedVersionCode = resolvedVersion.code

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
        manifestPlaceholders["appDisplayName"] = "@string/app_name"
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
            if (developerPreview) {
                applicationIdSuffix = ".devpreview"
                manifestPlaceholders["appDisplayName"] = "GearBox Dev Preview"
            }
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
            applicationIdSuffix = if (isolatedPreview) previewSuffix else ".beta"
            if (isolatedPreview) manifestPlaceholders["appDisplayName"] = previewLabel
            versionNameSuffix = "-b"
        }
    }
    compileOptions {
        isCoreLibraryDesugaringEnabled = true
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

// Only release outputs receive the preview version; debug identities and versions are unchanged.
androidComponents {
    onVariants(selector().withBuildType("release")) { variant ->
        developerPreviewVersion?.let { version ->
            variant.outputs.forEach { output ->
                output.versionCode.set(version.code)
                output.versionName.set(version.name)
            }
        }
    }
}

dependencies {
    implementation("org.jsoup:jsoup:1.24.1")
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs_nio:2.1.5")
    implementation("androidx.camera:camera-camera2:1.4.2")
    implementation("androidx.camera:camera-lifecycle:1.4.2")
    implementation("androidx.camera:camera-view:1.4.2")

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
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
    implementation(libs.zxing.core)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}