package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.*
import com.example.data.repository.TrackingRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class PerformanceAuditItem(
    val employee: Employee,
    val company: Company?,
    val teamLeader: TeamLeader?,
    val done: Int,
    val connected: Int,
    val nonConnect: Int,
    val interested: Int,
    val notInterested: Int,
    val connectRate: Float,
    val nonConnectRate: Float,
    val interestedRate: Float,
    val cheatScore: Float,
    val lastActivityAt: Long,
    val status: CallStatus
)

class TrackingViewModel(val repository: TrackingRepository = TrackingRepository()) : ViewModel() {

    val departments = repository.departments
    val companies = repository.companies
    val teamLeaders = repository.teamLeaders
    val employees = repository.employees
    val leads = repository.leads
    val activities = repository.activities
    val quickRemarks = repository.quickRemarks
    val calls = repository.calls
    val followups = repository.followups
    val messageTemplates = repository.messageTemplates
    val userSession = repository.userSession
    val isSyncing = repository.isSyncing
    val syncProgressText = repository.syncProgressText
    val syncProgressFraction = repository.syncProgressFraction
    val syncReportSummary = repository.syncReportSummary
    val lastSyncedAt = repository.lastSyncedAt
    val isOwnerUnlocked = repository.isOwnerUnlocked
    val auditLogs = repository.auditLogs
    val firebaseHealth = repository.firebaseHealth
    val syncStatusInfo = repository.syncStatusInfo

    private val _diagnosticResult = MutableStateFlow<DiagnosticTestResult?>(null)
    val diagnosticResult: StateFlow<DiagnosticTestResult?> = _diagnosticResult.asStateFlow()

    private val _isTestingHealth = MutableStateFlow(false)
    val isTestingHealth: StateFlow<Boolean> = _isTestingHealth.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCompanyFilter = MutableStateFlow("all")
    val selectedCompanyFilter: StateFlow<String> = _selectedCompanyFilter.asStateFlow()

    private val _selectedTlFilter = MutableStateFlow("all")
    val selectedTlFilter: StateFlow<String> = _selectedTlFilter.asStateFlow()

    private val _selectedStatusFilter = MutableStateFlow("all")
    val selectedStatusFilter: StateFlow<String> = _selectedStatusFilter.asStateFlow()

    // Follow-up Portal / Employee Login navigation selection
    private val _portalDepartmentId = MutableStateFlow<String?>(null)
    val portalDepartmentId: StateFlow<String?> = _portalDepartmentId.asStateFlow()

    private val _portalCompanyId = MutableStateFlow<String?>(null)
    val portalCompanyId: StateFlow<String?> = _portalCompanyId.asStateFlow()

    private val _portalTlId = MutableStateFlow<String?>(null)
    val portalTlId: StateFlow<String?> = _portalTlId.asStateFlow()

    private val _portalEmployeeId = MutableStateFlow<String?>(null)
    val portalEmployeeId: StateFlow<String?> = _portalEmployeeId.asStateFlow()

    private val _portalTab = MutableStateFlow("interested")
    val portalTab: StateFlow<String> = _portalTab.asStateFlow()

