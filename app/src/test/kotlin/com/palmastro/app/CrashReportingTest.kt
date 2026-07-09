package com.palmastro.app

import org.junit.jupiter.api.Test
import kotlin.test.assertFalse

class CrashReportingTest {

    @Test
    fun `CrashReporting methods do not throw when Firebase is not initialized`() {
        CrashReporting.log("test message")
        CrashReporting.recordException(RuntimeException("test"))
        CrashReporting.setUserId("user-123")
    }

    @Test
    fun `CrashReporting is an object singleton`() {
        val a = CrashReporting
        val b = CrashReporting
        assert(a === b)
    }
}
