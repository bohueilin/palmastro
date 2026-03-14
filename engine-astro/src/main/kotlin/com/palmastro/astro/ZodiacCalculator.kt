package com.palmastro.astro

import java.time.LocalDate
import java.time.MonthDay

data class ZodiacSign(val name: String, val element: String, val modality: String)

object ZodiacCalculator {
    private val signs = listOf(
        Triple(MonthDay.of(3, 21) to MonthDay.of(4, 19), "ARIES", ZodiacSign("Aries", "fire", "cardinal")),
        Triple(MonthDay.of(4, 20) to MonthDay.of(5, 20), "TAURUS", ZodiacSign("Taurus", "earth", "fixed")),
        Triple(MonthDay.of(5, 21) to MonthDay.of(6, 20), "GEMINI", ZodiacSign("Gemini", "air", "mutable")),
        Triple(MonthDay.of(6, 21) to MonthDay.of(7, 22), "CANCER", ZodiacSign("Cancer", "water", "cardinal")),
        Triple(MonthDay.of(7, 23) to MonthDay.of(8, 22), "LEO", ZodiacSign("Leo", "fire", "fixed")),
        Triple(MonthDay.of(8, 23) to MonthDay.of(9, 22), "VIRGO", ZodiacSign("Virgo", "earth", "mutable")),
        Triple(MonthDay.of(9, 23) to MonthDay.of(10, 22), "LIBRA", ZodiacSign("Libra", "air", "cardinal")),
        Triple(MonthDay.of(10, 23) to MonthDay.of(11, 21), "SCORPIO", ZodiacSign("Scorpio", "water", "fixed")),
        Triple(MonthDay.of(11, 22) to MonthDay.of(12, 21), "SAGITTARIUS", ZodiacSign("Sagittarius", "fire", "mutable")),
        Triple(MonthDay.of(12, 22) to MonthDay.of(1, 19), "CAPRICORN", ZodiacSign("Capricorn", "earth", "cardinal")),
        Triple(MonthDay.of(1, 20) to MonthDay.of(2, 18), "AQUARIUS", ZodiacSign("Aquarius", "air", "fixed")),
        Triple(MonthDay.of(2, 19) to MonthDay.of(3, 20), "PISCES", ZodiacSign("Pisces", "water", "mutable")),
    )

    fun sunSign(birthday: LocalDate): Pair<String, ZodiacSign> {
        val md = MonthDay.from(birthday)
        for ((range, key, sign) in signs) {
            val (start, end) = range
            if (start <= end) {
                if (md >= start && md <= end) return key to sign
            } else {
                if (md >= start || md <= end) return key to sign
            }
        }
        return "CAPRICORN" to ZodiacSign("Capricorn", "earth", "cardinal")
    }
}
