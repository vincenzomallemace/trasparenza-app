package com.trasparenza.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.trasparenza.app.data.model.ProductInfo

/**
 * Room Entity for saved products
 */
@Entity(tableName = "saved_products")
data class SavedProductEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    val productName: String?,
    val brandName: String?,
    val producerName: String?,
    val groupName: String?,
    val headquarterCountry: String?,
    val headquarterRegion: String?,
    val sourceUrls: String?, // JSON array as string
    val confidence: Double,
    val imageThumbnailPath: String?, // Local path to saved thumbnail
    val createdAt: Long = System.currentTimeMillis()
) {
    companion object {
        /**
         * Create entity from API ProductInfo
         */
        fun fromProductInfo(
            productInfo: ProductInfo,
            thumbnailPath: String? = null
        ): SavedProductEntity {
            return SavedProductEntity(
                productName = productInfo.productName,
                brandName = productInfo.brandName,
                producerName = productInfo.producerName,
                groupName = productInfo.groupName,
                headquarterCountry = productInfo.headquarterCountry,
                headquarterRegion = productInfo.headquarterRegion,
                sourceUrls = productInfo.sourceUrls.joinToString(","),
                confidence = productInfo.confidence,
                imageThumbnailPath = thumbnailPath
            )
        }
    }
    
    /**
     * Get source URLs as list
     */
    fun getSourceUrlsList(): List<String> {
        return sourceUrls?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
    }
}

