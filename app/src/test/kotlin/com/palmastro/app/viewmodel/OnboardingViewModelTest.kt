package com.palmastro.app.viewmodel

import com.palmastro.data.repository.UserRepository
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.jupiter.api.*
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
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

    // ── Initial state / no silent defaults ──

    @Test
    fun `initial state starts at welcome with no dominant hand chosen`() {
        val vm = createViewModel()
        assertEquals(OnboardingSteps.WELCOME, vm.state.value.step)
        assertNull(vm.state.value.dominantHand)
        assertEquals("system", vm.state.value.language)
        assertFalse(vm.state.value.isComplete)
    }

    @Test
    fun `setHand records explicit choice`() {
        val vm = createViewModel()
        vm.setHand("left")
        assertEquals("left", vm.state.value.dominantHand)
    }

    // ── Step navigation ──

    @Test
    fun `nextStep increments and clamps at last step`() {
        val vm = createViewModel()
        vm.nextStep()
        assertEquals(1, vm.state.value.step)
        repeat(OnboardingSteps.TOTAL + 3) { vm.nextStep() }
        assertEquals(OnboardingSteps.TOTAL - 1, vm.state.value.step)
    }

    @Test
    fun `prevStep does not go below 0`() {
        val vm = createViewModel()
        vm.prevStep()
        assertEquals(0, vm.state.value.step)
        vm.nextStep(); vm.nextStep(); vm.prevStep()
        assertEquals(1, vm.state.value.step)
    }

    // ── Required field gating ──

    @Test
    fun `cannot proceed past birthday step without a birthday`() {
        val vm = createViewModel()
        assertFalse(vm.canProceedFrom(OnboardingSteps.BIRTHDAY))
        vm.setBirthday(LocalDate.of(1990, 6, 15))
        assertTrue(vm.canProceedFrom(OnboardingSteps.BIRTHDAY))
    }

    @Test
    fun `cannot proceed past hand step without an explicit hand choice`() {
        val vm = createViewModel()
        assertFalse(vm.canProceedFrom(OnboardingSteps.HAND))
        vm.setHand("right")
        assertTrue(vm.canProceedFrom(OnboardingSteps.HAND))
    }

    @Test
    fun `optional steps never gate`() {
        val vm = createViewModel()
        assertTrue(vm.canProceedFrom(OnboardingSteps.WELCOME))
        assertTrue(vm.canProceedFrom(OnboardingSteps.PRIVACY))
        assertTrue(vm.canProceedFrom(OnboardingSteps.NAME))
        assertTrue(vm.canProceedFrom(OnboardingSteps.BIRTH_DETAILS))
        assertTrue(vm.canProceedFrom(OnboardingSteps.TONE))
        assertTrue(vm.canProceedFrom(OnboardingSteps.LANGUAGE))
    }

    @Test
    fun `name is optional and does not block proceeding or completion`() {
        val vm = createViewModel()
        vm.setBirthday(LocalDate.of(1990, 6, 15))
        vm.setHand("right")
        assertTrue(vm.canProceedFrom(OnboardingSteps.NAME))
        assertTrue(vm.canComplete())
    }

    // ── Completion guards ──

    @Test
    fun `completeOnboarding without birthday does nothing`() = runTest {
        val vm = createViewModel()
        vm.setHand("right")
        vm.completeOnboarding()
        assertFalse(vm.state.value.isComplete)
        coVerify(exactly = 0) { userRepository.save(any()) }
    }

    @Test
    fun `completeOnboarding without hand does nothing`() = runTest {
        val vm = createViewModel()
        vm.setBirthday(LocalDate.of(1990, 6, 15))
        vm.completeOnboarding()
        assertFalse(vm.state.value.isComplete)
        coVerify(exactly = 0) { userRepository.save(any()) }
    }

    @Test
    fun `completeOnboarding saves profile with required fields and sets isComplete`() = runTest {
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

    @Test
    fun `blank name is saved as null`() = runTest {
        val vm = createViewModel()
        vm.setBirthday(LocalDate.of(1990, 3, 21))
        vm.setHand("right")
        vm.setName("   ")
        vm.completeOnboarding()
        coVerify(exactly = 1) { userRepository.save(match { it.name == null }) }
    }

    @Test
    fun `language selection is persisted to the profile`() = runTest {
        val vm = createViewModel()
        vm.setBirthday(LocalDate.of(1990, 3, 21))
        vm.setHand("right")
        vm.setLanguage("zh-TW")
        vm.completeOnboarding()
        assertEquals("zh-TW", vm.state.value.language)
        coVerify(exactly = 1) { userRepository.save(match { it.language == "zh-TW" }) }
    }

    @Test
    fun `default language is system when never touched`() = runTest {
        val vm = createViewModel()
        vm.setBirthday(LocalDate.of(1990, 3, 21))
        vm.setHand("right")
        vm.completeOnboarding()
        coVerify(exactly = 1) { userRepository.save(match { it.language == "system" }) }
    }

    // ── Optional detail behavior ──

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
    fun `a place without a time stays L1 and records no birth time`() = runTest {
        val vm = createViewModel()
        vm.setBirthday(LocalDate.of(1990, 3, 21))
        vm.setHand("right")
        vm.setBirthDetails(null, null, "Taipei, Taiwan", 25.033, 121.565)
        vm.completeOnboarding()
        coVerify(exactly = 1) {
            userRepository.save(match { it.calcLevel == "L1" && it.birthTimeMinutes == null })
        }
    }

    @Test
    fun `setBirthDetails clears values the user removed on a return visit`() {
        val vm = createViewModel()
        vm.setBirthTime(8, 30)
        vm.setBirthPlace("台北市", 25.033, 121.565)
        vm.setBirthDetails(null, null, null, null, null)
        assertFalse(vm.state.value.hasBirthTime)
        assertFalse(vm.state.value.hasBirthPlace)
    }

    @Test
    fun `full birth details produce L2 calc level`() = runTest {
        val vm = createViewModel()
        vm.setBirthday(LocalDate.of(1990, 3, 21))
        vm.setHand("right")
        vm.setBirthTime(8, 30)
        vm.setBirthPlace("Taipei, Taiwan", 25.033, 121.565)
        vm.completeOnboarding()
        coVerify(exactly = 1) { userRepository.save(match { it.calcLevel == "L2" }) }
    }

    @Test
    fun `setTone updates tone`() {
        val vm = createViewModel()
        vm.setTone("healing")
        assertEquals("healing", vm.state.value.tone)
    }
}
