import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

// The Play upload key lives outside the repo; keystore.properties points at it
// and is gitignored. Without it, debug builds still work and release builds stop
// at checkReleaseBundle. See android/README.md.
val releaseApplicationId = "io.adambailey.framebeat"
val keystoreFile = rootProject.file("keystore.properties")
val keystore = Properties().apply {
    if (keystoreFile.isFile) keystoreFile.inputStream().use(::load)
}

android {
    namespace = "io.adambailey.framebeat"
    // The Compose BOM needs 37.2 to compile against; it does not change runtime behavior.
    compileSdk {
        version = release(37) {
            minorApiLevel = 2
        }
    }

    defaultConfig {
        // Permanent after the first bundle upload.
        applicationId = releaseApplicationId
        // First API level with AudioTrack low-latency performance mode. Do not lower.
        minSdk = 26
        // Play requires targetSdk 36 for new apps and updates since 2026-08-31.
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"
    }

    signingConfigs {
        if (keystoreFile.isFile) {
            create("release") {
                storeFile = rootProject.file(keystore.getProperty("storeFile"))
                storePassword = keystore.getProperty("storePassword")
                keyAlias = keystore.getProperty("keyAlias")
                keyPassword = keystore.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.findByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    lint {
        // targetSdk follows Play's floor, not the newest platform (issue #16).
        disable += "OldTargetApi"
        // Still reported, but as information: a newer release shipping would
        // otherwise fail CI on main with no code change.
        informational += listOf("GradleDependency", "AndroidGradlePluginVersion", "NewerVersionAvailable")
        warningsAsErrors = true
        abortOnError = true
    }
}

// Fails a release build before it packages anything Play would reject, or
// anything that would lock in the wrong application id on first upload.
abstract class CheckReleaseBundle : DefaultTask() {
    @get:Input abstract val applicationId: Property<String>
    @get:Input abstract val expectedApplicationId: Property<String>
    @get:Input abstract val signed: Property<Boolean>

    @TaskAction
    fun check() {
        val actual = applicationId.get()
        val expected = expectedApplicationId.get()
        check(actual == expected) {
            "Release application id is $actual, expected $expected. It is permanent after the first Play upload."
        }
        check(signed.get()) {
            "No upload key: create android/keystore.properties as described in android/README.md."
        }
    }
}

val checkReleaseBundle = tasks.register<CheckReleaseBundle>("checkReleaseBundle") {
    expectedApplicationId.set(releaseApplicationId)
    signed.set(keystoreFile.isFile)
}

androidComponents {
    onVariants(selector().withBuildType("release")) { variant ->
        checkReleaseBundle.configure { applicationId.set(variant.applicationId) }
    }
}

tasks.matching { it.name == "bundleRelease" || it.name == "assembleRelease" }.configureEach {
    dependsOn(checkReleaseBundle)
}

dependencies {
    implementation(project(":engine"))
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.ui)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
