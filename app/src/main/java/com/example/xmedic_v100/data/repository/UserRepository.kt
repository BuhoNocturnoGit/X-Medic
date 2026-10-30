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
                .addOnSuccessListener {
                    Log.d("XMEDIC_NAV", "Rol '${role.name}' persistido exitosamente en Firestore para UID=${user.uid}")
                }
                .addOnFailureListener { e ->
                    Log.e("XMEDIC_NAV", "Error al guardar el rol en Firestore", e)
                }
        } catch (e: Exception) {
            Log.e("XMEDIC_NAV", "Excepción al guardar rol en Firestore", e)
        }
    }

    /**
     * Consulta el perfil del usuario remotamente en Firestore para recuperar su rol si ya existía.
     */
    suspend fun fetchUserProfileRemote(): UserProfileResult {
        val user = getCurrentUser() ?: run {
            Log.d("XMEDIC_NAV", "fetchUserProfileRemote: No hay usuario autenticado actualmente.")
            return UserProfileResult.NoRole
        }

        return try {
            val db = FirebaseFirestore.getInstance()
            val doc = db.collection("users").document(user.uid).get().await()

            if (doc.exists()) {
                val roleName = doc.getString("role")
                if (!roleName.isNullOrBlank()) {
                    val role = try {
                        UserRole.valueOf(roleName)
                    } catch (_: Exception) {
                        UserRole.PACIENTE
                    }
                    // Guardar localmente
                    prefs.edit().putString("KEY_USER_ROLE", role.name).apply()
                    prefs.edit().putBoolean("KEY_ONBOARDING_COMPLETED", true).apply()
                    Log.d("XMEDIC_NAV", "Perfil remoto recuperado de Firestore para UID=${user.uid}: Rol=${role.name}")
                    UserProfileResult.HasRole(role)
                } else {
                    Log.d("XMEDIC_NAV", "Documento de Firestore existe para UID=${user.uid} pero no contiene 'role'.")
                    if (hasCompletedOnboarding()) {
                        UserProfileResult.HasRole(getUserRole())
                    } else {
                        UserProfileResult.NoRole
                    }
                }
            } else {
                Log.d("XMEDIC_NAV", "Documento de Firestore no existe para UID=${user.uid}.")
                if (hasCompletedOnboarding()) {
                    val role = getUserRole()
                    Log.d("XMEDIC_NAV", "Usando rol local previamente guardado: ${role.name}")
                    UserProfileResult.HasRole(role)
                } else {
                    UserProfileResult.NoRole
                }
            }
        } catch (e: Exception) {
            Log.e("XMEDIC_NAV", "Error de red al consultar Firestore. Verificando fallback local...", e)
            if (hasCompletedOnboarding()) {
                val role = getUserRole()
                Log.d("XMEDIC_NAV", "Fallback a SharedPreferences exitoso: Rol=${role.name}")
                UserProfileResult.HasRole(role)
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
        // Al cerrar sesión, no borramos SharedPreferences completas si se quiere conservar caché local
        prefs.edit().remove("KEY_TEMP_CACHE").apply()
    }
}
