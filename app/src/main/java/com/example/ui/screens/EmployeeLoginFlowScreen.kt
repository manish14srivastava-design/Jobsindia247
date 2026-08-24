package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.example.ui.theme.*
import com.example.ui.viewmodel.TrackingViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployeeLoginFlowScreen(
    viewModel: TrackingViewModel,
    onLoginSuccess: () -> Unit,
    onBack: () -> Unit
) {
    val companies by viewModel.companies.collectAsState()
    val teamLeaders by viewModel.teamLeaders.collectAsState()
    val employees by viewModel.employees.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()

    var selectedCompanyId by remember { mutableStateOf<String?>(null) }
    var selectedTlId by remember { mutableStateOf<String?>(null) }
    var selectedEmpId by remember { mutableStateOf<String?>(null) }

    var isEnteringAndSyncing by remember { mutableStateOf(false) }

    val filteredTls = remember(selectedCompanyId, teamLeaders) {
        if (selectedCompanyId == null) emptyList()
        else teamLeaders.filter { it.companyId == selectedCompanyId }
    }

    val filteredEmployees = remember(selectedTlId, employees) {
        if (selectedTlId == null) emptyList()
        else employees.filter { it.teamLeaderId == selectedTlId }
    }

    LaunchedEffect(selectedTlId) {
        if (selectedTlId != null) {
            viewModel.fetchEmployeesForTl(selectedTlId!!)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Telecaller Employee Login", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    if (isSyncing) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(20.dp)
                                .padding(end = 8.dp),
                            color = BrandGreenPrimary,
                            strokeWidth = 2.dp
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BrandNavySurface)
            )
        },
        containerColor = BrandNavyBg
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Progress / Steps
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                StepIndicator(step = 1, title = "Company", active = true, completed = selectedCompanyId != null)
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = BrandTextMuted, modifier = Modifier.size(16.dp))
                StepIndicator(step = 2, title = "Team Leader", active = selectedCompanyId != null, completed = selectedTlId != null)
                Icon(Icons.Default.ChevronRight, contentDescription = null, tint = BrandTextMuted, modifier = Modifier.size(16.dp))
                StepIndicator(step = 3, title = "Employee Name", active = selectedTlId != null, completed = selectedEmpId != null)
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // STEP 1: SELECT COMPANY (IND08 - SPIN101, IND15 - RUMMY77)
                item {
                    SectionCard(title = "STEP 1: SELECT COMPANY", icon = Icons.Default.Business) {
                        companies.forEach { comp ->
                            val isSelected = selectedCompanyId == comp.id
                            SelectableRow(
                                title = "${comp.companyCode} — ${comp.name}",
                                subtitle = "Active Telecalling Department",
                                isSelected = isSelected,
                                onClick = {
                                    selectedCompanyId = comp.id
                                    selectedTlId = null
                                    selectedEmpId = null
                                }
                            )
                        }
                    }
                }

                // STEP 2: SELECT TEAM LEADER
                if (selectedCompanyId != null) {
                    item {
                        SectionCard(title = "STEP 2: SELECT TEAM LEADER", icon = Icons.Default.SupervisorAccount) {
                            if (filteredTls.isEmpty()) {
                                Text(
                                    text = "No team leaders configured for this company.",
                                    color = BrandTextSecondary,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(8.dp)
                                )
                            } else {
                                filteredTls.forEach { tl ->
                                    val isSelected = selectedTlId == tl.id
                                    val count = employees.count { it.teamLeaderId == tl.id }
                                    SelectableRow(
                                        title = tl.name,
                                        subtitle = if (count > 0) "Connected Google Sheet • $count Telecaller Tabs" else "Connected Google Sheet • Tap to load telecaller tabs",
                                        isSelected = isSelected,
                                        onClick = {
                                            selectedTlId = tl.id
                                            selectedEmpId = null
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // STEP 3: SELECT EMPLOYEE (FETCHED FROM GOOGLE SHEET)
                if (selectedCompanyId != null && selectedTlId != null) {
                    item {
                        SectionCard(
                            title = "STEP 3: SELECT YOUR NAME (FROM GOOGLE SHEET)",
                            icon = Icons.Default.Person
                        ) {
                            if (filteredEmployees.isEmpty()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        color = BrandGreenPrimary,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "Fetching real employee tabs from Google Sheet...",
                                        color = BrandTextSecondary,
                                        fontSize = 12.sp
                                    )
                                }
                            } else {
                                filteredEmployees.forEach { emp ->
                                    val isSelected = selectedEmpId == emp.id
                                    SelectableRow(
                                        title = emp.name,
                                        subtitle = "Sheet Tab: \"${emp.employeeTabName}\"",
                                        isSelected = isSelected,
                                        onClick = {
                                            selectedEmpId = emp.id
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // ENTER TODAY'S WORK BUTTON
            val canProceed = selectedCompanyId != null && selectedTlId != null && selectedEmpId != null && !isEnteringAndSyncing
            Button(
                onClick = {
                    if (canProceed) {
                        isEnteringAndSyncing = true
                        val deptId = teamLeaders.find { it.id == selectedTlId }?.departmentId ?: "dept_telecalling"
                        viewModel.loginAndSyncEmployee(deptId, selectedTlId!!, selectedEmpId!!) {
                            isEnteringAndSyncing = false
                            onLoginSuccess()
                        }
                    }
                },
                enabled = canProceed,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = BrandGreenPrimary,
                    contentColor = Color(0xFF071120),
                    disabledContainerColor = Color(0x22FFFFFF),
                    disabledContentColor = Color(0x55FFFFFF)
                )
            ) {
                if (isEnteringAndSyncing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color(0xFF071120),
                        strokeWidth = 2.5.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "SYNCING GOOGLE SHEET & ENTERING...",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                } else {
                    Icon(Icons.Default.FlashOn, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "SYNC & ENTER TODAY'S WORK INBOX",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun StepIndicator(step: Int, title: String, active: Boolean, completed: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(
                    when {
                        completed -> BrandGreenPrimary
                        active -> BrandGreenPrimary.copy(alpha = 0.25f)
                        else -> Color(0x1AFFFFFF)
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            if (completed) {
                Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF071120), modifier = Modifier.size(16.dp))
            } else {
                Text(
                    text = step.toString(),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (active) BrandGreenPrimary else BrandTextMuted
                )
            }
        }
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = title,
            fontSize = 10.5.sp,
            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
            color = if (active) Color.White else BrandTextMuted
        )
    }
}

@Composable
private fun SectionCard(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = BrandNavySurface),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(Color(0x22FFFFFF))
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = BrandGreenPrimary, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = BrandGreenPrimary,
                    letterSpacing = 1.sp
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            content()
        }
    }
}

@Composable
private fun SelectableRow(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) BrandGreenPrimary.copy(alpha = 0.2f) else Color(0x0DFFFFFF))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontWeight = FontWeight.SemiBold,
                color = if (isSelected) Color.White else BrandTextSecondary,
                fontSize = 13.5.sp
            )
            Text(
                text = subtitle,
                fontSize = 10.5.sp,
                color = BrandTextMuted
            )
        }
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = BrandGreenPrimary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
