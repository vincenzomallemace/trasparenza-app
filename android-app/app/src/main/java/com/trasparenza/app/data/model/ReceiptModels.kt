package com.trasparenza.app.data.model

import com.google.gson.annotations.SerializedName

// ─── Receipt OCR result ───────────────────────────────────────────────────────

data class ReceiptData(
    @SerializedName("storeName") val storeName: String? = null,
    @SerializedName("storeAddress") val storeAddress: String? = null,
    @SerializedName("receiptDate") val receiptDate: String? = null,
    @SerializedName("receiptTime") val receiptTime: String? = null,
    @SerializedName("receiptNumber") val receiptNumber: String? = null,
    @SerializedName("totalAmount") val totalAmount: Double? = null,
    @SerializedName("currency") val currency: String = "EUR",
    @SerializedName("products") val products: List<ReceiptProduct> = emptyList(),
    @SerializedName("paymentMethod") val paymentMethod: String? = null,
    @SerializedName("vatNumber") val vatNumber: String? = null,
    @SerializedName("confidence") val confidence: Double = 0.0,
)

data class ReceiptProduct(
    @SerializedName("name") val name: String = "",
    @SerializedName("brand") val brand: String? = null,
    @SerializedName("category") val category: String? = null,
    @SerializedName("quantity") val quantity: Double = 1.0,
    @SerializedName("unit") val unit: String = "pz",
    @SerializedName("pricePerUnit") val pricePerUnit: Double? = null,
    @SerializedName("totalPrice") val totalPrice: Double? = null,
    @SerializedName("barcode") val barcode: String? = null,
    @SerializedName("isOrganic") val isOrganic: Boolean = false,
    @SerializedName("notes") val notes: String? = null,
)

// ─── Sustainability Report ────────────────────────────────────────────────────

data class SustainabilityReport(
    // Receipt metadata
    @SerializedName("storeName") val storeName: String? = null,
    @SerializedName("storeAddress") val storeAddress: String? = null,
    @SerializedName("receiptDate") val receiptDate: String? = null,
    @SerializedName("receiptNumber") val receiptNumber: String? = null,
    @SerializedName("totalSpent") val totalSpent: Double? = null,
    @SerializedName("currency") val currency: String = "EUR",
    @SerializedName("totalProducts") val totalProducts: Int = 0,
    @SerializedName("analyzedProducts") val analyzedProducts: Int = 0,

    // Plastic
    @SerializedName("totalPlasticGrams") val totalPlasticGrams: Int = 0,
    @SerializedName("totalRecyclablePlasticGrams") val totalRecyclablePlasticGrams: Int = 0,
    @SerializedName("totalNonRecyclablePlasticGrams") val totalNonRecyclablePlasticGrams: Int = 0,
    @SerializedName("plasticRecyclabilityPercentage") val plasticRecyclabilityPercentage: Int = 0,

    // Other materials
    @SerializedName("totalCardboardGrams") val totalCardboardGrams: Int = 0,
    @SerializedName("totalGlassGrams") val totalGlassGrams: Int = 0,
    @SerializedName("totalAluminumGrams") val totalAluminumGrams: Int = 0,
    @SerializedName("totalOtherGrams") val totalOtherGrams: Int = 0,
    @SerializedName("totalPackagingGrams") val totalPackagingGrams: Int = 0,

    // CO2
    @SerializedName("totalCo2PackagingKg") val totalCo2PackagingKg: Double = 0.0,

    // Score
    @SerializedName("averageRecyclabilityScore") val averageRecyclabilityScore: Int = 0,
    @SerializedName("overallEcoGrade") val overallEcoGrade: EcoGrade? = null,

    // Rankings
    @SerializedName("pollutionRanking") val pollutionRanking: List<ProductPollutionRank> = emptyList(),
    @SerializedName("recyclabilityData") val recyclabilityData: List<ProductRecyclability> = emptyList(),

    // Charts
    @SerializedName("materialBreakdown") val materialBreakdown: List<MaterialBreakdownItem> = emptyList(),

    // Tips
    @SerializedName("ecoTips") val ecoTips: List<EcoTip> = emptyList(),

    // Products with packaging
    @SerializedName("products") val products: List<EnrichedReceiptProduct> = emptyList(),

    @SerializedName("generatedAt") val generatedAt: String? = null,
)

