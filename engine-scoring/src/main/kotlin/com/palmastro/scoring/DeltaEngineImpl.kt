package com.palmastro.scoring

import com.palmastro.contracts.*
import com.palmastro.contracts.interfaces.DeltaEngine

class DeltaEngineImpl : DeltaEngine {

    override fun computeDelta(prev: MonthlyResult, current: MonthlyResult): DeltaResult {
        val domainDeltas = mutableMapOf<String, DeltaValue>()
        for (domain in listOf("career", "wealth", "family", "health")) {
            val prevScore = prev.scoringResult.domainScores[domain] ?: 50
            val currScore = current.scoringResult.domainScores[domain] ?: 50
            val diff = currScore - prevScore
            val arrow = when {
                diff > 0 -> "up"
                diff < 0 -> "down"
                else -> "flat"
            }
            domainDeltas[domain] = DeltaValue(diff, arrow)
        }

        val gradeShift = if (prev.scoringResult.grade != current.scoringResult.grade) {
            GradeShift(prev.scoringResult.grade, current.scoringResult.grade)
        } else null

        val comparabilityScore = computeComparability(prev, current)
        val bucket = when {
            comparabilityScore >= 70 -> ComparabilityBucket.HIGH
            comparabilityScore >= 50 -> ComparabilityBucket.MED
            else -> ComparabilityBucket.LOW
        }

        return DeltaResult(
            domainDeltas = domainDeltas,
            subdimDeltas = emptyMap(),
            gradeShift = gradeShift,
            comparabilityScore = comparabilityScore,
            comparabilityBucket = bucket,
            prevMonthKey = prev.monthKey,
            currentMonthKey = current.monthKey
        )
    }

    private fun computeComparability(prev: MonthlyResult, current: MonthlyResult): Int {
        val qualityDiff = Math.abs(current.scanQualityScore - prev.scanQualityScore)
        val qualityFactor = (maxOf(0.0, 1.0 - qualityDiff / 50.0) * 100).toInt()

        val coverageDiff = Math.abs(current.featureCoverage - prev.featureCoverage)
        val coverageFactor = (maxOf(0.0, 1.0 - coverageDiff / 0.3) * 100).toInt()

        val handMatchFactor = 100
        val calcLevelFactor = 100
        val timeGapFactor = 100

        return ((qualityFactor * 0.30 + coverageFactor * 0.25 +
                handMatchFactor * 0.20 + calcLevelFactor * 0.10 +
                timeGapFactor * 0.15)).toInt().coerceIn(0, 100)
    }
}
