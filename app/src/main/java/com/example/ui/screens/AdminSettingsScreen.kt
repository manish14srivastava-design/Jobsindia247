package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Department
import com.example.data.model.QuickRemark
import com.example.data.model.TeamLeader
import com.example.ui.theme.*
import com.example.ui.viewmodel.TrackingViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminSettingsScreen(
    viewModel: TrackingViewModel,
    onOpenDrawer: () -> Unit
) {
    val context = LocalContext.current
    val isOwnerUnlocked by viewModel.isOwnerUnlocked.collectAsState()
    val teamLeaders by viewModel.teamLeaders.collectAsState()
    val companies by viewModel.companies.collectAsState()
    val departments by viewModel.departments.collectAsState()
    val quickRemarks by viewModel.quickRemarks.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()

    val userSession by viewModel.userSession.collectAsState()
    val leads by viewModel.leads.collectAsState()
    val calls by viewModel.calls.collectAsState()
    val employees by viewModel.employees.collectAsState()
    val lastSyncedAt by viewModel.lastSyncedAt.collectAsState()
    val syncStatusInfo by viewModel.syncStatusInfo.collectAsState()
    val syncReportSummary by viewModel.syncReportSummary.collectAsState()
    val diagnosticResult by viewModel.diagnosticResult.collectAsState()
    val isTestingHealth by viewModel.isTestingHealth.collectAsState()

    var activeAdminTab by remember { mutableStateOf("SHEETS") } // SHEETS, REMARKS, DEPARTMENTS, DEBUG

    // Dialog States
    var showSheetDialog by remember { mutableStateOf<TeamLeader?>(null) }
    var isNewSheet by remember { mutableStateOf(false) }
    var draftSheetName by remember { mutableStateOf("") }
    var draftSheetCompanyId by remember { mutableStateOf("") }
    var draftSheetUrl by remember { mutableStateOf("") }
    var draftSheetSync by remember { mutableStateOf(true) }

    var showRemarkDialog by remember { mutableStateOf<QuickRemark?>(null) }
    var draftRemarkLabel by remember { mutableStateOf("") }
    var draftRemarkColor by remember { mutableStateOf("#10E57A") }
    var draftRemarkReqFollowup by remember { mutableStateOf(false) }

    var showDeptDialog by remember { mutableStateOf<Department?>(null) }
    var draftDeptName by remember { mutableStateOf("") }
    var draftDeptDesc by remember { mutableStateOf("") }

    // Owner Gate login dialog if locked
    if (!isOwnerUnlocked) {
        OwnerLoginDialog(
            onUnlock = { pwd -> viewModel.unlockOwner(pwd) }
        )
        return
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
                        Text("MASTER OPERATIONS CONTROL", color = BrandGreenPrimary, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        Text("Admin Settings", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(
                        onClick = { viewModel.lockOwner() },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Lock", fontSize = 11.sp)
                    }
                }
            }
        }

        // Admin Sub-tabs
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    "SHEETS" to "Sheets",
                    "REMARKS" to "Remarks",
                    "DEPARTMENTS" to "Departments",
                    "DEBUG" to "Debug / Live Session"
                ).forEach { (key, label) ->
                    val isSelected = activeAdminTab == key
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) BrandGreenPrimary else Color(0x14FFFFFF))
                            .clickable { activeAdminTab = key }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color(0xFF071120) else Color.White
                        )
                    }
                }
            }
        }

        // ================= SECTION 1: GOOGLE SHEETS & TEAM LEADERS =================
        if (activeAdminTab == "SHEETS") {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("CONNECTED TEAM SHEETS (${teamLeaders.size})", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Button(
                        onClick = {
                            isNewSheet = true
                            draftSheetName = ""
                            draftSheetCompanyId = companies.firstOrNull()?.id ?: ""
                            draftSheetUrl = ""
                            draftSheetSync = true
                            showSheetDialog = TeamLeader()
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandGreenPrimary, contentColor = Color(0xFF071120))
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Connect Sheet", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Summary 4 boxes
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AdminMetricBox("Companies", "${companies.size}", Icons.Default.Business, Modifier.weight(1f))
                    AdminMetricBox("Team Leaders", "${teamLeaders.size}", Icons.Default.SupervisorAccount, Modifier.weight(1f))
                    AdminMetricBox("Sheets", "${teamLeaders.count { !it.sheetUrl.isNullOrBlank() }}", Icons.Default.Link, Modifier.weight(1f))
                    AdminMetricBox("Sync Active", "${teamLeaders.count { it.syncEnabled }}", Icons.Default.Sync, Modifier.weight(1f))
                }
            }

            items(teamLeaders) { tl ->
                val comp = companies.find { it.id == tl.companyId }
                Card(
                    shape = RoundedCornerShape(12.dp),
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
                                Text(tl.name, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 15.sp)
                                Text(comp?.name ?: "—", fontSize = 11.sp, color = BrandTextSecondary)
                            }

                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Switch(
                                    checked = tl.syncEnabled,
                                    onCheckedChange = { viewModel.toggleTeamLeaderSync(tl.id, it) },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = BrandGreenPrimary,
                                        uncheckedThumbColor = Color.Gray,
                                        uncheckedTrackColor = BrandNavyBg
                                    )
                                )
                            }
                        }

                        // Sheet URL
                        Surface(
                            color = BrandNavySurfaceLight,
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.Link, contentDescription = null, tint = BrandGreenPrimary, modifier = Modifier.size(14.dp))
                                Text(
                                    text = tl.sheetUrl ?: "No URL connected",
                                    fontSize = 11.sp,
                                    color = BrandTextSecondary,
                                    maxLines = 1
                                )
                            }
                        }

                        // Action buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = {
                                    viewModel.syncTeamLeader(tl.id) { success, tabs, rows, error ->
                                        val msg = if (success) "Synced $tabs tabs ($rows rows) for ${tl.name}" else (error ?: "Sync failed")
                                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                    }
                                },
                                shape = RoundedCornerShape(6.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = "Sync", modifier = Modifier.size(12.dp), tint = BrandGreenPrimary)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Sync", fontSize = 11.sp)
                            }

                            Spacer(modifier = Modifier.width(6.dp))

                            IconButton(
                                onClick = {
                                    isNewSheet = false
                                    draftSheetName = tl.name
                                    draftSheetCompanyId = tl.companyId
                                    draftSheetUrl = tl.sheetUrl ?: ""
                                    draftSheetSync = tl.syncEnabled
                                    showSheetDialog = tl
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = BrandTextSecondary, modifier = Modifier.size(16.dp))
                            }

                            IconButton(
                                onClick = {
                                    viewModel.deleteTeamLeader(tl.id)
                                    Toast.makeText(context, "Deleted ${tl.name}", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = StatusDanger, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }

        // ================= SECTION 2: QUICK REMARKS MANAGER =================
        if (activeAdminTab == "REMARKS") {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("QUICK REMARK BUTTONS (${quickRemarks.size})", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("Configurable 1-click buttons shown in telecaller inbox", color = BrandTextMuted, fontSize = 11.sp)
                    }
                    Button(
                        onClick = {
                            draftRemarkLabel = ""
                            draftRemarkColor = "#10E57A"
                            draftRemarkReqFollowup = false
                            showRemarkDialog = QuickRemark()
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandGreenPrimary, contentColor = Color(0xFF071120))
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Remark", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            items(quickRemarks) { qr ->
                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = BrandNavySurface),
                    border = CardDefaults.outlinedCardBorder().copy(brush = Brush.verticalGradient(listOf(Color(0x33FFFFFF), Color(0x11FFFFFF))))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(
                                        try { Color(android.graphics.Color.parseColor(qr.colorHex)) }
                                        catch (e: Exception) { BrandGreenPrimary }
                                    )
                            )
                            Column {
                                Text(qr.label, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                                if (qr.requiresFollowup) {
                                    Text("Requires Follow-up scheduling", fontSize = 10.5.sp, color = BrandBlueSecondary)
                                }
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = {
                                    draftRemarkLabel = qr.label
                                    draftRemarkColor = qr.colorHex
                                    draftRemarkReqFollowup = qr.requiresFollowup
                                    showRemarkDialog = qr
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = BrandTextSecondary, modifier = Modifier.size(16.dp))
                            }
                            IconButton(
                                onClick = {
                                    viewModel.deleteQuickRemark(qr.id)
                                    Toast.makeText(context, "Deleted ${qr.label}", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = StatusDanger, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }

        // ================= SECTION 3: DEPARTMENTS =================
        if (activeAdminTab == "DEPARTMENTS") {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("DEPARTMENTS (${departments.size})", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("Calling queues & organizational branches", color = BrandTextMuted, fontSize = 11.sp)
                    }
                    Button(
                        onClick = {
                            draftDeptName = ""
                            draftDeptDesc = ""
                            showDeptDialog = Department()
                        },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandGreenPrimary, contentColor = Color(0xFF071120))
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Dept", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            items(departments) { dept ->
                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = BrandNavySurface),
                    border = CardDefaults.outlinedCardBorder().copy(brush = Brush.verticalGradient(listOf(Color(0x33FFFFFF), Color(0x11FFFFFF))))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(dept.name, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.5.sp)
                            Text(dept.description ?: "Active Telecalling Department", fontSize = 11.sp, color = BrandTextSecondary)
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = {
                                    draftDeptName = dept.name
                                    draftDeptDesc = dept.description ?: ""
                                    showDeptDialog = dept
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = BrandTextSecondary, modifier = Modifier.size(16.dp))
                            }
                            IconButton(
                                onClick = {
                                    viewModel.deleteDepartment(dept.id)
                                    Toast.makeText(context, "Deleted ${dept.name}", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = StatusDanger, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }

        // ================= SECTION 4: DEBUG MODE / ACTIVE SESSION & SYNC =================
        if (activeAdminTab == "DEBUG") {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = BrandNavySurface),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = Brush.verticalGradient(listOf(Color(0x44FFFFFF), Color(0x11FFFFFF)))
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.BugReport, contentDescription = null, tint = BrandGreenPrimary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("ACTIVE AUTH & SESSION DETAILS", color = BrandGreenPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 1.sp)
                        }

                        HorizontalDivider(color = Color(0x1AFFFFFF))

                        DebugFieldRow(label = "Active User ID", value = userSession.userId ?: "None (Logged Out)")
                        DebugFieldRow(label = "User Name", value = userSession.userName)
                        DebugFieldRow(label = "Active Role", value = userSession.role.name)
                        DebugFieldRow(label = "Active Company", value = userSession.companyId ?: "ALL (Root Admin)")
                        DebugFieldRow(label = "Active Team Leader", value = userSession.teamLeaderId ?: "ALL (Global Scope)")
                        DebugFieldRow(label = "Active Employee ID", value = userSession.employeeId ?: "N/A (Executive Role)")
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = BrandNavySurface),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = Brush.verticalGradient(listOf(Color(0x44FFFFFF), Color(0x11FFFFFF)))
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CloudSync, contentDescription = null, tint = BrandBlueSecondary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("DATA PIPELINE & SYNC STATUS", color = BrandBlueSecondary, fontWeight = FontWeight.Bold, fontSize = 12.sp, letterSpacing = 1.sp)
                        }

                        HorizontalDivider(color = Color(0x1AFFFFFF))

                        DebugFieldRow(label = "Mock Data Count", value = "0 (Strictly Blocked / Deleted)")
                        DebugFieldRow(label = "Real Leads in Memory", value = "${leads.size} leads")
                        DebugFieldRow(label = "Real Calls Recorded", value = "${calls.size} calls")
                        DebugFieldRow(label = "Real Employee Tabs Discovered", value = "${employees.size} employees")
                        DebugFieldRow(label = "Configured Companies", value = "${companies.size} companies (SPIN101, RUMMY77)")
                        DebugFieldRow(label = "Configured Team Leaders", value = "${teamLeaders.size} supervisors")
                        DebugFieldRow(
                            label = "Last Cloud/Sheet Sync",
                            value = if (lastSyncedAt != null) {
                                java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date(lastSyncedAt!!))
                            } else {
                                "Never synced yet"
                            }
                        )
                        DebugFieldRow(label = "Sync Pipeline State", value = syncStatusInfo.state.name)

                        Spacer(modifier = Modifier.height(6.dp))

                        Button(
                            onClick = {
                                viewModel.syncAll { summary ->
                                    Toast.makeText(context, "Full sync complete: ${summary.rowsSynced} rows", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = BrandGreenPrimary, contentColor = Color(0xFF071120))
                        ) {
                            Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Trigger Full Real Sync Now", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // Modal Dialogs
    if (showSheetDialog != null) {
        AlertDialog(
            onDismissRequest = { showSheetDialog = null },
            title = { Text(if (isNewSheet) "Connect Google Sheet" else "Edit Team Leader Sheet", color = Color.White) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = draftSheetName,
                        onValueChange = { draftSheetName = it },
                        label = { Text("Team Leader Name", color = BrandTextSecondary) },
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = BrandGreenPrimary)
                    )
                    OutlinedTextField(
                        value = draftSheetUrl,
                        onValueChange = { draftSheetUrl = it },
                        label = { Text("Google Sheet URL", color = BrandTextSecondary) },
                        placeholder = { Text("https://docs.google.com/spreadsheets/...", color = BrandTextMuted) },
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = BrandGreenPrimary)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (draftSheetName.isNotBlank()) {
                            viewModel.saveTeamLeader(
                                id = if (isNewSheet) null else showSheetDialog?.id,
                                name = draftSheetName,
                                companyId = draftSheetCompanyId,
                                sheetUrl = draftSheetUrl,
                                syncEnabled = draftSheetSync
                            )
                            showSheetDialog = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandGreenPrimary, contentColor = Color(0xFF071120))
                ) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showSheetDialog = null }) { Text("Cancel", color = BrandTextSecondary) } },
            containerColor = BrandNavySurface
        )
    }

    if (showRemarkDialog != null) {
        AlertDialog(
            onDismissRequest = { showRemarkDialog = null },
            title = { Text(if (showRemarkDialog?.id.isNullOrBlank()) "Add Quick Remark" else "Edit Quick Remark", color = Color.White) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = draftRemarkLabel,
                        onValueChange = { draftRemarkLabel = it },
                        label = { Text("Remark Label (e.g. Call Back)", color = BrandTextSecondary) },
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = BrandGreenPrimary)
                    )
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("Requires Follow-up scheduling", color = Color.White, fontSize = 12.sp)
                        Switch(
                            checked = draftRemarkReqFollowup,
                            onCheckedChange = { draftRemarkReqFollowup = it },
                            colors = SwitchDefaults.colors(checkedTrackColor = BrandGreenPrimary)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (draftRemarkLabel.isNotBlank()) {
                            viewModel.saveQuickRemark(
                                id = showRemarkDialog?.id?.ifBlank { null },
                                label = draftRemarkLabel,
                                colorHex = draftRemarkColor,
                                requiresFollowup = draftRemarkReqFollowup
                            )
                            showRemarkDialog = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandGreenPrimary, contentColor = Color(0xFF071120))
                ) { Text("Save Remark") }
            },
            dismissButton = { TextButton(onClick = { showRemarkDialog = null }) { Text("Cancel", color = BrandTextSecondary) } },
            containerColor = BrandNavySurface
        )
    }

    if (showDeptDialog != null) {
        AlertDialog(
            onDismissRequest = { showDeptDialog = null },
            title = { Text(if (showDeptDialog?.id.isNullOrBlank()) "Add Department" else "Edit Department", color = Color.White) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = draftDeptName,
                        onValueChange = { draftDeptName = it },
                        label = { Text("Department Name", color = BrandTextSecondary) },
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = BrandGreenPrimary)
                    )
                    OutlinedTextField(
                        value = draftDeptDesc,
                        onValueChange = { draftDeptDesc = it },
                        label = { Text("Description", color = BrandTextSecondary) },
                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedBorderColor = BrandGreenPrimary)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (draftDeptName.isNotBlank()) {
                            viewModel.saveDepartment(
                                id = showDeptDialog?.id?.ifBlank { null },
                                name = draftDeptName,
                                description = draftDeptDesc.ifBlank { null }
                            )
                            showDeptDialog = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandGreenPrimary, contentColor = Color(0xFF071120))
                ) { Text("Save Department") }
            },
            dismissButton = { TextButton(onClick = { showDeptDialog = null }) { Text("Cancel", color = BrandTextSecondary) } },
            containerColor = BrandNavySurface
        )
    }
}

