package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.ui.viewmodel.TrackingViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeamLeaderLoginScreen(
    viewModel: TrackingViewModel,
    onLoginSuccess: () -> Unit,
    onBack: () -> Unit
) {
    val teamLeaders by viewModel.teamLeaders.collectAsState()
    val companies by viewModel.companies.collectAsState()

    var selectedTlId by remember { mutableStateOf(teamLeaders.firstOrNull()?.id ?: "") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(teamLeaders) {
        if (selectedTlId.isEmpty() && teamLeaders.isNotEmpty()) {
            selectedTlId = teamLeaders.first().id
        }
    }

    fun doLogin() {
        if (selectedTlId.isBlank()) {
            errorMessage = "Please select your Team Leader profile"
            return
        }
        if (password.isBlank()) {
            errorMessage = "Please enter the Team Leader password"
            return
        }
        val success = viewModel.loginTeamLeader(selectedTlId, password.trim())
        if (success) {
            errorMessage = null
            onLoginSuccess()
        } else {
            errorMessage = "Invalid password. Default: TL@247"
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Team Leader Login", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BrandNavySurface)
            )
        },
        containerColor = BrandNavyBg
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 440.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(BrandBlueSecondary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.SupervisorAccount,
                        contentDescription = null,
                        tint = BrandBlueSecondary,
                        modifier = Modifier.size(30.dp)
                    )
                }

                Text(
                    text = "Team Leader Portal",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                )

                Text(
                    text = "Select your supervisor account and enter your security PIN/password",
                    fontSize = 12.sp,
                    color = BrandTextSecondary,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                // Select Team Leader List
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = BrandNavySurface)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "SELECT YOUR NAME / PROFILE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = BrandBlueSecondary,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        teamLeaders.forEach { tl ->
                            val isSelected = selectedTlId == tl.id
                            val comp = companies.find { it.id == tl.companyId }
                            val compLabel = if (comp != null) "${comp.companyCode} - ${comp.name}" else "Active Company"

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) BrandBlueSecondary.copy(alpha = 0.2f) else Color.Transparent)
                                    .clickable { selectedTlId = tl.id }
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = tl.name,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isSelected) Color.White else BrandTextSecondary,
                                        fontSize = 13.5.sp
                                    )
                                    Text(
                                        text = "$compLabel • Synced Sheet",
                                        fontSize = 10.5.sp,
                                        color = BrandTextMuted
                                    )
                                }
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = BrandBlueSecondary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        errorMessage = null
                    },
                    label = { Text("Team Leader Password") },
                    placeholder = { Text("TL@247") },
                    singleLine = true,
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { doLogin() }),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = null,
                                tint = BrandTextSecondary
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BrandBlueSecondary,
                        unfocusedBorderColor = Color(0x33FFFFFF),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage ?: "",
                        color = Color(0xFFFF5252),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Button(
                    onClick = { doLogin() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BrandBlueSecondary,
                        contentColor = Color.White
                    )
                ) {
                    Icon(Icons.Default.Login, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Access Supervisor Dashboard", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    }
}
