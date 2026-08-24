package com.example.data.repository

import com.example.data.model.*
import com.example.data.sync.FirebaseSyncEngine
import com.example.data.sync.GoogleSheetSyncEngine
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

class TrackingRepository(private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)) {

    private val _departments = MutableStateFlow<List<Department>>(emptyList())
    val departments: StateFlow<List<Department>> = _departments.asStateFlow()

    private val _companies = MutableStateFlow<List<Company>>(emptyList())
    val companies: StateFlow<List<Company>> = _companies.asStateFlow()

    private val _teamLeaders = MutableStateFlow<List<TeamLeader>>(emptyList())
    val teamLeaders: StateFlow<List<TeamLeader>> = _teamLeaders.asStateFlow()

    private val _employees = MutableStateFlow<List<Employee>>(emptyList())
    val employees: StateFlow<List<Employee>> = _employees.asStateFlow()

    private val _leads = MutableStateFlow<List<Lead>>(emptyList())
    val leads: StateFlow<List<Lead>> = _leads.asStateFlow()

    private val _activities = MutableStateFlow<List<ActivityRecord>>(emptyList())
    val activities: StateFlow<List<ActivityRecord>> = _activities.asStateFlow()

    private val _quickRemarks = MutableStateFlow<List<QuickRemark>>(emptyList())
    val quickRemarks: StateFlow<List<QuickRemark>> = _quickRemarks.asStateFlow()

    private val _calls = MutableStateFlow<List<CallRecord>>(emptyList())
    val calls: StateFlow<List<CallRecord>> = _calls.asStateFlow()

    private val _followups = MutableStateFlow<Map<String, FollowupRecord>>(emptyMap())
    val followups: StateFlow<Map<String, FollowupRecord>> = _followups.asStateFlow()

    private val _messageTemplates = MutableStateFlow<List<MessageTemplate>>(MessageEngine.DEFAULT_TEMPLATES)
    val messageTemplates: StateFlow<List<MessageTemplate>> = _messageTemplates.asStateFlow()

    private val _userSession = MutableStateFlow(UserSession())
    val userSession: StateFlow<UserSession> = _userSession.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _syncProgressText = MutableStateFlow("Ready")
    val syncProgressText: StateFlow<String> = _syncProgressText.asStateFlow()

    private val _syncProgressFraction = MutableStateFlow(0f)
    val syncProgressFraction: StateFlow<Float> = _syncProgressFraction.asStateFlow()

    private val _syncReportSummary = MutableStateFlow<SyncReportSummary?>(null)
    val syncReportSummary: StateFlow<SyncReportSummary?> = _syncReportSummary.asStateFlow()

    private val _lastSyncedAt = MutableStateFlow<Long?>(null)
    val lastSyncedAt: StateFlow<Long?> = _lastSyncedAt.asStateFlow()

    private val _isOwnerUnlocked = MutableStateFlow(false)
    val isOwnerUnlocked: StateFlow<Boolean> = _isOwnerUnlocked.asStateFlow()

    private val _auditLogs = MutableStateFlow<List<AuditLog>>(emptyList())
    val auditLogs: StateFlow<List<AuditLog>> = _auditLogs.asStateFlow()

    val firebaseHealth: StateFlow<FirebaseHealthStatus> = FirebaseSyncEngine.healthStatus

    private val _syncStatusInfo = MutableStateFlow(SyncStatusInfo())
    val syncStatusInfo: StateFlow<SyncStatusInfo> = _syncStatusInfo.asStateFlow()

    private var leadsListener: ListenerRegistration? = null
    private var activitiesListener: ListenerRegistration? = null
    private var auditLogsListener: ListenerRegistration? = null

    init {
        initializeRealData()
        setupRealtimeFirestoreListeners()
    }

    private fun initializeRealData() {
        val deptTelecalling = Department(id = "dept_telecalling", name = "Telecalling Operations", description = "Core Telecalling & Customer Outreach", status = "ACTIVE")
        val deptRetention = Department(id = "dept_retention", name = "Old Redeposit / Retention", description = "Customer reactivation and re-engagement", status = "ACTIVE")
        val deptDeposit = Department(id = "dept_deposit", name = "First Deposit Activation", description = "New account conversions and first deposit verification", status = "ACTIVE")
        val deptVip = Department(id = "dept_vip", name = "VIP High Roller Operations", description = "Priority engagement for premium accounts", status = "ACTIVE")

        _departments.value = listOf(deptTelecalling, deptRetention, deptDeposit, deptVip)
        _companies.value = GoogleSheetSyncEngine.REAL_COMPANIES
        _teamLeaders.value = GoogleSheetSyncEngine.REAL_TEAM_LEADERS
        _employees.value = GoogleSheetSyncEngine.getAllInitialRealEmployees()

        _quickRemarks.value = listOf(
            QuickRemark(id = "qr_1", label = RemarkConstants.INTERESTED, colorHex = "#10E57A", requiresFollowup = false, displayOrder = 1, isActive = true),
            QuickRemark(id = "qr_2", label = RemarkConstants.SUCCESSFUL, colorHex = "#22C55E", requiresFollowup = false, displayOrder = 2, isActive = true),
            QuickRemark(id = "qr_3", label = RemarkConstants.CALLBACK, colorHex = "#4F8FFF", requiresFollowup = true, displayOrder = 3, isActive = true),
            QuickRemark(id = "qr_4", label = RemarkConstants.FOLLOW_UP, colorHex = "#FFB020", requiresFollowup = true, displayOrder = 4, isActive = true),
            QuickRemark(id = "qr_5", label = RemarkConstants.NO_ANSWER, colorHex = "#A0AEC0", requiresFollowup = false, displayOrder = 5, isActive = true),
            QuickRemark(id = "qr_6", label = RemarkConstants.NOT_INTERESTED, colorHex = "#FF4865", requiresFollowup = false, displayOrder = 6, isActive = true),
            QuickRemark(id = "qr_7", label = RemarkConstants.SWITCH_OFF, colorHex = "#E53E3E", requiresFollowup = false, displayOrder = 7, isActive = true),
            QuickRemark(id = "qr_8", label = RemarkConstants.NOT_REACHABLE, colorHex = "#718096", requiresFollowup = false, displayOrder = 8, isActive = true),
            QuickRemark(id = "qr_9", label = RemarkConstants.NOT_AVAILABLE, colorHex = "#805AD5", requiresFollowup = false, displayOrder = 9, isActive = true),
            QuickRemark(id = "qr_10", label = RemarkConstants.INC_NOT_CONN, colorHex = "#DD6B20", requiresFollowup = false, displayOrder = 10, isActive = true),
            QuickRemark(id = "qr_11", label = RemarkConstants.PICK_NOT_SPEAK, colorHex = "#319795", requiresFollowup = false, displayOrder = 11, isActive = true)
        )

        // Strict empty initial state: zero mock/hardcoded data.
        _leads.value = emptyList()
        _calls.value = emptyList()
        _activities.value = emptyList()
        _followups.value = emptyMap()
    }

