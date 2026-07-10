package com.palmastro.app.navigation

import android.content.Intent

sealed class DeepLinkDestination {
    data object Home : DeepLinkDestination()
    data class Results(val monthKey: String?) : DeepLinkDestination()
    data class DomainDetail(val domain: String, val monthKey: String) : DeepLinkDestination()
    data object Scan : DeepLinkDestination()
    data object History : DeepLinkDestination()
    data object Settings : DeepLinkDestination()
}

object DeepLinkHandler {
    private val VALID_DOMAINS = setOf("career", "wealth", "family", "health")
    private val MONTH_KEY_PATTERN = Regex("""\d{4}-\d{2}""")

    fun parse(intent: Intent?): DeepLinkDestination? = parse(intent?.dataString)

    /** Pure-string parser so deep-link logic stays JVM unit-testable (no android.net.Uri). */
    fun parse(raw: String?): DeepLinkDestination? {
        if (raw == null || !raw.startsWith("palmastro://")) return null
        val rest = raw.removePrefix("palmastro://")
        val host = rest.substringBefore('?').substringBefore('/')
        val params = rest.substringAfter('?', "")
            .split('&')
            .filter { it.contains('=') }
            .associate { it.substringBefore('=') to it.substringAfter('=') }
        return when (host) {
            "results" -> DeepLinkDestination.Results(params["monthKey"]?.takeIf { isValidMonthKey(it) })
            "domain" -> {
                val domain = params["domain"]?.takeIf { it in VALID_DOMAINS } ?: return null
                val monthKey = params["monthKey"]?.takeIf { isValidMonthKey(it) } ?: return null
                DeepLinkDestination.DomainDetail(domain, monthKey)
            }
            "scan" -> DeepLinkDestination.Scan
            "history" -> DeepLinkDestination.History
            "settings" -> DeepLinkDestination.Settings
            else -> null
        }
    }

    private fun isValidMonthKey(key: String): Boolean {
        if (!MONTH_KEY_PATTERN.matches(key)) return false
        val parts = key.split("-")
        val year = parts[0].toIntOrNull() ?: return false
        val month = parts[1].toIntOrNull() ?: return false
        return year in 2020..2100 && month in 1..12
    }

    fun buildResultsLink(monthKey: String): String = "palmastro://results?monthKey=$monthKey"

    fun buildDomainLink(domain: String, monthKey: String): String =
        "palmastro://domain?domain=$domain&monthKey=$monthKey"

    /** Maps a parsed deep-link destination to its navigation route. */
    fun routeFor(destination: DeepLinkDestination): String = when (destination) {
        is DeepLinkDestination.Home -> "results"
        is DeepLinkDestination.Results ->
            destination.monthKey?.let { "results?monthKey=$it" } ?: "results"
        is DeepLinkDestination.DomainDetail ->
            "domain_detail/${destination.domain}/${destination.monthKey}"
        is DeepLinkDestination.Scan -> "scan"
        is DeepLinkDestination.History -> "history"
        is DeepLinkDestination.Settings -> "settings"
    }
}
