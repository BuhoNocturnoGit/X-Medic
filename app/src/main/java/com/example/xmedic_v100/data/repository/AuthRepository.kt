package com.example.xmedic_v100.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.tasks.await

/**
 * Repositorio para gestionar las operaciones de autenticación con Firebase Auth.
 */
open class AuthRepository(
    private val firebaseAuthSupplier: () -> FirebaseAuth? = {
        try {
            FirebaseAuth.getInstance()
        } catch (e: Exception) {
            null
        }
    }
) {
    /**
     * Obtiene el usuario actualmente autenticado en Firebase, si existe.
     */
    open fun getCurrentUser(): FirebaseUser? {
        return try {
            firebaseAuthSupplier()?.currentUser
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Comprueba si existe una sesión activa de usuario.
     */
    open fun isUserLoggedIn(): Boolean {
        return getCurrentUser() != null
    }

    /**
     * Inicia sesión con correo electrónico y contraseña en Firebase Auth.
     */
    open suspend fun signInWithEmail(email: String, password: String): Result<FirebaseUser> {
        return try {
            val auth = firebaseAuthSupplier()
                ?: return Result.failure(IllegalStateException("Firebase Auth no está inicializado"))
            val authResult = auth.signInWithEmailAndPassword(email, password).await()
            val user = authResult.user
            if (user != null) {
                Result.success(user)
            } else {
                Result.failure(IllegalStateException("No se pudo obtener el usuario"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Crea una nueva cuenta de usuario con correo electrónico y contraseña en Firebase Auth.
     */
    open suspend fun createUserWithEmail(
        email: String,
        password: String,
        displayName: String? = null
    ): Result<FirebaseUser> {
        return try {
            val auth = firebaseAuthSupplier()
                ?: return Result.failure(IllegalStateException("Firebase Auth no está inicializado"))
            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
            val user = authResult.user
            if (user != null) {
                if (!displayName.isNullOrBlank()) {
                    try {
                        val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                            .setDisplayName(displayName)
                            .build()
                        user.updateProfile(profileUpdates).await()
                    } catch (_: Exception) {
                        // Si falla la actualización del perfil, el registro en Auth igual es exitoso
                    }
                }
                Result.success(user)
            } else {
                Result.failure(IllegalStateException("No se pudo registrar el usuario"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Envía un correo electrónico de restablecimiento de contraseña.
     */
    open suspend fun sendPasswordReset(email: String): Result<Unit> {
        return try {
            val auth = firebaseAuthSupplier()
                ?: return Result.failure(IllegalStateException("Firebase Auth no está inicializado"))
            auth.sendPasswordResetEmail(email).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Cierra la sesión activa del usuario actual.
     */
    fun signOut() {
        try {
            firebaseAuthSupplier()?.signOut()
        } catch (e: Exception) {
            // Manejo silencioso en caso de error
        }
    }
}
