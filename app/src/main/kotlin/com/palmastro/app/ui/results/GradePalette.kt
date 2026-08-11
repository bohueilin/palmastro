package com.palmastro.app.ui.results

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.palmastro.app.R
import com.palmastro.app.ui.domainNameLocalized
import com.palmastro.app.ui.gradeNameLocalized
import com.palmastro.app.ui.theme.LocalPalmAstroExtendedColors

/**
 * Theme-aware grade colors shared by Results / Detail / History / Explainability:
 * a thin accessor over [LocalPalmAstroExtendedColors], the single brand token set.
 * Calm by design (PRD 12.3): no aggressive red outside true error states — a
 * "Watchout" month is a coaching signal, not an alarm. Color is never the only
 * indicator — grade names / signed numbers are always rendered next to it.
 */
@Composable
fun gradeColor(grade: String): Color {
    val extended = LocalPalmAstroExtendedColors.current
    return when (grade) {
        "Growing" -> extended.gradeGrowing
        "Stable" -> extended.gradeStable
        "Building" -> extended.gradeBuilding
        "Watchout" -> extended.gradeWatchOut
        else -> MaterialTheme.colorScheme.primary
    }
}

/**
 * Text tone paired with [gradeColor] for SOLID grade chips (each pair keeps
 * >= 4.5:1 by construction — see Theme.kt). Tinted 15%-alpha chips cannot carry
 * the raw grade color as small text; use a solid chip with this pair instead.
 */
@Composable
fun onGradeColor(grade: String): Color {
    val extended = LocalPalmAstroExtendedColors.current
    return when (grade) {
        "Growing" -> extended.onGradeGrowing
        "Stable" -> extended.onGradeStable
        "Building" -> extended.onGradeBuilding
        "Watchout" -> extended.onGradeWatchOut
        else -> MaterialTheme.colorScheme.onPrimary
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
