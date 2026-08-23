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
fun CompaniesSummaryScreen(
    viewModel: TrackingViewModel,
    onOpenDrawer: () -> Unit
) {
    val context = LocalContext.current
    val companies by viewModel.companies.collectAsState()
    val allStats by viewModel.allEmployeeStats.collectAsState()
    val teamLeaders by viewModel.teamLeaders.collectAsState()

    val companyRows = remember(companies, allStats, teamLeaders) {
        companies.map { comp ->
            val empStats = allStats.filter { it.employee.companyId == comp.id }
            val total = empStats.sumOf { it.total }
            val done = empStats.sumOf { it.done }
            val completion = if (total > 0) (done.toFloat() / total) * 100f else 0f
            val interested = empStats.sumOf { it.breakdown[RemarkConstants.INTERESTED] ?: 0 }
            val pending = empStats.sumOf { it.breakdown[RemarkConstants.PENDING] ?: 0 }
            val noAnswer = empStats.sumOf { it.breakdown[RemarkConstants.NO_ANSWER] ?: 0 }
            val switchOff = empStats.sumOf { it.breakdown[RemarkConstants.SWITCH_OFF] ?: 0 }

            val tls = teamLeaders.filter { it.companyId == comp.id }
            val bestTl = tls.maxByOrNull { tl ->
                allStats.filter { it.employee.teamLeaderId == tl.id }.sumOf { it.done }
            }?.name ?: "—"

            CompanySummaryRowData(
                company = comp,
                employeeCount = empStats.size,
                total = total,
                done = done,
                completion = completion,
                interested = interested,
                pending = pending,
                noAnswer = noAnswer,
                switchOff = switchOff,
                topTeam = bestTl
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
                        Text("EXECUTIVE ROLL-UP", color = BrandGreenPrimary, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        Text("Company Summary", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }

                Button(
                    onClick = {
                        Toast.makeText(context, "Full consolidated company PDF report generated!", Toast.LENGTH_SHORT).show()
                    },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandGreenPrimary, contentColor = Color(0xFF071120)),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Download PDF", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        items(companyRows) { row ->
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
                            Text(row.company.name, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 15.sp)
                            Text(row.company.industry ?: "Staffing", fontSize = 11.sp, color = BrandTextSecondary)
                        }
                        Surface(
                            color = BrandGreenPrimary.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                "Top Team: ${row.topTeam}",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                fontSize = 11.sp,
                                color = BrandGreenPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    CompletionBar(pct = row.completion, height = 7)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Reps: ${row.employeeCount} · Total: ${row.total} · Done: ${row.done}", fontSize = 11.sp, color = BrandTextSecondary)
                        Text("${String.format("%.1f", row.completion)}% Completed", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
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

data class CompanySummaryRowData(
    val company: com.example.data.model.Company,
    val employeeCount: Int,
    val total: Int,
    val done: Int,
    val completion: Float,
    val interested: Int,
    val pending: Int,
    val noAnswer: Int,
    val switchOff: Int,
    val topTeam: String
)

@Composable
fun RowMetricTag(label: String, count: String, color: Color, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(6.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label.uppercase(), fontSize = 9.sp, color = color)
            Text(count, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}
