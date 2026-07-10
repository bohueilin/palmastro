package com.palmastro.app.ui.results

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.palmastro.app.R
import com.palmastro.app.ui.domainNameLocalized
import com.palmastro.app.ui.gradeNameLocalized

/**
 * Theme-aware grade colors shared by Results / Detail / History / Explainability.
 *
 * TODO(integration): swap for `PalmAstroExtendedColors` from the ui-core theme once it
 * lands. These fallbacks are chosen for >= 4.5:1 contrast on the default light and dark
 * surfaces. Color is never the only indicator — grade names / signed numbers are always
 * rendered next to it.
 */
@Composable
fun gradeColor(grade: String): Color {
    val dark = isSystemInDarkTheme()
    return when (grade) {
        "Growing" -> if (dark) Color(0xFF81C784) else Color(0xFF2E7D32)
        "Stable" -> if (dark) Color(0xFF64B5F6) else Color(0xFF1565C0)
        "Building" -> if (dark) Color(0xFFFFB74D) else Color(0xFF9A4E00)
        "Watchout" -> if (dark) Color(0xFFEF9A9A) else Color(0xFFC62828)
        else -> MaterialTheme.colorScheme.primary
    }
}

/** Localized grade name; delegates to the shared helper so there is one source of truth. */
@Composable
fun gradeDisplayName(grade: String): String = gradeNameLocalized(grade)

/** Localized domain name; delegates to the shared helper so there is one source of truth. */
@Composable
fun domainDisplayName(domain: String): String = domainNameLocalized(domain)

@Composable
fun confidenceDisplayName(confidence: String): String = when (confidence.lowercase()) {
    "high" -> stringResource(R.string.results_confidence_high)
    "med", "medium" -> stringResource(R.string.results_confidence_med)
    "low" -> stringResource(R.string.results_confidence_low)
    else -> confidence
}
