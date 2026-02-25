package com.trasparenza.app.ui.scanner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trasparenza.app.data.model.ProductInfo
import com.trasparenza.app.data.repository.ProductRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UI State for Scanner screen
 */
sealed class ScannerUiState {
    object Idle : ScannerUiState()
    object Loading : ScannerUiState()
    data class Success(val productInfo: ProductInfo) : ScannerUiState()
    data class Error(val message: String) : ScannerUiState()
    object Saved : ScannerUiState()
}

/**
 * ViewModel for Scanner screen
 * Handles image capture, analysis, and product saving
 */
@HiltViewModel
class ScannerViewModel @Inject constructor(
    private val repository: ProductRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<ScannerUiState>(ScannerUiState.Idle)
    val uiState: StateFlow<ScannerUiState> = _uiState.asStateFlow()
    
    private var lastAnalyzedProduct: ProductInfo? = null
    private var lastImageBytes: ByteArray? = null
    
    /**
     * Analyze a captured image
     * @param imageBytes JPEG image bytes
     */
    fun analyzeImage(imageBytes: ByteArray) {
        viewModelScope.launch {
            _uiState.value = ScannerUiState.Loading
            lastImageBytes = imageBytes
            
            repository.analyzeImage(imageBytes)
                .onSuccess { productInfo ->
                    lastAnalyzedProduct = productInfo
                    _uiState.value = ScannerUiState.Success(productInfo)
                }
                .onFailure { error ->
                    _uiState.value = ScannerUiState.Error(
                        error.message ?: "Errore durante l'analisi"
                    )
                }
        }
    }
    
    /**
     * Save the last analyzed product
     */
    fun saveProduct() {
        val product = lastAnalyzedProduct ?: return
        
        viewModelScope.launch {
            try {
                repository.saveProduct(product)
                _uiState.value = ScannerUiState.Saved
                
                // Reset after a short delay
                kotlinx.coroutines.delay(2000)
                resetState()
            } catch (e: Exception) {
                _uiState.value = ScannerUiState.Error(
                    "Errore durante il salvataggio: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Reset to idle state
     */
    fun resetState() {
        _uiState.value = ScannerUiState.Idle
        lastAnalyzedProduct = null
        lastImageBytes = null
    }
    
    /**
     * Retry last analysis
     */
    fun retry() {
        lastImageBytes?.let { analyzeImage(it) }
            ?: run { _uiState.value = ScannerUiState.Idle }
    }
}

