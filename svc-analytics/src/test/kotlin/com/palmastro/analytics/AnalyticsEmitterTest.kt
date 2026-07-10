package com.palmastro.analytics

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AnalyticsEmitterTest {
    private val emitted = mutableListOf<Pair<String, Map<String, Any>>>()
    private val emitter = AnalyticsEmitterImpl(sink = { name, props -> emitted.add(name to props) })

    @Test
    fun `emit sends allowed events`() {
        emitter.emit("scan_start", mapOf("hand" to "left"))
        assertEquals(1, emitted.size)
        assertEquals("scan_start", emitted[0].first)
    }

    @Test
    fun `emit drops unrecognized events`() {
        emitter.emit("unknown_event", mapOf("foo" to "bar"))
        assertTrue(emitted.isEmpty())
    }

    @Test
    fun `emit strips palm_feature props`() {
        emitter.emit("inference_success", mapOf("palm_feature_vector" to listOf(1, 2, 3), "duration_ms" to 500))
        assertEquals(1, emitted.size)
        assertTrue("palm_feature_vector" !in emitted[0].second)
        assertEquals(500, emitted[0].second["duration_ms"])
    }

    @Test
    fun `emit strips biometric props`() {
        emitter.emit("scan_complete", mapOf("biometric_data" to "xyz", "quality" to 80))
        assertEquals(1, emitted.size)
        assertTrue("biometric_data" !in emitted[0].second)
    }

    @Test
    fun `emit strips journal text props`() {
        emitter.emit("results_view", mapOf("journal_text" to "my private diary", "domain" to "career"))
        assertTrue("journal_text" !in emitted[0].second)
    }

    @Test
    fun `emit strips birthday value props`() {
        emitter.emit("onboarding_complete", mapOf("birthday_value" to "1990-01-01", "enabled" to true))
        assertTrue("birthday_value" !in emitted[0].second)
        assertEquals(true, emitted[0].second["enabled"])
    }

    @Test
    fun `emit strips numeric arrays longer than 3`() {
        emitter.emit("scan_complete", mapOf("value" to listOf(1, 2, 3, 4, 5), "duration_ms" to 7))
        assertTrue("value" !in emitted[0].second)
        assertEquals(7, emitted[0].second["duration_ms"])
    }

    @Test
    fun `emit strips file path props with scan or media`() {
        emitter.emit("scan_complete", mapOf("screen" to "/data/scan/frame01.jpg", "value" to 80))
        assertTrue("screen" !in emitted[0].second)
    }

    @Test
    fun `all allowlisted events are accepted`() {
        val allowedEvents = listOf(
            "onboarding_start", "scan_start", "scan_complete", "inference_start",
            "inference_success", "results_view", "purchase_success", "delete_all_data_confirm"
        )
        for (event in allowedEvents) {
            emitted.clear()
            emitter.emit(event, mapOf("test" to true))
            assertEquals(1, emitted.size, "Event $event should be allowed")
        }
    }
}
