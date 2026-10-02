package com.example.xmedic_v100

import com.example.xmedic_v100.data.repository.AuthRepository
import com.example.xmedic_v100.ui.viewmodel.RegisterUiState
import com.example.xmedic_v100.ui.viewmodel.RegisterViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RegisterViewModelTest {

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
    fun emptyNameEmitsErrorWithoutCallingFirebase() = runTest {
        val viewModel = RegisterViewModel(authRepository = AuthRepository())
        viewModel.registerUser(
            fullName = "",
            email = "test@xmedic.com",
            phone = "12345678",
            password = "password123",
            confirmPassword = "password123",
            termsAccepted = true
        )

        val state = viewModel.uiState.value
        assertTrue(state is RegisterUiState.Error)
        assertEquals("Ingresa tu nombre completo", (state as RegisterUiState.Error).message)
    }

    @Test
    fun passwordMismatchEmitsError() = runTest {
        val viewModel = RegisterViewModel(authRepository = AuthRepository())
        viewModel.registerUser(
            fullName = "Juan Pérez",
            email = "test@xmedic.com",
            phone = "12345678",
            password = "password123",
            confirmPassword = "differentPassword",
            termsAccepted = true
        )

        val state = viewModel.uiState.value
        assertTrue(state is RegisterUiState.Error)
        assertEquals("Las contraseñas no coinciden", (state as RegisterUiState.Error).message)
    }

    @Test
    fun unacceptedTermsEmitsError() = runTest {
        val viewModel = RegisterViewModel(authRepository = AuthRepository())
        viewModel.registerUser(
            fullName = "Juan Pérez",
            email = "test@xmedic.com",
            phone = "12345678",
            password = "password123",
            confirmPassword = "password123",
            termsAccepted = false
        )

        val state = viewModel.uiState.value
        assertTrue(state is RegisterUiState.Error)
        assertEquals("Debes aceptar los términos y condiciones", (state as RegisterUiState.Error).message)
    }
}
