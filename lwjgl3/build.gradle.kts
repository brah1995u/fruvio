// Desktop launcher (LWJGL3) — runs the game on Windows so we can see + screenshot it
// without an Android emulator. The android module produces the APK from the same core.
plugins {
    kotlin("jvm")
    application
}

kotlin {
    jvmToolchain(17)
}

val gdxVersion = "1.13.1"

application {
    mainClass.set("studio.cortex.fruvio.lwjgl3.Lwjgl3LauncherKt")
}

dependencies {
    implementation(project(":core"))
    implementation("com.badlogicgames.gdx:gdx-backend-lwjgl3:$gdxVersion")
    implementation("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-desktop")
    implementation("com.badlogicgames.gdx:gdx-freetype:$gdxVersion")
    implementation("com.badlogicgames.gdx:gdx-freetype-platform:$gdxVersion:natives-desktop")
    implementation("com.badlogicgames.gdx:gdx-box2d-platform:$gdxVersion:natives-desktop")
}

// Resolve Gdx.files.internal(...) against the shared assets folder.
tasks.named<JavaExec>("run") {
    workingDir = rootProject.file("assets")
    maxHeapSize = "256m"
}
