package com.palmastro.app.ui.legal

import android.content.res.AssetManager
import android.webkit.WebView
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.palmastro.app.R

/**
 * Navigation contract for the offline legal viewer. NavGraph wiring is added by the
 * ui-core agent (see integration notes).
 */
const val LEGAL_ROUTE = "legal/{docType}"
fun legalRoute(docType: String) = "legal/$docType"
const val LEGAL_DOC_PRIVACY = "privacy"
const val LEGAL_DOC_TERMS = "terms"

private const val LEGAL_ASSET_DIR = "legal"

/**
 * Renders the bundled privacy policy / terms HTML from assets/legal/ in a WebView.
 * Picks the file matching the current app locale (zh-* -> zh-TW, otherwise en) and
 * falls back to the English file when the localized asset is missing. JavaScript stays
 * disabled — these are static local documents. On the rare devices with no WebView
 * provider (uninstalled/updating), the SAME asset is shown as plain text instead so
 * the legal text is always accessible.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LegalViewerScreen(docType: String, onBack: () -> Unit) {
    val titleRes = if (docType == LEGAL_DOC_TERMS) R.string.legal_terms_title else R.string.legal_privacy_title
    val configuration = LocalConfiguration.current
    val languageSuffix = run {
        val tag = configuration.locales.get(0)?.toLanguageTag().orEmpty()
        if (tag.startsWith("zh")) "zh-TW" else "en"
    }
    val baseName = if (docType == LEGAL_DOC_TERMS) "terms" else "privacy_policy"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(titleRes), fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                },
            )
        }
    ) { padding ->
        LegalDocumentBody(padding = padding, baseName = baseName, languageSuffix = languageSuffix)
    }
}

@Composable
private fun LegalDocumentBody(padding: PaddingValues, baseName: String, languageSuffix: String) {
    val context = LocalContext.current
    val loadErrorHtml = stringResource(R.string.legal_load_error)
    // WebView construction CRASHES when the device has no WebView provider (rare but
    // store-visible). Guard creation; null means "fall back to plain text".
    val webView = remember(baseName, languageSuffix) {
        runCatching {
            WebView(context).apply {
                settings.javaScriptEnabled = false
                // Required on API 30+ for file:///android_asset/ URLs.
                settings.allowFileAccess = true
                val fileName = resolveLegalAsset(context.assets, baseName, languageSuffix)
                if (fileName != null) {
                    loadUrl("file:///android_asset/$LEGAL_ASSET_DIR/$fileName")
                } else {
                    loadData(loadErrorHtml, "text/html; charset=utf-8", "utf-8")
                }
            }
        }.getOrNull()
    }

    if (webView != null) {
        AndroidView(modifier = Modifier.fillMaxSize().padding(padding), factory = { webView })
    } else {
        LegalPlainTextFallback(padding = padding, baseName = baseName, languageSuffix = languageSuffix)
    }
}

/** No-WebView fallback: the SAME asset HTML, tags stripped, in a plain scrollable column. */
@Composable
private fun LegalPlainTextFallback(padding: PaddingValues, baseName: String, languageSuffix: String) {
    val context = LocalContext.current
    val body = remember(baseName, languageSuffix) {
        runCatching {
            resolveLegalAsset(context.assets, baseName, languageSuffix)?.let { fileName ->
                context.assets.open("$LEGAL_ASSET_DIR/$fileName").bufferedReader().use { it.readText() }
            }
        }.getOrNull()?.let(::stripHtmlTags)
    }
    Column(
        modifier = Modifier.fillMaxSize().padding(padding)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
    ) {
        Text(
            stringResource(R.string.legal_fallback_caption),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))
        Text(
            body ?: stringResource(R.string.legal_load_error),
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(32.dp))
    }
}

/** Localized asset name for [baseName], falling back to English; null when neither exists. */
private fun resolveLegalAsset(assets: AssetManager, baseName: String, languageSuffix: String): String? {
    val available = runCatching { assets.list(LEGAL_ASSET_DIR)?.toSet() }.getOrNull().orEmpty()
    val localized = "${baseName}_$languageSuffix.html"
    val fallback = "${baseName}_en.html"
    return when {
        localized in available -> localized
        fallback in available -> fallback
        else -> null
    }
}

private val BLOCK_STRIP_REGEX = Regex("(?is)<(script|style|head)[^>]*>.*?</\\1>")
private val LINE_BREAK_TAG_REGEX = Regex("(?i)<br\\s*/?>")
private val BLOCK_END_TAG_REGEX = Regex("(?i)</(p|div|h[1-6]|li|tr)>")
private val ANY_TAG_REGEX = Regex("<[^>]+>")
private val HORIZONTAL_SPACE_REGEX = Regex("[ \\t\\u00A0]+")
private val SPACE_AROUND_NEWLINE_REGEX = Regex(" *\\n *")
private val EXCESS_NEWLINES_REGEX = Regex("\\n{3,}")

/**
 * Minimal tag strip for the no-WebView fallback: legal TEXT must survive; formatting
 * fidelity is explicitly out of scope (simple regex strip by design).
 */
internal fun stripHtmlTags(html: String): String = html
    .replace(BLOCK_STRIP_REGEX, " ")
    .replace(LINE_BREAK_TAG_REGEX, "\n")
    .replace(BLOCK_END_TAG_REGEX, "\n\n")
    .replace(ANY_TAG_REGEX, " ")
    .replace("&nbsp;", " ")
    .replace("&amp;", "&")
    .replace("&lt;", "<")
    .replace("&gt;", ">")
    .replace("&quot;", "\"")
    .replace("&#39;", "'")
    .replace(HORIZONTAL_SPACE_REGEX, " ")
    .replace(SPACE_AROUND_NEWLINE_REGEX, "\n")
    .replace(EXCESS_NEWLINES_REGEX, "\n\n")
    .trim()
