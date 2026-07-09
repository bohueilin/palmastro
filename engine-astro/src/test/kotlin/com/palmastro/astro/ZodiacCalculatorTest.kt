package com.palmastro.astro

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate
import kotlin.test.assertEquals

class ZodiacCalculatorTest {

    private fun signName(date: LocalDate): String = ZodiacCalculator.sunSign(date).first
    private fun signInfo(date: LocalDate): ZodiacSign = ZodiacCalculator.sunSign(date).second

    @Nested
    inner class MidRangeDates {
        @Test fun `Apr 5 is Aries`() = assertEquals("ARIES", signName(LocalDate.of(2000, 4, 5)))
        @Test fun `May 10 is Taurus`() = assertEquals("TAURUS", signName(LocalDate.of(2000, 5, 10)))
        @Test fun `Jun 5 is Gemini`() = assertEquals("GEMINI", signName(LocalDate.of(2000, 6, 5)))
        @Test fun `Jul 10 is Cancer`() = assertEquals("CANCER", signName(LocalDate.of(2000, 7, 10)))
        @Test fun `Aug 5 is Leo`() = assertEquals("LEO", signName(LocalDate.of(2000, 8, 5)))
        @Test fun `Sep 5 is Virgo`() = assertEquals("VIRGO", signName(LocalDate.of(2000, 9, 5)))
        @Test fun `Oct 5 is Libra`() = assertEquals("LIBRA", signName(LocalDate.of(2000, 10, 5)))
        @Test fun `Nov 5 is Scorpio`() = assertEquals("SCORPIO", signName(LocalDate.of(2000, 11, 5)))
        @Test fun `Dec 5 is Sagittarius`() = assertEquals("SAGITTARIUS", signName(LocalDate.of(2000, 12, 5)))
        @Test fun `Jan 5 is Capricorn`() = assertEquals("CAPRICORN", signName(LocalDate.of(2000, 1, 5)))
        @Test fun `Feb 5 is Aquarius`() = assertEquals("AQUARIUS", signName(LocalDate.of(2000, 2, 5)))
        @Test fun `Mar 5 is Pisces`() = assertEquals("PISCES", signName(LocalDate.of(2000, 3, 5)))
    }

    @Nested
    inner class CuspBoundaries {
        @Test fun `Mar 20 is Pisces, Mar 21 is Aries`() { assertEquals("PISCES", signName(LocalDate.of(2000, 3, 20))); assertEquals("ARIES", signName(LocalDate.of(2000, 3, 21))) }
        @Test fun `Apr 19 is Aries, Apr 20 is Taurus`() { assertEquals("ARIES", signName(LocalDate.of(2000, 4, 19))); assertEquals("TAURUS", signName(LocalDate.of(2000, 4, 20))) }
        @Test fun `May 20 is Taurus, May 21 is Gemini`() { assertEquals("TAURUS", signName(LocalDate.of(2000, 5, 20))); assertEquals("GEMINI", signName(LocalDate.of(2000, 5, 21))) }
        @Test fun `Jun 20 is Gemini, Jun 21 is Cancer`() { assertEquals("GEMINI", signName(LocalDate.of(2000, 6, 20))); assertEquals("CANCER", signName(LocalDate.of(2000, 6, 21))) }
        @Test fun `Jul 22 is Cancer, Jul 23 is Leo`() { assertEquals("CANCER", signName(LocalDate.of(2000, 7, 22))); assertEquals("LEO", signName(LocalDate.of(2000, 7, 23))) }
        @Test fun `Aug 22 is Leo, Aug 23 is Virgo`() { assertEquals("LEO", signName(LocalDate.of(2000, 8, 22))); assertEquals("VIRGO", signName(LocalDate.of(2000, 8, 23))) }
        @Test fun `Sep 22 is Virgo, Sep 23 is Libra`() { assertEquals("VIRGO", signName(LocalDate.of(2000, 9, 22))); assertEquals("LIBRA", signName(LocalDate.of(2000, 9, 23))) }
        @Test fun `Oct 22 is Libra, Oct 23 is Scorpio`() { assertEquals("LIBRA", signName(LocalDate.of(2000, 10, 22))); assertEquals("SCORPIO", signName(LocalDate.of(2000, 10, 23))) }
        @Test fun `Nov 21 is Scorpio, Nov 22 is Sagittarius`() { assertEquals("SCORPIO", signName(LocalDate.of(2000, 11, 21))); assertEquals("SAGITTARIUS", signName(LocalDate.of(2000, 11, 22))) }
        @Test fun `Dec 21 is Sagittarius, Dec 22 is Capricorn`() { assertEquals("SAGITTARIUS", signName(LocalDate.of(2000, 12, 21))); assertEquals("CAPRICORN", signName(LocalDate.of(2000, 12, 22))) }
        @Test fun `Jan 19 is Capricorn, Jan 20 is Aquarius`() { assertEquals("CAPRICORN", signName(LocalDate.of(2000, 1, 19))); assertEquals("AQUARIUS", signName(LocalDate.of(2000, 1, 20))) }
        @Test fun `Feb 18 is Aquarius, Feb 19 is Pisces`() { assertEquals("AQUARIUS", signName(LocalDate.of(2000, 2, 18))); assertEquals("PISCES", signName(LocalDate.of(2000, 2, 19))) }
    }

