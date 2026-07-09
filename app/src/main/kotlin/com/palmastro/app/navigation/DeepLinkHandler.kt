package com.palmastro.app.navigation

import android.content.Intent
import android.net.Uri

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

    fun parse(intent: Intent?): DeepLinkDestination? {
        val uri = intent?.data ?: return null
        if (uri.scheme != "palmastro") return null
        return parseUri(uri)
    }

    private fun parseUri(uri: Uri): DeepLinkDestination? {
        return when (uri.host) {
            "results" -> {
                val monthKey = uri.getQueryParameter("monthKey")?.takeIf { isValidMonthKey(it) }
                DeepLinkDestination.Results(monthKey)
            }
            "domain" -> {
                val domain = uri.getQueryParameter("domain")?.takeIf { it in VALID_DOMAINS } ?: return null
                val monthKey = uri.getQueryParameter("monthKey")?.takeIf { isValidMonthKey(it) } ?: return null
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
}