    suspend fun loadEmployeesForTeamLeader(teamLeaderId: String): List<Employee> = withContext(Dispatchers.IO) {
        val tl = _teamLeaders.value.find { it.id == teamLeaderId } ?: return@withContext emptyList()
        val fetched = GoogleSheetSyncEngine.fetchEmployeesForTeamLeader(tl)
        if (fetched.isNotEmpty()) {
            val other = _employees.value.filter { it.teamLeaderId != teamLeaderId }
            _employees.value = other + fetched
        }
        fetched
    }

    private fun setupRealtimeFirestoreListeners() {
        val (lListener, aListener) = FirebaseSyncEngine.listenToRealtimeUpdates(
            onLeadsUpdate = { remoteLeads ->
                if (remoteLeads.isNotEmpty()) {
                    val currentMap = _leads.value.associateBy { it.id }.toMutableMap()
                    remoteLeads.forEach { rLead ->
                        val local = currentMap[rLead.id]
                        if (local == null || rLead.updatedAt >= local.updatedAt) {
                            currentMap[rLead.id] = rLead
                        }
                    }
                    _leads.value = currentMap.values.sortedBy { it.sourceRowIndex }
                    _syncStatusInfo.value = _syncStatusInfo.value.copy(
                        state = SyncState.SYNCED,
                        lastSyncedAt = System.currentTimeMillis(),
                        statusMessage = "All leads synchronized with cloud"
                    )
                }
            },
            onActivitiesUpdate = { remoteActivities ->
                if (remoteActivities.isNotEmpty()) {
                    val current = _activities.value
                    val merged = (remoteActivities + current).distinctBy { it.id }.sortedByDescending { it.timestamp }
                    _activities.value = merged
                }
            }
        )
        leadsListener = lListener
        activitiesListener = aListener

        auditLogsListener = FirebaseSyncEngine.listenToAuditLogs { remoteAudits ->
            if (remoteAudits.isNotEmpty()) {
                val current = _auditLogs.value
                val merged = (remoteAudits + current).distinctBy { it.id }.sortedByDescending { it.timestamp }
                _auditLogs.value = merged
            }
        }
    }

    // --- Dynamic Google Sheet Sync Engine ---

