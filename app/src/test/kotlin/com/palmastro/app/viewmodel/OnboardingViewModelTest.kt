package com.palmastro.app.viewmodel

import com.palmastro.data.entities.UserProfileEntity
import com.palmastro.data.repository.UserRepository
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.jupiter.api.*
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingViewModelTest {
    private val userRepository = mockk<UserRepository>(relaxed = true)
    private val testDispatcher = UnconfinedTestDispatcher()

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        clearAllMocks()
    }

    @AfterEach
    fun tearDown() { Dispatchers.resetMain() }

    private fun createViewModel() = OnboardingViewModel(userRepository)

    @Test
    fun `initial state starts at step 0 with right hand`() {
        val vm = createViewModel()
        assertEquals(0, vm.state.value.step)
        assertEquals("right", vm.state.value.dominantHand)
        assertFalse(vm.state.value.isComplete)
    }

    @Test
    fun `setHand updates dominant hand`() {
        val vm = createViewModel()
        vm.setHand("left")
        assertEquals("left", vm.state.value.dominantHand)
    }

    @Test
    fun `setBirthday stores date`() {
        val vm = createViewModel()
        val date = LocalDate.of(1990, 6, 15)
        vm.setBirthday(date)
        assertEquals(date, vm.state.value.birthday)
    }

    @Test
    fun `nextStep increments step`() {
        val vm = createViewModel()
        vm.nextStep()
        assertEquals(1, vm.state.value.step)
        vm.nextStep()
        assertEquals(2, vm.state.value.step)
    }

    @Test
    fun `prevStep does not go below 0`() {
        val vm = createViewModel()
        vm.prevStep()
        assertEquals(0, vm.state.value.step)
    }

    @Test
    fun `setBirthTime sets hasBirthTime flag`() {
        val vm = createViewModel()
        vm.setBirthTime(14, 30)
        assertTrue(vm.state.value.hasBirthTime)
        assertEquals(14, vm.state.value.birthTimeHour)
        assertEquals(30, vm.state.value.birthTimeMinute)
    }

    @Test
    fun `setBirthPlace sets location`() {
        val vm = createViewModel()
        vm.setBirthPlace("台北市", 25.033, 121.565)
        assertTrue(vm.state.value.hasBirthPlace)
        assertEquals("台北市", vm.state.value.birthPlaceName)
    }

    @Test
    fun `skipBirthDetails clears birth time and place`() {
        val vm = createViewModel()
        vm.setBirthTime(10, 0)
        vm.setBirthPlace("台北市", 25.0, 121.0)
        vm.skipBirthDetails()
        assertFalse(vm.state.value.hasBirthTime)
        assertFalse(vm.state.value.hasBirthPlace)
    }

    @Test
    fun `setTone updates tone`() {
        val vm = createViewModel()
        vm.setTone("healing")
        assertEquals("healing", vm.state.value.tone)
    }

    @Test
    fun `completeOnboarding saves profile and sets isComplete`() = runTest {
        val vm = createViewModel()
        vm.setBirthday(LocalDate.of(1990, 3, 21))
        vm.setHand("left")
        vm.setTone("roast_safe")
        vm.completeOnboarding()
        assertTrue(vm.state.value.isComplete)
        coVerify(exactly = 1) {
            userRepository.save(match {
                it.dominantHand == "left" && it.tone == "roast_safe" && it.calcLevel == "L1"
            })
        }
    }
}