    val allEmployeeStats: StateFlow<List<EmployeeStats>> = combine(
        employees,
        calls,
        companies,
        teamLeaders
    ) { emps, allCalls, _, _ ->
        emps.map { emp ->
            val empCalls = allCalls.filter { it.employeeId == emp.id }
            repository.computeEmployeeStats(emp, empCalls)
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val filteredEmployeeStats: StateFlow<List<EmployeeStats>> = combine(
        allEmployeeStats,
        searchQuery,
        selectedCompanyFilter,
        selectedTlFilter,
        selectedStatusFilter
    ) { statsList, query, compFilter, tlFilter, statusFilter ->
        statsList.filter { stat ->
            val matchesCompany = compFilter == "all" || stat.employee.companyId == compFilter
            val matchesTl = tlFilter == "all" || stat.employee.teamLeaderId == tlFilter
            val matchesStatus = statusFilter == "all" || stat.status.label == statusFilter
            val matchesQuery = query.isBlank() ||
                    stat.employee.name.contains(query, ignoreCase = true) ||
                    (stat.company?.name?.contains(query, ignoreCase = true) == true) ||
                    (stat.teamLeader?.name?.contains(query, ignoreCase = true) == true) ||
                    (stat.employee.department?.contains(query, ignoreCase = true) == true)

            matchesCompany && matchesTl && matchesStatus && matchesQuery
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val totals = combine(allEmployeeStats, selectedCompanyFilter, selectedTlFilter) { stats, compFilter, tlFilter ->
        val scoped = stats.filter {
            (compFilter == "all" || it.employee.companyId == compFilter) &&
                    (tlFilter == "all" || it.employee.teamLeaderId == tlFilter)
        }
        var totalNumbers = 0
        var totalDone = 0
        var totalConnected = 0
        var inCallCount = 0
        var idleCount = 0
        var offlineCount = 0

        scoped.forEach { s ->
            totalNumbers += s.total
            totalDone += s.done
            totalConnected += s.connected
            when (s.status) {
                CallStatus.IN_CALL -> inCallCount++
                CallStatus.IDLE -> idleCount++
                CallStatus.OFFLINE -> offlineCount++
            }
        }

        val remaining = (totalNumbers - totalDone).coerceAtLeast(0)
        val overallCompletion = if (totalNumbers > 0) (totalDone.toFloat() / totalNumbers) * 100f else 0f
        val connectedPct = if (totalNumbers > 0) (totalConnected.toFloat() / totalNumbers) * 100f else 0f

        mapOf(
            "totalNumbers" to totalNumbers,
            "totalDone" to totalDone,
            "totalConnected" to totalConnected,
            "connectedPct" to connectedPct,
            "remaining" to remaining,
            "overallCompletion" to overallCompletion,
            "inCall" to inCallCount,
            "idle" to idleCount,
            "offline" to offlineCount
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())

    val auditItems: StateFlow<List<PerformanceAuditItem>> = allEmployeeStats.map { statsList ->
        statsList.map { s ->
            val b = s.breakdown
            val connected = (b[RemarkConstants.INTERESTED] ?: 0) +
                    (b[RemarkConstants.SUCCESSFUL] ?: 0) +
                    (b[RemarkConstants.DONE] ?: 0) +
                    (b[RemarkConstants.NOT_INTERESTED] ?: 0) +
                    (b[RemarkConstants.CALLBACK] ?: 0) +
                    (b[RemarkConstants.FOLLOW_UP] ?: 0) +
                    (b[RemarkConstants.PICK_NOT_SPEAK] ?: 0) +
                    (b[RemarkConstants.LANGUAGE_BARRIER] ?: 0)
            val nonConnect = (b[RemarkConstants.NO_ANSWER] ?: 0) +
                    (b[RemarkConstants.SWITCH_OFF] ?: 0) +
                    (b[RemarkConstants.NOT_REACHABLE] ?: 0) +
                    (b[RemarkConstants.NOT_AVAILABLE] ?: 0) +
                    (b[RemarkConstants.INC_NOT_CONN] ?: 0) +
                    (b[RemarkConstants.INVALID] ?: 0)

            val done = s.done
            val connectRate = if (done > 0) connected.toFloat() / done else 0f
            val nonConnectRate = if (done > 0) nonConnect.toFloat() / done else 0f
            val interestedRate = if (done > 0) (b[RemarkConstants.INTERESTED] ?: 0).toFloat() / done else 0f

            var cheatScore = 0f
            if (done >= 5) {
                cheatScore += nonConnectRate * 70f
                cheatScore += (1f - connectRate) * 20f
                if ((b[RemarkConstants.INTERESTED] ?: 0) == 0 && done >= 20) cheatScore += 15f
                if ((b[RemarkConstants.NOT_INTERESTED] ?: 0) == 0 && done >= 20) cheatScore += 5f
                cheatScore -= interestedRate * 30f
                cheatScore = cheatScore.coerceIn(0f, 100f)
            }

            PerformanceAuditItem(
                employee = s.employee,
                company = s.company,
                teamLeader = s.teamLeader,
                done = done,
                connected = connected,
                nonConnect = nonConnect,
                interested = b[RemarkConstants.INTERESTED] ?: 0,
                notInterested = b[RemarkConstants.NOT_INTERESTED] ?: 0,
                connectRate = connectRate,
                nonConnectRate = nonConnectRate,
                interestedRate = interestedRate,
                cheatScore = cheatScore,
                lastActivityAt = s.lastActivityAt,
                status = s.status
            )
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    // --- Authentication Actions ---
    fun loginOwner(password: String): Boolean = repository.loginAsOwner(password)
    fun loginTeamLeader(tlId: String, password: String): Boolean = repository.loginAsTeamLeader(tlId, password)
    fun loginEmployee(deptId: String, tlId: String, empId: String) = repository.loginAsEmployee(deptId, tlId, empId)
    fun logout() = repository.logout()

    // --- Work Queue Actions ---
    fun submitLeadRemark(leadId: String, employeeId: String, remark: String, note: String?, followUpAt: Long?) {
        repository.recordLeadActivity(leadId, employeeId, remark, note, followUpAt)
    }

    fun convertInterestedToSuccessful(leadId: String, employeeId: String, note: String? = null) {
        repository.convertInterestedToSuccessful(leadId, employeeId, note)
    }

    fun markLeadLinkSent(leadId: String, employeeId: String, templateId: String?) {
        repository.markLeadLinkSent(leadId, employeeId, templateId)
    }

    fun setSearchQuery(q: String) { _searchQuery.value = q }
    fun setCompanyFilter(id: String) {
        _selectedCompanyFilter.value = id
        _selectedTlFilter.value = "all"
    }
    fun setTlFilter(id: String) { _selectedTlFilter.value = id }
    fun setStatusFilter(status: String) { _selectedStatusFilter.value = status }

    fun setPortalDepartment(id: String?) {
        _portalDepartmentId.value = id
        _portalTlId.value = null
        _portalEmployeeId.value = null
    }
    fun setPortalCompany(id: String?) {
        _portalCompanyId.value = id
        _portalTlId.value = null
        _portalEmployeeId.value = null
    }
    fun setPortalTl(id: String?) {
        _portalTlId.value = id
        _portalEmployeeId.value = null
    }
    fun setPortalEmployee(id: String?) { _portalEmployeeId.value = id }
    fun setPortalTab(tab: String) { _portalTab.value = tab }

    fun syncAll(
        onProgress: (String, Int, Int) -> Unit = { _, _, _ -> },
        onComplete: (SyncReportSummary) -> Unit = {}
    ) {
        repository.syncAllSheets(onProgress, onComplete)
    }

    fun syncTeamLeader(
        tlId: String,
        onProgress: (String, Int, Int) -> Unit = { _, _, _ -> },
        onComplete: (Boolean, Int, Int, String?) -> Unit = { _, _, _, _ -> }
    ) {
        repository.syncTeamLeaderSheets(tlId, onProgress, onComplete)
    }

    fun syncEmployee(
        empId: String,
        onProgress: (String) -> Unit = {},
        onComplete: (Boolean, Int, String?) -> Unit = { _, _, _ -> }
    ) {
        repository.syncEmployeeSheet(empId, onProgress, onComplete)
    }

    fun syncEmployee(empId: String, onFinished: () -> Unit) {
        repository.syncEmployeeSheet(empId, onProgress = {}) { _, _, _ ->
            onFinished()
        }
    }

    fun toggleFollowupSuccessful(empId: String, phone: String, channel: String? = null) {
        repository.toggleFollowupSuccessful(empId, phone, channel)
    }

    fun toggleMessageSent(empId: String, phone: String, messageText: String? = null) {
        repository.toggleMessageSent(empId, phone, messageText)
    }

    fun unlockOwner(password: String): Boolean = repository.unlockOwner(password)
    fun lockOwner() = repository.lockOwner()

    fun saveTeamLeader(id: String?, name: String, companyId: String, sheetUrl: String, syncEnabled: Boolean) {
        repository.saveTeamLeader(id, name, companyId, sheetUrl, syncEnabled)
    }

    fun toggleTeamLeaderSync(id: String, enabled: Boolean) {
        repository.toggleTeamLeaderSync(id, enabled)
    }

    fun deleteTeamLeader(id: String) {
        repository.deleteTeamLeader(id)
    }

    fun saveQuickRemark(id: String?, label: String, colorHex: String, requiresFollowup: Boolean) {
        repository.saveQuickRemark(id, label, colorHex, requiresFollowup)
    }

    fun deleteQuickRemark(id: String) {
        repository.deleteQuickRemark(id)
    }

    fun saveDepartment(id: String?, name: String, description: String?) {
        repository.saveDepartment(id, name, description)
    }

    fun deleteDepartment(id: String) {
        repository.deleteDepartment(id)
    }

    // --- Diagnostic Suite Triggers ---

    fun runFirestoreHealthTest() {
        viewModelScope.launch {
            _isTestingHealth.value = true
            try {
                val result = repository.runFirestoreHealthTest()
                _diagnosticResult.value = result
            } finally {
                _isTestingHealth.value = false
            }
        }
    }

    fun runEmployeePersistenceTest(employeeId: String) {
        viewModelScope.launch {
            _isTestingHealth.value = true
            try {
                val result = repository.runEmployeePersistenceTest(employeeId)
                _diagnosticResult.value = result
            } finally {
                _isTestingHealth.value = false
            }
        }
    }

    fun clearDiagnosticResult() {
        _diagnosticResult.value = null
    }
}