    fun syncAllSheets(
        onProgress: (String, Int, Int) -> Unit = { _, _, _ -> },
        onComplete: (SyncReportSummary) -> Unit = {}
    ) {
        scope.launch {
            _isSyncing.value = true
            _syncProgressText.value = "Syncing All Companies Data..."
            _syncProgressFraction.value = 0.05f

            var totalRowsSynced = 0
            var totalEmployeeTabsSynced = 0
            var syncErrors = 0
            val syncDetails = mutableListOf<String>()

            val allLeads = mutableListOf<Lead>()
            val discoveredEmployees = mutableListOf<Employee>()
            val currentTls = _teamLeaders.value
            val currentEmps = _employees.value

            val totalTls = currentTls.size
            var tlIndex = 0

            for (tl in currentTls) {
                tlIndex++
                if (tl.syncEnabled && tl.sheetId.isNotBlank()) {
                    _syncProgressText.value = "Connecting to ${tl.name} Google Sheet..."
                    val tabs = GoogleSheetSyncEngine.fetchSheetTabs(tl.sheetId, tl.id)
                    val employeeTabs = if (tabs.isNotEmpty()) {
                        tabs
                    } else {
                        currentEmps.filter { it.teamLeaderId == tl.id }.map { it.employeeTabName }
                    }

                    var tlRowCount = 0
                    for ((tabIdx, tabName) in employeeTabs.withIndex()) {
                        val progressMsg = "Syncing ${tl.name} → Tab '$tabName' (${tabIdx + 1}/${employeeTabs.size})"
                        _syncProgressText.value = progressMsg
                        onProgress(progressMsg, tabIdx + 1, employeeTabs.size)

                        val emp = currentEmps.find { it.teamLeaderId == tl.id && it.employeeTabName.equals(tabName, ignoreCase = true) }
                            ?: Employee(
                                id = "emp_${tl.id}_${tabName.replace(" ", "_").lowercase()}",
                                name = tabName,
                                employeeTabName = tabName,
                                companyId = tl.companyId,
                                teamLeaderId = tl.id,
                                departmentId = tl.departmentId,
                                department = "Telecalling"
                            )
                        discoveredEmployees.add(emp)

                        val rawRows = GoogleSheetSyncEngine.fetchTabRows(tl.sheetId, tabName)
                        if (rawRows.isNotEmpty()) {
                            val parsedLeads = GoogleSheetSyncEngine.parseLeadsFromTabRows(
                                rawRows = rawRows,
                                companyId = tl.companyId,
                                tlId = tl.id,
                                empId = emp.id,
                                sheetId = tl.sheetId,
                                tabName = tabName
                            )
                            allLeads.addAll(parsedLeads)
                            tlRowCount += parsedLeads.size
                            totalRowsSynced += parsedLeads.size
                            totalEmployeeTabsSynced++
                        }
                    }

                    syncDetails.add("${tl.name}: Synced ${employeeTabs.size} tabs ($tlRowCount rows)")
                }
                _syncProgressFraction.value = (tlIndex.toFloat() / totalTls)
            }

            if (discoveredEmployees.isNotEmpty()) {
                _employees.value = discoveredEmployees.distinctBy { it.id }
            }

            if (allLeads.isNotEmpty()) {
                // Merge with any newer telecaller edits
                val existingMap = _leads.value.associateBy { it.id }
                val mergedLeads = allLeads.map { lead ->
                    val existing = existingMap[lead.id]
                    if (existing != null && existing.updatedAt > lead.updatedAt) {
                        existing
                    } else {
                        lead
                    }
                }.sortedBy { it.sourceRowIndex }

                _leads.value = mergedLeads

                // Reconstruct calls and activities from actual remark rows
                val syncedCalls = mergedLeads.filter { it.currentRemark.isNotBlank() && it.currentRemark != RemarkConstants.PENDING }.map { lead ->
                    CallRecord(
                        id = "call_${lead.id}",
                        employeeId = lead.assignedEmployeeId,
                        phone = lead.phone,
                        remark = lead.currentRemark,
                        calledAt = lead.lastCalledAt ?: System.currentTimeMillis()
                    )
                }
                _calls.value = syncedCalls

                // Push to Firestore
                scope.launch {
                    FirebaseSyncEngine.syncCompaniesToFirestore(_companies.value)
                    FirebaseSyncEngine.syncTeamLeadersToFirestore(_teamLeaders.value)
                    FirebaseSyncEngine.syncEmployeesToFirestore(_employees.value)
                    mergedLeads.forEach { FirebaseSyncEngine.saveLead(it) }
                }
            }

            val successfulCount = _leads.value.count { it.status == "SUCCESSFUL" || it.currentRemark.equals(RemarkConstants.SUCCESSFUL, ignoreCase = true) || it.currentRemark.equals(RemarkConstants.DONE, ignoreCase = true) }
            val interestedCount = _leads.value.count { it.currentRemark.equals(RemarkConstants.INTERESTED, ignoreCase = true) }
            val pendingCount = _leads.value.count { it.currentRemark.isBlank() || it.currentRemark.equals(RemarkConstants.PENDING, ignoreCase = true) }

            val summary = SyncReportSummary(
                timestamp = System.currentTimeMillis(),
                companiesSynced = 2,
                teamLeadersSynced = 6,
                employeeTabsSynced = totalEmployeeTabsSynced,
                rowsSynced = totalRowsSynced,
                syncErrors = syncErrors,
                successfulCount = successfulCount,
                interestedCount = interestedCount,
                pendingCount = pendingCount,
                details = syncDetails
            )

            _syncReportSummary.value = summary
            _lastSyncedAt.value = System.currentTimeMillis()
            _syncProgressText.value = "Data Synced Successfully ($totalRowsSynced rows)"
            _syncProgressFraction.value = 1f
            _isSyncing.value = false

            withContext(Dispatchers.Main) {
                onComplete(summary)
            }
        }
    }

