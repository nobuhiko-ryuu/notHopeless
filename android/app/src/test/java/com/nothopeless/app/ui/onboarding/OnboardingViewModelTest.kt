package com.nothopeless.app.ui.onboarding

import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import com.nothopeless.app.data.repository.SettingsRepository

@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var repo: SettingsRepository
    private lateinit var vm: OnboardingViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        repo = mockk(relaxed = true)
        vm = OnboardingViewModel(repo)
    }

    @After
    fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun `初期状態は page 0`() {
        assertEquals(0, vm.uiState.value.currentPage)
    }

    @Test
    fun `nextPage で page が 1 に進む`() {
        vm.nextPage()
        assertEquals(1, vm.uiState.value.currentPage)
    }

    @Test
    fun `page 2 で nextPage すると isCompleted が true になる`() = runTest {
        vm.nextPage(); vm.nextPage(); vm.nextPage()
        assertTrue(vm.uiState.value.isCompleted)
        coVerify { repo.completeOnboarding() }
    }
}
