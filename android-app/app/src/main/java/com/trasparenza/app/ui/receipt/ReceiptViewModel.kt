package com.trasparenza.app.ui.receipt

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Base64
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trasparenza.app.data.api.ApiService
import com.trasparenza.app.data.model.AnalyzeReceiptRequest
import com.trasparenza.app.data.model.ReceiptData
import com.trasparenza.app.data.model.SustainabilityReport
import com.trasparenza.app.data.model.SustainabilityReportRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.InputStream
import javax.inject.Inject

sealed class ReceiptUiState {
    object Idle : ReceiptUiState()
    data class LoadingOcr(val message: String = "Ssto leggendo lo scontrino…") : ReceiptUiState()
    data class ReceiptReady(val data: ReceiptData) : ReceiptUiState()
    data class LoadingReport(val message: String = "🔍 Ricercando informazioni packaging…") : ReceiptUiState()
    data class ReportReady(val report: SustainabilityReport) : ReceiptUiState()
    data class Error(val message: String) : ReceiptUiState()
}

@HiltViewModel
class ReceiptViewModel @Inject constructor(
    private val apiService: ApiService,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _uiState = MutableStateFlow<ReceiptUiState>(ReceiptUiState.Idle)
    val uiState: StateFlow<ReceiptUiState> = _uiState

    // ─── Step 1: OCR only ─────────────────────────────────────────────────────

    fun analyzeReceiptUri(uri: Uri) {
        viewModelScope.launch {
            _uiState.value = ReceiptUiState.LoadingOcr()
            try {
                val (base64, mimeType) = encodeUri(uri)
                val response = apiService.analyzeReceiptBase64(
                    AnalyzeReceiptRequest(receiptBase64 = base64, mimeType = mimeType)
                )
                if (response.isSuccessful && response.body() != null) {
                    _uiState.value = ReceiptUiState.ReceiptReady(response.body()!!)
                } else {
                    _uiState.value = ReceiptUiState.Error("Errore dal server: ${response.code()}")
                }
            } catch (e: Exception) {
                _uiState.value = ReceiptUiState.Error("Errore: ${e.localizedMessage}")
            }
        }
    }

    // ─── Step 2: Full pipeline (OCR + packaging + report) ────────────────────

    fun generateReportFromUri(uri: Uri) {
        viewModelScope.launch {
            _uiState.value = ReceiptUiState.LoadingOcr("📸 Lettura scontrino…")
            try {
                val (base64, mimeType) = encodeUri(uri)
                _uiState.value = ReceiptUiState.LoadingReport("🔍 Analisi packaging in corso…")
                val response = apiService.sustainabilityReportBase64(
                    SustainabilityReportRequest(receiptBase64 = base64, mimeType = mimeType)
                )
                if (response.isSuccessful && response.body() != null) {
                    _uiState.value = ReceiptUiState.ReportReady(response.body()!!)
                } else {
                    _uiState.value = ReceiptUiState.Error("Errore dal server: ${response.code()}")
                }
            } catch (e: Exception) {
                _uiState.value = ReceiptUiState.Error("Errore: ${e.localizedMessage}")
            }
        }
    }

    // ─── Step 2b: Report from already-parsed receipt ──────────────────────────

    fun generateReportFromReceiptData(data: ReceiptData) {
        viewModelScope.launch {
            _uiState.value = ReceiptUiState.LoadingReport("🔍 Analisi packaging in corso…")
            try {
                val response = apiService.sustainabilityReportBase64(
                    SustainabilityReportRequest(receiptData = data)
                )
                if (response.isSuccessful && response.body() != null) {
                    _uiState.value = ReceiptUiState.ReportReady(response.body()!!)
                } else {
                    _uiState.value = ReceiptUiState.Error("Errore dal server: ${response.code()}")
                }
            } catch (e: Exception) {
                _uiState.value = ReceiptUiState.Error("Errore: ${e.localizedMessage}")
            }
        }
    }

    fun reset() {
        _uiState.value = ReceiptUiState.Idle
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private fun encodeUri(uri: Uri): Pair<String, String> {
        val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"

        return when {
            mimeType.startsWith("video/") -> {
                // Extract middle frame from video
                val base64 = extractVideoFrame(uri)
                Pair(base64, "image/jpeg")
            }
            else -> {
                // Image or PDF: read raw bytes
                val inputStream: InputStream = context.contentResolver.openInputStream(uri)
                    ?: throw IllegalArgumentException("Cannot open URI: $uri")
                val bytes = inputStream.readBytes()
                inputStream.close()
                val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                Pair(base64, mimeType)
            }
        }
    }

    private fun extractVideoFrame(uri: Uri): String {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)
            val durationMs = retriever
                .extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull() ?: 0L
            // Pick frame at 30% of video (likely after opening)
            val timeUs = (durationMs * 300L).coerceAtLeast(0L)
            val bitmap = retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                ?: throw IllegalArgumentException("Cannot extract frame from video")
            val out = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
            Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
        } finally {
            retriever.release()
        }
    }
}