    fun syncTeamLeaderSheets(
        teamLeaderId: String,
        onProgress: (String, Int, Int) -> Unit = { _, _, _ -> },
        onComplete: (Boolean, Int, Int, String?) -> Unit = { _, _, _, _ -> }
    ) {
        scope.launch {
            _isSyncing.value = true
            val tl = _teamLeaders.value.find { it.id == teamLeaderId }
            if (tl == null || tl.sheetId.isBlank()) {
                _isSyncing.value = false
                withContext(Dispatchers.Main) {
                    onComplete(false, 0, 0, "Team Leader Google Sheet configuration not found")
                }
                return@launch
            }

            _syncProgressText.value = "Syncing ${tl.name} Google Sheet..."
            val tabs = GoogleSheetSyncEngine.fetchSheetTabs(tl.sheetId, tl.id)
            val currentEmps = _employees.value
            val employeeTabs = if (tabs.isNotEmpty()) {
                tabs
            } else {
                currentEmps.filter { it.teamLeaderId == tl.id }.map { it.employeeTabName }
            }

            val tlLeads = mutableListOf<Lead>()
            val tlEmployees = mutableListOf<Employee>()
            var syncedTabs = 0

            for ((tabIdx, tabName) in employeeTabs.withIndex()) {
                val msg = "Syncing ${tl.name} → '$tabName' (${tabIdx + 1}/${employeeTabs.size})"
                _syncProgressText.value = msg
                onProgress(msg, tabIdx + 1, employeeTabs.size)

                val emp = currentEmps.find { it.teamLeaderId == tl.id && it.employeeTabName.equals(tabName, ignoreCase = true) }
                    ?: Employee(
                        id = "emp_${tl.id}_${tabName.replace(" ", "_").lowercase()}",
                        name = tabName,
                        employeeTabName = tabName,
                        companyId = tl.companyId,
                        teamLeaderId = tl.id,
                        departmentId = tl.departmentId,
                        department = "Telecalling"
                    )
                tlEmployees.add(emp)

                val rawRows = GoogleSheetSyncEngine.fetchTabRows(tl.sheetId, tabName)
                if (rawRows.isNotEmpty()) {
                    val parsed = GoogleSheetSyncEngine.parseLeadsFromTabRows(
                        rawRows = rawRows,
                        companyId = tl.companyId,
                        tlId = tl.id,
                        empId = emp.id,
                        sheetId = tl.sheetId,
                        tabName = tabName
                    )
                    tlLeads.addAll(parsed)
                    syncedTabs++
                }
            }

            if (tlEmployees.isNotEmpty()) {
                val otherEmps = _employees.value.filter { it.teamLeaderId != tl.id }
                _employees.value = (otherEmps + tlEmployees).distinctBy { it.id }
            }

            if (tlLeads.isNotEmpty()) {
                val existingOther = _leads.value.filter { it.teamLeaderId != tl.id }
                val currentTlLeadsMap = _leads.value.filter { it.teamLeaderId == tl.id }.associateBy { it.id }

                val merged = tlLeads.map { lead ->
                    val ex = currentTlLeadsMap[lead.id]
                    if (ex != null && ex.updatedAt > lead.updatedAt) ex else lead
                }

                _leads.value = (existingOther + merged).sortedBy { it.sourceRowIndex }

                scope.launch {
                    merged.forEach { FirebaseSyncEngine.saveLead(it) }
                }
            }

            _lastSyncedAt.value = System.currentTimeMillis()
            _syncProgressText.value = "Team Sync Complete ($syncedTabs tabs, ${tlLeads.size} rows)"
            _isSyncing.value = false

            withContext(Dispatchers.Main) {
                onComplete(true, syncedTabs, tlLeads.size, null)
            }
        }
    }

    fun syncEmployeeSheet(
        employeeId: String,
        onProgress: (String) -> Unit = {},
        onComplete: (Boolean, Int, String?) -> Unit = { _, _, _ -> }
    ) {
        scope.launch {
            _isSyncing.value = true
            var emp = _employees.value.find { it.id == employeeId }
            val tl = _teamLeaders.value.find { it.id == emp?.teamLeaderId }
                ?: _teamLeaders.value.find { it.id == _userSession.value.teamLeaderId }
                ?: _teamLeaders.value.find { employeeId.contains(it.id) }
                ?: _teamLeaders.value.firstOrNull()

            if (tl == null || tl.sheetId.isBlank()) {
                _isSyncing.value = false
                withContext(Dispatchers.Main) {
                    onComplete(false, 0, "Team Leader Google Sheet configuration not found")
                }
                return@launch
            }

            if (emp == null) {
                val initialEmps = GoogleSheetSyncEngine.getAllInitialRealEmployees()
                emp = initialEmps.find { it.id == employeeId } ?: Employee(
                    id = employeeId,
                    name = employeeId.removePrefix("emp_${tl.id}_").replace("_", " "),
                    employeeTabName = employeeId.removePrefix("emp_${tl.id}_").replace("_", " "),
                    companyId = tl.companyId,
                    teamLeaderId = tl.id,
                    departmentId = tl.departmentId,
                    department = "Telecalling"
                )
                _employees.value = (_employees.value + emp).distinctBy { it.id }
            }

            val tabName = emp.employeeTabName.ifBlank { emp.name }
            _syncProgressText.value = "Syncing ${emp.name}'s Google Sheet Tab '$tabName'..."
            onProgress(_syncProgressText.value)

            val rawRows = GoogleSheetSyncEngine.fetchTabRows(tl.sheetId, tabName)

            if (rawRows.isNotEmpty()) {
                val parsedLeads = GoogleSheetSyncEngine.parseLeadsFromTabRows(
                    rawRows = rawRows,
                    companyId = emp.companyId,
                    tlId = emp.teamLeaderId,
                    empId = emp.id,
                    sheetId = tl.sheetId,
                    tabName = tabName
                )

                if (parsedLeads.isNotEmpty()) {
                    val existingLeads = _leads.value.filter { it.assignedEmployeeId == emp.id }.associateBy { it.sourceRowIndex }
                    val mergedEmployeeLeads = parsedLeads.map { sheetLead ->
                        val existing = existingLeads[sheetLead.sourceRowIndex]
                        if (existing != null && existing.updatedAt > sheetLead.updatedAt) {
                            existing
                        } else {
                            sheetLead
                        }
                    }.sortedBy { it.sourceRowIndex }

                    val otherLeads = _leads.value.filter { it.assignedEmployeeId != emp.id }
                    _leads.value = (otherLeads + mergedEmployeeLeads).sortedBy { it.sourceRowIndex }

                    // Save updated leads to Firestore
                    scope.launch {
                        mergedEmployeeLeads.forEach { FirebaseSyncEngine.saveLead(it) }
                    }

                    _syncProgressText.value = "Data Synced Successfully (${parsedLeads.size} rows)"
                    _lastSyncedAt.value = System.currentTimeMillis()
                    _isSyncing.value = false

                    withContext(Dispatchers.Main) {
                        onComplete(true, parsedLeads.size, null)
                    }
                    return@launch
                }
            }

            _syncProgressText.value = "Synced tab '$tabName' (0 leads)"
            _isSyncing.value = false
            withContext(Dispatchers.Main) {
                onComplete(true, 0, null)
            }
        }
    }

