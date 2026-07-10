package com.palmastro.data.repository

import com.palmastro.contracts.ComparabilityBucket
import com.palmastro.contracts.DeltaResult
import com.palmastro.contracts.DeltaValue
import com.palmastro.contracts.GradeShift
import com.palmastro.data.dao.DeltaDao
import com.palmastro.data.dao.MonthlyResultDao
import com.palmastro.data.entities.DeltaEntity
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

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
}
