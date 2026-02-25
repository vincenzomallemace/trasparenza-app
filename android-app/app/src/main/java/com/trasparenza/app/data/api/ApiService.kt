package com.trasparenza.app.data.api

import com.trasparenza.app.data.model.ProductInfo
import com.trasparenza.app.data.model.ReceiptData
import com.trasparenza.app.data.model.SustainabilityReport
import com.trasparenza.app.data.model.AnalyzeReceiptRequest
import com.trasparenza.app.data.model.SustainabilityReportRequest
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.*

/**
 * Retrofit API service for Trasparenza backend
 */
interface ApiService {

    // ─── Product image analysis ───────────────────────────────────────────────

    /**
     * Analyze a product image (multipart)
     */
    @Multipart
    @POST("analyze-image")
    suspend fun analyzeImage(
        @Part image: MultipartBody.Part
    ): Response<ProductInfo>

    /**
     * Analyze a product image (base64 JSON)
     */
    @POST("analyze-image")
    suspend fun analyzeImageBase64(
        @Body request: AnalyzeImageRequest
    ): Response<ProductInfo>

    // ─── Receipt analysis ─────────────────────────────────────────────────────

    /**
     * Extract products from a receipt image/PDF/video (multipart)
     */
    @Multipart
    @POST("analyze-receipt")
    suspend fun analyzeReceipt(
        @Part receipt: MultipartBody.Part
    ): Response<ReceiptData>

    /**
     * Extract products from a receipt (base64 JSON)
     */
    @POST("analyze-receipt")
    suspend fun analyzeReceiptBase64(
        @Body request: AnalyzeReceiptRequest
    ): Response<ReceiptData>

    // ─── Sustainability report ────────────────────────────────────────────────

    /**
     * Full pipeline: receipt OCR + packaging research + eco report (multipart)
     */
    @Multipart
    @POST("sustainability-report")
    suspend fun sustainabilityReport(
        @Part receipt: MultipartBody.Part
    ): Response<SustainabilityReport>

    /**
     * Full pipeline: receipt OCR + packaging research + eco report (base64 JSON)
     */
    @POST("sustainability-report")
    suspend fun sustainabilityReportBase64(
        @Body request: SustainabilityReportRequest
    ): Response<SustainabilityReport>

    // ─── Health check ─────────────────────────────────────────────────────────

    @GET("../health")
    suspend fun healthCheck(): Response<HealthResponse>
}

/**
 * Request body for base64 image analysis
 */
data class AnalyzeImageRequest(
    val imageBase64: String,
    val language: String = "it"
)

/**
 * Health check response
 */
data class HealthResponse(
    val status: String,
    val timestamp: String,
    val version: String
)

