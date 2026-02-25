package com.trasparenza.app.ui.saved

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.trasparenza.app.data.local.SavedProductEntity
import com.trasparenza.app.data.repository.ProductRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Saved Products screen
 */
@HiltViewModel
class SavedProductsViewModel @Inject constructor(
    private val repository: ProductRepository
) : ViewModel() {
    
    /**
     * All saved products as StateFlow
     */
    val savedProducts: StateFlow<List<SavedProductEntity>> = repository
        .getAllSavedProducts()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    
    private val _selectedProduct = MutableStateFlow<SavedProductEntity?>(null)
    val selectedProduct: StateFlow<SavedProductEntity?> = _selectedProduct.asStateFlow()
    
    /**
     * Select a product to view details
     */
    fun selectProduct(product: SavedProductEntity) {
        _selectedProduct.value = product
    }
    
    /**
     * Dismiss product detail
     */
    fun dismissDetail() {
        _selectedProduct.value = null
    }
    
    /**
     * Delete a saved product
     */
    fun deleteProduct(product: SavedProductEntity) {
        viewModelScope.launch {
            repository.deleteProduct(product.id)
            _selectedProduct.value = null
        }
    }
}

