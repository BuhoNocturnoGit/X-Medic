package com.example.xmedic_v100.ui.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.xmedic_v100.data.model.AnalysisResult
import com.example.xmedic_v100.data.repository.UserRepository
import com.example.xmedic_v100.utils.ImageClassifierHelper
import com.example.xmedic_v100.utils.InferenceResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface ProcessingUiState {
    object Idle : ProcessingUiState
    object ValidatingImage : ProcessingUiState
    object CheckingTokens : ProcessingUiState
    object Preprocessing : ProcessingUiState
    object RunningInference : ProcessingUiState
    object SavingResult : ProcessingUiState
    data class Completed(val result: AnalysisResult) : ProcessingUiState
    data class Error(val message: String) : ProcessingUiState
}

class ProcessingViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<ProcessingUiState>(ProcessingUiState.Idle)
    val uiState: StateFlow<ProcessingUiState> = _uiState.asStateFlow()

    private var tokenDeducted = false

    fun processImage(context: Context, imageUri: Uri) {
        if (_uiState.value !is ProcessingUiState.Idle) return // Prevenir ejecuciones múltiples simultáneas

        val userRepository = UserRepository(context)
        val classifierHelper = ImageClassifierHelper(context)

        viewModelScope.launch {
            try {
                // 1. Validar imagen
                _uiState.value = ProcessingUiState.ValidatingImage
                withContext(Dispatchers.IO) {
                    val inputStream = context.contentResolver.openInputStream(imageUri)
                    if (inputStream == null) {
                        throw IllegalArgumentException("No se pudo abrir la imagen capturada.")
                    }
                    inputStream.close()
                }

                // 2. Verificar tokens reales
                _uiState.value = ProcessingUiState.CheckingTokens
                val currentTokens = userRepository.getTokenCount()
                if (currentTokens <= 0) {
                    _uiState.value = ProcessingUiState.Error("No tienes tokens suficientes para realizar el análisis.")
                    return@launch
                }

                // 3. Descontar exactamente 1 token transaccionalmente
                if (!tokenDeducted) {
                    userRepository.saveTokenCount(currentTokens - 1)
                    tokenDeducted = true
                }

                // 4. Preprocesamiento e Inferencia TFLite offline en segundo plano
                _uiState.value = ProcessingUiState.Preprocessing
                _uiState.value = ProcessingUiState.RunningInference

                val inferenceResult = withContext(Dispatchers.IO) {
                    classifierHelper.runInferenceOnImage(imageUri)
                }

                // 5. Interpretar resultado
                _uiState.value = ProcessingUiState.SavingResult

                when (inferenceResult) {
                    is InferenceResult.Success -> {
                        val analysisResult = AnalysisResult(
                            imageUri = imageUri,
                            modelVersion = inferenceResult.modelVersion,
                            findings = inferenceResult.findings,
                            tokensConsumed = 1,
                            statusMessage = "Análisis completado exitosamente",
                            isModelAvailable = true
                        )
                        _uiState.value = ProcessingUiState.Completed(analysisResult)
                    }

                    is InferenceResult.ModelMissing -> {
                        val analysisResult = AnalysisResult(
                            imageUri = imageUri,
                            modelVersion = "Sin modelo local",
                            findings = emptyList(),
                            tokensConsumed = 1,
                            statusMessage = inferenceResult.message,
                            isModelAvailable = false
                        )
                        _uiState.value = ProcessingUiState.Completed(analysisResult)
                    }

                    is InferenceResult.Error -> {
                        // Política de reembolso: si falla la inferencia, se devuelve el token
                        if (tokenDeducted) {
                            val restoredTokens = userRepository.getTokenCount() + 1
                            userRepository.saveTokenCount(restoredTokens)
                            tokenDeducted = false
                        }
                        _uiState.value = ProcessingUiState.Error("Error en procesamiento: ${inferenceResult.message}")
                    }
                }
            } catch (e: Exception) {
                // Política de reverso ante excepción técnica
                if (tokenDeducted) {
                    val restoredTokens = userRepository.getTokenCount() + 1
                    userRepository.saveTokenCount(restoredTokens)
                    tokenDeducted = false
                }
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
