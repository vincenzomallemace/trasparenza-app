package com.trasparenza.app.ui.saved

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.trasparenza.app.data.local.SavedProductEntity
import com.trasparenza.app.data.model.CountryMapper
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedProductsScreen(viewModel: SavedProductsViewModel = hiltViewModel()) {
    val savedProducts by viewModel.savedProducts.collectAsState()
    val selectedProduct by viewModel.selectedProduct.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Prodotti Salvati") }) }
    ) { paddingValues ->
        if (savedProducts.isEmpty()) {
            EmptyState(modifier = Modifier.padding(paddingValues))
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(items = savedProducts, key = { it.id }) { product ->
                    ProductCard(product = product, onClick = { viewModel.selectProduct(product) })
                }
            }
        }
    }
    selectedProduct?.let { product ->
        ProductDetailDialog(
            product = product,
            onDismiss = { viewModel.dismissDetail() },
            onDelete = { viewModel.deleteProduct(product) }
        )
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Filled.Inventory2, null, Modifier.size(64.dp), MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
        Spacer(modifier = Modifier.height(16.dp))
        Text("Nessun prodotto salvato", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("Scansiona prodotti per salvarli qui", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
    }
}

@Composable
private fun ProductCard(product: SavedProductEntity, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(CountryMapper.getFlagEmoji(product.headquarterCountry), style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = product.productName ?: product.brandName ?: "Prodotto sconosciuto",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                if (product.brandName != null && product.productName != null) {
                    Text(product.brandName, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (product.producerName != null) {
                    Text(product.producerName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                }
            }
            Icon(Icons.Filled.ChevronRight, "Dettagli", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ProductDetailDialog(product: SavedProductEntity, onDismiss: () -> Unit, onDelete: () -> Unit) {
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.ITALY) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(product.productName ?: product.brandName ?: "Prodotto") },
        text = {
            Column {
                product.brandName?.let { DetailRow("Marca", it) }
                product.producerName?.let { DetailRow("Produttore", it) }
                product.groupName?.let { DetailRow("Gruppo", it) }
                product.headquarterCountry?.let {
                    DetailRow("Paese", "${CountryMapper.getFlagEmoji(it)} $it")
                }
                product.headquarterRegion?.let { DetailRow("Regione", it) }
                Spacer(modifier = Modifier.height(8.dp))
                Text("Salvato il ${dateFormat.format(Date(product.createdAt))}",
                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Chiudi") } },
        dismissButton = {
            TextButton(onClick = { showDeleteConfirm = true }, 
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) { 
                Text("Elimina") 
            }
        }
    )
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Conferma eliminazione") },
            text = { Text("Vuoi eliminare questo prodotto dai salvati?") },
            confirmButton = {
                TextButton(onClick = { showDeleteConfirm = false; onDelete() },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) { 
                    Text("Elimina") 
                }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("Annulla") } }
        )
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text("$label: ", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}
