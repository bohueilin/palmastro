package com.palmastro.astro

import com.palmastro.contracts.*
import com.palmastro.contracts.interfaces.AstroEngine
import java.time.LocalDate
import java.time.LocalTime

class AstroEngineImpl(
    private val version: String = "1.0.0"
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

        val (sunKey, sunSign) = ZodiacCalculator.sunSign(birthday)
        signals.add(AstroSignal("ASTRO_SUN_$sunKey", "+", 3, "high", "SAFE_GENERAL"))
        signals.add(AstroSignal("ASTRO_ELEMENT_${sunSign.element.uppercase()}", "+", 2, "high", "SAFE_GENERAL"))
        signals.add(AstroSignal("ASTRO_MODALITY_${sunSign.modality.uppercase()}", "+", 2, "high", "SAFE_GENERAL"))

        val moonKey = approximateMoonSign(birthday)
        signals.add(AstroSignal("ASTRO_MOON_$moonKey", "+", 2, if (calcLevel == CalcLevel.L2) "high" else "med", "SAFE_GENERAL"))

        addPlanetarySignals(birthday, signals)

        if (calcLevel == CalcLevel.L2) {
            val ascendant = computeAscendant(birthday, birthTime!!, birthPlaceLat!!)
            signals.add(AstroSignal("ASTRO_ASCENDANT_$ascendant", "+", 3, "high", "SAFE_GENERAL"))
            val houseEmphasis = computeHouseEmphasis(birthTime)
            signals.add(AstroSignal("ASTRO_HOUSE_${houseEmphasis}_EMPHASIS", "+", 2, "high", "SAFE_CAREER"))
        }

        return AstroResult(calcLevel, signals.toList(), version)
    }

    private fun approximateMoonSign(birthday: LocalDate): String {
        val signs = listOf("ARIES", "TAURUS", "GEMINI", "CANCER", "LEO", "VIRGO",
            "LIBRA", "SCORPIO", "SAGITTARIUS", "CAPRICORN", "AQUARIUS", "PISCES")
        val index = (birthday.dayOfYear * 13 / 366) % 12
        return signs[index]
    }

    private fun addPlanetarySignals(birthday: LocalDate, signals: MutableList<AstroSignal>) {
        val year = birthday.year
        if (year % 3 == 0) signals.add(AstroSignal("ASTRO_SATURN_STRONG", "+", 3, "med", "SAFE_CAREER"))
        if (year % 5 == 0) signals.add(AstroSignal("ASTRO_JUPITER_STRONG", "+", 3, "med", "SAFE_WEALTH_SOFT_ONLY"))
        if (year % 7 == 0) signals.add(AstroSignal("ASTRO_MARS_STRONG", "+", 2, "med", "SAFE_HEALTH_SOFT_ONLY"))
    }

    private fun computeAscendant(birthday: LocalDate, birthTime: LocalTime, lat: Double): String {
        val signs = listOf("ARIES", "TAURUS", "GEMINI", "CANCER", "LEO", "VIRGO",
            "LIBRA", "SCORPIO", "SAGITTARIUS", "CAPRICORN", "AQUARIUS", "PISCES")
        val index = (birthTime.hour + birthday.monthValue) % 12
        return signs[index]
    }

    private fun computeHouseEmphasis(birthTime: LocalTime): Int {
        return (birthTime.hour % 12) + 1
    }
}
