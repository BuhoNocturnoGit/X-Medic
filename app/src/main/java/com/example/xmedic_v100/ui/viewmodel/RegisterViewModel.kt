package com.example.xmedic_v100.ui.viewmodel

import android.content.Context
import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.xmedic_v100.data.repository.AuthRepository
import com.example.xmedic_v100.data.repository.GoogleAuthManager
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface RegisterUiState {
    object Idle : RegisterUiState
    object Loading : RegisterUiState
    data class Success(val user: FirebaseUser?) : RegisterUiState
    data class Error(val message: String) : RegisterUiState
}

open class RegisterViewModel(
    private val authRepository: AuthRepository = AuthRepository(),
    private val googleAuthManagerSupplier: (Context) -> GoogleAuthManager = {
        GoogleAuthManager(it)
    }
) : ViewModel() {

    private val _uiState = MutableStateFlow<RegisterUiState>(RegisterUiState.Idle)
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    fun registerUser(
        fullName: String,
        email: String,
        phone: String,
        password: String,
        confirmPassword: String,
        termsAccepted: Boolean
    ) {
        val trimmedName = fullName.trim()
        val trimmedEmail = email.trim()
        val trimmedPhone = phone.trim()
        val trimmedPassword = password.trim()
        val trimmedConfirmPassword = confirmPassword.trim()

        if (trimmedName.isEmpty()) {
            _uiState.value = RegisterUiState.Error("Ingresa tu nombre completo")
            return
        }

        if (trimmedEmail.isEmpty() || !isEmailValid(trimmedEmail)) {
            _uiState.value = RegisterUiState.Error("Ingresa un correo electrónico válido")
            return
        }

        if (trimmedPassword.isEmpty()) {
            _uiState.value = RegisterUiState.Error("La contraseña debe tener al menos 6 caracteres")
            return
        }

        if (trimmedPassword.length < 6) {
            _uiState.value = RegisterUiState.Error("La contraseña debe tener al menos 6 caracteres")
            return
        }

        if (trimmedPassword != trimmedConfirmPassword) {
            _uiState.value = RegisterUiState.Error("Las contraseñas no coinciden")
            return
        }

        if (!termsAccepted) {
            _uiState.value = RegisterUiState.Error("Debes aceptar los términos y condiciones")
            return
        }

        viewModelScope.launch {
            _uiState.value = RegisterUiState.Loading
            val result = authRepository.createUserWithEmail(
                email = trimmedEmail,
                password = trimmedPassword,
                displayName = trimmedName
            )

            result.fold(
                onSuccess = { user ->
                    _uiState.value = RegisterUiState.Success(user)
                },
                onFailure = { error ->
                    val message = when (error) {
                        is FirebaseAuthUserCollisionException -> "Ya existe una cuenta con este correo electrónico"
                        else -> {
                            val msg = error.localizedMessage ?: ""
                            if (msg.contains("network", ignoreCase = true)) {
                                "No se pudo conectar. Verifica tu conexión e inténtalo nuevamente"
                            } else if (msg.contains("badly formatted", ignoreCase = true) || msg.contains("invalid", ignoreCase = true)) {
                                "El correo electrónico no es válido"
                            } else {
                                error.localizedMessage ?: "Error al crear la cuenta"
                            }
                        }
                    }
                    _uiState.value = RegisterUiState.Error(message)
                }
            )
        }
    }

    fun performGoogleSignIn(
        context: Context,
        fullName: String = "",
        password: String = ""
    ) {
        val webClientId = "453107428098-gne4h6a2ddo2q1lutc2vqs9kq5o04qto.apps.googleusercontent.com"

        viewModelScope.launch {
            _uiState.value = RegisterUiState.Loading

            val googleAuthManager = googleAuthManagerSupplier(context)
            val result = googleAuthManager.signInWithCredentialManager(
                webClientId = webClientId,
                onlyExistingAccount = false
            )

            result.fold(
                onSuccess = { user ->
                    if (user != null) {
                        authRepository.linkPasswordAndProfileToUser(
                            user = user,
                            password = password.trim(),
                            displayName = fullName.trim()
                        )
                    }
                    _uiState.value = RegisterUiState.Success(user)
                },
                onFailure = { error ->
                    val msg = error.localizedMessage ?: ""
                    if (error is androidx.credentials.exceptions.GetCredentialCancellationException ||
                        msg.contains("cancel", ignoreCase = true)
                    ) {
                        _uiState.value = RegisterUiState.Idle
                    } else if (msg.contains("No credentials available", ignoreCase = true)) {
                        _uiState.value = RegisterUiState.Error(
                            "No hay cuentas de Google registradas en este dispositivo. Añade una cuenta en los Ajustes de tu teléfono."
                        )
                    } else {
                        _uiState.value = RegisterUiState.Error(
                            error.localizedMessage ?: "Error al autenticar con Google"
                        )
                    }
                }
            )
        }
    }

    private fun isEmailValid(email: String): Boolean {
        return try {
            Patterns.EMAIL_ADDRESS.matcher(email).matches()
        } catch (e: Exception) {
            email.contains("@") && email.contains(".")
        }
    }

    fun resetState() {
        _uiState.value = RegisterUiState.Idle
    }
}