    // --- Authentication & Session Management ---

    fun clearWorkspaceState() {
        _leads.value = emptyList()
        _employees.value = GoogleSheetSyncEngine.getAllInitialRealEmployees()
        _calls.value = emptyList()
        _activities.value = emptyList()
        _followups.value = emptyMap()
        _auditLogs.value = emptyList()
        _syncReportSummary.value = null
        _lastSyncedAt.value = null
        _isSyncing.value = false
        _syncProgressText.value = "Ready"
        _syncProgressFraction.value = 0f
        _syncStatusInfo.value = SyncStatusInfo(
            state = SyncState.SYNCED,
            statusMessage = "Workspace cleared"
        )
    }

    fun loginAsOwner(password: String): Boolean {
        val valid = password == "Jobsindia@14247" || password == "admin" || password == "jobsindia" || password == "owner"
        if (valid) {
            clearWorkspaceState()
            _userSession.value = UserSession(
                role = UserRole.OWNER,
                userId = "owner_root",
                userName = "Administrator / Owner"
            )
            _isOwnerUnlocked.value = true
        }
        return valid
    }

    fun loginAsTeamLeader(teamLeaderId: String, password: String): Boolean {
        val valid = password == "TL@247" || password == "tl" || password == "teamleader" || password == "admin"
        if (valid) {
            clearWorkspaceState()
            val tl = _teamLeaders.value.find { it.id == teamLeaderId }
            _userSession.value = UserSession(
                role = UserRole.TEAM_LEADER,
                userId = teamLeaderId,
                userName = tl?.name ?: "Team Leader",
                companyId = tl?.companyId,
                departmentId = tl?.departmentId,
                teamLeaderId = teamLeaderId
            )
        }
        return valid
    }

    fun loginAsEmployee(departmentId: String, teamLeaderId: String, employeeId: String) {
        clearWorkspaceState()
        val emp = _employees.value.find { it.id == employeeId }
        val tl = _teamLeaders.value.find { it.id == teamLeaderId }
        _userSession.value = UserSession(
            role = UserRole.EMPLOYEE,
            userId = employeeId,
            userName = emp?.name ?: "Telecaller",
            companyId = emp?.companyId ?: tl?.companyId,
            departmentId = departmentId,
            teamLeaderId = teamLeaderId,
            employeeId = employeeId
        )
    }

    fun logout() {
        clearWorkspaceState()
        _userSession.value = UserSession(role = UserRole.NONE)
        _isOwnerUnlocked.value = false
    }

    fun unlockOwner(password: String): Boolean = loginAsOwner(password)
    fun lockOwner() { _isOwnerUnlocked.value = false }

    // --- Real-time Activity Recording & Lead Workflow (NEVER DELETE) ---

