package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.TrackingViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeamLeaderDashboardScreen(
    viewModel: TrackingViewModel,
    onNavigateToEmployeeDetail: (String) -> Unit,
    onLogout: () -> Unit
) {
    val userSession by viewModel.userSession.collectAsState()
    val teamLeaders by viewModel.teamLeaders.collectAsState()
    val departments by viewModel.departments.collectAsState()
    val allStats by viewModel.allEmployeeStats.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val leads by viewModel.leads.collectAsState()

    val currentTl = remember(userSession, teamLeaders) {
        teamLeaders.find { it.id == userSession.teamLeaderId }
    }
    val currentDept = remember(userSession, departments) {
        departments.find { it.id == userSession.departmentId }
    }

    // Filter employees belonging to this Team Leader
    val teamStats = remember(allStats, userSession) {
        allStats.filter { it.employee.teamLeaderId == userSession.teamLeaderId }
    }

    val teamLeads = remember(leads, userSession) {
        leads.filter { it.teamLeaderId == userSession.teamLeaderId }
    }

    val totalReps = teamStats.size
    val activeReps = teamStats.count { it.status == CallStatus.IN_CALL || it.status == CallStatus.IDLE }
    val totalDoneCalls = teamStats.sumOf { it.done }
    val totalRemaining = teamStats.sumOf { it.remaining }
    val interestedLeads = teamLeads.count { it.currentRemark.equals(RemarkConstants.INTERESTED, ignoreCase = true) }
    val callbackLeads = teamLeads.count { it.currentRemark.equals(RemarkConstants.CALLBACK, ignoreCase = true) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = userSession.userName.ifBlank { "Team Leader Dashboard" },
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Dept: ${currentDept?.name ?: "Telecalling"} • Supervised Team",
                            fontSize = 11.sp,
                            color = BrandBlueSecondary
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            val tlId = userSession.teamLeaderId
                            if (tlId != null) {
                                viewModel.syncTeamLeader(tlId)
                            } else {
                                viewModel.syncAll()
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sync,
                            contentDescription = "Sync",
                            tint = if (isSyncing) BrandGreenPrimary else BrandTextSecondary
                        )
                    }
                    IconButton(onClick = onLogout) {
                        Icon(Icons.Default.Logout, contentDescription = "Logout", tint = BrandTextSecondary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BrandNavySurface)
            )
        },
        containerColor = BrandNavyBg
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // SUPERVISOR KPI GRID
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = BrandNavySurface)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "TEAM PERFORMANCE TODAY",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = BrandBlueSecondary,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            KPIBox(title = "Total Reps", value = "$totalReps", sub = "$activeReps Active", color = Color.White, modifier = Modifier.weight(1f))
                            Spacer(modifier = Modifier.width(8.dp))
                            KPIBox(title = "Calls Done", value = "$totalDoneCalls", sub = "$totalRemaining Left", color = BrandGreenPrimary, modifier = Modifier.weight(1f))
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            KPIBox(title = "Interested", value = "$interestedLeads", sub = "High Potential", color = BrandGreenPrimary, modifier = Modifier.weight(1f))
                            Spacer(modifier = Modifier.width(8.dp))
                            KPIBox(title = "Callbacks", value = "$callbackLeads", sub = "Scheduled", color = BrandBlueSecondary, modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            // TEAM REPS ROSTER & PROGRESS
            item {
                Text(
                    text = "TEAM MEMBERS (${teamStats.size})",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = BrandTextSecondary,
                    letterSpacing = 1.sp
                )
            }

            if (teamStats.isEmpty()) {
                item {
                    Text("No telecallers assigned to your team yet.", color = BrandTextMuted, fontSize = 12.sp)
                }
            } else {
                items(teamStats) { stat ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToEmployeeDetail(stat.employee.id) },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = BrandNavySurface)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(
                                                when (stat.status) {
                                                    CallStatus.IN_CALL -> BrandGreenPrimary
                                                    CallStatus.IDLE -> Color(0xFFFFB020)
                                                    CallStatus.OFFLINE -> Color(0xFF718096)
                                                }
                                            )
                                    )
                                    Text(
                                        text = stat.employee.name,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        fontSize = 14.sp
                                    )
                                }

                                Surface(
                                    color = when (stat.status) {
                                        CallStatus.IN_CALL -> BrandGreenPrimary.copy(alpha = 0.15f)
                                        CallStatus.IDLE -> Color(0x22FFB020)
                                        CallStatus.OFFLINE -> Color(0x1AFFFFFF)
                                    },
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = stat.status.label.uppercase(),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = when (stat.status) {
                                            CallStatus.IN_CALL -> BrandGreenPrimary
                                            CallStatus.IDLE -> Color(0xFFFFB020)
                                            CallStatus.OFFLINE -> BrandTextMuted
                                        },
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Linear Progress bar
                            LinearProgressIndicator(
                                progress = { (stat.completion / 100f).coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(5.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = BrandGreenPrimary,
                                trackColor = Color(0x1AFFFFFF)
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Done: ${stat.done}/${stat.total} (${stat.completion.toInt()}%)",
                                    fontSize = 11.sp,
                                    color = BrandTextSecondary
                                )
                                Text(
                                    text = "Connected: ${stat.connectedPct.toInt()}%",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = BrandGreenPrimary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun KPIBox(title: String, value: String, sub: String, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0x0DFFFFFF))
            .padding(10.dp)
    ) {
        Column {
            Text(text = title, fontSize = 10.5.sp, color = BrandTextSecondary)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = color)
            Text(text = sub, fontSize = 9.5.sp, color = BrandTextMuted)
        }
    }
}