    @Test fun `Dec 31 is Capricorn`() = assertEquals("CAPRICORN", signName(LocalDate.of(2000, 12, 31)))
    @Test fun `Jan 1 is Capricorn`() = assertEquals("CAPRICORN", signName(LocalDate.of(2001, 1, 1)))
    @Test fun `Feb 29 leap year is Pisces`() = assertEquals("PISCES", signName(LocalDate.of(2000, 2, 29)))

    @Nested
    inner class Elements {
        @Test fun `fire signs`() { assertEquals("fire", signInfo(LocalDate.of(2000, 4, 5)).element); assertEquals("fire", signInfo(LocalDate.of(2000, 8, 5)).element); assertEquals("fire", signInfo(LocalDate.of(2000, 12, 5)).element) }
        @Test fun `earth signs`() { assertEquals("earth", signInfo(LocalDate.of(2000, 5, 10)).element); assertEquals("earth", signInfo(LocalDate.of(2000, 9, 5)).element); assertEquals("earth", signInfo(LocalDate.of(2000, 1, 5)).element) }
        @Test fun `air signs`() { assertEquals("air", signInfo(LocalDate.of(2000, 6, 5)).element); assertEquals("air", signInfo(LocalDate.of(2000, 10, 5)).element); assertEquals("air", signInfo(LocalDate.of(2000, 2, 5)).element) }
        @Test fun `water signs`() { assertEquals("water", signInfo(LocalDate.of(2000, 7, 10)).element); assertEquals("water", signInfo(LocalDate.of(2000, 11, 5)).element); assertEquals("water", signInfo(LocalDate.of(2000, 3, 5)).element) }
    }

    @Nested
    inner class Modalities {
        @Test fun `cardinal signs`() { assertEquals("cardinal", signInfo(LocalDate.of(2000, 4, 5)).modality); assertEquals("cardinal", signInfo(LocalDate.of(2000, 7, 10)).modality); assertEquals("cardinal", signInfo(LocalDate.of(2000, 10, 5)).modality); assertEquals("cardinal", signInfo(LocalDate.of(2000, 1, 5)).modality) }
        @Test fun `fixed signs`() { assertEquals("fixed", signInfo(LocalDate.of(2000, 5, 10)).modality); assertEquals("fixed", signInfo(LocalDate.of(2000, 8, 5)).modality); assertEquals("fixed", signInfo(LocalDate.of(2000, 11, 5)).modality); assertEquals("fixed", signInfo(LocalDate.of(2000, 2, 5)).modality) }
        @Test fun `mutable signs`() { assertEquals("mutable", signInfo(LocalDate.of(2000, 6, 5)).modality); assertEquals("mutable", signInfo(LocalDate.of(2000, 9, 5)).modality); assertEquals("mutable", signInfo(LocalDate.of(2000, 12, 5)).modality); assertEquals("mutable", signInfo(LocalDate.of(2000, 3, 5)).modality) }
    }

    @Test
    fun `determinism - same date always returns same result`() {
        val date = LocalDate.of(1995, 6, 15)
        val results = (1..100).map { ZodiacCalculator.sunSign(date) }
        results.forEach { assertEquals(results.first(), it) }
    }
}
