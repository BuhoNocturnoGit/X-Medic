package com.example.xmedic_v100.ui.viewmodel

import android.content.Context
import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.xmedic_v100.data.repository.AuthRepository
import com.example.xmedic_v100.data.repository.GoogleAuthManager
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface LoginUiState {
    object Idle : LoginUiState
    object Loading : LoginUiState
    data class Success(val user: FirebaseUser?) : LoginUiState
    data class Error(val message: String) : LoginUiState
    data class PasswordResetSent(val message: String = "Se envió un correo para restablecer tu contraseña") : LoginUiState
}

open class LoginViewModel(
    private val authRepository: AuthRepository = AuthRepository(),
    private val googleAuthManagerSupplier: (Context) -> GoogleAuthManager = {
        GoogleAuthManager(it)
    }
) : ViewModel() {

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    /**
     * Inicia sesión con correo electrónico y contraseña en Firebase Auth.
     */
    fun signInWithEmail(email: String, password: String) {
        val trimmedEmail = email.trim()
        val trimmedPassword = password.trim()

        if (trimmedEmail.isEmpty()) {
            _uiState.value = LoginUiState.Error("El correo electrónico no puede estar vacío")
            return
        }

        if (!isEmailValid(trimmedEmail)) {
            _uiState.value = LoginUiState.Error("El formato del correo electrónico no es válido")
            return
        }

        if (trimmedPassword.isEmpty()) {
            _uiState.value = LoginUiState.Error("La contraseña no puede estar vacía")
            return
        }

        viewModelScope.launch {
            _uiState.value = LoginUiState.Loading
            val result = authRepository.signInWithEmail(trimmedEmail, trimmedPassword)

            result.fold(
                onSuccess = { user ->
                    _uiState.value = LoginUiState.Success(user)
                },
                onFailure = { error ->
                    val message = when (error) {
                        is FirebaseAuthInvalidCredentialsException,
                        is FirebaseAuthInvalidUserException -> "Correo o contraseña incorrectos"
                        else -> {
                            val msg = error.localizedMessage ?: ""
                            if (msg.contains("INVALID_LOGIN_CREDENTIALS", ignoreCase = true) ||
                                msg.contains("invalid", ignoreCase = true)
                            ) {
                                "Correo o contraseña incorrectos"
                            } else {
                                "Error al iniciar sesión. Inténtalo de nuevo."
                            }
                        }
                    }
                    _uiState.value = LoginUiState.Error(message)
                }
            )
        }
    }

    /**
     * Registra un nuevo usuario con correo electrónico y contraseña.
     */
    fun createUserWithEmail(email: String, password: String) {
        val trimmedEmail = email.trim()
        val trimmedPassword = password.trim()

        if (trimmedEmail.isEmpty()) {
            _uiState.value = LoginUiState.Error("El correo electrónico no puede estar vacío")
            return
        }

        if (!isEmailValid(trimmedEmail)) {
            _uiState.value = LoginUiState.Error("El formato del correo electrónico no es válido")
            return
        }

        if (trimmedPassword.isEmpty()) {
            _uiState.value = LoginUiState.Error("La contraseña no puede estar vacía")
            return
        }

        if (trimmedPassword.length < 6) {
            _uiState.value = LoginUiState.Error("La contraseña debe tener al menos 6 caracteres")
            return
        }

        viewModelScope.launch {
            _uiState.value = LoginUiState.Loading
            val result = authRepository.createUserWithEmail(trimmedEmail, trimmedPassword)

            result.fold(
                onSuccess = { user ->
                    _uiState.value = LoginUiState.Success(user)
                },
                onFailure = { error ->
                    val message = when (error) {
                        is FirebaseAuthUserCollisionException -> "El correo ya se encuentra registrado"
                        else -> error.localizedMessage ?: "Error al crear la cuenta"
                    }
                    _uiState.value = LoginUiState.Error(message)
                }
            )
        }
    }

    /**
     * Envía un correo de restablecimiento de contraseña.
     */
    fun sendPasswordReset(email: String) {
        val trimmedEmail = email.trim()

        if (trimmedEmail.isEmpty()) {
            _uiState.value = LoginUiState.Error("Ingresa tu correo para restablecer la contraseña")
            return
        }

        if (!isEmailValid(trimmedEmail)) {
            _uiState.value = LoginUiState.Error("El formato del correo electrónico no es válido")
            return
        }

        viewModelScope.launch {
            _uiState.value = LoginUiState.Loading
            val result = authRepository.sendPasswordReset(trimmedEmail)

            result.fold(
                onSuccess = {
                    _uiState.value = LoginUiState.PasswordResetSent("Se envió un correo para restablecer tu contraseña")
                },
                onFailure = { error ->
                    _uiState.value = LoginUiState.Error(
                        error.localizedMessage ?: "No se pudo enviar el correo de restablecimiento"
                    )
                }
            )
        }
    }

    /**
     * Inicia sesión con Google usando Credential Manager.
     */
    fun performGoogleSignIn(context: Context) {
        val webClientId =
            "453107428098-gne4h6a2ddo2q1lutc2vqs9kq5o04qto.apps.googleusercontent.com"

        viewModelScope.launch {
            _uiState.value = LoginUiState.Loading

            val googleAuthManager =
                googleAuthManagerSupplier(context)

            val result =
                googleAuthManager.signInWithCredentialManager(
                    webClientId = webClientId,
                    onlyExistingAccount = true
                )

            result.fold(
                onSuccess = { user ->
                    _uiState.value =
                        LoginUiState.Success(user)
                },
                onFailure = { error ->
                    val msg = error.localizedMessage ?: ""
                    if (error is androidx.credentials.exceptions.GetCredentialCancellationException ||
                        msg.contains("cancel", ignoreCase = true)
                    ) {
                        // El usuario canceló la selección de cuenta -> Permanecer en la pantalla en estado Idle
                        _uiState.value = LoginUiState.Idle
                    } else if (msg.contains("No credentials available", ignoreCase = true)) {
                        _uiState.value = LoginUiState.Error(
                            "No hay cuentas de Google registradas en este dispositivo. Añade una cuenta en los Ajustes de tu teléfono."
                        )
                    } else {
                        _uiState.value = LoginUiState.Error(
                            error.localizedMessage
                                ?: "Error al autenticar con Google"
                        )
                    }
                }
            )
        }
    }

    /**
     * Autenticación mediante un ID Token recibido externamente.
     */
    fun performGoogleSignInWithToken(
        context: Context,
        idToken: String
    ) {
        viewModelScope.launch {
            _uiState.value = LoginUiState.Loading

            val googleAuthManager =
                googleAuthManagerSupplier(context)

            val result =
                googleAuthManager.signInWithFirebaseToken(idToken)

            result.fold(
                onSuccess = { user ->
                    _uiState.value =
                        LoginUiState.Success(user)
                },
                onFailure = { error ->
                    _uiState.value =
                        LoginUiState.Error(
                            error.localizedMessage
                                ?: "Error en la autenticación con Firebase"
                        )
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
        _uiState.value = LoginUiState.Idle
    }
}
