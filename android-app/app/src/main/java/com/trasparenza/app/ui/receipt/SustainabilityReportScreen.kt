package com.trasparenza.app.ui.receipt

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.trasparenza.app.data.model.*

/**
 * Screen showing the full sustainability report for a shopping receipt.
 * Includes Material Breakdown pie chart, recyclability bar chart, pollution ranking.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SustainabilityReportScreen(
    onBack: () -> Unit,
    viewModel: ReceiptViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("🌿 Rapporto Sostenibilità") },
                navigationIcon = {
                    IconButton(onClick = { viewModel.reset(); onBack() }) {
                        Icon(Icons.Filled.ArrowBack, "Indietro")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                ),
            )
        }
    ) { padding ->
        when (val state = uiState) {
            is ReceiptUiState.ReportReady -> {
                ReportContent(
                    report = state.report,
                    modifier = Modifier.padding(padding),
                )
            }
            else -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        CircularProgressIndicator()
                        Text("Caricamento rapporto…")
                    }
                }
            }
        }
    }
}

// ─── Report Content ───────────────────────────────────────────────────────────

@Composable
private fun ReportContent(report: SustainabilityReport, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Header card
        HeaderCard(report)

        // Eco grade
        if (report.overallEcoGrade != null) {
            EcoGradeCard(report.overallEcoGrade, report.averageRecyclabilityScore)
        }

        // Key stats
        KeyStatsRow(report)

        // Material breakdown pie chart
        if (report.materialBreakdown.isNotEmpty()) {
            SectionCard(title = "🥧 Composizione packaging") {
                PieChartWithLegend(items = report.materialBreakdown)
            }
        }

        // Recyclability bar chart per product
        if (report.recyclabilityData.isNotEmpty()) {
            SectionCard(title = "♻️ Riciclabilità per prodotto") {
                RecyclabilityBarChart(items = report.recyclabilityData)
            }
        }

        // Pollution ranking
        if (report.pollutionRanking.isNotEmpty()) {
            SectionCard(title = "🏭 Prodotti più inquinanti") {
                PollutionRankingList(items = report.pollutionRanking.take(5))
            }
        }

        // Eco tips
        if (report.ecoTips.isNotEmpty()) {
            SectionCard(title = "💡 Consigli") {
                report.ecoTips.forEach { tip -> EcoTipRow(tip) }
            }
        }

        // CO2 info
        if (report.totalCo2PackagingKg > 0) {
            Co2Card(report.totalCo2PackagingKg)
        }

        Spacer(Modifier.height(24.dp))
    }
}

// ─── Header Card ─────────────────────────────────────────────────────────────

@Composable
private fun HeaderCard(report: SustainabilityReport) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                "🏪 ${report.storeName ?: "Supermercato"}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            if (report.receiptDate != null) {
                Text("📅 ${report.receiptDate}", style = MaterialTheme.typography.bodySmall)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("🛒 ${report.analyzedProducts}/${report.totalProducts} prodotti analizzati")
                if (report.totalSpent != null) {
                    Text("💰 €${"%.2f".format(report.totalSpent)}")
                }
            }
        }
    }
}

// ─── Eco Grade Card ───────────────────────────────────────────────────────────

@Composable
private fun EcoGradeCard(grade: EcoGrade, score: Int) {
    val gradeColor = try { Color(android.graphics.Color.parseColor(grade.color)) }
    catch (_: Exception) { MaterialTheme.colorScheme.primary }

    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Grade circle
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(gradeColor),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    grade.grade,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                )
            }
            Column {
                Text("Grado Ecologico", style = MaterialTheme.typography.labelMedium)
                Text(grade.label, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(
                    "Riciclabilità media: $score/100",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

// ─── Key Stats Row ────────────────────────────────────────────────────────────

@Composable
private fun KeyStatsRow(report: SustainabilityReport) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        StatCard(
            modifier = Modifier.weight(1f),
            value = "${report.totalPlasticGrams}g",
            label = "Plastica totale",
            icon = "🧴",
            color = MaterialTheme.colorScheme.errorContainer,
        )
        StatCard(
            modifier = Modifier.weight(1f),
            value = "${report.plasticRecyclabilityPercentage}%",
            label = "Plastica riciclabile",
            icon = "♻️",
            color = MaterialTheme.colorScheme.tertiaryContainer,
        )
        StatCard(
            modifier = Modifier.weight(1f),
            value = "${report.totalCardboardGrams}g",
            label = "Carta/Cartone",
            icon = "📦",
            color = MaterialTheme.colorScheme.secondaryContainer,
        )
    }
}

@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    value: String,
    label: String,
    icon: String,
    color: Color,
) {
    Card(modifier = modifier, colors = CardDefaults.cardColors(containerColor = color)) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(icon, fontSize = 20.sp)
            Text(value, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.titleMedium)
            Text(label, style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center)
        }
    }
}

// ─── Section Card ─────────────────────────────────────────────────────────────

@Composable
private fun SectionCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            content()
        }
    }
}

// ─── Pie Chart ────────────────────────────────────────────────────────────────

@Composable
private fun PieChartWithLegend(items: List<MaterialBreakdownItem>) {
    var animated by remember { mutableStateOf(false) }
    val progress by animateFloatAsState(
        targetValue = if (animated) 1f else 0f,
        animationSpec = tween(durationMillis = 1000),
        label = "pie_anim",
    )
    LaunchedEffect(Unit) { animated = true }

    val colors = items.map {
        try { Color(android.graphics.Color.parseColor(it.color)) }
        catch (_: Exception) { Color.Gray }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Pie chart
        Canvas(modifier = Modifier.size(140.dp)) {
            var startAngle = -90f
            items.forEachIndexed { i, item ->
                val sweepAngle = 360f * (item.percentage / 100f) * progress
                drawArc(
                    color = colors[i],
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = true,
                    topLeft = Offset(size.width * 0.05f, size.height * 0.05f),
                    size = Size(size.width * 0.9f, size.height * 0.9f),
                )
                startAngle += sweepAngle
            }
            // Inner hole
            drawCircle(
                color = Color.White,
                radius = size.minDimension * 0.25f,
            )
        }

        // Legend
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items.forEachIndexed { i, item ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(colors[i], shape = RoundedCornerShape(2.dp))
                    )
                    Text(
                        "${item.material} (${item.percentage}%)",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}

// ─── Recyclability Bar Chart ──────────────────────────────────────────────────

@Composable
private fun RecyclabilityBarChart(items: List<ProductRecyclability>) {
    var animated by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { animated = true }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items.forEach { item ->
            val targetProgress = item.recyclabilityScore / 100f
            val animatedProgress by animateFloatAsState(
                targetValue = if (animated) targetProgress else 0f,
                animationSpec = tween(durationMillis = 800),
                label = "bar_${item.productName}",
            )
            val barColor = when {
                item.recyclabilityScore >= 70 -> Color(0xFF4CAF50)
                item.recyclabilityScore >= 40 -> Color(0xFFFF9800)
                else -> Color(0xFFF44336)
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        item.productName.take(30),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        "${item.recyclabilityScore}/100",
                        style = MaterialTheme.typography.labelSmall,
                        color = barColor,
                        fontWeight = FontWeight.Bold,
                    )
                }
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                    color = barColor,
                    trackColor = barColor.copy(alpha = 0.2f),
                )
            }
        }
    }
}

// ─── Pollution Ranking ────────────────────────────────────────────────────────

@Composable
private fun PollutionRankingList(items: List<ProductPollutionRank>) {
    items.forEachIndexed { index, item ->
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = when (index) {
                    0 -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f)
                    1 -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                    else -> MaterialTheme.colorScheme.surfaceVariant
                }
            ),
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Rank number
                Text(
                    "#${index + 1}",
                    fontWeight = FontWeight.ExtraBold,
                    color = if (index == 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(item.productName, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "Plastica non riciclabile: ${item.nonRecyclablePlasticGrams}g · Materiale: ${item.primaryMaterial}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (item.ecoAlternativeSuggestion != null) {
                        Text(
                            "💡 ${item.ecoAlternativeSuggestion}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }
    }
}

// ─── Eco Tip Row ─────────────────────────────────────────────────────────────

@Composable
private fun EcoTipRow(tip: EcoTip) {
    val priorityColor = when (tip.priority) {
        "alta" -> MaterialTheme.colorScheme.errorContainer
        "media" -> MaterialTheme.colorScheme.tertiaryContainer
        else -> MaterialTheme.colorScheme.secondaryContainer
    }

    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = priorityColor)) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(tip.icon, fontSize = 20.sp)
            Column {
                Text(tip.title, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                Text(tip.description, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

// ─── CO2 Card ─────────────────────────────────────────────────────────────────

@Composable
private fun Co2Card(co2Kg: Double) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("🌍", fontSize = 32.sp)
            Column {
                Text("Emissioni CO₂ packaging stimata", style = MaterialTheme.typography.labelMedium)
                Text(
                    "${"%.2f".format(co2Kg)} kg CO₂",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    "Equivalente a ${"%.1f".format(co2Kg * 4)} km in auto",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}
