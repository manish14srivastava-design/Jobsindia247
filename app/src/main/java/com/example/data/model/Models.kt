package com.example.data.model

data class Department(
    val id: String = "",
    val name: String = "",
    val description: String? = null,
    val status: String = "ACTIVE",
    val createdAt: Long = System.currentTimeMillis()
)

data class Company(
    val id: String = "",
    val companyCode: String = "",
    val name: String = "",
    val officialLink: String = "",
    val industry: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

data class TeamLeader(
    val id: String = "",
    val name: String = "",
    val companyId: String = "",
    val departmentId: String = "dept_telecalling",
    val sheetId: String = "",
    val primaryGid: String = "",
    val sheetUrl: String? = null,
    val detectedTabsCount: Int = 0,
    val syncEnabled: Boolean = true,
    val syncStatus: String = "SYNCED", // SYNCED, PENDING, SYNCING, ERROR
    val lastSyncedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
)

data class Employee(
    val id: String = "",
    val name: String = "",
    val employeeTabName: String = "",
    val companyId: String = "",
    val teamLeaderId: String = "",
    val departmentId: String = "dept_telecalling",
    val department: String? = "Telecalling",
    val phone: String? = null,
    val totalAssigned: Int = 0,
    val totalDone: Int = 0,
    val totalConnected: Int = 0,
    val lastActivity: Long = System.currentTimeMillis(),
    val createdAt: Long = System.currentTimeMillis()
)

enum class LeadPriority {
    HIGH,
    NORMAL,
    LOW;

    val label: String
        get() = when (this) {
            HIGH -> "High"
            NORMAL -> "Normal"
            LOW -> "Low"
        }
}

data class Lead(
    val id: String = "",
    val customerName: String = "",
    val phone: String = "",
    val companyId: String = "",
    val departmentId: String = "dept_telecalling",
    val teamLeaderId: String = "",
    val assignedEmployeeId: String = "",
    val sheetId: String = "",
    val sheetTabName: String = "",
    val sourceRowIndex: Int = 1, // Stable 1-indexed row position from original Google Sheet (1, 2, 3... 300)
    val status: String = "PENDING", // PENDING, INTERESTED, CALLBACK, FOLLOW_UP, SUCCESSFUL, COMPLETED
    val priority: LeadPriority = LeadPriority.NORMAL,
    val currentRemark: String = "",
    val previousRemark: String? = null,
    val nextFollowupAt: Long? = null,
    val notes: String? = null,
    val callCount: Int = 0,
    val lastCalledAt: Long? = null,
    val linkSent: Boolean = false,
    val linkSentAt: Long? = null,
    val messageTemplateId: String? = null,
    val dateStr: String = "",
    val isToday: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

data class ActivityRecord(
    val id: String = "",
    val employeeId: String = "",
    val employeeName: String = "",
    val leadId: String = "",
    val phone: String = "",
    val remark: String = "",
    val note: String? = null,
    val followUpAt: Long? = null,
    val linkSent: Boolean = false,
    val departmentId: String = "",
    val teamLeaderId: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

data class QuickRemark(
    val id: String = "",
    val label: String = "",
    val colorHex: String = "#10E57A",
    val requiresFollowup: Boolean = false,
    val displayOrder: Int = 0,
    val isActive: Boolean = true
)

data class CallRecord(
    val id: String = "",
    val employeeId: String = "",
    val phone: String = "",
    val remark: String = "",
    val calledAt: Long = System.currentTimeMillis()
)

data class FollowupRecord(
    val id: String = "",
    val employeeId: String = "",
    val phone: String = "",
    val successful: Boolean = false,
    val messageSent: Boolean = false,
    val messageSentAt: Long? = null,
    val channel: String? = null,
    val notes: String? = null,
    val updatedAt: Long = System.currentTimeMillis()
)

data class MessageTemplate(
    val id: String = "",
    val title: String = "",
    val templateBody: String = "",
    val isDefault: Boolean = false
)

object MessageEngine {
    const val SPIN101_LINK = "https://spin101.game"
    const val RUMMY77_LINK = "http://allrummy77.com"

    fun getOfficialLinkForCompany(companyCodeOrId: String?): String {
        val code = companyCodeOrId?.uppercase() ?: ""
        return if (code.contains("IND08") || code.contains("SPIN") || code.contains("101")) {
            SPIN101_LINK
        } else {
            RUMMY77_LINK
        }
    }

    val DEFAULT_TEMPLATES = listOf(
        MessageTemplate(
            id = "tmpl_default",
            title = "Standard Professional (Post-Call)",
            templateBody = """
Hello Sir 👋

Abhi call par aapse baat hui thi. Jab aapko time mile, app open karke check kar lijiye.

Ye link kuch limited time ke liye valid hai.

Agar kisi bhi tarah ka issue aaye, to hume reply karke zaroor batayiye. Hum aapki madad ke liye available hain.

Thank you! 😊

Official Link 🔗
{{officialLink}}

Regards,
{{employeeName}}
            """.trimIndent(),
            isDefault = true
        ),
        MessageTemplate(
            id = "tmpl_welcome",
            title = "VIP Benefits & Welcome Offer",
            templateBody = """
Namaste Sir 🙏

Hamari baat chit ke anusaar, official app link aapko share kar raha hoon.

Is link se register/login karke aap latest offers aur VIP benefits ka fayda utha sakte hain.

Official Link 🔗
{{officialLink}}

Kisi bhi support ke liye hum hamesha available hain.

Warm Regards,
{{employeeName}}
            """.trimIndent()
        ),
        MessageTemplate(
            id = "tmpl_quick",
            title = "Quick Platform Access",
            templateBody = """
Dear Customer,

As discussed during our call, here is the official verified platform link:
{{officialLink}}

Please check and start enjoying exclusive perks today. Feel free to reply anytime if you have any questions!

Best regards,
{{employeeName}}
            """.trimIndent()
        )
    )

    fun formatMessage(
        templateBody: String,
        employeeName: String,
        companyName: String,
        companyCode: String,
        officialLink: String
    ): String {
        return templateBody
            .replace("{{employeeName}}", employeeName)
            .replace("[EMPLOYEE_NAME]", employeeName)
            .replace("{{companyName}}", companyName)
            .replace("[COMPANY_NAME]", companyName)
            .replace("{{companyCode}}", companyCode)
            .replace("[COMPANY_CODE]", companyCode)
            .replace("{{officialLink}}", officialLink)
            .replace("[COMPANY_LINK]", officialLink)
            .trim()
    }
}

enum class UserRole {
    OWNER,
    TEAM_LEADER,
    EMPLOYEE,
    NONE
}

data class UserSession(
    val role: UserRole = UserRole.NONE,
    val userId: String = "",
    val userName: String = "",
    val companyId: String? = null,
    val departmentId: String? = null,
    val teamLeaderId: String? = null,
    val employeeId: String? = null
)

enum class CallStatus {
    IN_CALL,
    IDLE,
    OFFLINE;

    val label: String
        get() = when (this) {
            IN_CALL -> "In Call"
            IDLE -> "Idle"
            OFFLINE -> "Offline"
        }
}

object RemarkConstants {
    const val INTERESTED = "Interested"
    const val SUCCESSFUL = "Successful"
    const val DONE = "Done"
    const val NOT_INTERESTED = "Not Interested"
    const val CALLBACK = "Callback"
    const val FOLLOW_UP = "Follow Up"
    const val NO_ANSWER = "No Answer"
    const val PENDING = "Pending Dialing"
    const val SWITCH_OFF = "Switch Off"
    const val INC_NOT_CONN = "Incoming Call Not Connecting"
    const val PICK_NOT_SPEAK = "Call Pick But Not Speak"
    const val NOT_AVAILABLE = "Not Available"
    const val NOT_REACHABLE = "Not Reachable"
    const val INVALID = "Invalid"
    const val LANGUAGE_BARRIER = "Language Barrier"

    val ALL_REMARKS = listOf(
        INTERESTED,
        SUCCESSFUL,
        NOT_INTERESTED,
        CALLBACK,
        FOLLOW_UP,
        NO_ANSWER,
        PENDING,
        SWITCH_OFF,
        INC_NOT_CONN,
        PICK_NOT_SPEAK,
        NOT_AVAILABLE,
        NOT_REACHABLE,
        INVALID,
        LANGUAGE_BARRIER
    )

    fun normalize(raw: String?): String {
        if (raw.isNullOrBlank()) return ""
        val clean = raw.trim()
        val lower = clean.lowercase()

        // Pure non-remark placeholders and filler strings
        if (clean == "-" || clean == "--" || clean == "—" || clean == "na" || clean == "n/a" || clean == "nil" ||
            clean == "null" || clean == "none" || clean == "0" || clean == "." || clean == ".." ||
            lower == "pending" || lower == "pending dialing" || lower == "uncalled" || lower == "to call" ||
            lower == "dial" || lower.startsWith("customer") || lower.startsWith("lead") || clean.all { it.isDigit() }
        ) {
            return ""
        }

        return when {
            lower == "interested" || lower.startsWith("interest") || lower.contains("intrest") || (lower.contains("interested") && !lower.contains("not") && !lower.contains("不感兴趣")) -> INTERESTED
            lower.contains("not interested") || lower.contains("not intrested") || lower.contains("not intrest") || lower.contains("no need") || lower.contains("not required") || lower.contains("refuse") || lower.contains("不感兴趣") -> NOT_INTERESTED
            lower == "rnr" || lower.contains("rnr") || lower.contains("no answer") || lower.contains("not answer") || lower.contains("did not pick") || lower.contains("ringing") || lower.contains("not pick") || lower.contains("cut call") || lower.contains("busy") || lower.contains("disconnect") || lower.contains("不接") || lower.contains("打不通") -> NO_ANSWER
            lower.contains("switch off") || lower.contains("switched off") || lower.contains("power off") || lower.contains("关机") -> SWITCH_OFF
            lower.contains("not reachable") || lower.contains("unreachable") || lower.contains("out of coverage") || lower.contains("network issue") || lower.contains("无法接通") -> NOT_REACHABLE
            lower.contains("not available") || lower.contains("unavailable") || lower.contains("call later") -> NOT_AVAILABLE
            lower.contains("callback") || lower.contains("call back") || lower.contains("call again") || lower.contains("reschedule") -> CALLBACK
            lower.contains("follow up") || lower.contains("followup") || lower.contains("follow-up") -> FOLLOW_UP
            lower.contains("pick but not speak") || lower.contains("silent") || lower.contains("mute") || lower.contains("接听不说话") -> PICK_NOT_SPEAK
            lower.contains("incoming call not connecting") || lower.contains("not connecting") || lower.contains("network error") -> INC_NOT_CONN
            lower.contains("invalid") || lower.contains("invailid") || lower.contains("wrong number") || lower.contains("wrong no") || lower.contains("blocked") -> INVALID
            lower.contains("language") || lower.contains("language barrier") -> LANGUAGE_BARRIER
            lower.contains("successful") || lower.contains("success") || lower.contains("converted") || lower.contains("deposit") || lower.contains("register") -> SUCCESSFUL
            lower == "done" || lower.contains("done") || lower.contains("completed") -> DONE
            else -> {
                // If it contains recognizable text longer than 2 characters and not pure numbers/IDs
                if (clean.length in 2..40 && !clean.all { it.isDigit() } && !clean.contains("http", ignoreCase = true)) clean else ""
            }
        }
    }

    fun isValidRemark(raw: String?): Boolean {
        if (raw.isNullOrBlank()) return false
        val normalized = normalize(raw)
        return normalized.isNotBlank() && !normalized.equals(PENDING, ignoreCase = true)
    }

    fun isConnected(remark: String): Boolean {
        val r = remark.trim()
        return r.equals(INTERESTED, ignoreCase = true) ||
                r.equals(SUCCESSFUL, ignoreCase = true) ||
                r.equals(DONE, ignoreCase = true) ||
                r.equals(NOT_INTERESTED, ignoreCase = true) ||
                r.equals(CALLBACK, ignoreCase = true) ||
                r.equals(FOLLOW_UP, ignoreCase = true) ||
                r.equals(PICK_NOT_SPEAK, ignoreCase = true) ||
                r.equals(LANGUAGE_BARRIER, ignoreCase = true)
    }

    fun isNonConnect(remark: String): Boolean {
        val r = remark.trim()
        return r.equals(NO_ANSWER, ignoreCase = true) ||
                r.equals(SWITCH_OFF, ignoreCase = true) ||
                r.equals(NOT_REACHABLE, ignoreCase = true) ||
                r.equals(NOT_AVAILABLE, ignoreCase = true) ||
                r.equals(INC_NOT_CONN, ignoreCase = true) ||
                r.equals(INVALID, ignoreCase = true)
    }
}

data class SyncReportSummary(
    val timestamp: Long = System.currentTimeMillis(),
    val companiesSynced: Int = 0,
    val teamLeadersSynced: Int = 0,
    val employeeTabsSynced: Int = 0,
    val rowsSynced: Int = 0,
    val syncErrors: Int = 0,
    val successfulCount: Int = 0,
    val interestedCount: Int = 0,
    val pendingCount: Int = 0,
    val details: List<String> = emptyList()
)

data class EmployeeStats(
    val employee: Employee,
    val company: Company?,
    val teamLeader: TeamLeader?,
    val total: Int,
    val done: Int,
    val connected: Int,
    val connectedPct: Float,
    val remaining: Int,
    val completion: Float,
    val breakdown: Map<String, Int>,
    val lastActivityAt: Long,
    val status: CallStatus
)

enum class SyncState {
    SYNCED,
    SYNCING,
    ERROR,
    OFFLINE
}

data class SyncStatusInfo(
    val state: SyncState = SyncState.SYNCED,
    val lastSyncedAt: Long = System.currentTimeMillis(),
    val pendingCount: Int = 0,
    val isOnline: Boolean = true,
    val statusMessage: String = "All data synchronized"
)

data class AuditLog(
    val id: String = "",
    val leadId: String = "",
    val employeeId: String = "",
    val employeeName: String = "",
    val teamLeaderId: String = "",
    val companyId: String = "",
    val actionType: String = "", // INTERESTED, SUCCESSFUL, CALL, SEND_LINK, REMARK_ADDED, FOLLOW_UP, CALLBACK
    val previousStatus: String = "",
    val newStatus: String = "",
    val remark: String = "",
    val phone: String = "",
    val note: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val syncedToCloud: Boolean = true
)

data class FirebaseHealthStatus(
    val isConnected: Boolean = true,
    val firestoreWorking: Boolean = true,
    val realtimeListenerWorking: Boolean = true,
    val authWorking: Boolean = true,
    val lastSuccessfulWrite: Long? = null,
    val lastSuccessfulRead: Long? = null,
    val pendingWrites: Int = 0,
    val failedWrites: Int = 0,
    val offlineCacheEnabled: Boolean = true,
    val isOnline: Boolean = true
)

data class DiagnosticTestResult(
    val testName: String = "",
    val passed: Boolean = false,
    val message: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val writePass: Boolean = false,
    val readPass: Boolean = false,
    val listenerPass: Boolean = false,
    val verifiedEmployeeId: String = "",
    val verifiedTlId: String = "",
    val verifiedCompanyId: String = "",
    val details: Map<String, String> = emptyMap()
)
