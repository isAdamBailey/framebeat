// Pure JVM: types, geometry, voices, and scheduling. No Android dependencies,
// so `./gradlew :engine:test` runs without an emulator.
plugins {
    alias(libs.plugins.kotlin.jvm)
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    testImplementation(libs.junit)
}