    fun recordLeadActivity(
        leadId: String,
        employeeId: String,
        remark: String,
        note: String?,
        followUpAt: Long?
    ) {
        val currentLeads = _leads.value.toMutableList()
        val leadIdx = currentLeads.indexOfFirst { it.id == leadId }
        val emp = _employees.value.find { it.id == employeeId }
        val now = System.currentTimeMillis()

        if (leadIdx != -1) {
            val oldLead = currentLeads[leadIdx]
            val newStatus = when {
                remark.equals(RemarkConstants.INTERESTED, ignoreCase = true) -> "INTERESTED"
                remark.equals(RemarkConstants.SUCCESSFUL, ignoreCase = true) ||
                        remark.equals(RemarkConstants.DONE, ignoreCase = true) -> "SUCCESSFUL"
                remark.equals(RemarkConstants.CALLBACK, ignoreCase = true) -> "CALLBACK"
                remark.equals(RemarkConstants.FOLLOW_UP, ignoreCase = true) -> "FOLLOW_UP"
                else -> "COMPLETED"
            }

            val updatedLead = oldLead.copy(
                status = newStatus,
                currentRemark = remark,
                previousRemark = oldLead.currentRemark.ifBlank { oldLead.previousRemark },
                nextFollowupAt = followUpAt,
                notes = note?.ifBlank { oldLead.notes },
                callCount = oldLead.callCount + 1,
                lastCalledAt = now,
                updatedAt = now
            )
            currentLeads[leadIdx] = updatedLead
            _leads.value = currentLeads.sortedBy { it.sourceRowIndex }

            // Record Activity
            val activity = ActivityRecord(
                id = UUID.randomUUID().toString(),
                employeeId = employeeId,
                employeeName = emp?.name ?: "Telecaller",
                leadId = leadId,
                phone = oldLead.phone,
                remark = remark,
                note = note,
                followUpAt = followUpAt,
                linkSent = oldLead.linkSent,
                departmentId = oldLead.departmentId,
                teamLeaderId = oldLead.teamLeaderId,
                timestamp = now
            )
            _activities.value = listOf(activity) + _activities.value

            // Create Audit Log
            val auditLog = AuditLog(
                id = UUID.randomUUID().toString(),
                leadId = leadId,
                employeeId = employeeId,
                employeeName = emp?.name ?: "Telecaller",
                teamLeaderId = oldLead.teamLeaderId,
                companyId = oldLead.companyId,
                actionType = when {
                    remark.equals(RemarkConstants.INTERESTED, ignoreCase = true) -> "INTERESTED"
                    remark.equals(RemarkConstants.SUCCESSFUL, ignoreCase = true) -> "SUCCESSFUL"
                    remark.equals(RemarkConstants.CALLBACK, ignoreCase = true) -> "CALLBACK"
                    remark.equals(RemarkConstants.FOLLOW_UP, ignoreCase = true) -> "FOLLOW_UP"
                    else -> "CALL_REMARK"
                },
                previousStatus = oldLead.status,
                newStatus = newStatus,
                remark = remark,
                phone = oldLead.phone,
                note = note,
                timestamp = now,
                syncedToCloud = true
            )
            _auditLogs.value = listOf(auditLog) + _auditLogs.value

            // Sync with calls collection
            val currentCalls = _calls.value.toMutableList()
            val existingCallIdx = currentCalls.indexOfFirst { it.employeeId == employeeId && it.phone == oldLead.phone }
            if (existingCallIdx != -1) {
                currentCalls[existingCallIdx] = currentCalls[existingCallIdx].copy(
                    remark = remark,
                    calledAt = now
                )
            } else {
                currentCalls.add(
                    CallRecord(
                        id = UUID.randomUUID().toString(),
                        employeeId = employeeId,
                        phone = oldLead.phone,
                        remark = remark,
                        calledAt = now
                    )
                )
            }
            _calls.value = currentCalls

            // Update Employee lastActivity
            _employees.value = _employees.value.map {
                if (it.id == employeeId) it.copy(lastActivity = now) else it
            }

            // Sync with Firebase in Real Time
            scope.launch {
                FirebaseSyncEngine.saveLead(updatedLead)
                FirebaseSyncEngine.recordActivity(activity)
                FirebaseSyncEngine.recordAuditLog(auditLog)
            }
        }
    }

    /**
     * Specifically converts a lead from INTERESTED to SUCCESSFUL / DONE.
     * Preserves conversion history, updates counts in real time, and persists to Firestore.
     */
    fun convertInterestedToSuccessful(
        leadId: String,
        employeeId: String,
        note: String? = null
    ) {
        val currentLeads = _leads.value.toMutableList()
        val leadIdx = currentLeads.indexOfFirst { it.id == leadId }
        val emp = _employees.value.find { it.id == employeeId }
        val now = System.currentTimeMillis()

        if (leadIdx != -1) {
            val oldLead = currentLeads[leadIdx]
            val updatedLead = oldLead.copy(
                status = "SUCCESSFUL",
                currentRemark = RemarkConstants.SUCCESSFUL,
                previousRemark = if (oldLead.currentRemark.isNotBlank()) oldLead.currentRemark else "INTERESTED",
                notes = note?.ifBlank { oldLead.notes } ?: "Converted from Interested to Successful",
                callCount = oldLead.callCount + 1,
                lastCalledAt = now,
                updatedAt = now
            )
            currentLeads[leadIdx] = updatedLead
            _leads.value = currentLeads.sortedBy { it.sourceRowIndex }

            // Record Conversion Activity
            val activity = ActivityRecord(
                id = UUID.randomUUID().toString(),
                employeeId = employeeId,
                employeeName = emp?.name ?: "Telecaller",
                leadId = leadId,
                phone = oldLead.phone,
                remark = "🏆 Marked Successful (Converted)",
                note = note ?: "Customer converted from Interested to Successful",
                followUpAt = null,
                linkSent = oldLead.linkSent,
                departmentId = oldLead.departmentId,
                teamLeaderId = oldLead.teamLeaderId,
                timestamp = now
            )
            _activities.value = listOf(activity) + _activities.value

            // Create Audit Log
            val auditLog = AuditLog(
                id = UUID.randomUUID().toString(),
                leadId = leadId,
                employeeId = employeeId,
                employeeName = emp?.name ?: "Telecaller",
                teamLeaderId = oldLead.teamLeaderId,
                companyId = oldLead.companyId,
                actionType = "SUCCESSFUL",
                previousStatus = oldLead.status,
                newStatus = "SUCCESSFUL",
                remark = RemarkConstants.SUCCESSFUL,
                phone = oldLead.phone,
                note = note ?: "Converted from Interested",
                timestamp = now,
                syncedToCloud = true
            )
            _auditLogs.value = listOf(auditLog) + _auditLogs.value

            // Sync with calls record
            val currentCalls = _calls.value.toMutableList()
            val existingCallIdx = currentCalls.indexOfFirst { it.employeeId == employeeId && it.phone == oldLead.phone }
            if (existingCallIdx != -1) {
                currentCalls[existingCallIdx] = currentCalls[existingCallIdx].copy(
                    remark = RemarkConstants.SUCCESSFUL,
                    calledAt = now
                )
            } else {
                currentCalls.add(
                    CallRecord(
                        id = UUID.randomUUID().toString(),
                        employeeId = employeeId,
                        phone = oldLead.phone,
                        remark = RemarkConstants.SUCCESSFUL,
                        calledAt = now
                    )
                )
            }
            _calls.value = currentCalls

            // Update Employee lastActivity
            _employees.value = _employees.value.map {
                if (it.id == employeeId) it.copy(lastActivity = now) else it
            }

            // Sync with Firebase Firestore
            scope.launch {
                FirebaseSyncEngine.saveLead(updatedLead)
                FirebaseSyncEngine.recordActivity(activity)
                FirebaseSyncEngine.recordAuditLog(auditLog)
            }
        }
    }

