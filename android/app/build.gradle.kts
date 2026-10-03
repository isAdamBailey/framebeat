plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
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
        applicationId = "io.adambailey.framebeat"
        // First API level with AudioTrack low-latency performance mode. Do not lower.
        minSdk = 26
        // Play requires targetSdk 36 for new apps and updates since 2026-08-31.
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
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
        // Version checks change with the calendar, not the code, so they
        // would fail CI on main the day a newer release ships.
        disable += listOf("OldTargetApi", "GradleDependency", "AndroidGradlePluginVersion", "NewerVersionAvailable")
        warningsAsErrors = true
        abortOnError = true
    }
}

dependencies {
    implementation(project(":engine"))
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.ui)
}
