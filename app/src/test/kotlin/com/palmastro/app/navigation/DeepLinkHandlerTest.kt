package com.palmastro.app.navigation

import android.content.Intent
import android.net.Uri
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DeepLinkHandlerTest {

    private fun intentWithUri(uri: String): Intent {
        val intent = mockk<Intent>()
        every { intent.data } returns Uri.parse(uri)
        return intent
    }

    @Test
    fun `parse returns null for null intent`() {
        assertNull(DeepLinkHandler.parse(null))
    }

    @Test
    fun `parse returns null for non-palmastro scheme`() {
        assertNull(DeepLinkHandler.parse(intentWithUri("https://example.com")))
    }

    @Test
    fun `parse returns Results for palmastro results`() {
        val dest = DeepLinkHandler.parse(intentWithUri("palmastro://results"))
        assertIs<DeepLinkDestination.Results>(dest)
        assertNull(dest.monthKey)
    }

    @Test
    fun `parse returns Results with valid monthKey`() {
        val dest = DeepLinkHandler.parse(intentWithUri("palmastro://results?monthKey=2026-03"))
        assertIs<DeepLinkDestination.Results>(dest)
        assertEquals("2026-03", dest.monthKey)
    }

    @Test
    fun `parse ignores invalid monthKey format`() {
        val dest = DeepLinkHandler.parse(intentWithUri("palmastro://results?monthKey=invalid"))
        assertIs<DeepLinkDestination.Results>(dest)
        assertNull(dest.monthKey)
    }

    @Test
    fun `parse rejects monthKey with impossible month`() {
        val dest = DeepLinkHandler.parse(intentWithUri("palmastro://results?monthKey=2026-13"))
        assertIs<DeepLinkDestination.Results>(dest)
        assertNull(dest.monthKey)
    }

    @Test
    fun `parse returns DomainDetail with valid params`() {
        val dest = DeepLinkHandler.parse(intentWithUri("palmastro://domain?domain=career&monthKey=2026-03"))
        assertIs<DeepLinkDestination.DomainDetail>(dest)
        assertEquals("career", dest.domain)
        assertEquals("2026-03", dest.monthKey)
    }

    @Test
    fun `parse rejects invalid domain name`() {
        val dest = DeepLinkHandler.parse(intentWithUri("palmastro://domain?domain=hacking&monthKey=2026-03"))
        assertNull(dest)
    }

    @Test
    fun `parse rejects domain without monthKey`() {
        val dest = DeepLinkHandler.parse(intentWithUri("palmastro://domain?domain=career"))
        assertNull(dest)
    }

    @Test
    fun `parse returns Scan for palmastro scan`() {
        assertIs<DeepLinkDestination.Scan>(DeepLinkHandler.parse(intentWithUri("palmastro://scan")))
    }

    @Test
    fun `parse returns History for palmastro history`() {
        assertIs<DeepLinkDestination.History>(DeepLinkHandler.parse(intentWithUri("palmastro://history")))
    }

    @Test
    fun `parse returns null for unknown host`() {
        assertNull(DeepLinkHandler.parse(intentWithUri("palmastro://unknown")))
    }

    @Test
    fun `buildResultsLink produces valid URI`() {
        val link = DeepLinkHandler.buildResultsLink("2026-03")
        assertEquals("palmastro://results?monthKey=2026-03", link)
    }

    @Test
    fun `buildDomainLink produces valid URI`() {
        val link = DeepLinkHandler.buildDomainLink("career", "2026-03")
        assertEquals("palmastro://domain?domain=career&monthKey=2026-03", link)
    }

    // ── Destination → navigation route mapping ──

    @Test
    fun `routeFor Home maps to results`() {
        assertEquals("results", DeepLinkHandler.routeFor(DeepLinkDestination.Home))
    }

    @Test
    fun `routeFor Results without monthKey maps to results`() {
        assertEquals("results", DeepLinkHandler.routeFor(DeepLinkDestination.Results(null)))
    }

    @Test
    fun `routeFor Results with monthKey maps to results query route`() {
        assertEquals(
            "results?monthKey=2026-03",
            DeepLinkHandler.routeFor(DeepLinkDestination.Results("2026-03")),
        )
    }

    @Test
    fun `routeFor DomainDetail maps to domain_detail route`() {
        assertEquals(
            "domain_detail/career/2026-03",
            DeepLinkHandler.routeFor(DeepLinkDestination.DomainDetail("career", "2026-03")),
        )
    }

    @Test
    fun `routeFor Scan History Settings map to their routes`() {
        assertEquals("scan", DeepLinkHandler.routeFor(DeepLinkDestination.Scan))
        assertEquals("history", DeepLinkHandler.routeFor(DeepLinkDestination.History))
        assertEquals("settings", DeepLinkHandler.routeFor(DeepLinkDestination.Settings))
    }

    @Test
    fun `parse returns Settings for palmastro settings`() {
        assertIs<DeepLinkDestination.Settings>(DeepLinkHandler.parse(intentWithUri("palmastro://settings")))
    }

    @Test
    fun `parsed deep link round-trips through routeFor`() {
        val dest = DeepLinkHandler.parse(intentWithUri(DeepLinkHandler.buildDomainLink("health", "2026-07")))
        assertIs<DeepLinkDestination.DomainDetail>(dest)
        assertEquals("domain_detail/health/2026-07", DeepLinkHandler.routeFor(dest))
    }
}
