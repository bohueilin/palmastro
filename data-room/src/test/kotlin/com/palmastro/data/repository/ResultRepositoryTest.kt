package com.palmastro.data.repository

import com.palmastro.contracts.ComparabilityBucket
import com.palmastro.contracts.DeltaResult
import com.palmastro.contracts.DeltaValue
import com.palmastro.contracts.GradeShift
import com.palmastro.data.dao.DeltaDao
import com.palmastro.data.dao.MonthlyResultDao
import com.palmastro.data.entities.DeltaEntity
import com.palmastro.data.entities.MonthlyResultEntity
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ResultRepositoryTest {
    private val monthlyResultDao = mockk<MonthlyResultDao>(relaxed = true)
    private val deltaDao = mockk<DeltaDao>(relaxed = true)
    private lateinit var repo: ResultRepository

    @BeforeEach
    fun setUp() {
        clearAllMocks()
        repo = ResultRepository(monthlyResultDao, deltaDao)
    }

    private fun makeDelta(gradeShift: GradeShift? = GradeShift(from = "B", to = "A")) = DeltaResult(
        domainDeltas = mapOf(
            "career" to DeltaValue(5, "up"),
            "wealth" to DeltaValue(-3, "down"),
        ),
        subdimDeltas = mapOf("career_focus" to DeltaValue(2, "up")),
        gradeShift = gradeShift,
        comparabilityScore = 82,
        comparabilityBucket = ComparabilityBucket.HIGH,
        prevMonthKey = "2026-06",
        currentMonthKey = "2026-07",
    )

    @Test
    fun `saveDelta then getDeltaFor roundtrips the DeltaResult`() = runTest {
        val delta = makeDelta()
        val stored = slot<DeltaEntity>()
        coEvery { deltaDao.insert(capture(stored)) } just Runs

        repo.saveDelta("2026-07", delta)
        coEvery { deltaDao.getByMonth("2026-07") } answers { stored.captured }

        assertEquals(delta, repo.getDeltaFor("2026-07"))
    }

    @Test
    fun `saveDelta with null gradeShift roundtrips`() = runTest {
        val delta = makeDelta(gradeShift = null)
        val stored = slot<DeltaEntity>()
        coEvery { deltaDao.insert(capture(stored)) } just Runs

        repo.saveDelta("2026-07", delta)
        coEvery { deltaDao.getByMonth("2026-07") } answers { stored.captured }

        val loaded = repo.getDeltaFor("2026-07")
        assertEquals(delta, loaded)
        assertNull(loaded?.gradeShift)
    }

    @Test
    fun `saveDelta replaces any existing delta for the month`() = runTest {
        repo.saveDelta("2026-07", makeDelta())
        coVerifyOrder {
            deltaDao.deleteByMonth("2026-07")
            deltaDao.insert(any())
        }
    }

    @Test
    fun `saveDelta keys the stored entity on the given monthKey`() = runTest {
        repo.saveDelta("2026-07", makeDelta())
        coVerify {
            deltaDao.insert(
                match {
                    it.currentMonthKey == "2026-07" &&
                        it.prevMonthKey == "2026-06" &&
                        it.comparabilityScore == 82 &&
                        it.comparabilityBucket == "HIGH"
                }
            )
        }
    }

    @Test
    fun `getDeltaFor returns null when nothing is stored`() = runTest {
        coEvery { deltaDao.getByMonth("2026-07") } returns null
        assertNull(repo.getDeltaFor("2026-07"))
    }

    // --- Monthly result: rescan replaces the month ---

    private fun makeResult(id: String = "r-1", monthKey: String = "2026-07", createdAt: Long = 1_000L) =
        MonthlyResultEntity(
            id = id, monthKey = monthKey, scanSessionId = "s-$id", calcLevel = "L2",
            confidenceLevel = "high", confidenceReasonsJson = "[]",
            domainScoresJson = """{"career":72}""", subdimScoresJson = "{}",
            grade = "Stable", semanticPayloadsJson = "{}",
            palmFeatureSummaryJson = "{}", astroSignalsJson = "[]", explainabilityJson = "[]",
            rulesetVersion = "1.0.0", contentVersion = "1.0.0",
            scanQualityScore = 85, featureCoverage = 0.9f, createdAt = createdAt,
        )

    @Test
    fun `saveResult deletes existing rows for the month before inserting`() = runTest {
        val rescan = makeResult(id = "r-2", monthKey = "2026-07")

        repo.saveResult(rescan)

        coVerifyOrder {
            monthlyResultDao.deleteByMonth("2026-07")
            monthlyResultDao.insert(rescan)
        }
    }

    @Test
    fun `saveResult keys the delete on the entity monthKey`() = runTest {
        repo.saveResult(makeResult(monthKey = "2026-08"))
        coVerify(exactly = 1) { monthlyResultDao.deleteByMonth("2026-08") }
        coVerify(exactly = 0) { monthlyResultDao.deleteByMonth("2026-07") }
    }

    @Test
    fun `getByMonth returns the row the dao resolved for the month`() = runTest {
        val newest = makeResult(id = "r-2", createdAt = 2_000L)
        coEvery { monthlyResultDao.getByMonth("2026-07") } returns newest
        assertEquals(newest, repo.getByMonth("2026-07"))
    }

    /**
     * Room's @Query has BINARY retention (not visible via reflection), so the newest-row
     * ordering contract is locked in against the DAO source: getByMonth must order by
     * createdAt DESC and take a single row, so a duplicated month can never surface an
     * old scan. Skipped (not failed) if the source tree is not present in the test cwd.
     */
    @Test
    fun `getByMonth query orders by createdAt desc and limits to one row`() {
        val dao = File("src/main/kotlin/com/palmastro/data/dao/MonthlyResultDao.kt")
        assumeTrue(dao.exists(), "DAO source not found from test working dir; skipping SQL contract check")
        val source = dao.readText()
        val query = source.lineSequence().first { "WHERE monthKey = :monthKey" in it && "SELECT" in it }
        assertTrue("ORDER BY createdAt DESC" in query, "getByMonth must prefer the newest scan")
        assertTrue("LIMIT 1" in query, "getByMonth must return a single row")
    }
}
