package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.data.model.UserRole
import com.example.ui.screens.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.TrackingViewModel
import kotlinx.coroutines.launch

sealed class Screen(val route: String, val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    object Splash : Screen("splash", "Splash", Icons.Default.FlashOn)
    object RoleLogin : Screen("role_login", "Role Login", Icons.Default.VpnKey)
    object OwnerLogin : Screen("owner_login", "Owner Login", Icons.Default.AdminPanelSettings)
    object TeamLeaderLogin : Screen("tl_login", "Team Leader Login", Icons.Default.SupervisorAccount)
    object EmployeeLogin : Screen("employee_login", "Employee Login", Icons.Default.HeadsetMic)
    object WorkspaceSyncLoading : Screen("workspace_sync_loading/{role}/{tlId}/{empId}", "Syncing Workspace", Icons.Default.Sync) {
        fun createRoute(role: UserRole, tlId: String = "none", empId: String = "none") =
            "workspace_sync_loading/${role.name}/$tlId/$empId"
    }
    object EmployeeWork : Screen("employee_work", "Today's Work Inbox", Icons.Default.Today)
    object TeamLeaderDashboard : Screen("tl_dashboard", "Supervisor Dashboard", Icons.Default.SupervisorAccount)
    object Dashboard : Screen("dashboard", "Live Dashboard", Icons.Default.Dashboard)
    object Portal : Screen("portal", "Follow-up Portal", Icons.Default.SupportAgent)
    object Audit : Screen("audit", "Performance Audit", Icons.Default.Security)
    object Companies : Screen("companies", "Company Summary", Icons.Default.Business)
    object TeamLeaders : Screen("team_leaders", "Team Leaders", Icons.Default.SupervisorAccount)
    object TopPerformers : Screen("top_performers", "Top Performers", Icons.Default.EmojiEvents)
    object DataSyncDebug : Screen("data_sync_debug", "Data Sync Debug", Icons.Default.BugReport)
    object FirebaseHealth : Screen("firebase_health", "Cloud Diagnostics", Icons.Default.CloudDone)
    object Admin : Screen("admin", "Admin Settings", Icons.Default.Settings)
    object EmployeeDetail : Screen("employee/{employeeId}", "Employee Details", Icons.Default.Person) {
        fun createRoute(employeeId: String) = "employee/$employeeId"
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            MyApplicationTheme {
                MainAppContent()
            }
        }
    }
}