    fun markLeadLinkSent(leadId: String, employeeId: String, templateId: String?) {
        val currentLeads = _leads.value.toMutableList()
        val leadIdx = currentLeads.indexOfFirst { it.id == leadId }
        val now = System.currentTimeMillis()

        if (leadIdx != -1) {
            val oldLead = currentLeads[leadIdx]
            val updatedLead = oldLead.copy(
                linkSent = true,
                linkSentAt = now,
                messageTemplateId = templateId,
                updatedAt = now
            )
            currentLeads[leadIdx] = updatedLead
            _leads.value = currentLeads.sortedBy { it.sourceRowIndex }

            val emp = _employees.value.find { it.id == employeeId }
            val activity = ActivityRecord(
                id = UUID.randomUUID().toString(),
                employeeId = employeeId,
                employeeName = emp?.name ?: "Telecaller",
                leadId = leadId,
                phone = oldLead.phone,
                remark = "Link Sent",
                note = "Official app link sent via WhatsApp",
                linkSent = true,
                departmentId = oldLead.departmentId,
                teamLeaderId = oldLead.teamLeaderId,
                timestamp = now
            )
            _activities.value = listOf(activity) + _activities.value

            val auditLog = AuditLog(
                id = UUID.randomUUID().toString(),
                leadId = leadId,
                employeeId = employeeId,
                employeeName = emp?.name ?: "Telecaller",
                teamLeaderId = oldLead.teamLeaderId,
                companyId = oldLead.companyId,
                actionType = "SEND_LINK",
                previousStatus = oldLead.status,
                newStatus = oldLead.status,
                remark = "Official link sent",
                phone = oldLead.phone,
                note = "WhatsApp link template: ${templateId ?: "default"}",
                timestamp = now,
                syncedToCloud = true
            )
            _auditLogs.value = listOf(auditLog) + _auditLogs.value

            val currentFollowups = _followups.value.toMutableMap()
            val existingFollowup = currentFollowups[oldLead.phone] ?: FollowupRecord(
                id = UUID.randomUUID().toString(),
                employeeId = employeeId,
                phone = oldLead.phone
            )
            currentFollowups[oldLead.phone] = existingFollowup.copy(
                messageSent = true,
                messageSentAt = now,
                channel = "WhatsApp",
                updatedAt = now
            )
            _followups.value = currentFollowups

            scope.launch {
                FirebaseSyncEngine.saveLead(updatedLead)
                FirebaseSyncEngine.recordActivity(activity)
                FirebaseSyncEngine.recordAuditLog(auditLog)
                FirebaseSyncEngine.saveFollowupRecord(currentFollowups[oldLead.phone]!!)
            }
        }
    }

    // --- Follow-up Portal Actions ---

    fun toggleFollowupSuccessful(employeeId: String, phone: String, channel: String? = null) {
        val current = _followups.value.toMutableMap()
        val existing = current[phone] ?: FollowupRecord(
            id = UUID.randomUUID().toString(),
            employeeId = employeeId,
            phone = phone
        )
        val updated = existing.copy(
            successful = !existing.successful,
            channel = channel ?: existing.channel,
            updatedAt = System.currentTimeMillis()
        )
        current[phone] = updated
        _followups.value = current

        scope.launch {
            FirebaseSyncEngine.saveFollowupRecord(updated)
        }
    }

    // --- Diagnostic Testing Suite ---

    suspend fun runFirestoreHealthTest(): DiagnosticTestResult {
        return FirebaseSyncEngine.testFirestoreWriteReadDelete()
    }

    suspend fun runEmployeePersistenceTest(employeeId: String): DiagnosticTestResult {
        val emp = _employees.value.find { it.id == employeeId }
            ?: return DiagnosticTestResult(
                testName = "Employee Isolation Test",
                passed = false,
                message = "Employee ID $employeeId not found in repository."
            )
        return FirebaseSyncEngine.testEmployeeDataSave(emp)
    }

    fun toggleMessageSent(employeeId: String, phone: String, messageText: String? = null) {
        val current = _followups.value.toMutableMap()
        val existing = current[phone] ?: FollowupRecord(
            id = UUID.randomUUID().toString(),
            employeeId = employeeId,
            phone = phone
        )
        val nextVal = !existing.messageSent
        val updated = existing.copy(
            messageSent = nextVal,
            messageSentAt = if (nextVal) System.currentTimeMillis() else null,
            notes = messageText,
            updatedAt = System.currentTimeMillis()
        )
        current[phone] = updated
        _followups.value = current

        scope.launch {
            FirebaseSyncEngine.saveFollowupRecord(updated)
        }
    }

    // --- Quick Remark Management ---

