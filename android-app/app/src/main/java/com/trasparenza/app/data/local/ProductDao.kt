package com.trasparenza.app.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for saved products
 */
@Dao
interface ProductDao {
    
    /**
     * Get all saved products, ordered by creation date (newest first)
     */
    @Query("SELECT * FROM saved_products ORDER BY createdAt DESC")
    fun getAllProducts(): Flow<List<SavedProductEntity>>
    
    /**
     * Get a single product by ID
     */
    @Query("SELECT * FROM saved_products WHERE id = :id")
    suspend fun getProductById(id: Long): SavedProductEntity?
    
    /**
     * Insert a new product
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: SavedProductEntity): Long
    
    /**
     * Delete a product
     */
    @Delete
    suspend fun deleteProduct(product: SavedProductEntity)
    
    /**
     * Delete a product by ID
     */
    @Query("DELETE FROM saved_products WHERE id = :id")
    suspend fun deleteProductById(id: Long)
    
    /**
     * Get products count
     */
    @Query("SELECT COUNT(*) FROM saved_products")
    suspend fun getProductsCount(): Int
    
    /**
     * Search products by name or brand
     */
    @Query("""
        SELECT * FROM saved_products 
        WHERE productName LIKE '%' || :query || '%' 
           OR brandName LIKE '%' || :query || '%'
           OR producerName LIKE '%' || :query || '%'
        ORDER BY createdAt DESC
    """)
    fun searchProducts(query: String): Flow<List<SavedProductEntity>>
}

