package com.palmastro.app

import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertTrue
import kotlin.test.assertContains

class ProguardCrashlyticsTest {

    private fun loadProguardRules(): String {
        val candidates = listOf(
            System.getProperty("user.dir") + "/app/proguard-rules.pro",
            System.getProperty("user.dir") + "/proguard-rules.pro",
        )
        val file = candidates.map { File(it) }.firstOrNull { it.exists() }
            ?: error("proguard-rules.pro not found in candidates: $candidates")
        return file.readText()
    }

    @Test
    fun `proguard keeps SourceFile and LineNumberTable for Crashlytics deobfuscation`() {
        val rules = loadProguardRules()
        assertTrue(
            rules.contains("-keepattributes") && rules.contains("SourceFile") && rules.contains("LineNumberTable"),
            "ProGuard must keep SourceFile,LineNumberTable for readable Crashlytics stack traces",
        )
    }

    @Test
    fun `proguard keeps Exception subclasses for Crashlytics reporting`() {
        val rules = loadProguardRules()
        assertContains(
            rules,
            "-keep public class * extends java.lang.Exception",
            message = "ProGuard must keep Exception subclasses so Crashlytics reports include class names",
        )
    }

    @Test
    fun `proguard keeps Firebase Crashlytics SDK classes`() {
        val rules = loadProguardRules()
        assertContains(
            rules,
            "-keep class com.google.firebase.crashlytics.**",
            message = "ProGuard must keep Firebase Crashlytics classes to prevent runtime NoClassDefFoundError",
        )
    }

    @Test
    fun `proguard does not strip MediaPipe classes needed alongside Crashlytics`() {
        val rules = loadProguardRules()
        assertContains(
            rules,
            "-keep class com.google.mediapipe.**",
            message = "MediaPipe keep rules must remain present after Crashlytics additions",
        )
    }

    @Test
    fun `proguard keeps Room database classes`() {
        val rules = loadProguardRules()
        assertContains(
            rules,
            "-keep class * extends androidx.room.RoomDatabase",
            message = "Room keep rules must remain present after Crashlytics additions",
        )
    }

    @Test
    fun `proguard keeps kotlinx serialization companion objects`() {
        val rules = loadProguardRules()
        assertTrue(
            rules.contains("kotlinx.serialization.KSerializer"),
            "Serialization keep rules must remain present after Crashlytics additions",
        )
    }
}
