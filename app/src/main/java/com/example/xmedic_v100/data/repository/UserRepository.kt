package com.example.xmedic_v100.data.repository

import android.content.Context
import com.example.xmedic_v100.ui.screens.UserRole
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

class UserRepository(context: Context) {
    private val prefs = context.getSharedPreferences("xmedic_user_prefs", Context.MODE_PRIVATE)

    fun saveUserRole(role: UserRole) {
        prefs.edit().putString("KEY_USER_ROLE", role.name).apply()
    }

    fun getUserRole(): UserRole {
        val roleName = prefs.getString("KEY_USER_ROLE", UserRole.PACIENTE.name) ?: UserRole.PACIENTE.name
        return try {
            UserRole.valueOf(roleName)
        } catch (_: Exception) {
            UserRole.PACIENTE
        }
    }

    fun getTokenCount(): Int {
        return prefs.getInt("KEY_TOKEN_COUNT", 120)
    }

    fun saveTokenCount(count: Int) {
        prefs.edit().putInt("KEY_TOKEN_COUNT", count).apply()
    }

    fun getCurrentUser(): FirebaseUser? {
        return try {
            FirebaseAuth.getInstance().currentUser
        } catch (_: Exception) {
            null
        }
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
}
