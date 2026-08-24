package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CallStatus
import com.example.data.model.EmployeeStats
import com.example.data.model.RemarkConstants
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.TrackingViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: TrackingViewModel,
    onNavigateEmployee: (String) -> Unit,
    onOpenDrawer: () -> Unit
) {
    val companies by viewModel.companies.collectAsState()
    val teamLeaders by viewModel.teamLeaders.collectAsState()
    val employees by viewModel.employees.collectAsState()
    val employeeStats by viewModel.filteredEmployeeStats.collectAsState()
    val totals by viewModel.totals.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val syncProgressText by viewModel.syncProgressText.collectAsState()
    val syncReportSummary by viewModel.syncReportSummary.collectAsState()
    val lastSyncedAt by viewModel.lastSyncedAt.collectAsState()
    val selectedCompanyFilter by viewModel.selectedCompanyFilter.collectAsState()
    val selectedTlFilter by viewModel.selectedTlFilter.collectAsState()
    val selectedStatusFilter by viewModel.selectedStatusFilter.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    var showSyncSnack by remember { mutableStateOf(false) }
    var syncMessage by remember { mutableStateOf("") }

    val topEmployees = remember(employeeStats) {
        employeeStats.sortedByDescending { it.done }.take(5)
    }

    val companyChartData = remember(companies, employeeStats) {
        companies.map { comp ->
            val empInComp = employeeStats.filter { it.employee.companyId == comp.id }
            val done = empInComp.sumOf { it.done }
            val total = empInComp.sumOf { it.total }
            val remaining = (total - done).coerceAtLeast(0)
            Triple(comp.name, done, remaining)
        }
    }

    val overallRemarkBreakdown = remember(employeeStats) {
        val map = mutableMapOf<String, Int>()
        RemarkConstants.ALL_REMARKS.forEach { r ->
            map[r] = employeeStats.sumOf { it.breakdown[r] ?: 0 }
        }
        map
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(BrandNavyBg)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 32.dp)
    ) {
        // --- Header Card ---
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = BrandNavySurface),
                border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(Color(0x33FFFFFF), Color(0x11FFFFFF))))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
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
                                Text(
                                    text = "OPERATIONS · LIVE",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = BrandGreenPrimary,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    )
                                )
                                Text(
                                    text = "Telecaller Monitoring",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                            }
                        }

                        // Sync button
                        Button(
                            onClick = {
                                viewModel.syncAll { summary ->
                                    syncMessage = "Synced ${summary.rowsSynced} rows across ${summary.employeeTabsSynced} tabs live!"
                                    showSyncSnack = true
                                }
                            },
                            enabled = !isSyncing,
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = BrandNavySurfaceLight,
                                contentColor = Color.White
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                            border = ButtonDefaults.outlinedButtonBorder().copy(brush = Brush.linearGradient(listOf(BrandGreenPrimary.copy(alpha = 0.5f), BrandBlueSecondary.copy(alpha = 0.5f))))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Sync",
                                tint = BrandGreenPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isSyncing) "Syncing…" else "Sync sheets",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    if (isSyncing) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                color = BrandGreenPrimary,
                                strokeWidth = 2.dp
                            )
                            Text(
                                text = syncProgressText,
                                fontSize = 11.5.sp,
                                color = BrandGreenPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = BrandNavyBg,
                            shape = RoundedCornerShape(6.dp),
                            border = ButtonDefaults.outlinedButtonBorder().copy(brush = Brush.linearGradient(listOf(Color(0x22FFFFFF), Color(0x11FFFFFF))))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(if (lastSyncedAt != null) StatusSuccess else BrandTextSecondary)
                                )
                                Text(
                                    text = lastSyncedAt?.let { "Synced ${formatTimeAgo(it)}" } ?: "Syncing Google Sheets...",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, color = BrandTextSecondary)
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(StatusSuccess))
                            Text("Realtime Firebase DB", fontSize = 11.sp, color = BrandTextSecondary)
                        }
                    }

                    if (showSyncSnack && !isSyncing) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(syncMessage, color = BrandGreenPrimary, fontSize = 11.sp)
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = Color(0x1AFFFFFF))
                    Spacer(modifier = Modifier.height(10.dp))

                    // Company Filter Chips
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = selectedCompanyFilter == "all",
                            onClick = { viewModel.setCompanyFilter("all") },
                            label = { Text("All Companies (${companies.size})", fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = BrandGreenPrimary,
                                selectedLabelColor = Color(0xFF071120),
                                containerColor = BrandNavySurfaceLight,
                                labelColor = Color.White
                            )
                        )
                        companies.forEach { c ->
                            val empCount = employees.count { it.companyId == c.id }
                            FilterChip(
                                selected = selectedCompanyFilter == c.id,
                                onClick = { viewModel.setCompanyFilter(c.id) },
                                label = { Text("${c.name} ($empCount)", fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = BrandGreenPrimary,
                                    selectedLabelColor = Color(0xFF071120),
                                    containerColor = BrandNavySurfaceLight,
                                    labelColor = Color.White
                                )
                            )
                        }
                    }
                }
            }
        }

        // --- KPI Metrics Grid ---
        item {
            Text(
                text = "KEY PERFORMANCE INDICATORS",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = BrandTextSecondary,
                    letterSpacing = 0.8.sp
                )
            )
            Spacer(modifier = Modifier.height(8.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatCard(
                        label = "Calls Done",
                        value = "${totals["totalDone"] ?: 0}",
                        icon = Icons.Default.PhoneCallback,
                        tone = StatusSuccess,
                        hint = "${String.format("%.1f", totals["overallCompletion"] ?: 0f)}% overall",
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        label = "Connected",
                        value = "${totals["totalConnected"] ?: 0}",
                        icon = Icons.Default.PhoneInTalk,
                        tone = BrandBlueSecondary,
                        hint = "${String.format("%.1f", totals["connectedPct"] ?: 0f)}% picked up",
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatCard(
                        label = "Remaining",
                        value = "${totals["remaining"] ?: 0}",
                        icon = Icons.Default.PhoneMissed,
                        tone = StatusWarning,
                        hint = "Pending dialing",
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        label = "Total Phone Nos",
                        value = "${totals["totalNumbers"] ?: 0}",
                        icon = Icons.Default.ContactPhone,
                        tone = BrandTealTertiary,
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatCard(
                        label = "In Call",
                        value = "${totals["inCall"] ?: 0}",
                        icon = Icons.Default.SupportAgent,
                        tone = StatusSuccess,
                        hint = "< 2m active",
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        label = "Idle",
                        value = "${totals["idle"] ?: 0}",
                        icon = Icons.Default.Timer,
                        tone = StatusWarning,
                        hint = "2-10m ago",
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        label = "Offline",
                        value = "${totals["offline"] ?: 0}",
                        icon = Icons.Default.PowerSettingsNew,
                        tone = StatusDanger,
                        hint = "10m+ inactive",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // --- Top 5 Performers Strip ---
        if (topEmployees.isNotEmpty()) {
            item {
                Text(
                    text = "TOP PERFORMERS LEADERBOARD",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = BrandTextSecondary,
                        letterSpacing = 0.8.sp
                    )
                )
                Spacer(modifier = Modifier.height(8.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    itemsIndexed(topEmployees) { idx, s ->
                        Card(
                            modifier = Modifier
                                .width(220.dp)
                                .clickable { onNavigateEmployee(s.employee.id) },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = BrandNavySurface),
                            border = CardDefaults.outlinedCardBorder().copy(brush = Brush.verticalGradient(listOf(Color(0x33FFFFFF), Color(0x11FFFFFF))))
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (idx == 0) StatusWarning.copy(alpha = 0.2f) else BrandNavySurfaceLight),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "#${idx + 1}",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = if (idx == 0) StatusWarning else BrandGreenPrimary
                                        )
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = s.employee.name,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${s.company?.name ?: "—"} · ${s.done} calls",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontSize = 11.sp,
                                            color = BrandTextSecondary
                                        ),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- Visual Analytics Charts ---
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = "VISUAL OPERATIONS ANALYTICS",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = BrandTextSecondary,
                        letterSpacing = 0.8.sp
                    )
                )
            }
            Spacer(modifier = Modifier.height(8.dp))

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Company Performance
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = BrandNavySurface),
                    border = CardDefaults.outlinedCardBorder().copy(brush = Brush.verticalGradient(listOf(Color(0x33FFFFFF), Color(0x11FFFFFF))))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Company Performance",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(BrandGreenPrimary))
                                Text("Done", fontSize = 10.sp, color = BrandTextSecondary)
                                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(BrandBlueSecondary.copy(alpha = 0.6f)))
                                Text("Remaining", fontSize = 10.sp, color = BrandTextSecondary)
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        CompanyPerformanceChart(companyData = companyChartData)
                    }
                }

                // Remark Breakdown Donut
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = BrandNavySurface),
                    border = CardDefaults.outlinedCardBorder().copy(brush = Brush.verticalGradient(listOf(Color(0x33FFFFFF), Color(0x11FFFFFF))))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Remark Distribution (Live)",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        RemarkDistributionDonut(breakdown = overallRemarkBreakdown)
                    }
                }
            }
        }

        // --- Filter & Search Section ---
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = BrandNavySurface),
                border = CardDefaults.outlinedCardBorder().copy(brush = Brush.verticalGradient(listOf(Color(0x33FFFFFF), Color(0x11FFFFFF))))
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.setSearchQuery(it) },
                        placeholder = { Text("Search employee, TL, company…", fontSize = 13.sp, color = BrandTextMuted) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = BrandTextSecondary) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear", tint = BrandTextSecondary)
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = BrandNavyBg,
                            unfocusedContainerColor = BrandNavyBg,
                            focusedBorderColor = BrandGreenPrimary,
                            unfocusedBorderColor = Color(0x22FFFFFF),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        singleLine = true
                    )

                    // Dropdowns row: TL and Status
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // TL Filter Chip Row
                        val tlOptions = listOf("all" to "All Team Leaders") + teamLeaders.map { it.id to it.name }
                        var tlExpanded by remember { mutableStateOf(false) }

                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedButton(
                                onClick = { tlExpanded = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.outlinedButtonColors(containerColor = BrandNavyBg, contentColor = Color.White),
                                border = ButtonDefaults.outlinedButtonBorder().copy(brush = Brush.linearGradient(listOf(Color(0x33FFFFFF), Color(0x11FFFFFF))))
                            ) {
                                Text(
                                    text = tlOptions.find { it.first == selectedTlFilter }?.second ?: "TL Filter",
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(16.dp))
                            }

                            DropdownMenu(
                                expanded = tlExpanded,
                                onDismissRequest = { tlExpanded = false },
                                modifier = Modifier.background(BrandNavySurface)
                            ) {
                                tlOptions.forEach { (id, name) ->
                                    DropdownMenuItem(
                                        text = { Text(name, color = Color.White, fontSize = 12.sp) },
                                        onClick = {
                                            viewModel.setTlFilter(id)
                                            tlExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        // Status Filter
                        val statusOptions = listOf("all" to "All Statuses", "In Call" to "In Call", "Idle" to "Idle", "Offline" to "Offline")
                        var statusExpanded by remember { mutableStateOf(false) }

                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedButton(
                                onClick = { statusExpanded = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.outlinedButtonColors(containerColor = BrandNavyBg, contentColor = Color.White),
                                border = ButtonDefaults.outlinedButtonBorder().copy(brush = Brush.linearGradient(listOf(Color(0x33FFFFFF), Color(0x11FFFFFF))))
                            ) {
                                Text(
                                    text = statusOptions.find { it.first == selectedStatusFilter }?.second ?: "Status",
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(16.dp))
                            }

                            DropdownMenu(
                                expanded = statusExpanded,
                                onDismissRequest = { statusExpanded = false },
                                modifier = Modifier.background(BrandNavySurface)
                            ) {
                                statusOptions.forEach { (key, label) ->
                                    DropdownMenuItem(
                                        text = { Text(label, color = Color.White, fontSize = 12.sp) },
                                        onClick = {
                                            viewModel.setStatusFilter(key)
                                            statusExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- Live Employee Monitoring List ---
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "LIVE EMPLOYEE MONITORING (${employeeStats.size})",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        color = BrandTextSecondary,
                        letterSpacing = 0.8.sp
                    )
                )
                Text(
                    text = "Click to inspect",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, color = BrandGreenPrimary)
                )
            }
        }

        if (employeeStats.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = BrandNavySurface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = StatusPending, modifier = Modifier.size(32.dp))
                        Text("NO REAL DATA AVAILABLE", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                        Text("No active live records found. Google Sheet sync may be in progress or empty.", color = BrandTextMuted, fontSize = 12.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                        Button(
                            onClick = { viewModel.syncAll() },
                            colors = ButtonDefaults.buttonColors(containerColor = BrandGreenPrimary, contentColor = Color(0xFF071120)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Resync Google Sheets", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else {
            items(employeeStats) { stat ->
                EmployeeDashboardCard(
                    stats = stat,
                    onClick = { onNavigateEmployee(stat.employee.id) }
                )
            }
        }
    }
}

@Composable
fun EmployeeDashboardCard(
    stats: EmployeeStats,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = BrandNavySurface),
        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.verticalGradient(listOf(Color(0x33FFFFFF), Color(0x11FFFFFF))))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(BrandGreenPrimary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stats.employee.name.split(" ").map { it.take(1) }.take(2).joinToString(""),
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = BrandGreenPrimary
                            )
                        )
                    }
                    Column {
                        Text(
                            text = stats.employee.name,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        )
                        Text(
                            text = "${stats.company?.name ?: "—"} · TL ${stats.teamLeader?.name ?: "—"}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 11.sp,
                                color = BrandTextSecondary
                            )
                        )
                    }
                }
                StatusPill(status = stats.status)
            }

            Spacer(modifier = Modifier.height(10.dp))
            CompletionBar(pct = stats.completion, height = 6)

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Calls: ${stats.done}/${stats.total} · Connected: ${stats.connected}",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, color = BrandTextSecondary)
                )
                Text(
                    text = formatTimeAgo(stats.lastActivityAt),
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.5.sp, color = BrandTextMuted)
                )
            }

            // Remark badges row
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val interested = stats.breakdown[RemarkConstants.INTERESTED] ?: 0
                val noAnswer = stats.breakdown[RemarkConstants.NO_ANSWER] ?: 0
                val switchOff = stats.breakdown[RemarkConstants.SWITCH_OFF] ?: 0
                val pending = stats.breakdown[RemarkConstants.PENDING] ?: 0

                RemarkTag(label = "Interested", count = interested, tone = StatusSuccess)
                RemarkTag(label = "No Answer", count = noAnswer, tone = StatusWarning)
                RemarkTag(label = "Switch Off", count = switchOff, tone = StatusDanger)
                RemarkTag(label = "Pending", count = pending, tone = Color(0xFF64748B))
            }
        }
    }
}

@Composable
fun RemarkTag(label: String, count: Int, tone: Color) {
    Surface(
        color = tone.copy(alpha = 0.12f),
        shape = RoundedCornerShape(6.dp),
        border = ButtonDefaults.outlinedButtonBorder().copy(brush = Brush.linearGradient(listOf(tone.copy(alpha = 0.3f), tone.copy(alpha = 0.1f))))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(text = label, style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, color = tone))
            Text(text = count.toString(), style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White))
        }
    }
}
