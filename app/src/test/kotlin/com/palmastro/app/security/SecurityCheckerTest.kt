package com.palmastro.app.security

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SecurityCheckerTest {

    @Test
    fun `SecurityReport with no threats is secure`() {
        val report = SecurityReport(false, false, false, true, emptyList())
        assertTrue(report.isSecure)
        assertEquals(ThreatLevel.NONE, report.threatLevel)
    }

    @Test
    fun `SecurityReport with rooted device is HIGH threat`() {
        val report = SecurityReport(true, false, false, true, listOf("DEVICE_ROOTED"))
        assertFalse(report.isSecure)
        assertEquals(ThreatLevel.HIGH, report.threatLevel)
    }

    @Test
    fun `SecurityReport with invalid signature is CRITICAL threat`() {
        val report = SecurityReport(false, false, false, false, listOf("SIGNATURE_INVALID"))
        assertEquals(ThreatLevel.CRITICAL, report.threatLevel)
    }

    @Test
    fun `SecurityReport with debuggable is MEDIUM threat`() {
        val report = SecurityReport(false, true, false, true, listOf("APP_DEBUGGABLE"))
        assertEquals(ThreatLevel.MEDIUM, report.threatLevel)
    }

    @Test
    fun `SecurityReport with emulator is LOW threat`() {
        val report = SecurityReport(false, false, true, true, listOf("RUNNING_ON_EMULATOR"))
        assertEquals(ThreatLevel.LOW, report.threatLevel)
    }

    @Test
    fun `threat level priority - CRITICAL beats HIGH`() {
        val report = SecurityReport(true, false, false, false, listOf("DEVICE_ROOTED", "SIGNATURE_INVALID"))
        assertEquals(ThreatLevel.CRITICAL, report.threatLevel)
    }

    @Test
    fun `threat level priority - HIGH beats MEDIUM`() {
        val report = SecurityReport(true, true, false, true, listOf("DEVICE_ROOTED", "APP_DEBUGGABLE"))
        assertEquals(ThreatLevel.HIGH, report.threatLevel)
    }

    @Test
    fun `ThreatLevel enum ordering`() {
        assertTrue(ThreatLevel.NONE < ThreatLevel.LOW)
        assertTrue(ThreatLevel.LOW < ThreatLevel.MEDIUM)
        assertTrue(ThreatLevel.MEDIUM < ThreatLevel.HIGH)
        assertTrue(ThreatLevel.HIGH < ThreatLevel.CRITICAL)
    }
}
