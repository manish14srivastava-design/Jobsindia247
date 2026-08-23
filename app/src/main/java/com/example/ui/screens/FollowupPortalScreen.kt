package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.RemarkConstants
import com.example.ui.theme.*
import com.example.ui.viewmodel.TrackingViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FollowupPortalScreen(
    viewModel: TrackingViewModel,
    onOpenDrawer: () -> Unit
) {
    val context = LocalContext.current
    val companies by viewModel.companies.collectAsState()
    val teamLeaders by viewModel.teamLeaders.collectAsState()
    val employees by viewModel.employees.collectAsState()
    val allCalls by viewModel.calls.collectAsState()
    val followups by viewModel.followups.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val lastSyncedAt by viewModel.lastSyncedAt.collectAsState()

    val portalCompanyId by viewModel.portalCompanyId.collectAsState()
    val portalTlId by viewModel.portalTlId.collectAsState()
    val portalEmployeeId by viewModel.portalEmployeeId.collectAsState()
    val portalTab by viewModel.portalTab.collectAsState()

    val selectedEmp = employees.find { it.id == portalEmployeeId }
    val empCalls = remember(allCalls, portalEmployeeId) {
        if (portalEmployeeId == null) emptyList() else allCalls.filter { it.employeeId == portalEmployeeId }
    }

    val interestedLeads = remember(empCalls) {
        empCalls.filter { it.remark.equals(RemarkConstants.INTERESTED, ignoreCase = true) }
    }

    val todayNumbers = remember(empCalls) {
        empCalls
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(BrandNavyBg)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 40.dp)
    ) {
        // Top Header
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
                        Text(
                            text = "TELECALLER PORTAL",
                            style = MaterialTheme.typography.labelSmall.copy(color = BrandGreenPrimary, fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Follow-up Control Room",
                            style = MaterialTheme.typography.titleMedium.copy(color = Color.White, fontWeight = FontWeight.Bold)
                        )
                    }
                }

                if (selectedEmp != null) {
                    TextButton(
                        onClick = { viewModel.setPortalCompany(null) }
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp), tint = BrandBlueSecondary)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Switch user", color = BrandBlueSecondary, fontSize = 12.sp)
                    }
                }
            }
        }

        // Stepper Selection if no employee picked yet
        if (selectedEmp == null) {
            item {
                // Step 1: Select Company
                StepSelectionCard(
                    stepNumber = 1,
                    title = "Select Your Company",
                    icon = Icons.Default.Business,
                    isSelected = portalCompanyId != null,
                    selectedValue = companies.find { it.id == portalCompanyId }?.name,
                    onReset = { viewModel.setPortalCompany(null) }
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        companies.forEach { comp ->
                            SelectionItem(
                                label = comp.name,
                                isChosen = portalCompanyId == comp.id,
                                onClick = { viewModel.setPortalCompany(comp.id) }
                            )
                        }
                    }
                }
            }

            if (portalCompanyId != null) {
                item {
                    val availableTls = teamLeaders.filter { it.companyId == portalCompanyId }
                    // Step 2: Select Team Leader
                    StepSelectionCard(
                        stepNumber = 2,
                        title = "Select Your Team Leader",
                        icon = Icons.Default.SupervisorAccount,
                        isSelected = portalTlId != null,
                        selectedValue = teamLeaders.find { it.id == portalTlId }?.name,
                        onReset = { viewModel.setPortalTl(null) }
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            availableTls.forEach { tl ->
                                SelectionItem(
                                    label = tl.name,
                                    isChosen = portalTlId == tl.id,
                                    onClick = { viewModel.setPortalTl(tl.id) }
                                )
                            }
                        }
                    }
                }
            }

            if (portalCompanyId != null && portalTlId != null) {
                item {
                    val availableEmps = employees.filter { it.companyId == portalCompanyId && it.teamLeaderId == portalTlId }
                    // Step 3: Select Name
                    StepSelectionCard(
                        stepNumber = 3,
                        title = "Select Your Name",
                        icon = Icons.Default.Person,
                        isSelected = false,
                        selectedValue = null
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            availableEmps.forEach { emp ->
                                SelectionItem(
                                    label = emp.name,
                                    isChosen = false,
                                    onClick = { viewModel.setPortalEmployee(emp.id) }
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // Selected Employee Workspace
            val successfulCount = interestedLeads.count { followups[it.phone]?.successful == true }
            val successPct = if (interestedLeads.isNotEmpty()) (successfulCount.toFloat() / interestedLeads.size) * 100f else 0f

            item {
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = BrandNavySurface),
                    border = CardDefaults.outlinedCardBorder().copy(brush = Brush.verticalGradient(listOf(Color(0x33FFFFFF), Color(0x11FFFFFF))))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Box(
                                modifier = Modifier.size(44.dp).clip(CircleShape).background(BrandGreenPrimary),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = selectedEmp.name.split(" ").map { it.take(1) }.take(2).joinToString(""),
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF071120)
                                )
                            }
                            Column {
                                Text(selectedEmp.name, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 16.sp)
                                Text(
                                    text = "${companies.find { it.id == selectedEmp.companyId }?.name ?: "—"} · TL ${teamLeaders.find { it.id == selectedEmp.teamLeaderId }?.name ?: "—"}",
                                    fontSize = 11.sp,
                                    color = BrandTextSecondary
                                )
                            }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            PortalKpiChip("Interested", "${interestedLeads.size}", BrandBlueSecondary)
                            PortalKpiChip("Successful", "$successfulCount (${String.format("%.0f", successPct)}%)", BrandGreenPrimary)
                        }
                    }
                }
            }

            // Tab Selector
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
                        label = "Interested Leads (${interestedLeads.size})",
                        isSelected = portalTab == "interested",
                        onClick = { viewModel.setPortalTab("interested") },
                        modifier = Modifier.weight(1f)
                    )
                    TabButton(
                        label = "Today's Inbox (${todayNumbers.size})",
                        isSelected = portalTab == "inbox",
                        onClick = { viewModel.setPortalTab("inbox") },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Leads List with Live Action Triggers
            val activeList = if (portalTab == "interested") interestedLeads else todayNumbers

            if (activeList.isEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = BrandNavySurface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(modifier = Modifier.padding(24.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text(
                                text = if (portalTab == "interested") "No interested leads assigned yet." else "No numbers found for today.",
                                color = BrandTextMuted,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            } else {
                itemsIndexed(activeList) { idx, lead ->
                    val phone10 = lead.phone.takeLast(10)
                    val followup = followups[lead.phone]
                    val isSuccess = followup?.successful == true
                    val isSent = followup?.messageSent == true

                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = if (isSuccess) StatusSuccess.copy(alpha = 0.08f) else BrandNavySurface),
                        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.verticalGradient(listOf(if (isSuccess) StatusSuccess.copy(alpha = 0.4f) else Color(0x22FFFFFF), Color(0x11FFFFFF))))
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("#${idx + 1}", fontSize = 12.sp, color = BrandTextMuted, fontWeight = FontWeight.Bold)
                                    Column {
                                        Text(
                                            text = "+91 $phone10",
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 14.sp
                                        )
                                        Text(
                                            text = "Called: ${lead.remark.ifBlank { "Pending Dialing" }}",
                                            fontSize = 10.5.sp,
                                            color = if (lead.remark.equals(RemarkConstants.INTERESTED, ignoreCase = true)) BrandGreenPrimary else BrandTextSecondary
                                        )
                                    }
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    // Mark Successful toggle
                                    Button(
                                        onClick = { viewModel.toggleFollowupSuccessful(selectedEmp.id, lead.phone) },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isSuccess) StatusSuccess else Color(0xFF1B2C4E),
                                            contentColor = if (isSuccess) Color(0xFF071120) else Color.White
                                        ),
                                        shape = RoundedCornerShape(6.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                        modifier = Modifier.height(28.dp)
                                    ) {
                                        Icon(if (isSuccess) Icons.Default.CheckCircle else Icons.Default.Check, contentDescription = null, modifier = Modifier.size(12.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(if (isSuccess) "Successful" else "Mark Success", fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold)
                                    }

                                    // Mark Done toggle
                                    Button(
                                        onClick = { viewModel.toggleMessageSent(selectedEmp.id, lead.phone, "Followed up on telecaller portal") },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isSent) BrandBlueSecondary else Color(0xFF16233F),
                                            contentColor = Color.White
                                        ),
                                        shape = RoundedCornerShape(6.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                        modifier = Modifier.height(28.dp)
                                    ) {
                                        Text(if (isSent) "Done" else "Mark Done", fontSize = 10.5.sp)
                                    }
                                }
                            }

                            // Quick Action Buttons Bar: Call, SMS/Inbox, WhatsApp, Telegram
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                // Call
                                QuickActionButton(
                                    label = "Call",
                                    icon = Icons.Default.Phone,
                                    color = BrandGreenPrimary,
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:+91$phone10"))
                                        context.startActivity(intent)
                                    }
                                )

                                // SMS
                                QuickActionButton(
                                    label = "SMS",
                                    icon = Icons.Default.Chat,
                                    color = BrandBlueSecondary,
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        val message = "Hello, following up regarding your application with JobsIndia247. Best regards, ${selectedEmp.name}"
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("sms:$phone10?body=${Uri.encode(message)}"))
                                        context.startActivity(intent)
                                    }
                                )

                                // WhatsApp
                                QuickActionButton(
                                    label = "WhatsApp",
                                    icon = Icons.Default.Send,
                                    color = Color(0xFF25D366),
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        val message = "Hi, this is ${selectedEmp.name} from JobsIndia247 following up with you. Please let us know your current availability."
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/91$phone10?text=${Uri.encode(message)}"))
                                        context.startActivity(intent)
                                    }
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
fun StepSelectionCard(
    stepNumber: Int,
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isSelected: Boolean,
    selectedValue: String?,
    onReset: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
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
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        modifier = Modifier.size(24.dp).clip(CircleShape).background(BrandGreenPrimary.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(stepNumber.toString(), color = BrandGreenPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                    Icon(icon, contentDescription = null, tint = BrandTextSecondary, modifier = Modifier.size(16.dp))
                    Text(title, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.5.sp)
                }

                if (isSelected && selectedValue != null && onReset != null) {
                    TextButton(onClick = onReset) {
                        Text("$selectedValue · Change", color = BrandGreenPrimary, fontSize = 11.sp)
                    }
                }
            }

            if (!isSelected) {
                Spacer(modifier = Modifier.height(10.dp))
                content()
            }
        }
    }
}

