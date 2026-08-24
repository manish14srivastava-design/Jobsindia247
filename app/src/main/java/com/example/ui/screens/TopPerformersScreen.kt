package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
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
import com.example.ui.components.CompletionBar
import com.example.ui.theme.*
import com.example.ui.viewmodel.TrackingViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopPerformersScreen(
    viewModel: TrackingViewModel,
    onNavigateEmployee: (String) -> Unit,
    onOpenDrawer: () -> Unit
) {
    val allStats by viewModel.allEmployeeStats.collectAsState()
    val teamLeaders by viewModel.teamLeaders.collectAsState()
    val companies by viewModel.companies.collectAsState()

    val topEmployees = remember(allStats) {
        allStats.sortedByDescending { it.done }.take(10)
    }

    val topTeams = remember(teamLeaders, allStats, companies) {
        teamLeaders.map { tl ->
            val empInTeam = allStats.filter { it.employee.teamLeaderId == tl.id }
            val done = empInTeam.sumOf { it.done }
            val compName = companies.find { it.id == tl.companyId }?.name ?: "—"
            Triple(tl.name, compName, done)
        }.sortedByDescending { it.third }.take(10)
    }

    val topCompanies = remember(companies, allStats) {
        companies.map { comp ->
            val empInComp = allStats.filter { it.employee.companyId == comp.id }
            val done = empInComp.sumOf { it.done }
            Triple(comp.name, comp.industry ?: "Staffing", done)
        }.sortedByDescending { it.third }
    }

    var selectedTab by remember { mutableStateOf("employees") } // "employees" | "teams" | "companies"

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
                        Text("LEADERBOARDS", color = BrandGreenPrimary, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        Text("Top Performers", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }
        }

        // Leaderboard Category Tabs
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
                    label = "Top Reps (10)",
                    isSelected = selectedTab == "employees",
                    onClick = { selectedTab = "employees" },
                    modifier = Modifier.weight(1f)
                )
                TabButton(
                    label = "Top Teams (10)",
                    isSelected = selectedTab == "teams",
                    onClick = { selectedTab = "teams" },
                    modifier = Modifier.weight(1f)
                )
                TabButton(
                    label = "Top Companies",
                    isSelected = selectedTab == "companies",
                    onClick = { selectedTab = "companies" },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        when (selectedTab) {
            "employees" -> {
                if (topEmployees.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = BrandNavySurface)
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp).fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Default.Leaderboard, contentDescription = null, tint = BrandTextSecondary, modifier = Modifier.size(36.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("No Live Calling Records Yet", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text(
                                    "Rankings are dynamically calculated from real employee calls logged in Google Sheets and Firestore.",
                                    color = BrandTextSecondary,
                                    fontSize = 11.5.sp,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }
                } else {
                    itemsIndexed(topEmployees) { idx, stat ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onNavigateEmployee(stat.employee.id) },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = BrandNavySurface),
                            border = CardDefaults.outlinedCardBorder().copy(brush = Brush.verticalGradient(listOf(Color(0x33FFFFFF), Color(0x11FFFFFF))))
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                RankBadge(rank = idx + 1)
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(stat.employee.name, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                                    Text(
                                        "${stat.company?.name ?: "—"} · TL ${stat.teamLeader?.name ?: "—"}",
                                        fontSize = 11.sp,
                                        color = BrandTextSecondary
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    CompletionBar(pct = stat.completion, height = 5)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("${stat.done}", fontWeight = FontWeight.Bold, color = BrandGreenPrimary, fontSize = 18.sp)
                                    Text("calls", fontSize = 10.sp, color = BrandTextMuted)
                                }
                            }
                        }
                    }
                }
            }

            "teams" -> {
                if (topTeams.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = BrandNavySurface)
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp).fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Default.SupervisorAccount, contentDescription = null, tint = BrandTextSecondary, modifier = Modifier.size(36.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("No Live Team Records", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("Team statistics will appear here when real call records are synced.", color = BrandTextSecondary, fontSize = 11.5.sp)
                            }
                        }
                    }
                } else {
                    itemsIndexed(topTeams) { idx, (tlName, compName, done) ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = BrandNavySurface),
                            border = CardDefaults.outlinedCardBorder().copy(brush = Brush.verticalGradient(listOf(Color(0x33FFFFFF), Color(0x11FFFFFF))))
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                RankBadge(rank = idx + 1)
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(tlName, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                                    Text(compName, fontSize = 11.sp, color = BrandTextSecondary)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("$done", fontWeight = FontWeight.Bold, color = BrandBlueSecondary, fontSize = 18.sp)
                                    Text("total calls", fontSize = 10.sp, color = BrandTextMuted)
                                }
                            }
                        }
                    }
                }
            }

            "companies" -> {
                if (topCompanies.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = BrandNavySurface)
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp).fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Default.Business, contentDescription = null, tint = BrandTextSecondary, modifier = Modifier.size(36.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("No Live Company Records", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("Company aggregate performance will calculate automatically from live leads.", color = BrandTextSecondary, fontSize = 11.5.sp)
                            }
                        }
                    }
                } else {
                    itemsIndexed(topCompanies) { idx, (cName, ind, done) ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = BrandNavySurface),
                            border = CardDefaults.outlinedCardBorder().copy(brush = Brush.verticalGradient(listOf(Color(0x33FFFFFF), Color(0x11FFFFFF))))
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                RankBadge(rank = idx + 1)
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(cName, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                                    Text(ind, fontSize = 11.sp, color = BrandTextSecondary)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("$done", fontWeight = FontWeight.Bold, color = BrandGreenPrimary, fontSize = 18.sp)
                                    Text("total calls", fontSize = 10.sp, color = BrandTextMuted)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RankBadge(rank: Int) {
    val bg = when (rank) {
        1 -> StatusWarning
        2 -> Color(0xFFC0C0C0)
        3 -> Color(0xFFCD7F32)
        else -> BrandNavySurfaceLight
    }
    val textCol = if (rank <= 3) Color(0xFF071120) else BrandTextSecondary

    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(bg),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "#$rank",
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            color = textCol
        )
    }
}
