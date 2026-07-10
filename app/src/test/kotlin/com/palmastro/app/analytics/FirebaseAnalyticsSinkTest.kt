package com.palmastro.app.analytics

import com.palmastro.analytics.AnalyticsEmitterImpl
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FirebaseAnalyticsSinkTest {

    @Test
    fun `AnalyticsEmitterImpl filters denied keys before sink`() {
        val received = mutableListOf<Pair<String, Map<String, Any>>>()
        val emitter = AnalyticsEmitterImpl { name, props -> received.add(name to props) }

        emitter.emit("scan_complete", mapOf(
            "value" to 80,
            "palm_feature_vector" to listOf(0.1, 0.2, 0.3, 0.4),
            "angle" to "front",
            "birthday_value" to "1990-03-21",
        ))

        assertEquals(1, received.size)
        val props = received[0].second
        assertEquals(80, props["value"])
        assertEquals("front", props["angle"])
        assertTrue("palm_feature_vector" !in props)
        assertTrue("birthday_value" !in props)
    }

    @Test
    fun `AnalyticsEmitterImpl drops events not in allowlist`() {
        val received = mutableListOf<Pair<String, Map<String, Any>>>()
        val emitter = AnalyticsEmitterImpl { name, props -> received.add(name to props) }

        emitter.emit("secret_internal_event", mapOf("data" to "sensitive"))
        assertTrue(received.isEmpty())
    }

    @Test
    fun `AnalyticsEmitterImpl passes allowed events to sink`() {
        val received = mutableListOf<Pair<String, Map<String, Any>>>()
        val emitter = AnalyticsEmitterImpl { name, props -> received.add(name to props) }

        emitter.emit("scan_start", mapOf("hand" to "left"))
        assertEquals(1, received.size)
        assertEquals("scan_start", received[0].first)
    }

    @Test
    fun `AnalyticsEmitterImpl filters file path values`() {
        val received = mutableListOf<Pair<String, Map<String, Any>>>()
        val emitter = AnalyticsEmitterImpl { name, props -> received.add(name to props) }

        emitter.emit("scan_complete", mapOf(
            "value" to 80,
            "image_path" to "/data/data/com.palmastro.app/files/scan/frame.jpg",
        ))

        assertEquals(1, received.size)
        assertTrue("image_path" !in received[0].second)
    }
}
