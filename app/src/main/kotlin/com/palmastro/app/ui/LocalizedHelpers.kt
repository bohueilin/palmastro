package com.palmastro.app.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import com.palmastro.app.R
import com.palmastro.contracts.Angle
import java.time.YearMonth
import java.time.format.DateTimeFormatter

/**
 * Storage month key ("2026-08") rendered in the app locale. The raw key is a database
 * detail and must never reach a label; a malformed key falls back to itself so a bad
 * row still renders something readable.
 */
@Composable
fun monthTitleLocalized(monthKey: String): String {
    val datePattern = stringResource(R.string.results_date_pattern)
    val configuration = LocalConfiguration.current
    return remember(monthKey, datePattern, configuration) {
        runCatching {
            val locale = configuration.locales.get(0)
            YearMonth.parse(monthKey).atDay(1).format(DateTimeFormatter.ofPattern(datePattern, locale))
        }.getOrDefault(monthKey)
    }
}

@Composable
fun gradeNameLocalized(grade: String): String = when (grade) {
    "Growing" -> stringResource(R.string.grade_growing)
    "Stable" -> stringResource(R.string.grade_stable)
    "Building" -> stringResource(R.string.grade_building)
    "Watchout" -> stringResource(R.string.grade_watchout)
    else -> grade
}

@Composable
fun domainNameLocalized(domain: String): String = when (domain) {
    "career" -> stringResource(R.string.domain_career)
    "wealth" -> stringResource(R.string.domain_wealth)
    "family" -> stringResource(R.string.domain_family)
    "health" -> stringResource(R.string.domain_health)
    else -> domain
}

@Composable
fun angleNameLocalized(angle: Angle): String = when (angle) {
    Angle.FRONT -> stringResource(R.string.scan_angle_front)
    Angle.LEFT_TILT -> stringResource(R.string.scan_angle_left_tilt)
    Angle.RIGHT_TILT -> stringResource(R.string.scan_angle_right_tilt)
    Angle.NEAR -> stringResource(R.string.scan_angle_near)
    Angle.FAR -> stringResource(R.string.scan_angle_far)
    Angle.UP_TILT -> stringResource(R.string.scan_angle_up_tilt)
    Angle.DOWN_TILT -> stringResource(R.string.scan_angle_down_tilt)
}

@Composable
fun angleInstructionLocalized(angle: Angle): String = when (angle) {
    Angle.FRONT -> stringResource(R.string.scan_instruction_front)
    Angle.LEFT_TILT -> stringResource(R.string.scan_instruction_left_tilt)
    Angle.RIGHT_TILT -> stringResource(R.string.scan_instruction_right_tilt)
    Angle.NEAR -> stringResource(R.string.scan_instruction_near)
    Angle.FAR -> stringResource(R.string.scan_instruction_far)
    Angle.UP_TILT -> stringResource(R.string.scan_instruction_up_tilt)
    Angle.DOWN_TILT -> stringResource(R.string.scan_instruction_down_tilt)
}
