package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@Composable
fun RoleLoginScreen(
    onSelectOwner: () -> Unit,
    onSelectTeamLeader: () -> Unit,
    onSelectEmployee: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BrandNavyBg)
            .padding(horizontal = 20.dp, vertical = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 480.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Header
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(BrandGreenPrimary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.VpnKey,
                    contentDescription = null,
                    tint = BrandGreenPrimary,
                    modifier = Modifier.size(28.dp)
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "LOGIN AS",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = BrandGreenPrimary,
                        letterSpacing = 2.sp
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Select your operational role to continue",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = BrandTextSecondary,
                        fontSize = 12.5.sp
                    )
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // 1. OWNER / ADMIN
            RoleSelectionCard(
                title = "OWNER / ADMIN",
                subtitle = "Full executive control, sheets configuration, reports & monitoring",
                icon = Icons.Default.AdminPanelSettings,
                accentColor = BrandGreenPrimary,
                badge = "Full Access",
                onClick = onSelectOwner
            )

            // 2. TEAM LEADER
            RoleSelectionCard(
                title = "TEAM LEADER",
                subtitle = "Department oversight, team performance & assigned leads",
                icon = Icons.Default.SupervisorAccount,
                accentColor = BrandBlueSecondary,
                badge = "Team View",
                onClick = onSelectTeamLeader
            )

            // 3. EMPLOYEE
            RoleSelectionCard(
                title = "EMPLOYEE / TELECALLER",
                subtitle = "Today's Work queue, one-click dialing, quick remarks & callbacks",
                icon = Icons.Default.HeadsetMic,
                accentColor = Color(0xFFFFB020),
                badge = "Fast Work Engine",
                onClick = onSelectEmployee
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "JobsIndia247 Enterprise Telecaller System v3.0",
                fontSize = 10.5.sp,
                color = BrandTextMuted
            )
        }
    }
}

@Composable
fun RoleSelectionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    badge: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BrandNavySurface),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = Brush.verticalGradient(
                listOf(accentColor.copy(alpha = 0.5f), Color(0x11FFFFFF))
            )
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = title,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 14.5.sp
                    )
                    Surface(
                        color = accentColor.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = badge,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = accentColor,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = subtitle,
                    fontSize = 11.5.sp,
                    color = BrandTextSecondary,
                    lineHeight = 15.sp
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = BrandTextSecondary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
