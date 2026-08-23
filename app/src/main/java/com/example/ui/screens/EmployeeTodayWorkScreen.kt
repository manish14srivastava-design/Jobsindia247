package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.TrackingViewModel
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployeeTodayWorkScreen(
    viewModel: TrackingViewModel,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val userSession by viewModel.userSession.collectAsState()
    val leads by viewModel.leads.collectAsState()
    val quickRemarks by viewModel.quickRemarks.collectAsState()
    val employees by viewModel.employees.collectAsState()
    val teamLeaders by viewModel.teamLeaders.collectAsState()
    val companies by viewModel.companies.collectAsState()
    val messageTemplates by viewModel.messageTemplates.collectAsState()
    val activities by viewModel.activities.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val syncStatusInfo by viewModel.syncStatusInfo.collectAsState()
    var showSyncInfoDialog by remember { mutableStateOf(false) }

    val currentEmployee = remember(userSession, employees) {
        employees.find { it.id == userSession.employeeId }
    }
    val currentTl = remember(userSession, teamLeaders) {
        teamLeaders.find { it.id == userSession.teamLeaderId }
    }
    val currentCompany = remember(userSession, currentEmployee, currentTl, companies) {
        val compId = currentEmployee?.companyId ?: currentTl?.companyId ?: userSession.companyId
        companies.find { it.id == compId } ?: companies.firstOrNull()
    }

    val officialLink = remember(currentCompany) {
        currentCompany?.officialLink?.ifBlank { null }
            ?: MessageEngine.getOfficialLinkForCompany(currentCompany?.companyCode ?: currentCompany?.name)
    }

    val todayFormattedDisplay = remember {
        java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault()).format(java.util.Date())
    }

    var onlyTodayMode by remember { mutableStateOf(true) }

    // All leads assigned to current telecaller from the Google Sheet
    val allEmployeeLeads = remember(leads, userSession) {
        leads.filter { it.assignedEmployeeId == userSession.employeeId }
            .sortedBy { it.sourceRowIndex }
    }

    val todayLeads = remember(allEmployeeLeads) {
        val filtered = allEmployeeLeads.filter { it.isToday }
        if (filtered.isNotEmpty()) filtered else allEmployeeLeads
    }

    // Strictly prioritize today's daily current date numbers as requested
    val myLeads = remember(allEmployeeLeads, todayLeads, onlyTodayMode) {
        if (onlyTodayMode) todayLeads else allEmployeeLeads
    }

    val totalAssigned = myLeads.size
    val totalAllSheetCount = allEmployeeLeads.size
    val pendingLeads = remember(myLeads) { myLeads.filter { it.status == "PENDING" && it.currentRemark.isBlank() } }
    val interestedLeads = remember(myLeads) {
        myLeads.filter { it.status == "INTERESTED" || it.currentRemark.equals(RemarkConstants.INTERESTED, ignoreCase = true) }
    }
    val doneLeads = remember(myLeads) {
        myLeads.filter { it.status == "SUCCESSFUL" || it.status == "COMPLETED" || (it.currentRemark.isNotBlank() && !it.currentRemark.equals(RemarkConstants.PENDING, ignoreCase = true) && !it.currentRemark.equals(RemarkConstants.INTERESTED, ignoreCase = true)) }
    }
    val successfulLeads = remember(myLeads) {
        myLeads.filter { it.status == "SUCCESSFUL" || it.currentRemark.equals(RemarkConstants.SUCCESSFUL, ignoreCase = true) }
    }
    val linkSentCount = remember(myLeads) { myLeads.count { it.linkSent } }

    // Navigation subviews
    var currentSubView by remember { mutableStateOf("INBOX") }

    // Active lead selection
    var selectedLeadId by remember { mutableStateOf<String?>(null) }
    val activeInboxLeads = remember(myLeads) {
        myLeads.filter { it.status == "PENDING" || it.status == "CALLBACK" || it.status == "FOLLOW_UP" || it.currentRemark.isBlank() }
    }

    val activeLead = remember(myLeads, selectedLeadId) {
        if (selectedLeadId != null) {
            myLeads.find { it.id == selectedLeadId }
                ?: myLeads.firstOrNull { it.currentRemark.isBlank() || it.currentRemark.equals(RemarkConstants.PENDING, true) }
                ?: myLeads.firstOrNull()
        } else {
            myLeads.firstOrNull { it.currentRemark.isBlank() || it.currentRemark.equals(RemarkConstants.PENDING, true) }
                ?: myLeads.firstOrNull()
        }
    }

    // Active Remark Form State
    var selectedRemark by remember(activeLead?.id) { mutableStateOf(activeLead?.currentRemark ?: "") }
    var customNote by remember(activeLead?.id) { mutableStateOf(activeLead?.notes ?: "") }
    var selectedFollowupOption by remember(activeLead?.id) { mutableStateOf<String?>("Today (+2 hrs)") }
    var followupTimestamp by remember(activeLead?.id) { mutableStateOf<Long?>(System.currentTimeMillis() + 7_200_000L) }

    // Action loading states
    var savingLeadId by remember { mutableStateOf<String?>(null) }
    var snackbarMessage by remember { mutableStateOf<String?>(null) }

    // Message Template Dialog State
    var showMessageDialog by remember { mutableStateOf(false) }
    var messageDialogLead by remember { mutableStateOf<Lead?>(null) }
    var selectedTemplateId by remember { mutableStateOf("tmpl_default") }
    var customMessagePreview by remember { mutableStateOf("") }

    fun showFeedback(msg: String) {
        snackbarMessage = msg
        coroutineScope.launch {
            delay(3000)
            if (snackbarMessage == msg) snackbarMessage = null
        }
    }

    fun buildMessageForLead(lead: Lead, templateId: String): String {
        val template = messageTemplates.find { it.id == templateId } ?: messageTemplates.first()
        val empName = userSession.userName.ifBlank { currentEmployee?.name ?: "Support Executive" }
        val compName = currentCompany?.name ?: "Official Platform"
        val compCode = currentCompany?.companyCode ?: "IND08"

        return MessageEngine.formatMessage(
            templateBody = template.templateBody,
            employeeName = empName,
            companyName = compName,
            companyCode = compCode,
            officialLink = officialLink
        )
    }

    fun dialNumber(phone: String) {
        try {
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Unable to dial $phone", Toast.LENGTH_SHORT).show()
        }
    }

    fun openOfficialLink() {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(officialLink))
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Cannot open $officialLink", Toast.LENGTH_SHORT).show()
        }
    }

    fun sendWhatsAppMessage(lead: Lead, templateId: String) {
        val messageText = buildMessageForLead(lead, templateId)
        viewModel.markLeadLinkSent(lead.id, userSession.employeeId ?: "", templateId)

        try {
            val cleanPhone = lead.phone.filter { it.isDigit() }
            val formattedPhone = if (cleanPhone.startsWith("91") && cleanPhone.length == 12) {
                cleanPhone
            } else if (cleanPhone.length == 10) {
                "91$cleanPhone"
            } else {
                cleanPhone
            }

            val encodedMsg = URLEncoder.encode(messageText, "UTF-8")
            val url = "https://api.whatsapp.com/send?phone=$formattedPhone&text=$encodedMsg"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(intent)
            showFeedback("📤 Official Link Sent via WhatsApp!")
        } catch (e: Exception) {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, messageText)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Send Link"))
        }
    }

    fun sendNormalSms(lead: Lead, templateId: String) {
        val messageText = buildMessageForLead(lead, templateId)
        viewModel.markLeadLinkSent(lead.id, userSession.employeeId ?: "", templateId)

        try {
            val cleanPhone = lead.phone.filter { it.isDigit() }
            val formattedPhone = if (cleanPhone.startsWith("91") && cleanPhone.length == 12) {
                "+$cleanPhone"
            } else if (cleanPhone.length == 10) {
                "+91$cleanPhone"
            } else {
                cleanPhone
            }

            val smsIntent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("smsto:$formattedPhone")
                putExtra("sms_body", messageText)
            }
            context.startActivity(smsIntent)
            showFeedback("✉️ Normal SMS Inbox Opened!")
        } catch (e: Exception) {
            try {
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse("sms:${lead.phone.filter { it.isDigit() }}")
                    putExtra("sms_body", messageText)
                }
                context.startActivity(intent)
                showFeedback("✉️ Redirected to Messages App")
            } catch (ex: Exception) {
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, messageText)
                }
                context.startActivity(Intent.createChooser(shareIntent, "Send Message"))
            }
        }
    }

    fun copyAndShareMessage(lead: Lead, templateId: String) {
        val messageText = buildMessageForLead(lead, templateId)
        viewModel.markLeadLinkSent(lead.id, userSession.employeeId ?: "", templateId)

        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        val clip = ClipData.newPlainText("Game Link Message", messageText)
        clipboard?.setPrimaryClip(clip)

        try {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, messageText)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share Official Link"))
            showFeedback("📋 Copied & Share sheet opened!")
        } catch (e: Exception) {
            showFeedback("📋 Link Copied to Clipboard!")
        }
    }

    fun instantSaveRemark(lead: Lead, remark: String) {
        if (savingLeadId != null) return
        savingLeadId = lead.id
        coroutineScope.launch {
            // Determine NEXT lead before updating repository to avoid race condition
            val allList = myLeads
            val currentIdx = allList.indexOfFirst { it.id == lead.id }

            val nextLead = if (currentIdx != -1) {
                // 1. Search ahead for next pending lead
                val nextPendingAhead = allList.drop(currentIdx + 1).firstOrNull {
                    it.currentRemark.isBlank() || it.currentRemark.equals(RemarkConstants.PENDING, true) || it.status == "PENDING"
                }
                // 2. Or the immediate next lead in sheet order
                nextPendingAhead
                    ?: if (currentIdx + 1 < allList.size) allList[currentIdx + 1]
                    // 3. Or any other remaining pending lead in list
                    else allList.firstOrNull { it.id != lead.id && (it.currentRemark.isBlank() || it.currentRemark.equals(RemarkConstants.PENDING, true)) }
            } else {
                allList.firstOrNull { it.id != lead.id }
            }

            // Immediately select next lead
            if (nextLead != null) {
                selectedLeadId = nextLead.id
            }

            viewModel.submitLeadRemark(
                leadId = lead.id,
                employeeId = userSession.employeeId ?: "",
                remark = remark,
                note = customNote.ifBlank { null },
                followUpAt = followupTimestamp
            )

            delay(150)
            savingLeadId = null
            val nextNote = if (nextLead != null) " → Next: Row #${nextLead.sourceRowIndex}" else ""
            showFeedback(
                when (remark) {
                    RemarkConstants.INTERESTED -> "⭐ Marked Interested!$nextNote"
                    RemarkConstants.SUCCESSFUL -> "🏆 Marked Successful!$nextNote"
                    else -> "Saved as $remark ✓$nextNote"
                }
            )
        }
    }

    fun markSuccessfulFromInterested(lead: Lead) {
        if (savingLeadId != null) return
        savingLeadId = lead.id
        coroutineScope.launch {
            viewModel.convertInterestedToSuccessful(
                leadId = lead.id,
                employeeId = userSession.employeeId ?: "",
                note = "Converted from Interested to Successful"
            )
            delay(200)
            savingLeadId = null
            showFeedback("🏆 Marked Successful! Converted into Successful lead.")
        }
    }

    fun saveAndNext() {
        if (activeLead == null) return
        if (selectedRemark.isBlank()) {
            Toast.makeText(context, "Please select a call remark first", Toast.LENGTH_SHORT).show()
            return
        }
        instantSaveRemark(activeLead, selectedRemark)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = userSession.userName.ifBlank { currentEmployee?.name ?: "Telecaller" },
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                color = BrandGreenPrimary.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = currentCompany?.name ?: "SPIN101",
                                    color = BrandGreenPrimary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "Preserved Sheet Order • TL: ${currentTl?.name ?: "Supervisor"}",
                            fontSize = 11.sp,
                            color = BrandTextSecondary
                        )
                    }
                },
                actions = {
                    // Real-time Cloud Sync status chip
                    Surface(
                        color = when (syncStatusInfo.state) {
                            SyncState.SYNCED -> BrandGreenPrimary.copy(alpha = 0.15f)
                            SyncState.SYNCING -> StatusInterested.copy(alpha = 0.15f)
                            SyncState.OFFLINE -> Color(0xFF6B7280).copy(alpha = 0.2f)
                            SyncState.ERROR -> StatusNotInterested.copy(alpha = 0.15f)
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .clickable { showSyncInfoDialog = true }
                            .padding(end = 4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .background(
                                        color = when (syncStatusInfo.state) {
                                            SyncState.SYNCED -> BrandGreenPrimary
                                            SyncState.SYNCING -> StatusInterested
                                            SyncState.OFFLINE -> Color(0xFF9CA3AF)
                                            SyncState.ERROR -> StatusNotInterested
                                        },
                                        shape = CircleShape
                                    )
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = when (syncStatusInfo.state) {
                                    SyncState.SYNCED -> "Cloud Active"
                                    SyncState.SYNCING -> "Syncing..."
                                    SyncState.OFFLINE -> "Offline Cache"
                                    SyncState.ERROR -> "Sync Warning"
                                },
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = when (syncStatusInfo.state) {
                                    SyncState.SYNCED -> BrandGreenPrimary
                                    SyncState.SYNCING -> StatusInterested
                                    SyncState.OFFLINE -> Color.White
                                    SyncState.ERROR -> StatusNotInterested
                                }
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            viewModel.syncEmployee(userSession.employeeId ?: "") {
                                showFeedback("Google Sheet Synchronized!")
                            }
                        }
                    ) {
                        if (isSyncing) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = BrandGreenPrimary, strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Sync, contentDescription = "Sync Sheet", tint = BrandGreenPrimary)
                        }
                    }
                    IconButton(onClick = onLogout) {
                        Icon(Icons.Default.Logout, contentDescription = "Sign Out", tint = StatusNotInterested)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BrandNavySurface)
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = BrandNavySurface,
                contentColor = Color.White
            ) {
                // 1. MY TODAY DATA (INBOX)
                NavigationBarItem(
                    selected = currentSubView == "INBOX",
                    onClick = { currentSubView = "INBOX" },
                    icon = {
                        BadgedBox(
                            badge = {
                                if (pendingLeads.isNotEmpty()) {
                                    Badge(containerColor = StatusPending, contentColor = Color.Black) {
                                        Text("${pendingLeads.size}", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        ) {
                            Icon(Icons.Default.Today, contentDescription = "My Today Data")
                        }
                    },
                    label = { Text("My Today Data", fontSize = 10.5.sp, fontWeight = if (currentSubView == "INBOX") FontWeight.Bold else FontWeight.Normal, maxLines = 1) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = StatusCallback,
                        selectedTextColor = StatusCallback,
                        indicatorColor = StatusCallbackBg,
                        unselectedIconColor = BrandTextSecondary,
                        unselectedTextColor = BrandTextSecondary
                    )
                )

                // 2. INTERESTED (Amber Highlight)
                NavigationBarItem(
                    selected = currentSubView == "INTERESTED",
                    onClick = { currentSubView = "INTERESTED" },
                    icon = {
                        BadgedBox(
                            badge = {
                                if (interestedLeads.isNotEmpty()) {
                                    Badge(containerColor = StatusInterested, contentColor = Color.Black) {
                                        Text("${interestedLeads.size}", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        ) {
                            Icon(Icons.Default.Star, contentDescription = "Interested")
                        }
                    },
                    label = { Text("Interested", fontSize = 11.sp, fontWeight = if (currentSubView == "INTERESTED") FontWeight.Bold else FontWeight.Normal) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = StatusInterested,
                        selectedTextColor = StatusInterested,
                        indicatorColor = StatusInterestedBg,
                        unselectedIconColor = BrandTextSecondary,
                        unselectedTextColor = BrandTextSecondary
                    )
                )

                // 3. DONE / SUCCESSFUL (Trophy Green)
                NavigationBarItem(
                    selected = currentSubView == "DONE",
                    onClick = { currentSubView = "DONE" },
                    icon = {
                        BadgedBox(
                            badge = {
                                if (doneLeads.isNotEmpty()) {
                                    Badge(containerColor = StatusSuccessful, contentColor = Color(0xFF071120)) {
                                        Text("${doneLeads.size}", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        ) {
                            Icon(Icons.Default.EmojiEvents, contentDescription = "Done / Successful")
                        }
                    },
                    label = { Text("Done", fontSize = 11.sp, fontWeight = if (currentSubView == "DONE") FontWeight.Bold else FontWeight.Normal) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = StatusSuccessful,
                        selectedTextColor = StatusSuccessful,
                        indicatorColor = StatusSuccessfulBg,
                        unselectedIconColor = BrandTextSecondary,
                        unselectedTextColor = BrandTextSecondary
                    )
                )

                // 4. METRICS / PERFORMANCE
                NavigationBarItem(
                    selected = currentSubView == "PERFORMANCE",
                    onClick = { currentSubView = "PERFORMANCE" },
                    icon = { Icon(Icons.Default.Analytics, contentDescription = "Performance") },
                    label = { Text("Metrics", fontSize = 11.sp, fontWeight = if (currentSubView == "PERFORMANCE") FontWeight.Bold else FontWeight.Normal) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = BrandGreenPrimary,
                        selectedTextColor = BrandGreenPrimary,
                        indicatorColor = BrandGreenPrimary.copy(alpha = 0.15f),
                        unselectedIconColor = BrandTextSecondary,
                        unselectedTextColor = BrandTextSecondary
                    )
                )

                // 5. PROFILE
                NavigationBarItem(
                    selected = currentSubView == "PROFILE",
                    onClick = { currentSubView = "PROFILE" },
                    icon = { Icon(Icons.Default.Person, contentDescription = "Profile") },
                    label = { Text("Profile", fontSize = 11.sp, fontWeight = if (currentSubView == "PROFILE") FontWeight.Bold else FontWeight.Normal) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = BrandGreenPrimary,
                        selectedTextColor = BrandGreenPrimary,
                        indicatorColor = BrandGreenPrimary.copy(alpha = 0.15f),
                        unselectedIconColor = BrandTextSecondary,
                        unselectedTextColor = BrandTextSecondary
                    )
                )
            }
        },
        containerColor = BrandNavyBg
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (currentSubView) {
                "INBOX" -> {
                    EmployeeInboxView(
                        myLeads = myLeads,
                        activeLead = activeLead,
                        activeInboxLeads = activeInboxLeads,
                        totalAssigned = totalAssigned,
                        totalAllSheetCount = totalAllSheetCount,
                        todayLeadsCount = todayLeads.size,
                        todayFormattedDisplay = todayFormattedDisplay,
                        onlyTodayMode = onlyTodayMode,
                        onToggleDateMode = { onlyTodayMode = !onlyTodayMode },
                        pendingCount = pendingLeads.size,
                        interestedCount = interestedLeads.size,
                        doneCount = doneLeads.size,
                        linkSentCount = linkSentCount,
                        selectedRemark = selectedRemark,
                        customNote = customNote,
                        selectedFollowupOption = selectedFollowupOption,
                        officialLink = officialLink,
                        companyName = currentCompany?.name ?: "SPIN101",
                        quickRemarks = quickRemarks,
                        savingLeadId = savingLeadId,
                        onSelectLead = { selectedLeadId = it.id },
                        onSelectRemark = {
                            selectedRemark = it
                            if (it.equals(RemarkConstants.CALLBACK, true) || it.equals(RemarkConstants.FOLLOW_UP, true)) {
                                selectedFollowupOption = "Today (+2 hrs)"
                                followupTimestamp = System.currentTimeMillis() + 7_200_000L
                            }
                        },
                        onCustomNoteChange = { customNote = it },
                        onSelectFollowupOption = { opt, ts ->
                            selectedFollowupOption = opt
                            followupTimestamp = ts
                        },
                        onDial = { dialNumber(it) },
                        onSendWhatsApp = { lead -> sendWhatsAppMessage(lead, "tmpl_default") },
                        onSendNormalSms = { lead -> sendNormalSms(lead, "tmpl_default") },
                        onOpenSendLinkDialog = {
                            if (activeLead != null) {
                                messageDialogLead = activeLead
                                selectedTemplateId = "tmpl_default"
                                customMessagePreview = buildMessageForLead(activeLead, "tmpl_default")
                                showMessageDialog = true
                            }
                        },
                        onOpenOfficialLink = { openOfficialLink() },
                        onInstantInterested = { lead -> instantSaveRemark(lead, RemarkConstants.INTERESTED) },
                        onInstantDone = { lead -> instantSaveRemark(lead, RemarkConstants.SUCCESSFUL) },
                        onSaveAndNext = { saveAndNext() }
                    )
                }

                "INTERESTED" -> {
                    EmployeeInterestedView(
                        interestedLeads = interestedLeads,
                        savingLeadId = savingLeadId,
                        onDial = { dialNumber(it) },
                        onSendWhatsApp = { lead -> sendWhatsAppMessage(lead, "tmpl_default") },
                        onSendNormalSms = { lead -> sendNormalSms(lead, "tmpl_default") },
                        onSendLink = { lead ->
                            messageDialogLead = lead
                            selectedTemplateId = "tmpl_default"
                            customMessagePreview = buildMessageForLead(lead, "tmpl_default")
                            showMessageDialog = true
                        },
                        onMarkSuccessful = { lead ->
                            markSuccessfulFromInterested(lead)
                        }
                    )
                }

                "DONE" -> {
                    EmployeeDoneView(
                        doneLeads = doneLeads,
                        onDial = { dialNumber(it) }
                    )
                }

                "PERFORMANCE" -> {
                    EmployeePerformanceView(
                        myLeads = myLeads,
                        activities = activities.filter { it.employeeId == userSession.employeeId },
                        companyName = currentCompany?.name ?: "SPIN101",
                        officialLink = officialLink
                    )
                }

                "PROFILE" -> {
                    EmployeeProfileView(
                        userSession = userSession,
                        currentEmployee = currentEmployee,
                        currentTl = currentTl,
                        currentCompany = currentCompany,
                        officialLink = officialLink,
                        isSyncing = isSyncing,
                        onSync = {
                            viewModel.syncEmployee(userSession.employeeId ?: "") {
                                showFeedback("Google Sheet Synchronized Successfully!")
                            }
                        },
                        onLogout = onLogout
                    )
                }
            }

            // Real-Time Animated Confirmation Banner
            AnimatedVisibility(
                visible = snackbarMessage != null,
                enter = fadeIn() + slideInVertically(initialOffsetY = { -it }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { -it }),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(16.dp)
            ) {
                Surface(
                    color = BrandNavySurfaceElevated,
                    shape = RoundedCornerShape(12.dp),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(BrandGreenPrimary)
                    ),
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = BrandGreenPrimary, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = snackbarMessage ?: "",
                            color = Color.White,
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        // Multi-Channel Link Generator & Dispatch Dialog
        if (showMessageDialog && messageDialogLead != null) {
            val lead = messageDialogLead!!
            AlertDialog(
                onDismissRequest = { showMessageDialog = false },
                containerColor = BrandNavySurface,
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Send, contentDescription = null, tint = StatusWhatsApp, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Send Verified App Link", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                },
                text = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Customer: ${lead.customerName} • +91 ${lead.phone}",
                            fontSize = 12.sp,
                            color = BrandTextSecondary
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        Text("SELECT TEMPLATE", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = StatusWhatsApp)
                        Spacer(modifier = Modifier.height(6.dp))

                        messageTemplates.forEach { tmpl ->
                            val isSelected = selectedTemplateId == tmpl.id
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) StatusWhatsApp.copy(alpha = 0.15f) else Color(0x0DFFFFFF))
                                    .border(1.dp, if (isSelected) StatusWhatsApp else Color.Transparent, RoundedCornerShape(8.dp))
                                    .clickable {
                                        selectedTemplateId = tmpl.id
                                        customMessagePreview = buildMessageForLead(lead, tmpl.id)
                                    }
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = {
                                        selectedTemplateId = tmpl.id
                                        customMessagePreview = buildMessageForLead(lead, tmpl.id)
                                    },
                                    colors = RadioButtonDefaults.colors(selectedColor = StatusWhatsApp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = tmpl.title,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Color.White else BrandTextSecondary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Text("MESSAGE PREVIEW", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = StatusWhatsApp)
                        Spacer(modifier = Modifier.height(4.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0x1A000000))
                                .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(8.dp))
                                .padding(10.dp)
                        ) {
                            Text(
                                text = customMessagePreview.ifBlank { buildMessageForLead(lead, selectedTemplateId) },
                                fontSize = 11.5.sp,
                                color = Color(0xFFE2E8F0),
                                lineHeight = 16.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "🔗 Verified URL: $officialLink",
                            fontSize = 11.sp,
                            color = StatusWhatsApp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                },
                confirmButton = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // 1. WhatsApp Direct
                        Button(
                            onClick = {
                                sendWhatsAppMessage(lead, selectedTemplateId)
                                showMessageDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = StatusWhatsApp, contentColor = Color.White),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Send via WhatsApp", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }

                        // 2. Normal SMS / Messages App Redirect
                        Button(
                            onClick = {
                                sendNormalSms(lead, selectedTemplateId)
                                showMessageDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = StatusCallback, contentColor = Color.White),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Send Normal SMS / Messages Inbox", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }

                        // 3. Copy & Share Sheet
                        OutlinedButton(
                            onClick = {
                                copyAndShareMessage(lead, selectedTemplateId)
                                showMessageDialog = false
                            },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth(),
                            border = CardDefaults.outlinedCardBorder().copy(
                                brush = androidx.compose.ui.graphics.SolidColor(Color(0x44FFFFFF))
                            )
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Copy Link & Share System Sheet", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                        }
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showMessageDialog = false }, modifier = Modifier.fillMaxWidth()) {
                        Text("Close", color = BrandTextSecondary)
                    }
                }
            )
        }

        // Live Cloud Persistence & Sync Info Dialog
        if (showSyncInfoDialog) {
            AlertDialog(
                onDismissRequest = { showSyncInfoDialog = false },
                containerColor = BrandNavySurface,
                titleContentColor = Color.White,
                textContentColor = BrandTextSecondary,
                icon = {
                    Icon(
                        Icons.Default.CloudDone,
                        contentDescription = null,
                        tint = BrandGreenPrimary,
                        modifier = Modifier.size(32.dp)
                    )
                },
                title = {
                    Text(
                        "Cloud Data Persistence Status",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = Color.White
                    )
                },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "Every call, remark, link sent, and conversion action is instantly persisted locally and streamed to Firestore.",
                            fontSize = 12.5.sp,
                            color = BrandTextSecondary
                        )

                        Surface(
                            color = Color(0x14FFFFFF),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Telecaller Tab:", fontSize = 12.sp, color = BrandTextSecondary)
                                    Text(
                                        currentEmployee?.employeeTabName ?: userSession.userName,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Company:", fontSize = 12.sp, color = BrandTextSecondary)
                                    Text(
                                        currentCompany?.name ?: "SPIN101",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = BrandGreenPrimary
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Firestore Engine:", fontSize = 12.sp, color = BrandTextSecondary)
                                    Text(
                                        "🟢 Real-time Active",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = BrandGreenPrimary
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Offline Cache:", fontSize = 12.sp, color = BrandTextSecondary)
                                    Text(
                                        "🟢 Enabled",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = BrandGreenPrimary
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Pending Cloud Sync:", fontSize = 12.sp, color = BrandTextSecondary)
                                    Text(
                                        "${syncStatusInfo.pendingCount} items",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.syncEmployee(userSession.employeeId ?: "") {
                                showFeedback("Sync refreshed!")
                            }
                            showSyncInfoDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BrandGreenPrimary, contentColor = Color(0xFF071120)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Sync Sheet Now", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showSyncInfoDialog = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Close", color = BrandTextSecondary)
                    }
                }
            )
        }
    }
}

// ==========================================
// SUB-VIEWS
// ==========================================

@Composable
private fun EmployeeInboxView(
    myLeads: List<Lead>,
    activeLead: Lead?,
    activeInboxLeads: List<Lead>,
    totalAssigned: Int,
    totalAllSheetCount: Int,
    todayLeadsCount: Int,
    todayFormattedDisplay: String,
    onlyTodayMode: Boolean,
    onToggleDateMode: () -> Unit,
    pendingCount: Int,
    interestedCount: Int,
    doneCount: Int,
    linkSentCount: Int,
    selectedRemark: String,
    customNote: String,
    selectedFollowupOption: String?,
    officialLink: String,
    companyName: String,
    quickRemarks: List<QuickRemark>,
    savingLeadId: String?,
    onSelectLead: (Lead) -> Unit,
    onSelectRemark: (String) -> Unit,
    onCustomNoteChange: (String) -> Unit,
    onSelectFollowupOption: (String, Long) -> Unit,
    onDial: (String) -> Unit,
    onSendWhatsApp: (Lead) -> Unit,
    onSendNormalSms: (Lead) -> Unit,
    onOpenSendLinkDialog: () -> Unit,
    onOpenOfficialLink: () -> Unit,
    onInstantInterested: (Lead) -> Unit,
    onInstantDone: (Lead) -> Unit,
    onSaveAndNext: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Daily Current Date & Active Scope Banner
        item {
            Surface(
                color = Color(0x1810E57A),
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x3310E57A))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = BrandGreenPrimary.copy(alpha = 0.2f),
                            shape = CircleShape
                        ) {
                            Icon(
                                Icons.Default.DateRange,
                                contentDescription = null,
                                tint = BrandGreenPrimary,
                                modifier = Modifier
                                    .padding(5.dp)
                                    .size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "DAILY DATE: $todayFormattedDisplay",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    color = BrandGreenPrimary,
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "CURRENT DATE",
                                        color = Color(0xFF071120),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                            Text(
                                text = if (onlyTodayMode) "Keval Aaj Ka Number Active ($todayLeadsCount numbers)" else "Showing All Sheet History ($totalAllSheetCount numbers)",
                                fontSize = 10.5.sp,
                                color = BrandTextSecondary
                            )
                        }
                    }

                    if (totalAllSheetCount > todayLeadsCount && todayLeadsCount > 0) {
                        Surface(
                            onClick = onToggleDateMode,
                            color = if (onlyTodayMode) BrandGreenPrimary.copy(alpha = 0.25f) else Color(0x22FFFFFF),
                            shape = RoundedCornerShape(6.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (onlyTodayMode) BrandGreenPrimary else Color(0x44FFFFFF))
                        ) {
                            Text(
                                text = if (onlyTodayMode) "Aaj Ka Only ✓" else "Show Aaj Ka",
                                color = if (onlyTodayMode) BrandGreenPrimary else Color.White,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }
        }

        // Distinct Semantic KPI Banner
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = BrandNavySurface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp, horizontal = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    MiniKPI(label = "Today Data", value = "$totalAssigned", color = Color.White)
                    Divider(modifier = Modifier.height(24.dp).width(1.dp), color = Color(0x22FFFFFF))
                    MiniKPI(label = "Pending", value = "$pendingCount", color = StatusPending)
                    Divider(modifier = Modifier.height(24.dp).width(1.dp), color = Color(0x22FFFFFF))
                    MiniKPI(label = "Interested", value = "$interestedCount", color = StatusInterested)
                    Divider(modifier = Modifier.height(24.dp).width(1.dp), color = Color(0x22FFFFFF))
                    MiniKPI(label = "Done", value = "$doneCount", color = StatusSuccessful)
                    Divider(modifier = Modifier.height(24.dp).width(1.dp), color = Color(0x22FFFFFF))
                    MiniKPI(label = "Link Sent", value = "$linkSentCount", color = StatusWhatsApp)
                }
            }
        }

        // Active Lead Work Station
        item {
            if (activeLead != null) {
                val isSavingThis = savingLeadId == activeLead.id
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = BrandNavySurface),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(StatusCallback.copy(alpha = 0.6f))
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    color = StatusCallback,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "#${activeLead.sourceRowIndex}",
                                        color = Color.White,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 13.sp,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = activeLead.customerName.ifBlank { "Customer #${activeLead.sourceRowIndex}" },
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 17.sp,
                                        color = Color.White
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "Row #${activeLead.sourceRowIndex} • Calls: ${activeLead.callCount}",
                                            fontSize = 11.sp,
                                            color = BrandTextMuted
                                        )
                                        if (activeLead.dateStr.isNotBlank()) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Surface(
                                                color = Color(0x2210E57A),
                                                shape = RoundedCornerShape(3.dp)
                                            ) {
                                                Text(
                                                    text = "📅 ${activeLead.dateStr}",
                                                    color = BrandGreenPrimary,
                                                    fontSize = 9.5.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            if (activeLead.linkSent) {
                                StatusBadge(label = "LINK SENT", color = StatusWhatsApp, icon = Icons.Default.Check)
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Primary Action: CALL NOW
                        AnimatedActionButton(
                            onClick = { onDial(activeLead.phone) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp),
                            containerColor = StatusCallback,
                            contentColor = Color.White
                        ) {
                            Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "CALL +91 ${activeLead.phone}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.5.sp,
                                maxLines = 1
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Multi-Channel Dispatch Actions: WhatsApp, Normal SMS & Custom Templates
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            // 1. WhatsApp Direct
                            AnimatedActionButton(
                                onClick = { onSendWhatsApp(activeLead) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp),
                                containerColor = StatusWhatsApp,
                                contentColor = Color.White
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("WhatsApp", fontWeight = FontWeight.Bold, fontSize = 11.5.sp)
                            }

                            // 2. Normal SMS / Messages Inbox Direct
                            AnimatedActionButton(
                                onClick = { onSendNormalSms(activeLead) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(40.dp),
                                containerColor = Color(0xFF0284C7),
                                contentColor = Color.White
                            ) {
                                Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Normal SMS", fontWeight = FontWeight.Bold, fontSize = 11.5.sp)
                            }

                            // 3. Templates & Share Dialog
                            AnimatedActionButton(
                                onClick = onOpenSendLinkDialog,
                                modifier = Modifier
                                    .weight(0.9f)
                                    .height(40.dp),
                                containerColor = Color(0x22FFFFFF),
                                contentColor = Color.White,
                                borderColor = Color(0x33FFFFFF)
                            ) {
                                Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(13.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Share ↗", fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Official Game Link Chip
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0x14FFFFFF))
                                .clickable { onOpenOfficialLink() }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.OpenInBrowser, contentDescription = null, tint = BrandGreenPrimary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "$companyName: $officialLink",
                                    fontSize = 11.5.sp,
                                    color = BrandGreenPrimary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Text("Open ↗", fontSize = 11.sp, color = BrandTextSecondary)
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Fast 1-Tap Workflow: INTERESTED & SUCCESSFUL (Distinct Identity)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AnimatedActionButton(
                                onClick = { onInstantInterested(activeLead) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp),
                                containerColor = StatusInterestedBg,
                                contentColor = StatusInterested,
                                borderColor = StatusInterested,
                                isLoading = isSavingThis && selectedRemark == RemarkConstants.INTERESTED
                            ) {
                                Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("⭐ Interested", fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                            }

                            AnimatedActionButton(
                                onClick = { onInstantDone(activeLead) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp),
                                containerColor = StatusSuccessful,
                                contentColor = Color(0xFF071120),
                                isLoading = isSavingThis && selectedRemark == RemarkConstants.SUCCESSFUL
                            ) {
                                Icon(Icons.Default.EmojiEvents, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("🏆 Mark Done", fontSize = 12.5.sp, fontWeight = FontWeight.ExtraBold)
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Remark Selection Grid
                        Text(
                            text = "SELECT CALL REMARK",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = StatusCallback,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        val remarksList = if (quickRemarks.isNotEmpty()) quickRemarks.map { it.label } else RemarkConstants.ALL_REMARKS
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            remarksList.chunked(3).forEach { rowRemarks ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    rowRemarks.forEach { remark ->
                                        val isSelected = selectedRemark.equals(remark, ignoreCase = true)
                                        val chipColor = when {
                                            remark.equals(RemarkConstants.INTERESTED, true) -> StatusInterested
                                            remark.equals(RemarkConstants.SUCCESSFUL, true) || remark.equals(RemarkConstants.DONE, true) -> StatusSuccessful
                                            remark.equals(RemarkConstants.CALLBACK, true) || remark.equals(RemarkConstants.FOLLOW_UP, true) -> StatusCallback
                                            remark.equals(RemarkConstants.NOT_INTERESTED, true) || remark.equals(RemarkConstants.SWITCH_OFF, true) -> StatusNotInterested
                                            else -> Color(0xFF94A3B8)
                                        }

                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (isSelected) chipColor else Color(0x14FFFFFF))
                                                .border(1.dp, if (isSelected) chipColor else Color.Transparent, RoundedCornerShape(8.dp))
                                                .clickable { onSelectRemark(remark) }
                                                .padding(vertical = 9.dp, horizontal = 4.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = remark,
                                                fontSize = 11.sp,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = if (isSelected) (if (chipColor == StatusSuccessful) Color(0xFF071120) else Color.White) else Color.White,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                    repeat(3 - rowRemarks.size) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }

                        // Follow-up Schedule
                        val isFollowupNeeded = selectedRemark.equals(RemarkConstants.CALLBACK, true) ||
                                selectedRemark.equals(RemarkConstants.FOLLOW_UP, true)

                        if (isFollowupNeeded) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "SCHEDULE CALLBACK / FOLLOW-UP",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = StatusCallback
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf(
                                    "Today (+2 hrs)" to (System.currentTimeMillis() + 7_200_000L),
                                    "Tomorrow (10 AM)" to (System.currentTimeMillis() + 86_400_000L),
                                    "In 2 Days" to (System.currentTimeMillis() + 172_800_000L)
                                ).forEach { (label, ts) ->
                                    val isSelected = selectedFollowupOption == label
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (isSelected) StatusCallback else Color(0x14FFFFFF))
                                            .clickable { onSelectFollowupOption(label, ts) }
                                            .padding(vertical = 7.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = label,
                                            fontSize = 10.5.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = Color.White
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Custom Notes
                        OutlinedTextField(
                            value = customNote,
                            onValueChange = onCustomNoteChange,
                            placeholder = { Text("Call remarks (e.g. Will deposit after 7 PM)...", fontSize = 12.sp, color = BrandTextMuted) },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 2,
                            maxLines = 3,
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = StatusCallback,
                                unfocusedBorderColor = Color(0x33FFFFFF),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Save & Next
                        AnimatedActionButton(
                            onClick = onSaveAndNext,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            containerColor = Color(0xFF1E293B),
                            contentColor = Color.White,
                            borderColor = Color(0x44FFFFFF)
                        ) {
                            Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("SAVE & NEXT NUMBER", fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                        }
                    }
                }
            }
        }

        // Queue List
        item {
            Text(
                text = "MY TODAY DATA (${myLeads.size} NUMBERS IN SHEET ORDER)",
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                color = BrandTextSecondary,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(top = 6.dp)
            )
        }

        items(myLeads) { lead ->
            val isCurrent = lead.id == activeLead?.id
            val statusColor = when (lead.status) {
                "INTERESTED" -> StatusInterested
                "SUCCESSFUL" -> StatusSuccessful
                "CALLBACK", "FOLLOW_UP" -> StatusCallback
                "SWITCH_OFF", "NOT_INTERESTED" -> StatusNotInterested
                else -> StatusPending
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isCurrent) BrandNavySurface.copy(alpha = 0.9f) else Color(0x0DFFFFFF))
                    .border(
                        1.dp,
                        if (isCurrent) StatusCallback else Color.Transparent,
                        RoundedCornerShape(10.dp)
                    )
                    .clickable { onSelectLead(lead) }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = statusColor.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = "#${lead.sourceRowIndex}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = statusColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "+91 ${lead.phone}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = if (isCurrent) StatusCallback else Color.White
                            )
                            if (lead.dateStr.isNotBlank()) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = lead.dateStr,
                                    fontSize = 10.sp,
                                    color = BrandTextMuted
                                )
                            }
                        }
                        Text(
                            text = if (lead.currentRemark.isNotBlank()) "Remark: ${lead.currentRemark}" else "Status: ${lead.status}",
                            fontSize = 11.sp,
                            color = statusColor
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (lead.linkSent) {
                        Icon(Icons.Default.Send, contentDescription = "Link Sent", tint = StatusWhatsApp, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    IconButton(
                        onClick = { onDial(lead.phone) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.Phone, contentDescription = "Call", tint = StatusCallback, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

// ==========================================
// INTERESTED TAB (WITH PROMINENT MARK SUCCESSFUL)
// ==========================================

@Composable
private fun EmployeeInterestedView(
    interestedLeads: List<Lead>,
    savingLeadId: String?,
    onDial: (String) -> Unit,
    onSendWhatsApp: (Lead) -> Unit,
    onSendNormalSms: (Lead) -> Unit,
    onSendLink: (Lead) -> Unit,
    onMarkSuccessful: (Lead) -> Unit
) {
    val timeFormat = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Star, contentDescription = null, tint = StatusInterested, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "INTERESTED CUSTOMERS (${interestedLeads.size})",
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = StatusInterested
                    )
                }
                Text(
                    text = "Convert to Successful",
                    fontSize = 11.sp,
                    color = BrandTextSecondary
                )
            }
        }

        if (interestedLeads.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(
                            shape = CircleShape,
                            color = StatusInterestedBg,
                            modifier = Modifier.size(64.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.StarBorder, contentDescription = null, tint = StatusInterested, modifier = Modifier.size(32.dp))
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("No Interested Leads in Queue", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "When a customer shows interest, tap ⭐ Interested in My Today Data.",
                            color = BrandTextSecondary,
                            fontSize = 12.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                    }
                }
            }
        } else {
            items(interestedLeads, key = { it.id }) { lead ->
                val isSaving = savingLeadId == lead.id

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = BrandNavySurface),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = androidx.compose.ui.graphics.SolidColor(StatusInterested.copy(alpha = 0.5f))
                    )
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        // Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    color = StatusInterested,
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = "#${lead.sourceRowIndex}",
                                        color = Color(0xFF071120),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.5.sp,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "+91 ${lead.phone}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = Color.White
                                )
                            }

                            StatusBadge(label = "INTERESTED", color = StatusInterested, icon = Icons.Default.Star)
                        }

                        // Status Journey Timeline
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            color = Color(0x0FFFFFFF),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "CUSTOMER JOURNEY",
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = StatusInterested
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("• Added from Sheet Row #${lead.sourceRowIndex}", fontSize = 11.sp, color = BrandTextSecondary)
                                }
                                if (lead.linkSent) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("• Official Game Link Sent via WhatsApp", fontSize = 11.sp, color = StatusWhatsApp)
                                    }
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    val markedTime = if (lead.updatedAt > 0) timeFormat.format(Date(lead.updatedAt)) else "Today"
                                    Text("• Marked Interested — $markedTime", fontSize = 11.sp, color = StatusInterested, fontWeight = FontWeight.SemiBold)
                                }
                                if (!lead.notes.isNullOrBlank()) {
                                    Text("• Note: ${lead.notes}", fontSize = 11.sp, color = Color.White)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // HIGH-PROMINENCE ACTION: [ 🏆 MARK SUCCESSFUL ]
                        AnimatedActionButton(
                            onClick = { onMarkSuccessful(lead) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp),
                            containerColor = StatusSuccessful,
                            contentColor = Color(0xFF071120),
                            isLoading = isSaving
                        ) {
                            Icon(Icons.Default.EmojiEvents, contentDescription = null, modifier = Modifier.size(20.dp), tint = Color(0xFF071120))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "🏆 MARK SUCCESSFUL / CONVERT",
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.5.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Multi-Channel Action Row: Call, WhatsApp & Normal SMS
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            AnimatedActionButton(
                                onClick = { onDial(lead.phone) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp),
                                containerColor = StatusCallbackBg,
                                contentColor = StatusCallback,
                                borderColor = StatusCallback.copy(alpha = 0.5f)
                            ) {
                                Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Call", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            AnimatedActionButton(
                                onClick = { onSendWhatsApp(lead) },
                                modifier = Modifier
                                    .weight(1.1f)
                                    .height(38.dp),
                                containerColor = StatusWhatsAppBg,
                                contentColor = StatusWhatsApp,
                                borderColor = StatusWhatsApp.copy(alpha = 0.5f)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("WhatsApp", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                            }

                            AnimatedActionButton(
                                onClick = { onSendNormalSms(lead) },
                                modifier = Modifier
                                    .weight(1.1f)
                                    .height(38.dp),
                                containerColor = Color(0x200284C7),
                                contentColor = Color(0xFF38BDF8),
                                borderColor = Color(0xFF0284C7).copy(alpha = 0.5f)
                            ) {
                                Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Normal SMS", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// DONE / SUCCESSFUL TAB
// ==========================================

@Composable
private fun EmployeeDoneView(
    doneLeads: List<Lead>,
    onDial: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = StatusSuccessful, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "COMPLETED / SUCCESSFUL (${doneLeads.size})",
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = StatusSuccessful
                    )
                }
                Text(
                    text = "Permanent Records",
                    fontSize = 11.sp,
                    color = BrandTextSecondary
                )
            }
        }

        if (doneLeads.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.CheckCircleOutline, contentDescription = null, tint = BrandTextMuted, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("No Completed Leads Yet", color = Color.White, fontWeight = FontWeight.Bold)
                        Text("Completed numbers remain safely preserved here.", color = BrandTextSecondary, fontSize = 12.sp)
                    }
                }
            }
        } else {
            items(doneLeads, key = { it.id }) { lead ->
                val isConvertedFromInterested = lead.previousRemark.equals("INTERESTED", true) ||
                        lead.notes?.contains("Interested", true) == true

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = BrandNavySurface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = StatusSuccessfulBg,
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "#${lead.sourceRowIndex}",
                                    color = StatusSuccessful,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "+91 ${lead.phone}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.5.sp,
                                    color = Color.White
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = lead.currentRemark.ifBlank { lead.status },
                                        fontSize = 11.5.sp,
                                        color = StatusSuccessful,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    if (isConvertedFromInterested) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            color = StatusInterestedBg,
                                            shape = RoundedCornerShape(3.dp)
                                        ) {
                                            Text(
                                                text = "⭐ Converted",
                                                color = StatusInterested,
                                                fontSize = 9.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        IconButton(onClick = { onDial(lead.phone) }, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Default.Phone, contentDescription = "Call", tint = StatusCallback, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// PERFORMANCE / METRICS TAB (WITH CONVERSION RATE)
// ==========================================

@Composable
private fun EmployeePerformanceView(
    myLeads: List<Lead>,
    activities: List<ActivityRecord>,
    companyName: String,
    officialLink: String
) {
    val total = myLeads.size
    val done = myLeads.count { it.status == "COMPLETED" || it.status == "SUCCESSFUL" || (it.currentRemark.isNotBlank() && it.currentRemark != RemarkConstants.PENDING) }
    val remaining = (total - done).coerceAtLeast(0)
    val completionPct = if (total > 0) (done.toFloat() / total) * 100f else 0f

    val connected = myLeads.count { RemarkConstants.isConnected(it.currentRemark) }
    val nonConnected = myLeads.count { RemarkConstants.isNonConnect(it.currentRemark) }
    val interested = myLeads.count { it.currentRemark.equals(RemarkConstants.INTERESTED, true) || it.status == "INTERESTED" }
    val successful = myLeads.count { it.status == "SUCCESSFUL" || it.currentRemark.equals(RemarkConstants.SUCCESSFUL, true) }
    val linksSent = myLeads.count { it.linkSent }

    // Conversion rate: Interested -> Successful
    val totalOpportunities = (interested + successful).coerceAtLeast(1)
    val conversionRate = if (totalOpportunities > 0) (successful.toFloat() / totalOpportunities) * 100f else 0f

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("TODAY'S PERFORMANCE & CONVERSION", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BrandGreenPrimary, letterSpacing = 1.sp)
        }

        // Interested -> Successful Conversion Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = BrandNavySurface),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(StatusInterested.copy(alpha = 0.6f))
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Interested → Successful Conversion", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.5.sp)
                            Text("$successful Converted out of $totalOpportunities Interested", color = BrandTextSecondary, fontSize = 11.5.sp)
                        }
                        Surface(
                            color = StatusSuccessfulBg,
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "${"%.1f".format(conversionRate)}%",
                                color = StatusSuccessful,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    LinearProgressIndicator(
                        progress = { (conversionRate / 100f).coerceIn(0f, 1f) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = StatusSuccessful,
                        trackColor = Color(0x22FFFFFF)
                    )
                }
            }
        }

        // Completion Progress Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = BrandNavySurface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Queue Completion", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.5.sp)
                        Text("${"%.1f".format(completionPct)}%", color = StatusCallback, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { completionPct / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = StatusCallback,
                        trackColor = Color(0x22FFFFFF)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Done: $done / $total", color = BrandTextSecondary, fontSize = 12.sp)
                        Text("Remaining: $remaining", color = StatusPending, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Metrics Grid (Distinct Semantic Colors)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricCard(
                    modifier = Modifier.weight(1f),
                    title = "Successful Leads",
                    value = "$successful",
                    color = StatusSuccessful,
                    icon = Icons.Default.EmojiEvents
                )
                MetricCard(
                    modifier = Modifier.weight(1f),
                    title = "Interested Leads",
                    value = "$interested",
                    color = StatusInterested,
                    icon = Icons.Default.Star
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricCard(
                    modifier = Modifier.weight(1f),
                    title = "Links Sent",
                    value = "$linksSent",
                    color = StatusWhatsApp,
                    icon = Icons.Default.Send
                )
                MetricCard(
                    modifier = Modifier.weight(1f),
                    title = "Connected Calls",
                    value = "$connected",
                    color = StatusCallback,
                    icon = Icons.Default.PhoneInTalk
                )
            }
        }

        // Remarks Breakdown
        item {
            Text("CALL REMARKS AUDIT BREAKDOWN", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = BrandTextSecondary, modifier = Modifier.padding(top = 6.dp))
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = BrandNavySurface)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    RemarkConstants.ALL_REMARKS.forEach { remark ->
                        val count = myLeads.count { it.currentRemark.equals(remark, true) }
                        if (count > 0) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(remark, color = Color.White, fontSize = 12.sp)
                                Text("$count", color = StatusCallback, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                            Divider(color = Color(0x0DFFFFFF))
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// PROFILE TAB
// ==========================================

@Composable
private fun EmployeeProfileView(
    userSession: UserSession,
    currentEmployee: Employee?,
    currentTl: TeamLeader?,
    currentCompany: Company?,
    officialLink: String,
    isSyncing: Boolean,
    onSync: () -> Unit,
    onLogout: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("LOGGED-IN TELECALLER PROFILE", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BrandGreenPrimary)

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = BrandNavySurface)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ProfileRow("Name", userSession.userName.ifBlank { currentEmployee?.name ?: "Telecaller" })
                ProfileRow("Assigned Sheet Tab", currentEmployee?.employeeTabName ?: currentEmployee?.name ?: "Default")
                ProfileRow("Supervisor (TL)", currentTl?.name ?: "Assigned TL")
                ProfileRow("Company", currentCompany?.name ?: "SPIN101")
                ProfileRow("Official Game Link", officialLink)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        AnimatedActionButton(
            onClick = onSync,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            containerColor = StatusCallback,
            contentColor = Color.White
        ) {
            if (isSyncing) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Synchronizing Google Sheet...", fontWeight = FontWeight.Bold)
            } else {
                Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("SYNC LATEST GOOGLE SHEET DATA", fontWeight = FontWeight.Bold)
            }
        }

        AnimatedActionButton(
            onClick = onLogout,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            containerColor = StatusNotInterestedBg,
            contentColor = StatusNotInterested,
            borderColor = StatusNotInterested
        ) {
            Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("SIGN OUT / SWITCH USER", fontWeight = FontWeight.Bold)
        }
    }
}

// ==========================================
// REUSABLE MICRO-INTERACTIVE COMPONENTS
// ==========================================

@Composable
fun AnimatedActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color,
    contentColor: Color,
    borderColor: Color? = null,
    isLoading: Boolean = false,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
        label = "button_scale"
    )

    Surface(
        onClick = onClick,
        enabled = enabled && !isLoading,
        shape = RoundedCornerShape(10.dp),
        color = if (enabled) containerColor else containerColor.copy(alpha = 0.5f),
        contentColor = contentColor,
        border = borderColor?.let { androidx.compose.foundation.BorderStroke(1.dp, it) },
        interactionSource = interactionSource,
        modifier = modifier.scale(scale)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = contentColor,
                    strokeWidth = 2.dp
                )
            } else {
                content()
            }
        }
    }
}

@Composable
fun StatusBadge(
    label: String,
    color: Color,
    icon: ImageVector? = null
) {
    Surface(
        color = color.copy(alpha = 0.18f),
        shape = RoundedCornerShape(6.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(11.dp))
                Spacer(modifier = Modifier.width(3.dp))
            }
            Text(
                text = label,
                color = color,
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}

@Composable
private fun ProfileRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = BrandTextSecondary, fontSize = 12.sp)
        Text(value, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 12.5.sp)
    }
}

@Composable
private fun MetricCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    color: Color,
    icon: ImageVector
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = BrandNavySurface)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text(text = title, fontSize = 11.sp, color = BrandTextSecondary)
        }
    }
}

@Composable
private fun MiniKPI(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, fontWeight = FontWeight.Bold, fontSize = 13.5.sp, color = color)
        Text(text = label, fontSize = 9.sp, color = BrandTextSecondary)
    }
}
