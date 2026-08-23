package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.formatTimeAgo
import com.example.ui.theme.*
import com.example.ui.viewmodel.PerformanceAuditItem
import com.example.ui.viewmodel.TrackingViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PerformanceAuditScreen(
    viewModel: TrackingViewModel,
    onNavigateEmployee: (String) -> Unit,
    onOpenDrawer: () -> Unit
) {
    val auditItems by viewModel.auditItems.collectAsState()
    var tab by remember { mutableStateOf("cheaters") } // "cheaters" | "genuine"

    val suspiciousCount = remember(auditItems) { auditItems.count { it.cheatScore >= 60f } }
    val watchCount = remember(auditItems) { auditItems.count { it.cheatScore in 35f..59.9f } }
    val cleanCount = remember(auditItems) { auditItems.count { it.cheatScore < 35f && it.done >= 5 } }
    val avgConnectRate = remember(auditItems) {
        val active = auditItems.filter { it.done >= 5 }
        if (active.isNotEmpty()) (active.sumOf { it.connectRate.toDouble() } / active.size).toFloat() * 100f else 0f
    }

    val sortedList = remember(auditItems, tab) {
        if (tab == "cheaters") {
            auditItems.filter { it.done >= 1 }.sortedByDescending { it.cheatScore }
        } else {
            auditItems.filter { it.done >= 1 }.sortedWith(
                compareByDescending<PerformanceAuditItem> { it.interestedRate }
                    .thenByDescending { it.connectRate }
                    .thenByDescending { it.done }
            )
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(BrandNavyBg)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 40.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Default.Menu, contentDescription = "Menu", tint = Color.White)
                    }
                    Column {
                        Text("INTELLIGENCE AUDIT", color = BrandGreenPrimary, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        Text("Performance & Integrity Audit", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }
        }

        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = BrandNavySurface),
                border = CardDefaults.outlinedCardBorder().copy(brush = Brush.verticalGradient(listOf(Color(0x33FFFFFF), Color(0x11FFFFFF))))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Detect Sheet-Gaming vs. Genuine Callers",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = Color.White)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Calculates heuristic cheat scores by weighing easily-faked remarks (No Answer, Switch Off, Not Reachable) vs genuine phone conversations (Interested, Not Interested).",
                        style = MaterialTheme.typography.bodySmall.copy(color = BrandTextSecondary, fontSize = 11.5.sp)
                    )
                }
            }
        }

        // 4 Summary KPI cards
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AuditMetricCard("High Risk (Gaming)", "$suspiciousCount", "Score ≥ 60", StatusDanger, Modifier.weight(1f))
                    AuditMetricCard("Needs Watch", "$watchCount", "Score 35–60", StatusWarning, Modifier.weight(1f))
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AuditMetricCard("Clean / Genuine", "$cleanCount", "Score < 35", StatusSuccess, Modifier.weight(1f))
                    AuditMetricCard("Avg Connect Rate", "${String.format("%.1f", avgConnectRate)}%", "Across active reps", BrandBlueSecondary, Modifier.weight(1f))
                }
            }
        }

        // Tab Row
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(BrandNavySurface)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                TabButton(
                    label = "Suspected Cheaters ($suspiciousCount)",
                    isSelected = tab == "cheaters",
                    onClick = { tab = "cheaters" },
                    modifier = Modifier.weight(1f)
                )
                TabButton(
                    label = "Genuine Performers ($cleanCount)",
                    isSelected = tab == "genuine",
                    onClick = { tab = "genuine" },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Audit Items List
        itemsIndexed(sortedList) { idx, r ->
            AuditRowCard(
                rank = idx + 1,
                item = r,
                onClick = { onNavigateEmployee(r.employee.id) }
            )
        }
    }
}

@Composable
fun AuditMetricCard(label: String, value: String, hint: String, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = BrandNavySurface),
        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.verticalGradient(listOf(color.copy(alpha = 0.5f), Color(0x11FFFFFF))))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(label.uppercase(), fontSize = 10.sp, color = BrandTextSecondary, fontWeight = FontWeight.Bold)
            Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = color)
            Text(hint, fontSize = 9.5.sp, color = BrandTextMuted)
        }
    }
}

@Composable
fun AuditRowCard(rank: Int, item: PerformanceAuditItem, onClick: () -> Unit) {
    val scoreTone = when {
        item.cheatScore >= 60f -> StatusDanger
        item.cheatScore >= 35f -> StatusWarning
        else -> StatusSuccess
    }

    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = BrandNavySurface),
        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.verticalGradient(listOf(Color(0x33FFFFFF), Color(0x11FFFFFF))))
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("#$rank", fontSize = 12.sp, color = BrandTextMuted, fontWeight = FontWeight.Bold)
                    Column {
                        Text(item.employee.name, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                        Text(
                            text = "${item.company?.name ?: "—"} · TL ${item.teamLeader?.name ?: "—"}",
                            fontSize = 11.sp,
                            color = BrandTextSecondary
                        )
                    }
                }

                Surface(
                    color = scoreTone.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(6.dp),
                    border = ButtonDefaults.outlinedButtonBorder().copy(brush = Brush.linearGradient(listOf(scoreTone, scoreTone.copy(alpha = 0.3f))))
                ) {
                    Text(
                        text = "Cheat Score: ${String.format("%.0f", item.cheatScore)}",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = scoreTone
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Done: ${item.done} · Interested: ${item.interested} · Not Int: ${item.notInterested}",
                    fontSize = 11.sp,
                    color = BrandTextSecondary
                )
                Text(
                    text = "Connect: ${String.format("%.0f", item.connectRate * 100)}%",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (item.connectRate < 0.15f) StatusDanger else StatusSuccess
                )
            }
        }
    }
}
