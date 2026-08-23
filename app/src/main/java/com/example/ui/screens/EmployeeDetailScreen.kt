package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
import androidx.compose.material.icons.outlined.*
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CallRecord
import com.example.data.model.EmployeeStats
import com.example.data.model.RemarkConstants
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.TrackingViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployeeDetailScreen(
    employeeId: String,
    viewModel: TrackingViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val allStats by viewModel.allEmployeeStats.collectAsState()
    val allCalls by viewModel.calls.collectAsState()
    val followups by viewModel.followups.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()

    val stat = allStats.find { it.employee.id == employeeId }
    val employeeCalls = remember(allCalls, employeeId) {
        allCalls.filter { it.employeeId == employeeId }
    }

    val interestedCalls = remember(employeeCalls) {
        employeeCalls.filter { it.remark.equals(RemarkConstants.INTERESTED, ignoreCase = true) }
    }

    val noAnswerCalls = remember(employeeCalls) {
        employeeCalls.filter { it.remark.equals(RemarkConstants.NO_ANSWER, ignoreCase = true) }
    }

    val timelineCalls = remember(employeeCalls) {
        employeeCalls.filter { it.remark.isNotBlank() }.sortedByDescending { it.calledAt }.take(12)
    }

    var syncDoneMsg by remember { mutableStateOf<String?>(null) }

    if (stat == null) {
        Box(
            modifier = Modifier.fillMaxSize().background(BrandNavyBg),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Employee not found", color = Color.White, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = onBack) { Text("Back to Dashboard") }
            }
        }
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(BrandNavyBg)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 40.dp)
    ) {
        // --- Top Bar ---
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            viewModel.syncEmployee(employeeId) { success, rows, error ->
                                syncDoneMsg = if (success) "Synced ${stat.employee.name}'s sheet ($rows rows) live!" else (error ?: "Sync completed")
                            }
                        },
                        enabled = !isSyncing,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandNavySurfaceLight, contentColor = Color.White),
                        border = ButtonDefaults.outlinedButtonBorder().copy(brush = Brush.linearGradient(listOf(BrandGreenPrimary.copy(alpha = 0.5f), BrandBlueSecondary.copy(alpha = 0.5f))))
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Sync", tint = BrandGreenPrimary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isSyncing) "Syncing…" else "Sync this employee", fontSize = 11.5.sp)
                    }
                }
            }
            if (syncDoneMsg != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(syncDoneMsg!!, color = BrandGreenPrimary, fontSize = 11.sp)
            }
        }

        // --- Profile Banner Card ---
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = BrandNavySurface),
                border = CardDefaults.outlinedCardBorder().copy(brush = Brush.verticalGradient(listOf(Color(0x33FFFFFF), Color(0x11FFFFFF))))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(CircleShape)
                                    .background(BrandGreenPrimary),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = stat.employee.name.split(" ").map { it.take(1) }.take(2).joinToString(""),
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF071120)
                                    )
                                )
                            }
                            Column {
                                Text(
                                    text = stat.employee.name,
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                )
                                Text(
                                    text = "${stat.employee.department ?: "Telecalling"} · ID: ${stat.employee.id.take(8).uppercase()}",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 11.sp,
                                        color = BrandTextSecondary
                                    )
                                )
                            }
                        }
                        StatusPill(status = stat.status)
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = Color(0x1AFFFFFF))
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        InfoColumn(label = "COMPANY", value = stat.company?.name ?: "—", icon = Icons.Default.Business)
                        InfoColumn(label = "TEAM LEADER", value = stat.teamLeader?.name ?: "—", icon = Icons.Default.SupervisorAccount)
                        InfoColumn(label = "LAST ACTIVITY", value = formatTimeAgo(stat.lastActivityAt), icon = Icons.Default.AccessTime)
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    CompletionBar(pct = stat.completion, height = 8)

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${stat.done} of ${stat.total} calls done · ${stat.remaining} remaining",
                            style = MaterialTheme.typography.bodySmall.copy(color = BrandTextSecondary, fontSize = 11.sp)
                        )
                        Text(
                            text = "Connected ${stat.connected} (${String.format("%.1f", stat.connectedPct)}%)",
                            style = MaterialTheme.typography.bodySmall.copy(color = BrandGreenPrimary, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        )
                    }
                }
            }
        }

        // --- Remark Grid ---
        item {
            Text(
                text = "REMARK BREAKDOWN COUNTS",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = BrandTextSecondary,
                    letterSpacing = 0.8.sp
                )
            )
            Spacer(modifier = Modifier.height(8.dp))

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MiniRemarkCard("Interested", stat.breakdown[RemarkConstants.INTERESTED] ?: 0, StatusSuccess, Modifier.weight(1f))
                    MiniRemarkCard("Not Interested", stat.breakdown[RemarkConstants.NOT_INTERESTED] ?: 0, BrandBlueSecondary, Modifier.weight(1f))
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MiniRemarkCard("No Answer", stat.breakdown[RemarkConstants.NO_ANSWER] ?: 0, StatusWarning, Modifier.weight(1f))
                    MiniRemarkCard("Switch Off", stat.breakdown[RemarkConstants.SWITCH_OFF] ?: 0, StatusDanger, Modifier.weight(1f))
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MiniRemarkCard("Pick Not Speak", stat.breakdown[RemarkConstants.PICK_NOT_SPEAK] ?: 0, Color(0xFFB388FF), Modifier.weight(1f))
                    MiniRemarkCard("Not Reachable", stat.breakdown[RemarkConstants.NOT_REACHABLE] ?: 0, Color(0xFFFF8A80), Modifier.weight(1f))
                }
            }
        }

        // --- Visual Remark Bars & Timeline ---
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                // Visual Bars
                Card(
                    modifier = Modifier.weight(1.3f),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = BrandNavySurface),
                    border = CardDefaults.outlinedCardBorder().copy(brush = Brush.verticalGradient(listOf(Color(0x33FFFFFF), Color(0x11FFFFFF))))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Remark Frequency",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = Color.White)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        HorizontalRemarkBars(breakdown = stat.breakdown)
                    }
                }

                // Recent Calls Timeline
                Card(
                    modifier = Modifier.weight(1.2f),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = BrandNavySurface),
                    border = CardDefaults.outlinedCardBorder().copy(brush = Brush.verticalGradient(listOf(Color(0x33FFFFFF), Color(0x11FFFFFF))))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Recent Activity",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = Color.White)
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        if (timelineCalls.isEmpty()) {
                            Text("No activity yet", fontSize = 11.sp, color = BrandTextMuted)
                        } else {
                            timelineCalls.take(6).forEach { call ->
                                Column(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = call.phone.takeLast(10),
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Medium,
                                                color = Color.White,
                                                fontSize = 11.sp
                                            )
                                        )
                                        Text(
                                            text = call.remark,
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontSize = 10.sp,
                                                color = BrandGreenPrimary,
                                                fontWeight = FontWeight.SemiBold
                                            ),
                                            maxLines = 1
                                        )
                                    }
                                    Text(
                                        text = formatTimeAgo(call.calledAt),
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp, color = BrandTextMuted)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- Interested Numbers Panel ---
        item {
            NumbersActionCard(
                title = "Interested Numbers",
                subtitle = "Leads marked 'Interested' — toggle sent, copy, or export CSV campaign",
                calls = interestedCalls,
                employeeName = stat.employee.name,
                followups = followups,
                onToggleSent = { phone -> viewModel.toggleMessageSent(employeeId, phone) },
                onCopy = { text ->
                    copyToClipboard(context, text)
                    Toast.makeText(context, "Copied $text", Toast.LENGTH_SHORT).show()
                },
                onCopyAll = { text ->
                    copyToClipboard(context, text)
                    Toast.makeText(context, "Copied all interested numbers!", Toast.LENGTH_SHORT).show()
                }
            )
        }

        // --- No Answer Numbers Panel ---
        item {
            NumbersActionCard(
                title = "No Answer Numbers",
                subtitle = "Leads marked 'No Answer' — ready for follow-up message blasts",
                calls = noAnswerCalls,
                employeeName = stat.employee.name,
                followups = followups,
                onToggleSent = { phone -> viewModel.toggleMessageSent(employeeId, phone) },
                onCopy = { text ->
                    copyToClipboard(context, text)
                    Toast.makeText(context, "Copied $text", Toast.LENGTH_SHORT).show()
                },
                onCopyAll = { text ->
                    copyToClipboard(context, text)
                    Toast.makeText(context, "Copied all No Answer numbers!", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }
}

@Composable
fun InfoColumn(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Column {
        Text(text = label, style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp, color = BrandTextMuted))
        Spacer(modifier = Modifier.height(2.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(icon, contentDescription = null, tint = BrandTextSecondary, modifier = Modifier.size(12.dp))
            Text(text = value, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium, color = Color.White, fontSize = 11.5.sp), maxLines = 1)
        }
    }
}

@Composable
fun MiniRemarkCard(label: String, count: Int, tone: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = BrandNavySurface),
        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.verticalGradient(listOf(Color(0x22FFFFFF), Color(0x11FFFFFF))))
    ) {
        Row(
            modifier = Modifier.padding(10.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = label, style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, color = BrandTextSecondary), maxLines = 1)
            Text(text = count.toString(), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = tone, fontSize = 16.sp))
        }
    }
}

