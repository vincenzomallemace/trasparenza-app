package com.trasparenza.app.data.model

import com.google.gson.annotations.SerializedName

/**
 * Product information returned from the API
 */
data class ProductInfo(
    @SerializedName("productId")
    val productId: String? = null,
    
    @SerializedName("productName")
    val productName: String? = null,
    
    @SerializedName("brandName")
    val brandName: String? = null,
    
    @SerializedName("producerName")
    val producerName: String? = null,
    
    @SerializedName("groupName")
    val groupName: String? = null,
    
    @SerializedName("headquarterCountry")
    val headquarterCountry: String? = null,
    
    @SerializedName("headquarterRegion")
    val headquarterRegion: String? = null,
    
    @SerializedName("sourceUrls")
    val sourceUrls: List<String> = emptyList(),
    
    @SerializedName("confidence")
    val confidence: Double = 0.0,
    
    @SerializedName("rawData")
    val rawData: RawData? = null
)

data class RawData(
    @SerializedName("labels")
    val labels: List<Label> = emptyList(),
    
    @SerializedName("detectedText")
    val detectedText: String? = null
)

data class Label(
    @SerializedName("description")
    val description: String,
    
    @SerializedName("score")
    val score: Double
)

/**
 * API Error response
 */
data class ApiError(
    @SerializedName("error")
    val error: ErrorDetail
)

data class ErrorDetail(
    @SerializedName("message")
    val message: String,
    
    @SerializedName("code")
    val code: String
)

/**
 * Country code mapping for flags
 */
object CountryMapper {
    private val countryToCode = mapOf(
        "italia" to "IT",
        "italy" to "IT",
        "svizzera" to "CH",
        "switzerland" to "CH",
        "francia" to "FR",
        "france" to "FR",
        "germania" to "DE",
        "germany" to "DE",
        "stati uniti" to "US",
        "united states" to "US",
        "spagna" to "ES",
        "spain" to "ES",
        "regno unito" to "GB",
        "united kingdom" to "GB",
        "paesi bassi" to "NL",
        "netherlands" to "NL",
        "belgio" to "BE",
        "belgium" to "BE",
        "austria" to "AT"
    )

    fun getCountryCode(country: String?): String? {
        if (country == null) return null
        return countryToCode[country.lowercase()] ?: country.take(2).uppercase()
    }
    
    fun getFlagEmoji(country: String?): String {
        val code = getCountryCode(country) ?: return "🏳️"
        return code.uppercase()
            .map { char -> Character.toChars(0x1F1E6 - 'A'.code + char.code).concatToString() }
            .joinToString("")
    }
}

