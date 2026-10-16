package com.example.xmedic_v100.data.repository

import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await

/**
 * Gestor para la autenticación Single Sign-On (SSO) con Google y Firebase Auth.
 */
open class GoogleAuthManager(
    private val context: Context,
    private val firebaseAuthSupplier: () -> FirebaseAuth? = {
        try {
            FirebaseAuth.getInstance()
        } catch (e: Exception) {
            null
        }
    }
) {
    private val credentialManager by lazy { CredentialManager.create(context) }

    /**
     * Inicia sesión utilizando Android Credential Manager y solicita la credencial de Google.
     * @param onlyExistingAccount Si es true, exige que la cuenta de Google ya exista en Firebase Auth.
     */
    open suspend fun signInWithCredentialManager(
        webClientId: String,
        onlyExistingAccount: Boolean = false
    ): Result<FirebaseUser> {
        return try {
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(webClientId)
                .setAutoSelectEnabled(false)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val response: GetCredentialResponse = credentialManager.getCredential(
                request = request,
                context = context
            )

            val credential = response.credential
            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
            val idToken = googleIdTokenCredential.idToken

            signInWithFirebaseToken(idToken, onlyExistingAccount)
        } catch (e: GetCredentialCancellationException) {
            Result.failure(e)
        } catch (e: GetCredentialException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Retorna el cliente de GoogleSignInClient para ser lanzado con ActivityResultLauncher.
     */
    open fun getGoogleSignInClient(webClientId: String): GoogleSignInClient {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(webClientId)
            .requestEmail()
            .build()
        return GoogleSignIn.getClient(context, gso)
    }

    /**
     * Procesa la cuenta obtenida de GoogleSignInAccount para autenticar contra Firebase.
     */
    open suspend fun signInWithGoogleAccount(
        account: GoogleSignInAccount,
        onlyExistingAccount: Boolean = false
    ): Result<FirebaseUser> {
        val idToken = account.idToken ?: return Result.failure(
            IllegalArgumentException("No se encontró el ID Token en la cuenta de Google")
        )
        return signInWithFirebaseToken(idToken, onlyExistingAccount)
    }

    /**
     * Autentica el token de ID de Google en Firebase Auth usando GoogleAuthProvider.
     */
    open suspend fun signInWithFirebaseToken(
        idToken: String,
        onlyExistingAccount: Boolean = false
    ): Result<FirebaseUser> {
        return try {
            val firebaseAuth = firebaseAuthSupplier()
                ?: return Result.failure(IllegalStateException("Firebase Auth no está inicializado"))

            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = firebaseAuth.signInWithCredential(credential).await()
            val user = authResult.user

            val isNewUser = authResult.additionalUserInfo?.isNewUser == true

            if (onlyExistingAccount && isNewUser) {
                // Si estamos en la pestaña Iniciar Sesión y la cuenta de Google no existía previamente:
                try {
                    user?.delete()?.await()
                } catch (_: Exception) {}
                try {
                    firebaseAuth.signOut()
                } catch (_: Exception) {}

                return Result.failure(
                    IllegalStateException("No existe una cuenta registrada con este correo de Google. Por favor, pulsa 'Crear cuenta' para registrarte primero.")
                )
            }

            if (user != null) {
                Result.success(user)
            } else {
                Result.failure(IllegalStateException("No se pudo obtener el usuario de Firebase"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
