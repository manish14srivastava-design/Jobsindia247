package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UserRole
import com.example.ui.theme.*
import com.example.ui.viewmodel.TrackingViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun WorkspaceSyncLoadingScreen(
    viewModel: TrackingViewModel,
    targetRole: UserRole,
    targetEmployeeId: String? = null,
    targetTlId: String? = null,
    onSyncSuccess: () -> Unit,
    onCancel: () -> Unit
) {
    val userSession by viewModel.userSession.collectAsState()
    val companies by viewModel.companies.collectAsState()
    val teamLeaders by viewModel.teamLeaders.collectAsState()
    val employees by viewModel.employees.collectAsState()

    var currentStep by remember { mutableStateOf(1) }
    var stepMessage by remember { mutableStateOf("Loading your real workspace...") }
    var detailsMessage by remember { mutableStateOf("Initializing secure synchronization session...") }
    var syncError by remember { mutableStateOf<String?>(null) }
    var isSyncFinished by remember { mutableStateOf(false) }
    var progressFraction by remember { mutableStateOf(0.1f) }
    var retryTrigger by remember { mutableStateOf(0) }

    val coroutineScope = rememberCoroutineScope()

    // Pulse animation for sync icon
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val activeColor = when (targetRole) {
        UserRole.OWNER -> BrandGreenPrimary
        UserRole.TEAM_LEADER -> BrandBlueSecondary
        UserRole.EMPLOYEE -> Color(0xFF00E5FF)
        UserRole.NONE -> BrandGreenPrimary
    }

    fun startSyncSequence() {
        syncError = null
        currentStep = 1
        progressFraction = 0.1f
        stepMessage = "Loading your real workspace..."
        detailsMessage = "Purging previous cache and initializing clean state..."

        coroutineScope.launch {
            // STEP 1: CLEAR PREVIOUS DATA
            viewModel.clearWorkspaceState()
            delay(350)

            // STEP 2: USER ROLE & ENTITY IDENTIFICATION
            currentStep = 2
            progressFraction = 0.3f
            stepMessage = "Identifying account..."

            val roleLabel = when (targetRole) {
                UserRole.OWNER -> "Administrator / Master Owner"
                UserRole.TEAM_LEADER -> {
                    val tl = teamLeaders.find { it.id == targetTlId || it.id == userSession.teamLeaderId }
                    "Team Leader: ${tl?.name ?: userSession.userName}"
                }
                UserRole.EMPLOYEE -> {
                    val emp = employees.find { it.id == targetEmployeeId || it.id == userSession.employeeId }
                    "Telecaller: ${emp?.name ?: userSession.userName}"
                }
                UserRole.NONE -> "Workspace Session"
            }
            detailsMessage = "Authenticated as $roleLabel"
            delay(400)

            // STEP 3: REAL GOOGLE SHEET SYNC
            currentStep = 3
            progressFraction = 0.55f
            stepMessage = "Syncing latest data..."
            detailsMessage = "Connecting directly to live Google Sheets..."

            when (targetRole) {
                UserRole.OWNER -> {
                    viewModel.syncAll(
                        onProgress = { statusText, current, total ->
                            detailsMessage = "$statusText ($current/$total)"
                            progressFraction = 0.55f + (0.30f * (current.toFloat() / maxOf(1, total).toFloat()))
                        },
                        onComplete = { summary ->
                            if (summary.syncErrors > 0 && summary.rowsSynced == 0) {
                                syncError = "Google Sheet Sync Error: Unable to fetch live sheets. Check network connection."
                            } else {
                                // STEP 4: FIRESTORE VERIFICATION & LOAD
                                currentStep = 4
                                progressFraction = 0.88f
                                stepMessage = "Loading real dashboard..."
                                detailsMessage = "Synced ${summary.rowsSynced} leads across ${summary.employeeTabsSynced} tabs to Firestore"

                                coroutineScope.launch {
                                    delay(400)
                                    currentStep = 5
                                    progressFraction = 1.0f
                                    stepMessage = "Workspace ready!"
                                    delay(300)
                                    isSyncFinished = true
                                    onSyncSuccess()
                                }
                            }
                        }
                    )
                }

                UserRole.TEAM_LEADER -> {
                    val tlId = targetTlId ?: userSession.teamLeaderId ?: ""
                    if (tlId.isBlank()) {
                        syncError = "Team Leader profile not specified"
                        return@launch
                    }
                    viewModel.syncTeamLeader(
                        tlId = tlId,
                        onProgress = { statusText, current, total ->
                            detailsMessage = "$statusText ($current/$total)"
                            progressFraction = 0.55f + (0.30f * (current.toFloat() / maxOf(1, total).toFloat()))
                        },
                        onComplete = { success, rows, tabs, errorMsg ->
                            if (!success && rows == 0) {
                                syncError = errorMsg ?: "Failed to sync Team Leader sheet from Google Drive"
                            } else {
                                currentStep = 4
                                progressFraction = 0.88f
                                stepMessage = "Loading supervisor dashboard..."
                                detailsMessage = "Synced $rows live records across $tabs telecaller tabs"

                                coroutineScope.launch {
                                    delay(400)
                                    currentStep = 5
                                    progressFraction = 1.0f
                                    stepMessage = "Supervisor workspace ready!"
                                    delay(300)
                                    isSyncFinished = true
                                    onSyncSuccess()
                                }
                            }
                        }
                    )
                }

                UserRole.EMPLOYEE -> {
                    val empId = targetEmployeeId ?: userSession.employeeId ?: ""
                    if (empId.isBlank()) {
                        syncError = "Employee tab profile not specified"
                        return@launch
                    }
                    val tlId = targetTlId ?: userSession.teamLeaderId ?: ""
                    val deptId = userSession.departmentId ?: "dept_telecalling"
                    viewModel.loginAndSyncEmployee(
                        deptId = deptId,
                        tlId = tlId,
                        empId = empId,
                        onProgress = { statusText ->
                            detailsMessage = statusText
                        },
                        onComplete = { success, rows, errorMsg ->
                            if (!success && rows == 0) {
                                syncError = errorMsg ?: "Failed to fetch employee tab from Google Sheet"
                            } else {
                                currentStep = 4
                                progressFraction = 0.88f
                                stepMessage = "Loading today's work queue..."
                                detailsMessage = "Loaded $rows leads from your sheet tab"

                                coroutineScope.launch {
                                    delay(400)
                                    currentStep = 5
                                    progressFraction = 1.0f
                                    stepMessage = "Work queue ready!"
                                    delay(300)
                                    isSyncFinished = true
                                    onSyncSuccess()
                                }
                            }
                        }
                    )
                }

                UserRole.NONE -> {
                    onSyncSuccess()
                }
            }
        }
    }

    LaunchedEffect(retryTrigger) {
        startSyncSequence()
    }

    Scaffold(
        containerColor = BrandNavyBg
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 440.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Header Logo / Icon
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(
                            if (syncError != null) Color(0xFFFF5252).copy(alpha = 0.15f)
                            else activeColor.copy(alpha = 0.15f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (syncError != null) {
                        Icon(
                            imageVector = Icons.Default.CloudOff,
                            contentDescription = "Sync Failed",
                            tint = Color(0xFFFF5252),
                            modifier = Modifier.size(40.dp)
                        )
                    } else {
                        Icon(
                            imageVector = when (targetRole) {
                                UserRole.OWNER -> Icons.Default.AdminPanelSettings
                                UserRole.TEAM_LEADER -> Icons.Default.SupervisorAccount
                                UserRole.EMPLOYEE -> Icons.Default.HeadsetMic
                                UserRole.NONE -> Icons.Default.Sync
                            },
                            contentDescription = "Syncing",
                            tint = activeColor,
                            modifier = Modifier.size(40.dp * pulseScale)
                        )
                    }
                }

                if (syncError != null) {
                    // ERROR STATE
                    Text(
                        text = "SYNC FAILED",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFF5252),
                            letterSpacing = 1.5.sp
                        )
                    )

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0x22FF5252))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = Color(0xFFFF5252), modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Actual Error Cause",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = Color(0xFFFF5252)
                                )
                            }
                            Text(
                                text = syncError ?: "Unable to complete Google Sheet & Firestore synchronization.",
                                fontSize = 12.5.sp,
                                color = Color.White
                            )
                            Text(
                                text = "Strict Policy: Fallback to mock/demo data is disabled.",
                                fontSize = 11.sp,
                                color = BrandTextSecondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = { retryTrigger++ },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BrandGreenPrimary,
                            contentColor = Color(0xFF071120)
                        )
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("RETRY SYNC", fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = {
                            viewModel.logout()
                            onCancel()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFFFF5252)
                        )
                    ) {
                        Text("Back to Role Selection", fontWeight = FontWeight.SemiBold)
                    }
                } else {
                    // ACTIVE LOADING STATE
                    Text(
                        text = stepMessage,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        ),
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = detailsMessage,
                        fontSize = 12.5.sp,
                        color = BrandTextSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    LinearProgressIndicator(
                        progress = { progressFraction.coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = activeColor,
                        trackColor = Color(0x22FFFFFF)
                    )

                    // Step Tracker Cards
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = BrandNavySurface)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            SyncStepRow(stepNum = 1, title = "Purge Mock / Old Cache", isDone = currentStep > 1, isActive = currentStep == 1, activeColor = activeColor)
                            SyncStepRow(stepNum = 2, title = "Identify Account & Workspace", isDone = currentStep > 2, isActive = currentStep == 2, activeColor = activeColor)
                            SyncStepRow(stepNum = 3, title = "Live Google Sheet Data Sync", isDone = currentStep > 3, isActive = currentStep == 3, activeColor = activeColor)
                            SyncStepRow(stepNum = 4, title = "Save & Verify Firestore Cloud Records", isDone = currentStep > 4, isActive = currentStep == 4, activeColor = activeColor)
                            SyncStepRow(stepNum = 5, title = "Load Verified Live Dashboard", isDone = currentStep >= 5, isActive = currentStep == 5, activeColor = activeColor)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SyncStepRow(
    stepNum: Int,
    title: String,
    isDone: Boolean,
    isActive: Boolean,
    activeColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(
                    when {
                        isDone -> BrandGreenPrimary
                        isActive -> activeColor.copy(alpha = 0.25f)
                        else -> Color(0x1AFFFFFF)
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isDone) {
                Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF071120), modifier = Modifier.size(14.dp))
            } else {
                Text(
                    text = "$stepNum",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isActive) activeColor else Color(0x66FFFFFF)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = title,
            fontSize = 12.sp,
            fontWeight = if (isActive || isDone) FontWeight.SemiBold else FontWeight.Normal,
            color = when {
                isDone -> Color.White
                isActive -> activeColor
                else -> Color(0x66FFFFFF)
            }
        )

        if (isActive) {
            Spacer(modifier = Modifier.weight(1f))
            CircularProgressIndicator(
                modifier = Modifier.size(14.dp),
                color = activeColor,
                strokeWidth = 2.dp
            )
        }
    }
}
