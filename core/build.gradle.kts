// libGDX core — the game (screens, rendering, view). Platform-agnostic (no desktop/android backend).
plugins {
    kotlin("jvm")
}

kotlin {
    jvmToolchain(17)
}

val gdxVersion = "1.13.1"

dependencies {
    implementation(project(":engine"))
    implementation("com.badlogicgames.gdx:gdx:$gdxVersion")
    implementation("com.badlogicgames.gdx:gdx-freetype:$gdxVersion")
    implementation("com.badlogicgames.gdx:gdx-box2d:$gdxVersion")
    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.3")
    testImplementation("com.badlogicgames.gdx:gdx-box2d-platform:$gdxVersion:natives-desktop")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
    testLogging { events("passed", "skipped", "failed") }
}

// Pack the loose symbol/UI/icon PNGs into one TextureAtlas. Run: gradlew :core:packTextures
// (kept off the compile/runtime classpath so gdx-tools never ships in the app).
val texturePacker: Configuration by configurations.creating
dependencies {
    texturePacker("com.badlogicgames.gdx:gdx-tools:$gdxVersion")
}
tasks.register<JavaExec>("packTextures") {
    group = "assets"
    description = "Pack design/atlas-src/*.png into assets/atlas/fruvio.atlas"
    classpath = texturePacker
    mainClass.set("com.badlogic.gdx.tools.texturepacker.TexturePacker")
    jvmArgs("-Djava.awt.headless=true")
    maxHeapSize = "256m"
    args(
        rootProject.file("design/atlas-src").absolutePath,
        rootProject.file("assets/atlas").absolutePath,
        "fruvio",
    )
}
