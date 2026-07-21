// Pure-Kotlin/JVM engine — NO libGDX, NO Android imports (non-negotiable).
// Deterministic, seeded, unit-testable, server-runnable. The renderer only ever
// reveals what this module already decided.
plugins {
    kotlin("jvm")
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.3")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
    testLogging { events("passed", "skipped", "failed") }
}

// No runSim task here (unlike Flame Jester's engine module) — Fruvio has no RTP/gambling
// concept to simulate (design doc §11), so there is nothing for a Monte-Carlo task to check.
