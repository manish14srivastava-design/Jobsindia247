package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AuditLog
import com.example.data.model.DiagnosticTestResult
import com.example.data.model.FirebaseHealthStatus
import com.example.ui.viewmodel.TrackingViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FirebaseHealthScreen(
    viewModel: TrackingViewModel,
    onBack: () -> Unit
) {
    val health by viewModel.firebaseHealth.collectAsState()
    val isTesting by viewModel.isTestingHealth.collectAsState()
    val diagnosticResult by viewModel.diagnosticResult.collectAsState()
    val auditLogs by viewModel.auditLogs.collectAsState()
    val employees by viewModel.employees.collectAsState()
    val companies by viewModel.companies.collectAsState()

    var selectedEmpForTest by remember { mutableStateOf(employees.firstOrNull()?.id ?: "") }
    var showEmpDropdown by remember { mutableStateOf(false) }

    val dateFormat = remember { SimpleDateFormat("dd MMM, hh:mm:ss a", Locale.getDefault()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Firebase Health & Diagnostics",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            "Real-time Cloud Persistence & Audit Logs",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("btn_back_firebase_health")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.runFirestoreHealthTest() },
                        enabled = !isTesting,
                        modifier = Modifier.testTag("btn_refresh_health")
                    ) {
                        if (isTesting) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh Health")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                // Top Status Overview Card
                FirebaseOverviewCard(health = health, dateFormat = dateFormat)
            }

            item {
                // Interactive Diagnostic Suite Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.FactCheck,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Automated Diagnostics Suite",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }

                        Text(
                            "Execute end-to-end cloud roundtrip checks to verify read, write, listener sync, and multi-tenant employee data isolation.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { viewModel.runFirestoreHealthTest() },
                                enabled = !isTesting,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("btn_test_firestore_write"),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.CloudDone, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Test Write/Read", fontSize = 13.sp)
                            }
                        }

                        // Employee Isolation Test Section
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                        Text(
                            "Multi-Employee Data Isolation Check",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(modifier = Modifier.weight(1f)) {
                                OutlinedButton(
                                    onClick = { showEmpDropdown = true },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("btn_select_test_emp"),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    val currentEmp = employees.find { it.id == selectedEmpForTest }
                                    Text(
                                        text = currentEmp?.name ?: "Select Telecaller",
                                        maxLines = 1,
                                        fontSize = 13.sp
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                }

                                DropdownMenu(
                                    expanded = showEmpDropdown,
                                    onDismissRequest = { showEmpDropdown = false }
                                ) {
                                    employees.forEach { emp ->
                                        DropdownMenuItem(
                                            text = {
                                                Column {
                                                    Text(emp.name, fontWeight = FontWeight.Medium)
                                                    Text(
                                                        "${emp.department ?: "Telecalling"} • Tab: ${emp.employeeTabName}",
                                                        fontSize = 11.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            },
                                            onClick = {
                                                selectedEmpForTest = emp.id
                                                showEmpDropdown = false
                                            }
                                        )
                                    }
                                }
                            }

                            FilledTonalButton(
                                onClick = {
                                    if (selectedEmpForTest.isNotBlank()) {
                                        viewModel.runEmployeePersistenceTest(selectedEmpForTest)
                                    }
                                },
                                enabled = !isTesting && selectedEmpForTest.isNotBlank(),
                                modifier = Modifier.testTag("btn_run_emp_isolation_test"),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Verify Isolation", fontSize = 13.sp)
                            }
                        }
                    }
                }
            }

            // Diagnostic Result Banner
            if (diagnosticResult != null) {
                item {
                    DiagnosticResultCard(
                        result = diagnosticResult!!,
                        onDismiss = { viewModel.clearDiagnosticResult() }
                    )
                }
            }

            // Audit Logs Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.HistoryEdu,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Real-time Telecaller Audit Logs",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                    Badge(containerColor = MaterialTheme.colorScheme.primaryContainer) {
                        Text(
                            "${auditLogs.size} logs",
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                    }
                }
            }

            if (auditLogs.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "No telecaller activities recorded yet. When employees mark calls, interested leads, or conversions, real-time audit logs will stream here.",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(auditLogs, key = { it.id }) { log ->
                    AuditLogItemCard(log = log, dateFormat = dateFormat)
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun FirebaseOverviewCard(
    health: FirebaseHealthStatus,
    dateFormat: SimpleDateFormat
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(
                                color = if (health.firestoreWorking) Color(0xFF10B981) else Color(0xFFEF4444),
                                shape = CircleShape
                            )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        if (health.firestoreWorking) "Firebase Engine Online" else "Firebase Offline / Fallback",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }

                Surface(
                    color = if (health.firestoreWorking) Color(0xFF10B981).copy(alpha = 0.15f) else Color(0xFFEF4444).copy(alpha = 0.15f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (health.firestoreWorking) "HEALTHY" else "ATTENTION",
                        color = if (health.firestoreWorking) Color(0xFF059669) else Color(0xFFDC2626),
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            HorizontalDivider()

            // Metrics grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                HealthMetric(
                    label = "Last Cloud Write",
                    value = health.lastSuccessfulWrite?.let { dateFormat.format(Date(it)) } ?: "Awaiting Write"
                )
                HealthMetric(
                    label = "Last Cloud Read",
                    value = health.lastSuccessfulRead?.let { dateFormat.format(Date(it)) } ?: "Active Listener"
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                HealthMetric(
                    label = "Realtime Listener",
                    value = if (health.realtimeListenerWorking) "🟢 Connected" else "🔴 Reconnecting"
                )
                HealthMetric(
                    label = "Offline Storage",
                    value = if (health.offlineCacheEnabled) "🟢 Enabled" else "⚪ Disabled"
                )
            }
        }
    }
}

