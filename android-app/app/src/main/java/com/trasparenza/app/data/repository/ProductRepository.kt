package com.trasparenza.app.data.repository

import android.util.Base64
import com.trasparenza.app.data.api.AnalyzeImageRequest
import com.trasparenza.app.data.api.ApiService
import com.trasparenza.app.data.local.ProductDao
import com.trasparenza.app.data.local.SavedProductEntity
import com.trasparenza.app.data.model.ProductInfo
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for product operations
 * Handles both API calls and local database operations
 */
@Singleton
class ProductRepository @Inject constructor(
    private val apiService: ApiService,
    private val productDao: ProductDao
) {
    /**
     * Analyze a product image
     * @param imageBytes JPEG image bytes
     * @return Result with ProductInfo or error
     */
    suspend fun analyzeImage(imageBytes: ByteArray): Result<ProductInfo> {
        return try {
            // Convert to base64
            val base64Image = Base64.encodeToString(imageBytes, Base64.NO_WRAP)
            val request = AnalyzeImageRequest(imageBase64 = base64Image)
            
            val response = apiService.analyzeImageBase64(request)
            
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorMsg = response.errorBody()?.string() ?: "Unknown error"
                Result.failure(Exception("API Error: ${response.code()} - $errorMsg"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Save a product to local database
     * @param productInfo Product info from API
     * @param thumbnailPath Optional local path for thumbnail
     * @return ID of saved product
     */
    suspend fun saveProduct(
        productInfo: ProductInfo,
        thumbnailPath: String? = null
    ): Long {
        val entity = SavedProductEntity.fromProductInfo(productInfo, thumbnailPath)
        return productDao.insertProduct(entity)
    }
    
    /**
     * Get all saved products as Flow
     */
    fun getAllSavedProducts(): Flow<List<SavedProductEntity>> {
        return productDao.getAllProducts()
    }
    
    /**
     * Get a saved product by ID
     */
    suspend fun getSavedProduct(id: Long): SavedProductEntity? {
        return productDao.getProductById(id)
    }
    
    /**
     * Delete a saved product
     */
    suspend fun deleteProduct(id: Long) {
        productDao.deleteProductById(id)
    }
    
    /**
     * Search saved products
     */
    fun searchProducts(query: String): Flow<List<SavedProductEntity>> {
        return productDao.searchProducts(query)
    }
    
    /**
     * Check if backend is healthy
     */
    suspend fun checkHealth(): Boolean {
        return try {
            val response = apiService.healthCheck()
            response.isSuccessful && response.body()?.status == "ok"
        } catch (e: Exception) {
            false
        }
    }
}

