// Android launcher → APK. Same core, plus the Android backend and native .so libraries.
plugins {
    id("com.android.application")
    kotlin("android")
}

val gdxVersion = "1.13.1"
val releaseKeystorePath = providers.environmentVariable("FRUVIO_KEYSTORE_PATH").orNull
val releaseKeystorePassword = providers.environmentVariable("FRUVIO_KEYSTORE_PASSWORD").orNull
val releaseKeyAlias = providers.environmentVariable("FRUVIO_KEY_ALIAS").orNull
val releaseKeyPassword = providers.environmentVariable("FRUVIO_KEY_PASSWORD").orNull
val releaseSigningReady = listOf(
    releaseKeystorePath,
    releaseKeystorePassword,
    releaseKeyAlias,
    releaseKeyPassword,
).all { !it.isNullOrBlank() }

android {
    namespace = "studio.cortex.fruvio"
    compileSdk = 36

    defaultConfig {
        applicationId = "studio.cortex.fruvio"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"
    }

    sourceSets["main"].apply {
        jniLibs.srcDirs("libs")
        assets.srcDirs("../assets")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    signingConfigs {
        if (releaseSigningReady) {
            create("release") {
                storeFile = file(requireNotNull(releaseKeystorePath))
                storePassword = releaseKeystorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        named("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfigs.findByName("release")?.let { signingConfig = it }
        }
    }

    packaging {
        resources.excludes.add("META-INF/robovm/ios/robovm.xml")
    }
}

kotlin {
    jvmToolchain(17)
}

// libGDX native .so libraries, copied into libs/<abi>/ before packaging.
val natives: Configuration by configurations.creating

dependencies {
    implementation(project(":core"))
    implementation("com.badlogicgames.gdx:gdx-backend-android:$gdxVersion")
    implementation("com.badlogicgames.gdx:gdx-freetype:$gdxVersion")
    natives("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-armeabi-v7a")
    natives("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-arm64-v8a")
    natives("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-x86")
    natives("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-x86_64")
    natives("com.badlogicgames.gdx:gdx-freetype-platform:$gdxVersion:natives-armeabi-v7a")
    natives("com.badlogicgames.gdx:gdx-freetype-platform:$gdxVersion:natives-arm64-v8a")
    natives("com.badlogicgames.gdx:gdx-freetype-platform:$gdxVersion:natives-x86")
    natives("com.badlogicgames.gdx:gdx-freetype-platform:$gdxVersion:natives-x86_64")
    natives("com.badlogicgames.gdx:gdx-box2d-platform:$gdxVersion:natives-armeabi-v7a")
    natives("com.badlogicgames.gdx:gdx-box2d-platform:$gdxVersion:natives-arm64-v8a")
    natives("com.badlogicgames.gdx:gdx-box2d-platform:$gdxVersion:natives-x86")
    natives("com.badlogicgames.gdx:gdx-box2d-platform:$gdxVersion:natives-x86_64")
}

tasks.register("copyAndroidNatives") {
    doFirst {
        configurations["natives"].files.forEach { jar ->
            val abi = jar.name.substringAfter("natives-").substringBeforeLast(".jar")
            val out = file("libs/$abi").apply { mkdirs() }
            copy {
                from(zipTree(jar))
                into(out)
                include("*.so")
            }
        }
    }
}

tasks.matching { it.name.startsWith("merge") && it.name.endsWith("JniLibFolders") }
    .configureEach { dependsOn("copyAndroidNatives") }
tasks.named("preBuild") { dependsOn("copyAndroidNatives") }
