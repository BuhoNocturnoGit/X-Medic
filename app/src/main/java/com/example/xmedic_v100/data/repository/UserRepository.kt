package com.example.xmedic_v100.data.repository

import android.content.Context
import android.util.Log
import com.example.xmedic_v100.ui.screens.UserRole
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

sealed interface UserProfileResult {
    data class HasRole(val role: UserRole) : UserProfileResult
    object NoRole : UserProfileResult
    data class Error(val exception: Exception) : UserProfileResult
}

class UserRepository(context: Context) {
    private val prefs = context.getSharedPreferences("xmedic_user_prefs", Context.MODE_PRIVATE)

    fun getCurrentUser(): FirebaseUser? {
        return try {
            FirebaseAuth.getInstance().currentUser
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Guarda el rol del usuario localmente en SharedPreferences y remotamente en Firestore.
     */
    fun saveUserRole(role: UserRole) {
        prefs.edit().putString("KEY_USER_ROLE", role.name).apply()
        prefs.edit().putBoolean("KEY_ONBOARDING_COMPLETED", true).apply()

        val user = getCurrentUser() ?: return
        try {
            val db = FirebaseFirestore.getInstance()
            val userMap = mapOf(
                "uid" to user.uid,
                "displayName" to getUserDisplayName(),
                "email" to getUserEmail(),
                "role" to role.name,
                "onboardingCompleted" to true
            )
            db.collection("users").document(user.uid)
                .set(userMap, SetOptions.merge())
                .addOnFailureListener { e ->
                    Log.e("UserRepository", "Error al guardar el rol en Firestore", e)
                }
        } catch (e: Exception) {
            Log.e("UserRepository", "Excepción al guardar rol en Firestore", e)
        }
    }

    /**
     * Consulta el perfil del usuario remotamente en Firestore para recuperar su rol si ya existía.
     */
    suspend fun fetchUserProfileRemote(): UserProfileResult {
        val user = getCurrentUser() ?: return UserProfileResult.NoRole
        return try {
            val db = FirebaseFirestore.getInstance()
            val doc = db.collection("users").document(user.uid).get().await()

            if (doc.exists()) {
                val roleName = doc.getString("role")
                val onboardingCompleted = doc.getBoolean("onboardingCompleted") ?: false

                if (!roleName.isNullOrBlank() && onboardingCompleted) {
                    val role = try {
                        UserRole.valueOf(roleName)
                    } catch (_: Exception) {
                        UserRole.PACIENTE
                    }
                    // Guardar localmente
                    prefs.edit().putString("KEY_USER_ROLE", role.name).apply()
                    prefs.edit().putBoolean("KEY_ONBOARDING_COMPLETED", true).apply()
                    UserProfileResult.HasRole(role)
                } else {
                    UserProfileResult.NoRole
                }
            } else {
                UserProfileResult.NoRole
            }
        } catch (e: Exception) {
            Log.e("UserRepository", "Error al consultar perfil remoto en Firestore", e)
            // Fallback a SharedPreferences si hay rol local previamente guardado
            if (prefs.getBoolean("KEY_ONBOARDING_COMPLETED", false)) {
                UserProfileResult.HasRole(getUserRole())
            } else {
                UserProfileResult.Error(e)
            }
        }
    }

    fun getUserRole(): UserRole {
        val roleName = prefs.getString("KEY_USER_ROLE", UserRole.PACIENTE.name) ?: UserRole.PACIENTE.name
        return try {
            UserRole.valueOf(roleName)
        } catch (_: Exception) {
            UserRole.PACIENTE
        }
    }

    fun hasCompletedOnboarding(): Boolean {
        return prefs.getBoolean("KEY_ONBOARDING_COMPLETED", false)
    }

    fun getTokenCount(): Int {
        return prefs.getInt("KEY_TOKEN_COUNT", 120)
    }

    fun saveTokenCount(count: Int) {
        prefs.edit().putInt("KEY_TOKEN_COUNT", count).apply()
    }

    fun getUserDisplayName(): String {
        val user = getCurrentUser()
        val name = user?.displayName
        if (!name.isNullOrBlank()) return name
        val email = user?.email
        if (!email.isNullOrBlank()) return email.substringBefore("@")
        return "Usuario"
    }

    fun getUserEmail(): String {
        return getCurrentUser()?.email ?: "usuario@xmedic.com"
    }

    fun getUserInitials(): String {
        val name = getUserDisplayName()
        val parts = name.trim().split(" ")
        return if (parts.size >= 2) {
            "${parts[0].firstOrNull()?.uppercase() ?: ""}${parts[1].firstOrNull()?.uppercase() ?: ""}"
        } else if (name.isNotEmpty()) {
            name.take(2).uppercase()
        } else {
            "XM"
        }
    }

    fun clearLocalData() {
        prefs.edit().clear().apply()
    }
}
