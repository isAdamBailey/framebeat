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
}
