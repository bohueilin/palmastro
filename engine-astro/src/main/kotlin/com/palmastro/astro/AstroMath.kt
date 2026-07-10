package com.palmastro.astro

import java.time.LocalDate
import java.time.LocalTime
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tan

/**
 * Deterministic astronomical math used by [AstroEngineImpl] (L2 calculations).
 *
 * Formulas follow Meeus, "Astronomical Algorithms":
 * - Julian Day from a Gregorian calendar date (ch. 7).
 * - Greenwich mean sidereal time polynomial (12.4).
 * - Low-precision lunar ecliptic longitude: mean longitude plus the six
 *   largest periodic terms of the ELP truncated series (ch. 47, reduced).
 *   Documented accuracy of this truncation is about 0.3 degrees, which is
 *   sign-accurate except within ~0.3 degrees of a sign boundary.
 * - Ascendant from local sidereal time, obliquity and geographic latitude.
 *
 * Assumption (editable): birth time is treated as local mean time; the UTC
 * offset is derived purely from geography as lon / 15 hours (east positive).
 * No timezone database or DST history is consulted.
 */
object AstroMath {

    /** Mean obliquity of the ecliptic, degrees (frozen launch constant). */
    const val OBLIQUITY_DEG = 23.4367

    private const val J2000 = 2451545.0

    /**
     * Julian Day in UT for a birth date/time at geographic longitude [lonDeg]
     * (degrees, east positive), applying the local-mean-time assumption above.
     */
    fun julianDayUt(date: LocalDate, time: LocalTime, lonDeg: Double): Double {
        val a = (14 - date.monthValue) / 12
        val y = date.year + 4800 - a
        val m = date.monthValue + 12 * a - 3
        val jdn = date.dayOfMonth +
            (153 * m + 2) / 5 +
            365L * y + y / 4 - y / 100 + y / 400 -
            32045L
        val dayFraction = (time.hour - 12).toDouble() / 24.0 +
            time.minute / 1440.0 +
            time.second / 86400.0
        val jdLocalMeanTime = jdn + dayFraction
        return jdLocalMeanTime - lonDeg / 15.0 / 24.0
    }

    /**
     * Geocentric ecliptic longitude of the Moon in degrees [0, 360),
     * Meeus low-precision truncated series (~0.3 degree accuracy).
     */
    fun moonEclipticLongitudeDeg(jdUt: Double): Double {
        val t = (jdUt - J2000) / 36525.0
        val meanLongitude = normalizeDegrees(218.3164477 + 481267.88123421 * t)
        val meanElongation = normalizeDegrees(297.8501921 + 445267.1114034 * t)
        val sunMeanAnomaly = normalizeDegrees(357.5291092 + 35999.0502909 * t)
        val moonMeanAnomaly = normalizeDegrees(134.9633964 + 477198.8675055 * t)
        val argumentOfLatitude = normalizeDegrees(93.2720950 + 483202.0175233 * t)

        val d = Math.toRadians(meanElongation)
        val m = Math.toRadians(sunMeanAnomaly)
        val mp = Math.toRadians(moonMeanAnomaly)
        val f = Math.toRadians(argumentOfLatitude)

        val longitude = meanLongitude +
            6.288774 * sin(mp) +
            1.274027 * sin(2 * d - mp) +
            0.658314 * sin(2 * d) +
            0.213618 * sin(2 * mp) -
            0.185116 * sin(m) -
            0.114332 * sin(2 * f)
        return normalizeDegrees(longitude)
    }

    /** Greenwich mean sidereal time in degrees [0, 360), Meeus 12.4. */
    fun gmstDegrees(jdUt: Double): Double {
        val t = (jdUt - J2000) / 36525.0
        val gmst = 280.46061837 +
            360.98564736629 * (jdUt - J2000) +
            0.000387933 * t * t -
            t * t * t / 38710000.0
        return normalizeDegrees(gmst)
    }

    /** Local sidereal time in degrees [0, 360) for longitude [lonDeg] (east positive). */
    fun localSiderealDegrees(jdUt: Double, lonDeg: Double): Double =
        normalizeDegrees(gmstDegrees(jdUt) + lonDeg)

    /**
     * Ecliptic longitude of the ascendant in degrees [0, 360).
     *
     * asc = atan2(cos LST, -(sin LST * cos eps + tan lat * sin eps))
     *
     * atan2 provides correct quadrant handling; the result is normalized to
     * [0, 360). Latitude is clamped to +/-89.9 degrees to keep tan finite.
     */
    fun ascendantDegrees(jdUt: Double, latDeg: Double, lonDeg: Double): Double {
        val lst = Math.toRadians(localSiderealDegrees(jdUt, lonDeg))
        val eps = Math.toRadians(OBLIQUITY_DEG)
        val lat = Math.toRadians(latDeg.coerceIn(-89.9, 89.9))
        val y = cos(lst)
        val x = -(sin(lst) * cos(eps) + tan(lat) * sin(eps))
        return normalizeDegrees(Math.toDegrees(atan2(y, x)))
    }

    /** Normalizes an angle in degrees to [0, 360). */
    fun normalizeDegrees(degrees: Double): Double {
        val normalized = degrees % 360.0
        return if (normalized < 0) normalized + 360.0 else normalized
    }
}
