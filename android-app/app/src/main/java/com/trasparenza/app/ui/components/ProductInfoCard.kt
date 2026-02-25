package com.trasparenza.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.trasparenza.app.data.model.CountryMapper
import com.trasparenza.app.data.model.ProductInfo

/**
 * Card displaying product information
 */
@Composable
fun ProductInfoCard(
    productInfo: ProductInfo,
    onSaveClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Product Name
            if (productInfo.productName != null) {
                Text(
                    text = productInfo.productName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
            
            // Brand
            if (productInfo.brandName != null) {
                InfoRow(
                    icon = Icons.Filled.LocalOffer,
                    label = "Marca",
                    value = productInfo.brandName
                )
            }
            
            // Producer
            if (productInfo.producerName != null) {
                InfoRow(
                    icon = Icons.Filled.Business,
                    label = "Produttore",
                    value = productInfo.producerName
                )
            }
            
            // Group
            if (productInfo.groupName != null) {
                InfoRow(
                    icon = Icons.Filled.AccountTree,
                    label = "Gruppo",
                    value = productInfo.groupName
                )
            }
            
            // Country with flag
            if (productInfo.headquarterCountry != null) {
                val flag = CountryMapper.getFlagEmoji(productInfo.headquarterCountry)
                InfoRow(
                    icon = Icons.Filled.Flag,
                    label = "Paese",
                    value = "$flag ${productInfo.headquarterCountry}"
                )
            }
            
            // Region
            if (productInfo.headquarterRegion != null) {
                InfoRow(
                    icon = Icons.Filled.LocationOn,
                    label = "Regione",
                    value = productInfo.headquarterRegion
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Confidence indicator
            ConfidenceIndicator(confidence = productInfo.confidence)
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Save button
            Button(
                onClick = onSaveClick,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Salva Prodotto")
            }
            
            // Source attribution
            Text(
                text = "Dati da Google",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
private fun InfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "$label: ",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun ConfidenceIndicator(confidence: Double) {
    val percentage = (confidence * 100).toInt()
    val color = when {
        percentage >= 70 -> MaterialTheme.colorScheme.primary
        percentage >= 40 -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.error
    }
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Affidabilità: $percentage%",
            style = MaterialTheme.typography.labelMedium
        )
        Spacer(modifier = Modifier.width(8.dp))
        LinearProgressIndicator(
            progress = { confidence.toFloat() },
            modifier = Modifier
                .weight(1f)
                .height(8.dp),
            color = color
        )
    }
}

