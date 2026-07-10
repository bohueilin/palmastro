package com.palmastro.app.ui.legal

import android.webkit.WebView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
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
 * disabled — these are static local documents.
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
    val loadErrorHtml = stringResource(R.string.legal_load_error)

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
        AndroidView(
            modifier = Modifier.fillMaxSize().padding(padding),
            factory = { ctx ->
                WebView(ctx).apply {
                    settings.javaScriptEnabled = false
                    // Required on API 30+ for file:///android_asset/ URLs.
                    settings.allowFileAccess = true
                    val available = runCatching { ctx.assets.list(LEGAL_ASSET_DIR)?.toSet() }.getOrNull().orEmpty()
                    val localized = "${baseName}_$languageSuffix.html"
                    val fallback = "${baseName}_en.html"
                    val fileName = when {
                        localized in available -> localized
                        fallback in available -> fallback
                        else -> null
                    }
                    if (fileName != null) {
                        loadUrl("file:///android_asset/$LEGAL_ASSET_DIR/$fileName")
                    } else {
                        loadData(loadErrorHtml, "text/html; charset=utf-8", "utf-8")
                    }
                }
            },
        )
    }
}