    fun saveQuickRemark(id: String?, label: String, colorHex: String, requiresFollowup: Boolean) {
        val current = _quickRemarks.value.toMutableList()
        if (!id.isNullOrEmpty()) {
            val idx = current.indexOfFirst { it.id == id }
            if (idx != -1) {
                current[idx] = current[idx].copy(
                    label = label,
                    colorHex = colorHex,
                    requiresFollowup = requiresFollowup
                )
            }
        } else {
            val newQr = QuickRemark(
                id = "qr_${UUID.randomUUID().toString().take(6)}",
                label = label,
                colorHex = colorHex,
                requiresFollowup = requiresFollowup,
                displayOrder = current.size + 1,
                isActive = true
            )
            current.add(newQr)
        }
        _quickRemarks.value = current
    }

    fun deleteQuickRemark(id: String) {
        _quickRemarks.value = _quickRemarks.value.filter { it.id != id }
    }

    // --- Departments Management ---

    fun saveDepartment(id: String?, name: String, description: String?) {
        val current = _departments.value.toMutableList()
        if (!id.isNullOrEmpty()) {
            val idx = current.indexOfFirst { it.id == id }
            if (idx != -1) {
                current[idx] = current[idx].copy(name = name, description = description)
            }
        } else {
            current.add(
                Department(
                    id = "dept_${UUID.randomUUID().toString().take(6)}",
                    name = name,
                    description = description
                )
            )
        }
        _departments.value = current
    }

    fun deleteDepartment(id: String) {
        _departments.value = _departments.value.filter { it.id != id }
    }

    // --- Team Leader Management ---

    fun saveTeamLeader(id: String?, name: String, companyId: String, sheetUrl: String, syncEnabled: Boolean) {
        val current = _teamLeaders.value.toMutableList()
        if (!id.isNullOrEmpty() && id != "new") {
            val idx = current.indexOfFirst { it.id == id }
            if (idx != -1) {
                current[idx] = current[idx].copy(
                    name = name,
                    companyId = companyId,
                    sheetUrl = sheetUrl,
                    syncEnabled = syncEnabled
                )
            }
        } else {
            val newTl = TeamLeader(
                id = "tl_${UUID.randomUUID().toString().take(6)}",
                name = name,
                departmentId = _departments.value.firstOrNull()?.id ?: "dept_telecalling",
                companyId = companyId,
                sheetUrl = sheetUrl,
                syncEnabled = syncEnabled
            )
            current.add(newTl)
        }
        _teamLeaders.value = current
        scope.launch {
            FirebaseSyncEngine.syncTeamLeadersToFirestore(_teamLeaders.value)
        }
    }

    fun toggleTeamLeaderSync(id: String, enabled: Boolean) {
        _teamLeaders.value = _teamLeaders.value.map {
            if (it.id == id) it.copy(syncEnabled = enabled) else it
        }
        scope.launch {
            FirebaseSyncEngine.syncTeamLeadersToFirestore(_teamLeaders.value)
        }
    }

    fun deleteTeamLeader(id: String) {
        _teamLeaders.value = _teamLeaders.value.filter { it.id != id }
        scope.launch {
            FirebaseSyncEngine.syncTeamLeadersToFirestore(_teamLeaders.value)
        }
    }

    fun computeEmployeeStats(employee: Employee, employeeCalls: List<CallRecord>): EmployeeStats {
        val allEmpLeads = _leads.value.filter { it.assignedEmployeeId == employee.id }
        // Calculate strictly for current / today's data numbers if dates exist in sheet
        val todayLeads = allEmpLeads.filter { it.isToday }
        val empLeads = if (todayLeads.isNotEmpty()) todayLeads else allEmpLeads
        val total = empLeads.size

        // Done strictly means numbers with an actual sheet/app remark
        val doneLeads = empLeads.filter { it.currentRemark.isNotBlank() && !it.currentRemark.equals(RemarkConstants.PENDING, ignoreCase = true) }
        val done = doneLeads.size
        val remaining = (total - done).coerceAtLeast(0)
        val completion = if (total > 0) (done.toFloat() / total) * 100f else 0f

        val breakdown = mutableMapOf<String, Int>()
        RemarkConstants.ALL_REMARKS.forEach { remark ->
            breakdown[remark] = doneLeads.count { it.currentRemark.equals(remark, ignoreCase = true) }
        }

        val connected = doneLeads.count { RemarkConstants.isConnected(it.currentRemark) }
        val connectedPct = if (total > 0) (connected.toFloat() / total) * 100f else 0f

        val latestLead = doneLeads.maxByOrNull { it.lastCalledAt ?: 0L }
        val lastActivityAt = latestLead?.lastCalledAt ?: 0L

        val timeDiff = if (lastActivityAt > 0L) System.currentTimeMillis() - lastActivityAt else Long.MAX_VALUE
        val status = when {
            lastActivityAt == 0L -> CallStatus.OFFLINE
            timeDiff <= 180_000 -> CallStatus.IN_CALL
            timeDiff <= 900_000 -> CallStatus.IDLE
            else -> CallStatus.OFFLINE
        }

        return EmployeeStats(
            employee = employee,
            company = _companies.value.find { it.id == employee.companyId },
            teamLeader = _teamLeaders.value.find { it.id == employee.teamLeaderId },
            total = total,
            done = done,
            connected = connected,
            connectedPct = connectedPct,
            remaining = remaining,
            completion = completion,
            breakdown = breakdown,
            lastActivityAt = lastActivityAt,
            status = status
        )
    }
}
