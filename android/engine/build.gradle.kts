// Pure JVM: types, geometry, voices, and scheduling. No Android dependencies,
// so `./gradlew :engine:test` runs without an emulator.
plugins {
    alias(libs.plugins.kotlin.jvm)
}

// Java's target must match Kotlin's, or Gradle fails the build on a newer JDK.
java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17
    }
}

dependencies {
    testImplementation(libs.junit)
    // Test-only: parses the shared spec/ cases. Nothing here ships in the app.
    testImplementation(libs.kotlinx.serialization.json)
}

// The shared JSON cases at the repo root. Declared as an input so a spec/
// change reruns the tests instead of reusing a cached result.
val specDir = rootProject.layout.projectDirectory.dir("../spec")

tasks.test {
    inputs.dir(specDir).withPropertyName("spec").withPathSensitivity(PathSensitivity.RELATIVE)
    // Through an argument provider, so the machine's absolute path is not part
    // of the task's cache key; the directory input above already is.
    val specPath = specDir.asFile.absolutePath
    jvmArgumentProviders += CommandLineArgumentProvider { listOf("-Dframebeat.specDir=$specPath") }
}
