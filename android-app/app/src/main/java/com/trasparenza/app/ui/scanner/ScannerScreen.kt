package com.trasparenza.app.ui.scanner

import android.Manifest
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import com.trasparenza.app.ui.components.ProductInfoCard

/**
 * Scanner screen with camera preview and product analysis
 */
@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ScannerScreen(
    viewModel: ScannerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    var cameraController by remember { mutableStateOf<CameraController?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            cameraPermissionState.status.isGranted -> {
                // Camera preview
                CameraPreview(
                    modifier = Modifier.fillMaxSize(),
                    onControllerReady = { cameraController = it }
                )

                // Scan overlay
                ScanOverlay()

                // Top bar
                TopAppBar(
                    title = { Text("Trasparenza") },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = Color.White
                    ),
                    modifier = Modifier.align(Alignment.TopCenter)
                )

                // Bottom sheet with state
                BottomSheet(
                    uiState = uiState,
                    onScanClick = {
                        cameraController?.captureImage(
                            onImageCaptured = { bytes ->
                                viewModel.analyzeImage(bytes)
                            },
                            onError = { /* Handle error */ }
                        )
                    },
                    onSaveClick = { viewModel.saveProduct() },
                    onRetryClick = { viewModel.retry() },
                    onDismiss = { viewModel.resetState() },
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }

            cameraPermissionState.status.shouldShowRationale -> {
                PermissionRationale(
                    onRequestPermission = { cameraPermissionState.launchPermissionRequest() }
                )
            }

            else -> {
                LaunchedEffect(Unit) {
                    cameraPermissionState.launchPermissionRequest()
                }
                PermissionDenied()
            }
        }
    }
}

@Composable
private fun ScanOverlay() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // Semi-transparent overlay with hole
        Box(
            modifier = Modifier
                .size(280.dp)
                .border(
                    width = 3.dp,
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(16.dp)
                )
        )

        // Corner indicators
        Box(
            modifier = Modifier
                .size(280.dp)
                .padding(8.dp)
        ) {
            // Corners would go here (simplified for MVP)
        }
    }
}

@Composable
private fun BottomSheet(
    uiState: ScannerUiState,
    onScanClick: () -> Unit,
    onSaveClick: () -> Unit,
    onRetryClick: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Handle
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .height(4.dp)
                    .background(
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        RoundedCornerShape(2.dp)
                    )
            )

            Spacer(modifier = Modifier.height(16.dp))

            when (uiState) {
                is ScannerUiState.Idle -> IdleContent(onScanClick)
                is ScannerUiState.Loading -> LoadingContent()
                is ScannerUiState.Success -> SuccessContent(uiState.productInfo, onSaveClick)
                is ScannerUiState.Error -> ErrorContent(uiState.message, onRetryClick)
                is ScannerUiState.Saved -> SavedContent(onDismiss)
            }
        }
    }
}

@Composable
private fun PermissionRationale(onRequestPermission: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Filled.CameraAlt,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Permesso fotocamera necessario",
            style = MaterialTheme.typography.titleLarge
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Per scansionare i prodotti è necessario accedere alla fotocamera.",
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onRequestPermission) {
            Text("Concedi permesso")
        }
    }
}

@Composable
private fun PermissionDenied() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Filled.CameraAlt,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Permesso negato",
            style = MaterialTheme.typography.titleLarge
        )
        Text(
            text = "Vai nelle impostazioni per concedere l'accesso alla fotocamera.",
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun IdleContent(onScanClick: () -> Unit) {
    Text(
        text = "Inquadra un prodotto per iniziare",
        style = MaterialTheme.typography.bodyLarge,
        textAlign = TextAlign.Center
    )
    Spacer(modifier = Modifier.height(16.dp))
    FloatingActionButton(
        onClick = onScanClick,
        containerColor = MaterialTheme.colorScheme.primary
    ) {
        Icon(Icons.Filled.CameraAlt, contentDescription = "Scansiona")
    }
}

@Composable
private fun LoadingContent() {
    CircularProgressIndicator()
    Spacer(modifier = Modifier.height(16.dp))
    Text("Riconoscimento in corso…", style = MaterialTheme.typography.bodyLarge)
}

@Composable
private fun SuccessContent(productInfo: com.trasparenza.app.data.model.ProductInfo, onSaveClick: () -> Unit) {
    ProductInfoCard(productInfo = productInfo, onSaveClick = onSaveClick)
}

@Composable
private fun ErrorContent(message: String, onRetryClick: () -> Unit) {
    Icon(Icons.Filled.Error, contentDescription = null, tint = MaterialTheme.colorScheme.error)
    Text(text = message, color = MaterialTheme.colorScheme.error)
    Spacer(modifier = Modifier.height(8.dp))
    Button(onClick = onRetryClick) { Text("Riprova") }
}

@Composable
private fun SavedContent(onDismiss: () -> Unit) {
    Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(48.dp))
    Text("Prodotto salvato!", style = MaterialTheme.typography.titleMedium)
    Spacer(modifier = Modifier.height(8.dp))
    TextButton(onClick = onDismiss) { Text("Scansiona altro") }
}

