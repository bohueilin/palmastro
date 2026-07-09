package com.palmastro.integration

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class JsonRoundtripTest {

    private val json = Json { prettyPrint = false }

    @Test
    fun `standard 4-domain scores roundtrip`() {
        val original = mapOf("career" to 78, "wealth" to 60, "family" to 50, "health" to 52)
        val encoded = json.encodeToString(original)
        val decoded = json.decodeFromString<Map<String, Int>>(encoded)
        assertEquals(original, decoded)
        assertEquals(4, decoded.size)
        assertEquals(78, decoded["career"])
        assertEquals(60, decoded["wealth"])
        assertEquals(50, decoded["family"])
        assertEquals(52, decoded["health"])
    }

    @Test
    fun `empty map roundtrip`() {
        val original = emptyMap<String, Int>()
        val encoded = json.encodeToString(original)
        val decoded = json.decodeFromString<Map<String, Int>>(encoded)
        assertEquals(original, decoded)
        assertTrue(decoded.isEmpty())
        assertEquals("{}", encoded)
    }

    @Test
    fun `single domain roundtrip`() {
        val original = mapOf("career" to 85)
        val encoded = json.encodeToString(original)
        val decoded = json.decodeFromString<Map<String, Int>>(encoded)
        assertEquals(original, decoded)
        assertEquals(1, decoded.size)
        assertEquals(85, decoded["career"])
    }

    @Test
    fun `score at boundary values 0 and 100 roundtrip`() {
        val original = mapOf("career" to 0, "wealth" to 100, "family" to 0, "health" to 100)
        val encoded = json.encodeToString(original)
        val decoded = json.decodeFromString<Map<String, Int>>(encoded)
        assertEquals(original, decoded)
        assertEquals(0, decoded["career"])
        assertEquals(100, decoded["wealth"])
        assertEquals(0, decoded["family"])
        assertEquals(100, decoded["health"])
    }

    @Test
    fun `scores with all same value roundtrip`() {
        val original = mapOf("career" to 50, "wealth" to 50, "family" to 50, "health" to 50)
        val encoded = json.encodeToString(original)
        val decoded = json.decodeFromString<Map<String, Int>>(encoded)
        assertEquals(original, decoded)
        decoded.values.forEach { assertEquals(50, it) }
    }
}