@Composable
fun SelectionItem(label: String, isChosen: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        color = if (isChosen) BrandGreenPrimary.copy(alpha = 0.15f) else BrandNavySurfaceLight,
        shape = RoundedCornerShape(8.dp),
        border = ButtonDefaults.outlinedButtonBorder().copy(brush = Brush.linearGradient(listOf(if (isChosen) BrandGreenPrimary else Color(0x22FFFFFF), Color(0x11FFFFFF))))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = BrandTextSecondary, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
fun PortalKpiChip(label: String, value: String, color: Color) {
    Surface(
        color = color.copy(alpha = 0.12f),
        shape = RoundedCornerShape(8.dp),
        border = ButtonDefaults.outlinedButtonBorder().copy(brush = Brush.linearGradient(listOf(color.copy(alpha = 0.4f), color.copy(alpha = 0.1f))))
    ) {
        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label.uppercase(), fontSize = 9.sp, color = color, fontWeight = FontWeight.Bold)
            Text(value, fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun TabButton(label: String, isSelected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) BrandGreenPrimary else Color.Transparent)
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) Color(0xFF071120) else BrandTextSecondary
        )
    }
}

@Composable
fun QuickActionButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.clickable { onClick() },
        color = color.copy(alpha = 0.12f),
        shape = RoundedCornerShape(6.dp),
        border = ButtonDefaults.outlinedButtonBorder().copy(brush = Brush.linearGradient(listOf(color.copy(alpha = 0.4f), color.copy(alpha = 0.1f))))
    ) {
        Row(
            modifier = Modifier.padding(vertical = 6.dp, horizontal = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = label, tint = color, modifier = Modifier.size(13.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(label, fontSize = 11.sp, color = color, fontWeight = FontWeight.Bold)
        }
    }
}
