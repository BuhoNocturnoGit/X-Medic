package com.example.xmedic_v100.ui.viewmodel

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.xmedic_v100.data.model.AnalysisResult
import com.example.xmedic_v100.data.repository.ReserveTokensResult
import com.example.xmedic_v100.data.repository.TokenBackendRepository
import com.example.xmedic_v100.data.repository.UserRepository
import com.example.xmedic_v100.utils.ImageClassifierHelper
import com.example.xmedic_v100.utils.InferenceResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

sealed interface ProcessingUiState {
    object Idle : ProcessingUiState
    object ValidatingImage : ProcessingUiState
    object CheckingTokens : ProcessingUiState
    object Preprocessing : ProcessingUiState
    object RunningInference : ProcessingUiState
    object SavingResult : ProcessingUiState
    data class Completed(val result: AnalysisResult) : ProcessingUiState
    data class ClinicalModelMissing(val imageUri: Uri, val message: String) : ProcessingUiState
    data class Error(val message: String) : ProcessingUiState
}

class ProcessingViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<ProcessingUiState>(ProcessingUiState.Idle)
    val uiState: StateFlow<ProcessingUiState> = _uiState.asStateFlow()

    private var tokenDeducted = false

    fun processImage(context: Context, imageUri: Uri) {
        if (_uiState.value !is ProcessingUiState.Idle) return

        val userRepository = UserRepository(context)
        val tokenBackendRepo = TokenBackendRepository(context)
        val classifierHelper = ImageClassifierHelper(context)

        viewModelScope.launch {
            try {
                // 1. Verificar primero si el modelo clínico xmedic_model.tflite está disponible en assets
                if (!classifierHelper.isModelAvailable()) {
                    Log.d("XMEDIC_AI", "Modelo clínico 'xmedic_model.tflite' no disponible en assets. Omitiendo deducción de tokens.")
                    _uiState.value = ProcessingUiState.ClinicalModelMissing(
                        imageUri = imageUri,
                        message = "Análisis clínico no disponible. El modelo de diagnóstico (xmedic_model.tflite) no se encuentra instalado en la aplicación. Tu imagen se ha guardado correctamente y NO se han descontado tokens."
                    )
                    return@launch
                }

                // 2. Validar imagen
                _uiState.value = ProcessingUiState.ValidatingImage
                withContext(Dispatchers.IO) {
                    val inputStream = context.contentResolver.openInputStream(imageUri)
                        ?: throw IllegalArgumentException("No se pudo abrir la imagen capturada.")
                    inputStream.close()
                }

                // 3. Verificar tokens y solicitar reserva atómica de 5 tokens al backend seguro
                _uiState.value = ProcessingUiState.CheckingTokens
                val operationId = "op_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(8)}"

                val reserveResult = tokenBackendRepo.reserveAnalysisTokens(operationId)

                when (reserveResult) {
                    is ReserveTokensResult.InsufficientTokens -> {
                        _uiState.value = ProcessingUiState.Error("No posees los 5 tokens requeridos para autorizar este análisis médico. Saldo actual: ${reserveResult.currentBalance}")
                        return@launch
                    }
                    is ReserveTokensResult.Error -> {
                        // Corrección 6: Sin conexión o error con el backend, NO se permite realizar el análisis médico
                        _uiState.value = ProcessingUiState.Error("Sin conexión con el servidor de tokens. No se pudo autorizar el análisis médico: ${reserveResult.message}")
                        return@launch
                    }
                    is ReserveTokensResult.Success -> {
                        userRepository.saveTokenCount(reserveResult.newBalance)
                        tokenDeducted = true
                    }
                }

                // 4. Preprocesamiento e Inferencia TFLite
                _uiState.value = ProcessingUiState.Preprocessing
                _uiState.value = ProcessingUiState.RunningInference

                val inferenceResult = withContext(Dispatchers.IO) {
                    classifierHelper.runInferenceOnImage(imageUri)
                }

                _uiState.value = ProcessingUiState.SavingResult

                when (inferenceResult) {
                    is InferenceResult.Success -> {
                        val analysisResult = AnalysisResult(
                            imageUri = imageUri,
                            modelVersion = inferenceResult.modelVersion,
                            findings = inferenceResult.findings,
                            tokensConsumed = 5,
                            statusMessage = "Análisis completado exitosamente",
                            isModelAvailable = true
                        )
                        _uiState.value = ProcessingUiState.Completed(analysisResult)
                    }

                    is InferenceResult.ModelMissing -> {
                        _uiState.value = ProcessingUiState.ClinicalModelMissing(
                            imageUri = imageUri,
                            message = "Análisis clínico no disponible."
                        )
                    }

                    is InferenceResult.Error -> {
                        _uiState.value = ProcessingUiState.Error("Error en procesamiento: ${inferenceResult.message}")
                    }
                }
            } catch (e: Exception) {
                _uiState.value = ProcessingUiState.Error("Excepción en procesamiento: ${e.localizedMessage}")
            } finally {
                classifierHelper.close()
            }
        }
    }

    fun resetState() {
        tokenDeducted = false
        _uiState.value = ProcessingUiState.Idle
    }
}