@Composable
fun OwnerLoginDialog(onUnlock: (String) -> Boolean) {
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier.fillMaxSize().background(BrandNavyBg).padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = BrandNavySurface),
            border = CardDefaults.outlinedCardBorder().copy(brush = Brush.verticalGradient(listOf(Color(0x33FFFFFF), Color(0x11FFFFFF)))),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier.size(48.dp).clip(CircleShape).background(BrandGreenPrimary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = BrandGreenPrimary, modifier = Modifier.size(24.dp))
                }
                Text("Owner / Admin Login", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 17.sp)
                Text(
                    "Password required to access Google Sheets configuration & sync settings.",
                    fontSize = 11.5.sp,
                    color = BrandTextSecondary,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        error = false
                    },
                    label = { Text("Admin Master Password", color = BrandTextSecondary) },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = BrandGreenPrimary,
                        unfocusedBorderColor = Color(0x33FFFFFF)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                if (error) {
                    Text("Incorrect administrator password", color = StatusDanger, fontSize = 11.sp)
                }

                Button(
                    onClick = {
                        val ok = onUnlock(password)
                        if (!ok) error = true
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandGreenPrimary, contentColor = Color(0xFF071120))
                ) {
                    Text("Unlock Admin Settings", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun AdminMetricBox(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = BrandNavySurface),
        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.verticalGradient(listOf(Color(0x22FFFFFF), Color(0x11FFFFFF))))
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Icon(icon, contentDescription = null, tint = BrandGreenPrimary, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text(label, fontSize = 10.sp, color = BrandTextMuted)
        }
    }
}

@Composable
fun DebugFieldRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = BrandTextSecondary, fontSize = 12.sp)
        Text(text = value, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp)
    }
}
