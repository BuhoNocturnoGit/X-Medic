package com.example.xmedic_v100.ui.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.xmedic_v100.data.repository.AuthRepository
import com.example.xmedic_v100.data.repository.UserProfileResult
import com.example.xmedic_v100.data.repository.UserRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface SplashDestination {
    object Idle : SplashDestination
    object Login : SplashDestination
    object RoleSelection : SplashDestination
    object Home : SplashDestination
}

/**
 * ViewModel que maneja la lógica de inicio, retardo inicial (delay) y
 * verificación de sesión del usuario en el Splash.
 */
class SplashViewModel(
    private val authRepository: AuthRepository = AuthRepository()
) : ViewModel() {

    private val _destination = MutableStateFlow<SplashDestination>(SplashDestination.Idle)
    val destination: StateFlow<SplashDestination> = _destination.asStateFlow()

    init {
        checkSessionAndNavigate()
    }

    fun checkSessionAndNavigate(context: Context? = null, minDelayMillis: Long = 2000L) {
        viewModelScope.launch {
            delay(minDelayMillis)
            if (authRepository.isUserLoggedIn()) {
                if (context != null) {
                    val userRepo = UserRepository(context)
                    val profileResult = userRepo.fetchUserProfileRemote()
                    if (profileResult is UserProfileResult.HasRole) {
                        _destination.value = SplashDestination.Home
                    } else {
                        _destination.value = SplashDestination.RoleSelection
                    }
                } else {
                    _destination.value = SplashDestination.Home
                }
            } else {
                _destination.value = SplashDestination.Login
            }
        }
    }
}
