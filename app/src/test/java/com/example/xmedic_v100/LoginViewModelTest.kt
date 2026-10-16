package com.example.xmedic_v100

import com.example.xmedic_v100.data.repository.AuthRepository
import com.example.xmedic_v100.data.repository.GoogleAuthManager
import com.example.xmedic_v100.ui.viewmodel.LoginUiState
import com.example.xmedic_v100.ui.viewmodel.LoginViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

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
    fun invalidEmailValidationEmitsErrorWithoutCallingFirebase() = runTest {
        val fakeAuthRepo = object : AuthRepository() {
            override suspend fun signInWithEmail(email: String, password: String) =
                throw IllegalStateException("Firebase no debió ser llamado")
        }

        val viewModel = LoginViewModel(authRepository = fakeAuthRepo)
        viewModel.signInWithEmail("correo_invalido", "123456")

        val state = viewModel.uiState.value
        assertTrue(state is LoginUiState.Error)
        assertEquals("El formato del correo electrónico no es válido", (state as LoginUiState.Error).message)
    }

    @Test
    fun googleSignInFailureEmitsErrorState() = runTest {
        val fakeManager = FakeGoogleAuthManager()

        val viewModel = LoginViewModel(googleAuthManagerSupplier = { fakeManager })
        viewModel.performGoogleSignInWithToken(dummyContext(), "fakeToken")

        advanceUntilIdle()

        assertTrue(viewModel.uiState.value is LoginUiState.Error)
    }

    private fun dummyContext(): android.content.Context {
        val constructor = android.content.ContextWrapper::class.java.getDeclaredConstructor(android.content.Context::class.java)
        constructor.isAccessible = true
        return constructor.newInstance(null as android.content.Context?)
    }

    private class FakeGoogleAuthManager : GoogleAuthManager(
        context = android.content.ContextWrapper(null),
        firebaseAuthSupplier = { null }
    ) {
        override suspend fun signInWithFirebaseToken(idToken: String, onlyExistingAccount: Boolean) = Result.failure<com.google.firebase.auth.FirebaseUser>(
            IllegalStateException("Firebase Auth no inicializado")
        )
    }
}
