# libGDX resolves native entry points and the Android launcher across JNI/framework boundaries.
-keep class studio.cortex.fruvio.android.AndroidLauncher { *; }
-keepclasseswithmembers,includedescriptorclasses class * {
    native <methods>;
}
-dontwarn com.badlogic.gdx.backends.android.**