@Composable
private fun HealthMetric(
    label: String,
    value: String
) {
    Column {
        Text(
            text = label,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun DiagnosticResultCard(
    result: DiagnosticTestResult,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (result.passed) Color(0xFFECFDF5) else Color(0xFFFEF2F2)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (result.passed) Icons.Default.CheckCircle else Icons.Default.Error,
                        contentDescription = null,
                        tint = if (result.passed) Color(0xFF059669) else Color(0xFFDC2626),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        result.testName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = if (result.passed) Color(0xFF065F46) else Color(0xFF991B1B)
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Dismiss",
                        tint = Color.Gray,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Text(
                result.message,
                fontSize = 13.sp,
                color = if (result.passed) Color(0xFF047857) else Color(0xFFB91C1C)
            )

            if (result.details.isNotEmpty()) {
                Surface(
                    color = Color.White.copy(alpha = 0.8f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        result.details.forEach { (k, v) ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    k,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color.DarkGray
                                )
                                Text(
                                    v,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = if (v.startsWith("PASS")) Color(0xFF059669) else Color.DarkGray
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
private fun AuditLogItemCard(
    log: AuditLog,
    dateFormat: SimpleDateFormat
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val badgeColor = when (log.actionType) {
                        "INTERESTED" -> Color(0xFF10E57A)
                        "SUCCESSFUL" -> Color(0xFF22C55E)
                        "SEND_LINK" -> Color(0xFF3B82F6)
                        "CALLBACK" -> Color(0xFF6366F1)
                        "FOLLOW_UP" -> Color(0xFFF59E0B)
                        else -> MaterialTheme.colorScheme.primary
                    }

                    Surface(
                        color = badgeColor.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = log.actionType,
                            color = badgeColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        log.employeeName,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                }

                Text(
                    text = dateFormat.format(Date(log.timestamp)),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Phone: ${log.phone}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (log.remark.isNotBlank()) {
                    Text(
                        text = "Remark: ${log.remark}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (!log.note.isNullOrBlank()) {
                Text(
                    text = "Note: ${log.note}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
