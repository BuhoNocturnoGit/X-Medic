package com.example.xmedic_v100

import com.example.xmedic_v100.data.repository.AuthRepository
import com.example.xmedic_v100.ui.viewmodel.SplashDestination
import com.example.xmedic_v100.ui.viewmodel.SplashViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SplashViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun userNotLoggedInNavigatesToLoginAfterDelay() = runTest {
        val fakeRepo = AuthRepository(firebaseAuthSupplier = { null })
        val viewModel = SplashViewModel(authRepository = fakeRepo)

        // Inicialmente está en Idle
        assertEquals(SplashDestination.Idle, viewModel.destination.value)

        // Avanzamos el tiempo de delay
        advanceTimeBy(2001)

        assertEquals(SplashDestination.Login, viewModel.destination.value)
    }

    @Test
    fun userLoggedInNavigatesToHomeAfterDelay() = runTest {
        // repositorio que retorna isUserLoggedIn = true
        val fakeRepo = object : AuthRepository(firebaseAuthSupplier = { null }) {
            override fun isUserLoggedIn(): Boolean = true
        }
        val viewModel = SplashViewModel(authRepository = fakeRepo)

        assertEquals(SplashDestination.Idle, viewModel.destination.value)

        advanceTimeBy(2001)

        assertEquals(SplashDestination.Home, viewModel.destination.value)
    }
}