@Composable
fun NumbersActionCard(
    title: String,
    subtitle: String,
    calls: List<CallRecord>,
    employeeName: String,
    followups: Map<String, com.example.data.model.FollowupRecord>,
    onToggleSent: (String) -> Unit,
    onCopy: (String) -> Unit,
    onCopyAll: (String) -> Unit
) {
    val sentCount = calls.count { followups[it.phone]?.messageSent == true }
    val allPhones = calls.joinToString("\n") { it.phone.takeLast(10) }

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
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, color = Color.White)
                        )
                        Box(
                            modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(BrandGreenPrimary.copy(alpha = 0.15f)).padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("${calls.size} total", color = BrandGreenPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                        Box(
                            modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(StatusSuccess.copy(alpha = 0.15f)).padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("$sentCount sent", color = StatusSuccess, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Text(text = subtitle, style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.5.sp, color = BrandTextMuted))
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(
                        onClick = { onCopyAll(allPhones) },
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy All", modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Copy all", fontSize = 11.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (calls.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                    Text("No records in this list.", color = BrandTextMuted, fontSize = 12.sp)
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    calls.forEachIndexed { idx, call ->
                        val phone10 = call.phone.takeLast(10)
                        val isSent = followups[call.phone]?.messageSent == true

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSent) StatusSuccess.copy(alpha = 0.08f) else BrandNavySurfaceLight)
                                .border(1.dp, if (isSent) StatusSuccess.copy(alpha = 0.3f) else Color(0x11FFFFFF), RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("#${idx + 1}", fontSize = 11.sp, color = BrandTextMuted, fontWeight = FontWeight.Bold)
                                Column {
                                    Text(
                                        text = "${employeeName.replace(" ", "_")}_${idx + 1}",
                                        fontSize = 10.5.sp,
                                        color = BrandTextSecondary,
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Text(
                                        text = "+91 $phone10",
                                        fontSize = 12.5.sp,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                // Mark Sent Button
                                Button(
                                    onClick = { onToggleSent(call.phone) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isSent) StatusSuccess else Color(0xFF1B2C4E),
                                        contentColor = if (isSent) Color(0xFF071120) else Color.White
                                    ),
                                    shape = RoundedCornerShape(6.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Icon(if (isSent) Icons.Default.Check else Icons.Default.Send, contentDescription = null, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(if (isSent) "Sent" else "Mark sent", fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold)
                                }

                                // Copy single button
                                IconButton(
                                    onClick = { onCopy(phone10) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = BrandTextSecondary, modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("phone_numbers", text)
    clipboard.setPrimaryClip(clip)
}
