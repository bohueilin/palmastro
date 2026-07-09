package com.palmastro.app.viewmodel

import android.content.Context
import com.palmastro.data.entities.UserProfileEntity
import com.palmastro.data.repository.UserRepository
import com.palmastro.data.repository.WipeManager
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.jupiter.api.*
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    private val context = mockk<Context>(relaxed = true)
    private val userRepository = mockk<UserRepository>(relaxed = true)
    private val wipeManager = mockk<WipeManager>(relaxed = true)
    private val testDispatcher = UnconfinedTestDispatcher()

    private fun makeProfile(tone: String = "scientific", reminders: String = "monthly") = UserProfileEntity(
        dominantHand = "right", birthdayEpochDay = 7384, tone = tone, reminders = reminders,
    )

    @BeforeEach fun setUp() { Dispatchers.setMain(testDispatcher); clearAllMocks() }
    @AfterEach fun tearDown() { Dispatchers.resetMain() }

    private fun createViewModel(): SettingsViewModel {
        coEvery { userRepository.get() } returns makeProfile()
        return SettingsViewModel(context, userRepository, wipeManager)
    }

    @Test
    fun `loads settings from profile`() = runTest {
        val vm = createViewModel()
        assertEquals("scientific", vm.state.value.tone)
        assertEquals("right", vm.state.value.dominantHand)
    }

    @Test
    fun `setTone updates state and saves`() = runTest {
        val vm = createViewModel()
        vm.setTone("healing")
        assertEquals("healing", vm.state.value.tone)
        coVerify { userRepository.save(match { it.tone == "healing" }) }
    }

    @Test
    fun `setReminders updates state and saves`() = runTest {
        val vm = createViewModel()
        vm.setReminders("off")
        assertEquals("off", vm.state.value.reminders)
        coVerify { userRepository.save(match { it.reminders == "off" }) }
    }

    @Test
    fun `setRetention true saves without deleting images`() = runTest {
        val vm = createViewModel()
        vm.setRetention(true)
        assertTrue(vm.state.value.rawMediaRetention)
        coVerify(exactly = 0) { wipeManager.deleteAllScanImages() }
    }

    @Test
    fun `setRetention false deletes images`() = runTest {
        val vm = createViewModel()
        vm.setRetention(false)
        assertFalse(vm.state.value.rawMediaRetention)
        coVerify(exactly = 1) { wipeManager.deleteAllScanImages() }
    }

    @Test
    fun `deleteAllData calls wipeManager and sets isWipeComplete`() = runTest {
        val vm = createViewModel()
        vm.deleteAllData()
        assertTrue(vm.state.value.isWipeComplete)
        coVerify(exactly = 1) { wipeManager.deleteAllData() }
    }

    @Test
    fun `deleteAllData sets isWiping during operation`() = runTest {
        coEvery { wipeManager.deleteAllData() } coAnswers {
            // During execution, isWiping should be true
        }
        val vm = createViewModel()
        vm.deleteAllData()
        assertFalse(vm.state.value.isWiping)
        assertTrue(vm.state.value.isWipeComplete)
    }

    @Test
    fun `handles missing profile gracefully`() = runTest {
        coEvery { userRepository.get() } returns null
        val vm = SettingsViewModel(context, userRepository, wipeManager)
        assertEquals("scientific", vm.state.value.tone)
    }
}
