package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.RemarkConstants
import com.example.ui.components.CompletionBar
import com.example.ui.theme.*
import com.example.ui.viewmodel.TrackingViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeamLeadersSummaryScreen(
    viewModel: TrackingViewModel,
    onOpenDrawer: () -> Unit
) {
    val context = LocalContext.current
    val teamLeaders by viewModel.teamLeaders.collectAsState()
    val companies by viewModel.companies.collectAsState()
    val allStats by viewModel.allEmployeeStats.collectAsState()

    val tlRows = remember(teamLeaders, companies, allStats) {
        teamLeaders.map { tl ->
            val empStats = allStats.filter { it.employee.teamLeaderId == tl.id }
            val total = empStats.sumOf { it.total }
            val done = empStats.sumOf { it.done }
            val remaining = (total - done).coerceAtLeast(0)
            val completion = if (total > 0) (done.toFloat() / total) * 100f else 0f
            val interested = empStats.sumOf { it.breakdown[RemarkConstants.INTERESTED] ?: 0 }
            val noAnswer = empStats.sumOf { it.breakdown[RemarkConstants.NO_ANSWER] ?: 0 }
            val switchOff = empStats.sumOf { it.breakdown[RemarkConstants.SWITCH_OFF] ?: 0 }

            val topEmployee = empStats.maxByOrNull { it.done }?.employee?.name ?: "—"
            val companyName = companies.find { it.id == tl.companyId }?.name ?: "—"

            TlSummaryRowData(
                tl = tl,
                companyName = companyName,
                employeeCount = empStats.size,
                total = total,
                done = done,
                remaining = remaining,
                completion = completion,
                interested = interested,
                noAnswer = noAnswer,
                switchOff = switchOff,
                topEmployee = topEmployee
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
                        Text("TEAM LEADER SUMMARY", color = BrandGreenPrimary, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        Text("Team Roll-Up & Performance", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }
        }

        items(tlRows) { row ->
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = BrandNavySurface),
                border = CardDefaults.outlinedCardBorder().copy(brush = Brush.verticalGradient(listOf(Color(0x33FFFFFF), Color(0x11FFFFFF))))
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(row.tl.name, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 15.sp)
                            Text(row.companyName, fontSize = 11.sp, color = BrandTextSecondary)
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Surface(
                                color = BrandGreenPrimary.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    "Top Rep: ${row.topEmployee}",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    fontSize = 11.sp,
                                    color = BrandGreenPrimary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            IconButton(
                                onClick = {
                                    Toast.makeText(context, "Exported PDF report for ${row.tl.name}", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.Download, contentDescription = "PDF", tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }
                    }

                    CompletionBar(pct = row.completion, height = 7)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Reps: ${row.employeeCount} · Done: ${row.done}/${row.total} · Rem: ${row.remaining}", fontSize = 11.sp, color = BrandTextSecondary)
                        Text("${String.format("%.1f", row.completion)}%", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        RowMetricTag("Interested", "${row.interested}", StatusSuccess, Modifier.weight(1f))
                        RowMetricTag("No Answer", "${row.noAnswer}", StatusWarning, Modifier.weight(1f))
                        RowMetricTag("Switch Off", "${row.switchOff}", StatusDanger, Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

data class TlSummaryRowData(
    val tl: com.example.data.model.TeamLeader,
    val companyName: String,
    val employeeCount: Int,
    val total: Int,
    val done: Int,
    val remaining: Int,
    val completion: Float,
    val interested: Int,
    val noAnswer: Int,
    val switchOff: Int,
    val topEmployee: String
)
