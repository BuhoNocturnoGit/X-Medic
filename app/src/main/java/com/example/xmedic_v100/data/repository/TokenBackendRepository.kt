package com.example.xmedic_v100.data.repository

import android.content.Context
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

sealed interface ReserveTokensResult {
    data class Success(val operationId: String, val cost: Int, val newBalance: Int) : ReserveTokensResult
    data class InsufficientTokens(val currentBalance: Int, val requiredCost: Int) : ReserveTokensResult
    data class Error(val message: String) : ReserveTokensResult
}

/**
 * Repositorio Android que consume el backend seguro de tokens en Cloudflare Workers.
 */
class TokenBackendRepository(context: Context) {

    // URL base del backend en Cloudflare Workers (desarrollo local o producción)
    private val backendBaseUrl = "https://xmedic-token-backend.buho.workers.dev" // o http://10.0.2.2:8787 para wrangler dev

    private suspend fun getFirebaseIdToken(): String? {
        val user = FirebaseAuth.getInstance().currentUser ?: return null
        return try {
            val tokenResult = user.getIdToken(false).await()
            tokenResult.token
        } catch (e: Exception) {
            Log.e("TokenBackend", "Error al obtener ID Token de Firebase", e)
            null
        }
    }

    /**
     * Inicializa de forma atómica e idempotente el bono de 120 tokens en el servidor
     */
    suspend fun initializeUserTokens(): Int? = withContext(Dispatchers.IO) {
        val idToken = getFirebaseIdToken() ?: return@withContext null
        return@withContext try {
            val url = URL("$backendBaseUrl/api/tokens/initialize")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Authorization", "Bearer $idToken")
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true

            val writer = OutputStreamWriter(conn.outputStream)
            writer.write("{}")
            writer.flush()
            writer.close()

            if (conn.responseCode == 200) {
                val reader = BufferedReader(InputStreamReader(conn.inputStream))
                val json = JSONObject(reader.readText())
                reader.close()
                val balance = json.getInt("balance")
                Log.d("TokenBackend", "Bono de tokens inicializado en backend. Saldo actual: $balance")
                balance
            } else {
                Log.e("TokenBackend", "Error HTTP al inicializar tokens: ${conn.responseCode}")
                null
            }
        } catch (e: Exception) {
            Log.e("TokenBackend", "Excepción al conectar con backend de inicialización", e)
            null
        }
    }

    /**
     * Consulta el saldo oficial del usuario en el servidor Cloudflare / Firestore
     */
    suspend fun fetchOfficialTokenBalance(): Int? = withContext(Dispatchers.IO) {
        val idToken = getFirebaseIdToken() ?: return@withContext null
        return@withContext try {
            val url = URL("$backendBaseUrl/api/tokens/balance")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.setRequestProperty("Authorization", "Bearer $idToken")

            if (conn.responseCode == 200) {
                val reader = BufferedReader(InputStreamReader(conn.inputStream))
                val json = JSONObject(reader.readText())
                reader.close()
                val balance = json.getInt("balance")
                Log.d("TokenBackend", "Saldo oficial obtenido del backend: $balance")
                balance
            } else {
                Log.e("TokenBackend", "Error HTTP al consultar saldo oficial: ${conn.responseCode}")
                null
            }
        } catch (e: Exception) {
            Log.e("TokenBackend", "Excepción al consultar saldo oficial", e)
            null
        }
    }

    /**
     * Solicita la reserva y descuento transaccional de 5 tokens antes de ejecutar un análisis médico
     */
    suspend fun reserveAnalysisTokens(operationId: String): ReserveTokensResult = withContext(Dispatchers.IO) {
        val idToken = getFirebaseIdToken() ?: return@withContext ReserveTokensResult.Error("No hay sesión de usuario activa")
        return@withContext try {
            val url = URL("$backendBaseUrl/api/analysis/reserve")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Authorization", "Bearer $idToken")
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true

            val bodyJson = JSONObject().apply {
                put("operationId", operationId)
            }

            val writer = OutputStreamWriter(conn.outputStream)
            writer.write(bodyJson.toString())
            writer.flush()
            writer.close()

            val statusCode = conn.responseCode
            if (statusCode == 200) {
                val reader = BufferedReader(InputStreamReader(conn.inputStream))
                val json = JSONObject(reader.readText())
                reader.close()
                val newBalance = json.getInt("balance")
                val cost = json.getInt("cost")
                Log.d("TokenBackend", "Reserva de 5 tokens autorizada para op $operationId. Nuevo saldo: $newBalance")
                ReserveTokensResult.Success(operationId, cost, newBalance)
            } else if (statusCode == 402) {
                val reader = BufferedReader(InputStreamReader(conn.errorStream))
                val json = JSONObject(reader.readText())
                reader.close()
                val balance = json.optInt("balance", 0)
                Log.w("TokenBackend", "Saldo insuficiente en servidor para realizar análisis.")
                ReserveTokensResult.InsufficientTokens(currentBalance = balance, requiredCost = 5)
            } else {
                Log.e("TokenBackend", "Error HTTP en reserva de tokens: $statusCode")
                ReserveTokensResult.Error("Error de servidor HTTP $statusCode")
            }
        } catch (e: Exception) {
            Log.e("TokenBackend", "Excepción al solicitar reserva de tokens en backend", e)
            ReserveTokensResult.Error("Excepción de red: ${e.localizedMessage}")
        }
    }
}