data class EcoGrade(
    @SerializedName("grade") val grade: String = "",
    @SerializedName("label") val label: String = "",
    @SerializedName("color") val color: String = "#000000",
)

data class ProductPollutionRank(
    @SerializedName("productName") val productName: String = "",
    @SerializedName("brand") val brand: String? = null,
    @SerializedName("totalPrice") val totalPrice: Double? = null,
    @SerializedName("primaryMaterial") val primaryMaterial: String = "",
    @SerializedName("nonRecyclablePlasticGrams") val nonRecyclablePlasticGrams: Int = 0,
    @SerializedName("totalPlasticGrams") val totalPlasticGrams: Int = 0,
    @SerializedName("co2PackagingKg") val co2PackagingKg: Double = 0.0,
    @SerializedName("recyclabilityScore") val recyclabilityScore: Int = 0,
    @SerializedName("pollutionScore") val pollutionScore: Int = 0,
    @SerializedName("environmentalNotes") val environmentalNotes: String? = null,
    @SerializedName("ecoAlternativeSuggestion") val ecoAlternativeSuggestion: String? = null,
)

data class ProductRecyclability(
    @SerializedName("productName") val productName: String = "",
    @SerializedName("brand") val brand: String? = null,
    @SerializedName("recyclabilityScore") val recyclabilityScore: Int = 0,
    @SerializedName("recyclabilityLabel") val recyclabilityLabel: String = "",
    @SerializedName("primaryMaterial") val primaryMaterial: String = "",
)

data class MaterialBreakdownItem(
    @SerializedName("material") val material: String = "",
    @SerializedName("grams") val grams: Int = 0,
    @SerializedName("percentage") val percentage: Int = 0,
    @SerializedName("color") val color: String = "#000000",
)

data class EcoTip(
    @SerializedName("icon") val icon: String = "",
    @SerializedName("title") val title: String = "",
    @SerializedName("description") val description: String = "",
    @SerializedName("priority") val priority: String = "bassa",
)

data class EnrichedReceiptProduct(
    @SerializedName("name") val name: String = "",
    @SerializedName("brand") val brand: String? = null,
    @SerializedName("category") val category: String? = null,
    @SerializedName("quantity") val quantity: Double = 1.0,
    @SerializedName("unit") val unit: String = "pz",
    @SerializedName("totalPrice") val totalPrice: Double? = null,
    @SerializedName("packaging") val packaging: PackagingInfo? = null,
)

data class PackagingInfo(
    @SerializedName("productName") val productName: String = "",
    @SerializedName("brand") val brand: String? = null,
    @SerializedName("primaryPackagingMaterial") val primaryPackagingMaterial: String = "",
    @SerializedName("packagingDescription") val packagingDescription: String? = null,
    @SerializedName("plasticGrams") val plasticGrams: Double = 0.0,
    @SerializedName("recyclablePlasticGrams") val recyclablePlasticGrams: Double = 0.0,
    @SerializedName("nonRecyclablePlasticGrams") val nonRecyclablePlasticGrams: Double = 0.0,
    @SerializedName("cardboardGrams") val cardboardGrams: Double = 0.0,
    @SerializedName("isFullyRecyclable") val isFullyRecyclable: Boolean = false,
    @SerializedName("recyclabilityScore") val recyclabilityScore: Int = 0,
    @SerializedName("recyclabilityLabel") val recyclabilityLabel: String = "",
    @SerializedName("recyclingInstructions") val recyclingInstructions: String? = null,
    @SerializedName("co2PackagingKg") val co2PackagingKg: Double = 0.0,
    @SerializedName("certifications") val certifications: List<String> = emptyList(),
    @SerializedName("environmentalNotes") val environmentalNotes: String? = null,
    @SerializedName("dataConfidence") val dataConfidence: Double = 0.0,
)

// ─── API request models ───────────────────────────────────────────────────────

data class AnalyzeReceiptRequest(
    val receiptBase64: String,
    val mimeType: String = "image/jpeg",
)

data class SustainabilityReportRequest(
    val receiptBase64: String? = null,
    val mimeType: String = "image/jpeg",
    val receiptData: ReceiptData? = null,
)
