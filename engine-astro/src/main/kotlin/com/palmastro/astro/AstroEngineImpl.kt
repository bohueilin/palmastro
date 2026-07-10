package com.palmastro.astro

import com.palmastro.contracts.*
import com.palmastro.contracts.interfaces.AstroEngine
import java.time.LocalDate
import java.time.LocalTime

/**
 * Tropical-zodiac astro engine (PRD section 17).
 *
 * L1 (birthday only): sun sign plus its element and modality. No moon,
 * ascendant, houses, or house-based signals are emitted at L1.
 *
 * L2 (birthday + time + place) additionally emits:
 * - ASTRO_MOON_<ELEMENT> from the Meeus low-precision lunar longitude
 *   (~0.3 degree accuracy, sign-accurate away from boundaries).
 * - ASTRO_ASC_<ELEMENT> from the standard ascendant formula
 *   (GMST -> LST -> atan2 with obliquity and latitude).
 *
 * Fabricated planetary-strength and house-emphasis signals from v1 were
 * removed entirely; every emitted signal is now derived from real math.
 */
class AstroEngineImpl(
    private val version: String = "2.0.0"
) : AstroEngine {

    override fun compute(
        birthday: LocalDate,
        birthTime: LocalTime?,
        birthPlaceLat: Double?,
        birthPlaceLon: Double?
    ): AstroResult {
        val calcLevel = if (birthTime != null && birthPlaceLat != null && birthPlaceLon != null)
            CalcLevel.L2 else CalcLevel.L1

        val signals = mutableListOf<AstroSignal>()

        // L1: sun sign (informational, kept first for display) + element + modality.
        val (sunKey, sunSign) = ZodiacCalculator.sunSign(birthday)
        signals.add(AstroSignal("ASTRO_SUN_$sunKey", "+", 3, "high", "SAFE_GENERAL"))
        signals.add(sunElementSignal(sunSign.element))
        signals.add(AstroSignal("ASTRO_SUN_${sunSign.modality.uppercase()}", "+", 2, "high", "SAFE_GENERAL"))

        if (calcLevel == CalcLevel.L2) {
            val jdUt = AstroMath.julianDayUt(birthday, birthTime!!, birthPlaceLon!!)

            val moonLongitude = AstroMath.moonEclipticLongitudeDeg(jdUt)
            val (_, moonSign) = ZodiacCalculator.signAtLongitude(moonLongitude)
            signals.add(bodyElementSignal("MOON", moonSign.element))

            val ascendantLongitude = AstroMath.ascendantDegrees(jdUt, birthPlaceLat!!, birthPlaceLon)
            val (_, ascSign) = ZodiacCalculator.signAtLongitude(ascendantLongitude)
            signals.add(bodyElementSignal("ASC", ascSign.element))
        }

        return AstroResult(calcLevel, signals.toList(), version)
    }

    private fun sunElementSignal(element: String): AstroSignal =
        AstroSignal("ASTRO_SUN_${element.uppercase()}", "+", 2, "high", elementSafetyTag("SUN", element))

    private fun bodyElementSignal(body: String, element: String): AstroSignal =
        AstroSignal("ASTRO_${body}_${element.uppercase()}", "+", 2, "high", elementSafetyTag(body, element))

    /**
     * Safety tags mirror the ruleset v2 definitions (PRD Appendix A2):
     * water elements touch the health domain (soft-only); a fire ascendant
     * maps to career; everything else is general.
     */
    private fun elementSafetyTag(body: String, element: String): String = when {
        element == "water" -> "SAFE_HEALTH_SOFT_ONLY"
        body == "ASC" && element == "fire" -> "SAFE_CAREER"
        else -> "SAFE_GENERAL"
    }
}
