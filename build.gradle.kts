// Root build — plugins declared here, applied per-module.
// Version matrix (verified compatible): Gradle 8.11.1 · AGP 8.9.2 · Kotlin 2.1.20 · libGDX 1.13.1 · JDK 17
plugins {
    kotlin("jvm") version "2.1.20" apply false
    kotlin("android") version "2.1.20" apply false
    id("com.android.application") version "8.9.2" apply false
}

allprojects {
    group = "studio.cortex.fruvio"
    version = "1.0.0"
}