@Composable
fun MainAppContent() {
    val navController = rememberNavController()
    val viewModel: TrackingViewModel = viewModel()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val userSession by viewModel.userSession.collectAsState()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val isAuthScreen = currentRoute == Screen.Splash.route ||
            currentRoute == Screen.RoleLogin.route ||
            currentRoute == Screen.OwnerLogin.route ||
            currentRoute == Screen.TeamLeaderLogin.route ||
            currentRoute == Screen.EmployeeLogin.route ||
            (currentRoute?.startsWith("workspace_sync_loading") == true) ||
            currentRoute == Screen.EmployeeWork.route

    val drawerItems = if (userSession.role == UserRole.TEAM_LEADER) {
        listOf(
            Screen.TeamLeaderDashboard,
            Screen.TopPerformers
        )
    } else {
        listOf(
            Screen.Dashboard,
            Screen.Portal,
            Screen.Audit,
            Screen.Companies,
            Screen.TeamLeaders,
            Screen.TopPerformers,
            Screen.DataSyncDebug,
            Screen.FirebaseHealth,
            Screen.Admin
        )
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = !isAuthScreen,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = BrandNavySurface,
                drawerContentColor = Color.White,
                modifier = Modifier.width(300.dp)
            ) {
                Spacer(modifier = Modifier.height(24.dp))
                Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                    Text(
                        text = "JobsIndia247",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = BrandGreenPrimary
                        )
                    )
                    Text(
                        text = when (userSession.role) {
                            UserRole.OWNER -> "Owner / Admin Operations"
                            UserRole.TEAM_LEADER -> "Supervisor: ${userSession.userName}"
                            UserRole.EMPLOYEE -> "Telecaller: ${userSession.userName}"
                            else -> "Enterprise Telecalling System"
                        },
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.5.sp,
                            color = BrandTextSecondary
                        )
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = Color(0x1AFFFFFF))
                Spacer(modifier = Modifier.height(8.dp))

                drawerItems.forEach { screen ->
                    val isSelected = currentRoute == screen.route
                    NavigationDrawerItem(
                        icon = {
                            Icon(
                                imageVector = screen.icon,
                                contentDescription = screen.title,
                                tint = if (isSelected) BrandGreenPrimary else BrandTextSecondary
                            )
                        },
                        label = {
                            Text(
                                text = screen.title,
                                fontSize = 13.5.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) BrandGreenPrimary else Color.White
                            )
                        },
                        selected = isSelected,
                        onClick = {
                            scope.launch { drawerState.close() }
                            if (currentRoute != screen.route) {
                                navController.navigate(screen.route) {
                                    val startDest = if (userSession.role == UserRole.TEAM_LEADER) "tl_dashboard" else "dashboard"
                                    popUpTo(startDest) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        colors = NavigationDrawerItemDefaults.colors(
                            selectedContainerColor = BrandGreenPrimary.copy(alpha = 0.12f),
                            unselectedContainerColor = Color.Transparent
                        ),
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // Logout Item in Drawer
                HorizontalDivider(color = Color(0x1AFFFFFF))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .clickable {
                            scope.launch { drawerState.close() }
                            viewModel.logout()
                            navController.navigate(Screen.RoleLogin.route) {
                                popUpTo(0) { inclusive = true }
                            }
                        },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Default.Logout, contentDescription = "Logout", tint = Color(0xFFFF5252))
                    Text(
                        text = "Sign Out / Switch Role",
                        color = Color(0xFFFF5252),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.5.sp
                    )
                }
            }
        }
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize().background(BrandNavyBg),
            containerColor = BrandNavyBg,
            contentWindowInsets = WindowInsets.safeDrawing
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding)) {
                NavHost(
                    navController = navController,
                    startDestination = Screen.Splash.route
                ) {
                    // 1. Splash Screen
                    composable(Screen.Splash.route) {
                        SplashScreen(
                            onSplashFinished = {
                                val target = when (userSession.role) {
                                    UserRole.OWNER -> Screen.Dashboard.route
                                    UserRole.TEAM_LEADER -> Screen.TeamLeaderDashboard.route
                                    UserRole.EMPLOYEE -> Screen.EmployeeWork.route
                                    else -> Screen.RoleLogin.route
                                }
                                navController.navigate(target) {
                                    popUpTo(Screen.Splash.route) { inclusive = true }
                                }
                            }
                        )
                    }

                    // 2. Role Login Selection
                    composable(Screen.RoleLogin.route) {
                        RoleLoginScreen(
                            onSelectOwner = { navController.navigate(Screen.OwnerLogin.route) },
                            onSelectTeamLeader = { navController.navigate(Screen.TeamLeaderLogin.route) },
                            onSelectEmployee = { navController.navigate(Screen.EmployeeLogin.route) }
                        )
                    }

                    // 3. Owner Login
                    composable(Screen.OwnerLogin.route) {
                        OwnerLoginScreen(
                            viewModel = viewModel,
                            onLoginSuccess = {
                                navController.navigate(Screen.WorkspaceSyncLoading.createRoute(UserRole.OWNER)) {
                                    popUpTo(Screen.RoleLogin.route) { inclusive = false }
                                }
                            },
                            onBack = { navController.popBackStack() }
                        )
                    }

                    // 4. Team Leader Login
                    composable(Screen.TeamLeaderLogin.route) {
                        TeamLeaderLoginScreen(
                            viewModel = viewModel,
                            onLoginSuccess = {
                                val tlId = userSession.teamLeaderId ?: "none"
                                navController.navigate(Screen.WorkspaceSyncLoading.createRoute(UserRole.TEAM_LEADER, tlId = tlId)) {
                                    popUpTo(Screen.RoleLogin.route) { inclusive = false }
                                }
                            },
                            onBack = { navController.popBackStack() }
                        )
                    }

                    // 5. Employee Multi-Step Login
                    composable(Screen.EmployeeLogin.route) {
                        EmployeeLoginFlowScreen(
                            viewModel = viewModel,
                            onLoginSuccess = {
                                val tlId = userSession.teamLeaderId ?: "none"
                                val empId = userSession.employeeId ?: "none"
                                navController.navigate(Screen.WorkspaceSyncLoading.createRoute(UserRole.EMPLOYEE, tlId = tlId, empId = empId)) {
                                    popUpTo(Screen.RoleLogin.route) { inclusive = false }
                                }
                            },
                            onBack = { navController.popBackStack() }
                        )
                    }

                    // Workspace Sync Loading & Cloud Verification
                    composable(
                        route = Screen.WorkspaceSyncLoading.route,
                        arguments = listOf(
                            navArgument("role") { type = NavType.StringType },
                            navArgument("tlId") { type = NavType.StringType; defaultValue = "none" },
                            navArgument("empId") { type = NavType.StringType; defaultValue = "none" }
                        )
                    ) { backStackEntry ->
                        val roleStr = backStackEntry.arguments?.getString("role") ?: "NONE"
                        val tlId = backStackEntry.arguments?.getString("tlId")?.takeIf { it != "none" }
                        val empId = backStackEntry.arguments?.getString("empId")?.takeIf { it != "none" }
                        val targetRole = try { UserRole.valueOf(roleStr) } catch (e: Exception) { UserRole.NONE }

                        WorkspaceSyncLoadingScreen(
                            viewModel = viewModel,
                            targetRole = targetRole,
                            targetTlId = tlId,
                            targetEmployeeId = empId,
                            onSyncSuccess = {
                                val destination = when (targetRole) {
                                    UserRole.OWNER -> Screen.Dashboard.route
                                    UserRole.TEAM_LEADER -> Screen.TeamLeaderDashboard.route
                                    UserRole.EMPLOYEE -> Screen.EmployeeWork.route
                                    UserRole.NONE -> Screen.RoleLogin.route
                                }
                                navController.navigate(destination) {
                                    popUpTo(Screen.RoleLogin.route) { inclusive = true }
                                }
                            },
                            onCancel = {
                                navController.navigate(Screen.RoleLogin.route) {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                        )
                    }

                    // 6. Employee Dedicated Today Work Screen
                    composable(Screen.EmployeeWork.route) {
                        EmployeeTodayWorkScreen(
                            viewModel = viewModel,
                            onLogout = {
                                viewModel.logout()
                                navController.navigate(Screen.RoleLogin.route) {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                        )
                    }

                    // 7. Team Leader Dashboard
                    composable(Screen.TeamLeaderDashboard.route) {
                        TeamLeaderDashboardScreen(
                            viewModel = viewModel,
                            onNavigateToEmployeeDetail = { empId ->
                                navController.navigate(Screen.EmployeeDetail.createRoute(empId))
                            },
                            onLogout = {
                                viewModel.logout()
                                navController.navigate(Screen.RoleLogin.route) {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                        )
                    }

                    // 8. Owner / Admin Live Dashboard
                    composable(Screen.Dashboard.route) {
                        DashboardScreen(
                            viewModel = viewModel,
                            onNavigateEmployee = { empId ->
                                navController.navigate(Screen.EmployeeDetail.createRoute(empId))
                            },
                            onOpenDrawer = { scope.launch { drawerState.open() } }
                        )
                    }

                    // 9. Followup Portal
                    composable(Screen.Portal.route) {
                        FollowupPortalScreen(
                            viewModel = viewModel,
                            onOpenDrawer = { scope.launch { drawerState.open() } }
                        )
                    }

                    // 10. Performance Audit
                    composable(Screen.Audit.route) {
                        PerformanceAuditScreen(
                            viewModel = viewModel,
                            onNavigateEmployee = { empId ->
                                navController.navigate(Screen.EmployeeDetail.createRoute(empId))
                            },
                            onOpenDrawer = { scope.launch { drawerState.open() } }
                        )
                    }

                    // 11. Companies Summary
                    composable(Screen.Companies.route) {
                        CompaniesSummaryScreen(
                            viewModel = viewModel,
                            onOpenDrawer = { scope.launch { drawerState.open() } }
                        )
                    }

                    // 12. Team Leaders Summary
                    composable(Screen.TeamLeaders.route) {
                        TeamLeadersSummaryScreen(
                            viewModel = viewModel,
                            onOpenDrawer = { scope.launch { drawerState.open() } }
                        )
                    }

                    // 13. Top Performers
                    composable(Screen.TopPerformers.route) {
                        TopPerformersScreen(
                            viewModel = viewModel,
                            onNavigateEmployee = { empId ->
                                navController.navigate(Screen.EmployeeDetail.createRoute(empId))
                            },
                            onOpenDrawer = { scope.launch { drawerState.open() } }
                        )
                    }

                    // 14. Data Sync Debug Screen
                    composable(Screen.DataSyncDebug.route) {
                        DataSyncDebugScreen(
                            viewModel = viewModel,
                            onBack = { navController.popBackStack() },
                            onOpenDrawer = { scope.launch { drawerState.open() } }
                        )
                    }

                    // 15. Firebase Health & Cloud Diagnostics
                    composable(Screen.FirebaseHealth.route) {
                        FirebaseHealthScreen(
                            viewModel = viewModel,
                            onBack = { navController.popBackStack() }
                        )
                    }

                    // 16. Admin Settings
                    composable(Screen.Admin.route) {
                        AdminSettingsScreen(
                            viewModel = viewModel,
                            onOpenDrawer = { scope.launch { drawerState.open() } }
                        )
                    }

                    // 17. Employee Detail
                    composable(
                        route = Screen.EmployeeDetail.route,
                        arguments = listOf(navArgument("employeeId") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val employeeId = backStackEntry.arguments?.getString("employeeId") ?: ""
                        EmployeeDetailScreen(
                            employeeId = employeeId,
                            viewModel = viewModel,
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}
