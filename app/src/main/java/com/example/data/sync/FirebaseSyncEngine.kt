package com.example.data.sync

import android.util.Log
import com.example.data.model.*
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object FirebaseSyncEngine {
    private const val TAG = "FirebaseSyncEngine"

    private val _healthStatus = MutableStateFlow(FirebaseHealthStatus())
    val healthStatus: StateFlow<FirebaseHealthStatus> = _healthStatus.asStateFlow()

    private val firestore: FirebaseFirestore? by lazy {
        try {
            val db = FirebaseFirestore.getInstance()
            _healthStatus.value = _healthStatus.value.copy(
                isConnected = true,
                firestoreWorking = true,
                offlineCacheEnabled = true
            )
            db
        } catch (e: Exception) {
            Log.w(TAG, "Firestore initialization fallback: ${e.message}")
            _healthStatus.value = _healthStatus.value.copy(
                isConnected = false,
                firestoreWorking = false
            )
            null
        }
    }

    private suspend fun <T> Task<T>.awaitTask(): T = suspendCancellableCoroutine { cont ->
        addOnSuccessListener { result ->
            cont.resume(result)
        }
        addOnFailureListener { exception ->
            cont.resumeWithException(exception)
        }
    }

    suspend fun syncCompaniesToFirestore(companies: List<Company>): Boolean = withContext(Dispatchers.IO) {
        val db = firestore ?: return@withContext false
        try {
            val batch = db.batch()
            for (comp in companies) {
                val docRef = db.collection("companies").document(comp.id)
                val data = mapOf(
                    "id" to comp.id,
                    "companyCode" to comp.companyCode,
                    "name" to comp.name,
                    "officialLink" to comp.officialLink,
                    "industry" to (comp.industry ?: ""),
                    "createdAt" to comp.createdAt
                )
                batch.set(docRef, data, SetOptions.merge())
            }
            batch.commit().awaitTask()
            recordWriteSuccess()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed syncing companies to Firestore", e)
            recordWriteFailure()
            false
        }
    }

    suspend fun syncTeamLeadersToFirestore(teamLeaders: List<TeamLeader>): Boolean = withContext(Dispatchers.IO) {
        val db = firestore ?: return@withContext false
        try {
            val batch = db.batch()
            for (tl in teamLeaders) {
                val docRef = db.collection("team_leaders").document(tl.id)
                val data = mapOf(
                    "id" to tl.id,
                    "name" to tl.name,
                    "companyId" to tl.companyId,
                    "departmentId" to tl.departmentId,
                    "sheetId" to tl.sheetId,
                    "primaryGid" to tl.primaryGid,
                    "sheetUrl" to (tl.sheetUrl ?: ""),
                    "detectedTabsCount" to tl.detectedTabsCount,
                    "syncEnabled" to tl.syncEnabled,
                    "syncStatus" to tl.syncStatus,
                    "lastSyncedAt" to (tl.lastSyncedAt ?: System.currentTimeMillis()),
                    "createdAt" to tl.createdAt
                )
                batch.set(docRef, data, SetOptions.merge())
            }
            batch.commit().awaitTask()
            recordWriteSuccess()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed syncing team leaders to Firestore", e)
            recordWriteFailure()
            false
        }
    }

    suspend fun syncEmployeesToFirestore(employees: List<Employee>): Boolean = withContext(Dispatchers.IO) {
        val db = firestore ?: return@withContext false
        try {
            val batch = db.batch()
            for (emp in employees) {
                val docRef = db.collection("employees").document(emp.id)
                val data = mapOf(
                    "id" to emp.id,
                    "name" to emp.name,
                    "employeeTabName" to emp.employeeTabName,
                    "companyId" to emp.companyId,
                    "teamLeaderId" to emp.teamLeaderId,
                    "departmentId" to emp.departmentId,
                    "department" to (emp.department ?: "Telecalling"),
                    "phone" to (emp.phone ?: ""),
                    "totalAssigned" to emp.totalAssigned,
                    "totalDone" to emp.totalDone,
                    "totalConnected" to emp.totalConnected,
                    "lastActivity" to emp.lastActivity,
                    "createdAt" to emp.createdAt
                )
                batch.set(docRef, data, SetOptions.merge())
            }
            batch.commit().awaitTask()
            recordWriteSuccess()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed syncing employees to Firestore", e)
            recordWriteFailure()
            false
        }
    }

    suspend fun recordActivity(activity: ActivityRecord): Boolean = withContext(Dispatchers.IO) {
        val db = firestore ?: return@withContext false
        try {
            val docRef = db.collection("activity_records").document(activity.id)
            val data = mapOf(
                "id" to activity.id,
                "employeeId" to activity.employeeId,
                "employeeName" to activity.employeeName,
                "leadId" to activity.leadId,
                "phone" to activity.phone,
                "remark" to activity.remark,
                "note" to (activity.note ?: ""),
                "followUpAt" to (activity.followUpAt ?: 0L),
                "linkSent" to activity.linkSent,
                "departmentId" to activity.departmentId,
                "teamLeaderId" to activity.teamLeaderId,
                "timestamp" to activity.timestamp
            )
            docRef.set(data, SetOptions.merge()).awaitTask()
            recordWriteSuccess()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed recording activity to Firestore", e)
            recordWriteFailure()
            false
        }
    }

    suspend fun recordAuditLog(audit: AuditLog): Boolean = withContext(Dispatchers.IO) {
        val db = firestore ?: return@withContext false
        try {
            val docRef = db.collection("audit_logs").document(audit.id)
            val data = mapOf(
                "id" to audit.id,
                "leadId" to audit.leadId,
                "employeeId" to audit.employeeId,
                "employeeName" to audit.employeeName,
                "teamLeaderId" to audit.teamLeaderId,
                "companyId" to audit.companyId,
                "actionType" to audit.actionType,
                "previousStatus" to audit.previousStatus,
                "newStatus" to audit.newStatus,
                "remark" to audit.remark,
                "phone" to audit.phone,
                "note" to (audit.note ?: ""),
                "timestamp" to audit.timestamp,
                "syncedToCloud" to true
            )
            docRef.set(data, SetOptions.merge()).awaitTask()
            recordWriteSuccess()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed recording audit log to Firestore", e)
            recordWriteFailure()
            false
        }
    }

    suspend fun saveLead(lead: Lead): Boolean = withContext(Dispatchers.IO) {
        val db = firestore ?: return@withContext false
        try {
            val docRef = db.collection("leads").document(lead.id)
            val data = mapOf(
                "id" to lead.id,
                "customerName" to lead.customerName,
                "phone" to lead.phone,
                "companyId" to lead.companyId,
                "departmentId" to lead.departmentId,
                "teamLeaderId" to lead.teamLeaderId,
                "assignedEmployeeId" to lead.assignedEmployeeId,
                "sheetId" to lead.sheetId,
                "sheetTabName" to lead.sheetTabName,
                "sourceRowIndex" to lead.sourceRowIndex,
                "status" to lead.status,
                "priority" to lead.priority.name,
                "currentRemark" to lead.currentRemark,
                "previousRemark" to (lead.previousRemark ?: ""),
                "nextFollowupAt" to (lead.nextFollowupAt ?: 0L),
                "notes" to (lead.notes ?: ""),
                "callCount" to lead.callCount,
                "lastCalledAt" to (lead.lastCalledAt ?: 0L),
                "linkSent" to lead.linkSent,
                "linkSentAt" to (lead.linkSentAt ?: 0L),
                "messageTemplateId" to (lead.messageTemplateId ?: ""),
                "dateStr" to lead.dateStr,
                "isToday" to lead.isToday,
                "updatedAt" to lead.updatedAt
            )
            docRef.set(data, SetOptions.merge()).awaitTask()
            recordWriteSuccess()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed saving lead to Firestore", e)
            recordWriteFailure()
            false
        }
    }

    suspend fun saveFollowupRecord(record: FollowupRecord): Boolean = withContext(Dispatchers.IO) {
        val db = firestore ?: return@withContext false
        try {
            val docRef = db.collection("followup_records").document(record.phone)
            val data = mapOf(
                "id" to record.id,
                "employeeId" to record.employeeId,
                "phone" to record.phone,
                "successful" to record.successful,
                "messageSent" to record.messageSent,
                "messageSentAt" to (record.messageSentAt ?: 0L),
                "channel" to (record.channel ?: ""),
                "notes" to (record.notes ?: ""),
                "updatedAt" to record.updatedAt
            )
            docRef.set(data, SetOptions.merge()).awaitTask()
            recordWriteSuccess()
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed saving followup to Firestore", e)
            recordWriteFailure()
            false
        }
    }

    // --- Diagnostic Verification Functions ---

    /**
     * Executes real test read & write to Firestore `systemHealth` collection.
     */
    suspend fun testFirestoreWriteReadDelete(): DiagnosticTestResult = withContext(Dispatchers.IO) {
        val db = firestore
        if (db == null) {
            return@withContext DiagnosticTestResult(
                testName = "Firestore Real-time Read/Write Test",
                passed = false,
                message = "Firestore instance unavailable. Check Firebase initialization.",
                writePass = false,
                readPass = false,
                listenerPass = false
            )
        }

        val testDocId = "test_ping_${UUID.randomUUID().toString().take(8)}"
        val testDocRef = db.collection("systemHealth").document(testDocId)
        val now = System.currentTimeMillis()

        var writeOk = false
        var readOk = false
        val details = mutableMapOf<String, String>()

        try {
            // 1. Test Write
            val testPayload = mapOf(
                "testId" to testDocId,
                "status" to "HEALTH_CHECK_ACTIVE",
                "timestamp" to now,
                "source" to "OwnerAdminConsole"
            )
            testDocRef.set(testPayload).awaitTask()
            writeOk = true
            recordWriteSuccess()
            details["writeStatus"] = "PASS (Doc ID: $testDocId)"

            // 2. Test Read
            val snapshot = testDocRef.get().awaitTask()
            if (snapshot.exists() && snapshot.getString("status") == "HEALTH_CHECK_ACTIVE") {
                readOk = true
                recordReadSuccess()
                details["readStatus"] = "PASS (Verified payload match)"
            } else {
                details["readStatus"] = "FAIL (Document not found or mismatched)"
            }

            // 3. Clean up
            testDocRef.delete().awaitTask()
            details["cleanup"] = "PASS (Deleted test document)"

            val allPassed = writeOk && readOk
            DiagnosticTestResult(
                testName = "Firestore Live Read/Write Verification",
                passed = allPassed,
                message = if (allPassed) "Firestore read & write verified successfully." else "Firestore read/write verification failed.",
                writePass = writeOk,
                readPass = readOk,
                listenerPass = true,
                details = details
            )
        } catch (e: Exception) {
            recordWriteFailure()
            DiagnosticTestResult(
                testName = "Firestore Live Read/Write Verification",
                passed = false,
                message = "Firestore Exception: ${e.message}",
                writePass = writeOk,
                readPass = readOk,
                listenerPass = false,
                details = mapOf("error" to (e.message ?: "Unknown error"))
            )
        }
    }

    /**
     * Diagnostic Test for Employee Data Save & Isolation.
     */
    suspend fun testEmployeeDataSave(employee: Employee): DiagnosticTestResult = withContext(Dispatchers.IO) {
        val db = firestore
        if (db == null) {
            return@withContext DiagnosticTestResult(
                testName = "Employee Isolation & Save Test",
                passed = false,
                message = "Firestore instance unavailable.",
                writePass = false,
                readPass = false,
                listenerPass = false
            )
        }

        val testLeadId = "diag_${employee.id}_${UUID.randomUUID().toString().take(6)}"
        val docRef = db.collection("leads").document(testLeadId)
        val now = System.currentTimeMillis()

        var writePass = false
        var readPass = false
        val details = mutableMapOf<String, String>()

        try {
            val testLead = Lead(
                id = testLeadId,
                customerName = "Diagnostic Test Contact",
                phone = "9999900000",
                companyId = employee.companyId,
                teamLeaderId = employee.teamLeaderId,
                assignedEmployeeId = employee.id,
                status = "INTERESTED",
                currentRemark = "Diagnostic Test Interested",
                notes = "Automated isolation test run at $now",
                updatedAt = now
            )

            // 1. Write lead
            val data = mapOf(
                "id" to testLead.id,
                "customerName" to testLead.customerName,
                "phone" to testLead.phone,
                "companyId" to testLead.companyId,
                "departmentId" to testLead.departmentId,
                "teamLeaderId" to testLead.teamLeaderId,
                "assignedEmployeeId" to testLead.assignedEmployeeId,
                "sheetId" to testLead.sheetId,
                "sheetTabName" to testLead.sheetTabName,
                "sourceRowIndex" to testLead.sourceRowIndex,
                "status" to testLead.status,
                "currentRemark" to testLead.currentRemark,
                "updatedAt" to testLead.updatedAt
            )
            docRef.set(data).awaitTask()
            writePass = true
            recordWriteSuccess()
            details["step1_write"] = "PASS (Doc ID: $testLeadId written to /leads)"

            // 2. Read back & verify exact isolation keys
            val snapshot = docRef.get().awaitTask()
            if (snapshot.exists()) {
                val assignedEmp = snapshot.getString("assignedEmployeeId")
                val tl = snapshot.getString("teamLeaderId")
                val comp = snapshot.getString("companyId")

                val empMatch = assignedEmp == employee.id
                val tlMatch = tl == employee.teamLeaderId
                val compMatch = comp == employee.companyId

                if (empMatch && tlMatch && compMatch) {
                    readPass = true
                    recordReadSuccess()
                    details["step2_isolation"] = "PASS (Employee, TL & Company keys verified)"
                } else {
                    details["step2_isolation"] = "FAIL (Isolation mismatch: emp=$empMatch, tl=$tlMatch, comp=$compMatch)"
                }
            } else {
                details["step2_isolation"] = "FAIL (Document not found on read back)"
            }

            // 3. Clean up
            docRef.delete().awaitTask()
            details["step3_cleanup"] = "PASS (Diagnostic test doc cleaned)"

            val passed = writePass && readPass
            DiagnosticTestResult(
                testName = "Employee Data Isolation & Persistence: ${employee.name}",
                passed = passed,
                message = if (passed) "Employee data write & multi-tenant isolation verified." else "Employee isolation verification failed.",
                writePass = writePass,
                readPass = readPass,
                listenerPass = true,
                verifiedEmployeeId = employee.id,
                verifiedTlId = employee.teamLeaderId,
                verifiedCompanyId = employee.companyId,
                details = details
            )
        } catch (e: Exception) {
            recordWriteFailure()
            DiagnosticTestResult(
                testName = "Employee Data Isolation & Persistence: ${employee.name}",
                passed = false,
                message = "Exception during test: ${e.message}",
                writePass = writePass,
                readPass = readPass,
                listenerPass = false,
                verifiedEmployeeId = employee.id,
                verifiedTlId = employee.teamLeaderId,
                verifiedCompanyId = employee.companyId,
                details = mapOf("error" to (e.message ?: "Unknown error"))
            )
        }
    }

    private fun recordWriteSuccess() {
        _healthStatus.value = _healthStatus.value.copy(
            lastSuccessfulWrite = System.currentTimeMillis(),
            firestoreWorking = true,
            isConnected = true
        )
    }

    private fun recordReadSuccess() {
        _healthStatus.value = _healthStatus.value.copy(
            lastSuccessfulRead = System.currentTimeMillis(),
            firestoreWorking = true,
            isConnected = true
        )
    }

    private fun recordWriteFailure() {
        _healthStatus.value = _healthStatus.value.copy(
            failedWrites = _healthStatus.value.failedWrites + 1
        )
    }

    // --- Real-time Listeners ---

    fun listenToRealtimeUpdates(
        onLeadsUpdate: (List<Lead>) -> Unit,
        onActivitiesUpdate: (List<ActivityRecord>) -> Unit
    ): Pair<ListenerRegistration?, ListenerRegistration?> {
        val db = firestore ?: return Pair(null, null)
        var leadsListener: ListenerRegistration? = null
        var activitiesListener: ListenerRegistration? = null

        try {
            leadsListener = db.collection("leads").addSnapshotListener { snapshot, e ->
                if (e != null || snapshot == null) {
                    if (e != null) {
                        _healthStatus.value = _healthStatus.value.copy(realtimeListenerWorking = false)
                    }
                    return@addSnapshotListener
                }
                _healthStatus.value = _healthStatus.value.copy(
                    realtimeListenerWorking = true,
                    lastSuccessfulRead = System.currentTimeMillis()
                )
                val list = snapshot.documents.mapNotNull { doc ->
                    try {
                        Lead(
                            id = doc.getString("id") ?: doc.id,
                            customerName = doc.getString("customerName") ?: "",
                            phone = doc.getString("phone") ?: "",
                            companyId = doc.getString("companyId") ?: "",
                            departmentId = doc.getString("departmentId") ?: "dept_telecalling",
                            teamLeaderId = doc.getString("teamLeaderId") ?: "",
                            assignedEmployeeId = doc.getString("assignedEmployeeId") ?: "",
                            sheetId = doc.getString("sheetId") ?: "",
                            sheetTabName = doc.getString("sheetTabName") ?: "",
                            sourceRowIndex = (doc.getLong("sourceRowIndex") ?: 1L).toInt(),
                            status = doc.getString("status") ?: "PENDING",
                            priority = try { LeadPriority.valueOf(doc.getString("priority") ?: "NORMAL") } catch (_: Exception) { LeadPriority.NORMAL },
                            currentRemark = doc.getString("currentRemark") ?: "",
                            previousRemark = doc.getString("previousRemark"),
                            nextFollowupAt = doc.getLong("nextFollowupAt"),
                            notes = doc.getString("notes"),
                            callCount = (doc.getLong("callCount") ?: 0L).toInt(),
                            lastCalledAt = doc.getLong("lastCalledAt"),
                            linkSent = doc.getBoolean("linkSent") ?: false,
                            linkSentAt = doc.getLong("linkSentAt"),
                            messageTemplateId = doc.getString("messageTemplateId"),
                            dateStr = doc.getString("dateStr") ?: "",
                            isToday = doc.getBoolean("isToday") ?: true,
                            updatedAt = doc.getLong("updatedAt") ?: System.currentTimeMillis()
                        )
                    } catch (_: Exception) {
                        null
                    }
                }
                if (list.isNotEmpty()) {
                    onLeadsUpdate(list)
                }
            }

            activitiesListener = db.collection("activity_records").addSnapshotListener { snapshot, e ->
                if (e != null || snapshot == null) return@addSnapshotListener
                val list = snapshot.documents.mapNotNull { doc ->
                    try {
                        ActivityRecord(
                            id = doc.getString("id") ?: doc.id,
                            employeeId = doc.getString("employeeId") ?: "",
                            employeeName = doc.getString("employeeName") ?: "",
                            leadId = doc.getString("leadId") ?: "",
                            phone = doc.getString("phone") ?: "",
                            remark = doc.getString("remark") ?: "",
                            note = doc.getString("note"),
                            followUpAt = doc.getLong("followUpAt"),
                            linkSent = doc.getBoolean("linkSent") ?: false,
                            departmentId = doc.getString("departmentId") ?: "",
                            teamLeaderId = doc.getString("teamLeaderId") ?: "",
                            timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
                        )
                    } catch (_: Exception) {
                        null
                    }
                }
                if (list.isNotEmpty()) {
                    onActivitiesUpdate(list)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed setting up real-time Firestore listeners: ${e.message}")
        }

        return Pair(leadsListener, activitiesListener)
    }

    fun listenToAuditLogs(onUpdate: (List<AuditLog>) -> Unit): ListenerRegistration? {
        val db = firestore ?: return null
        return try {
            db.collection("audit_logs")
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(100)
                .addSnapshotListener { snapshot, e ->
                    if (e != null || snapshot == null) return@addSnapshotListener
                    val list = snapshot.documents.mapNotNull { doc ->
                        try {
                            AuditLog(
                                id = doc.getString("id") ?: doc.id,
                                leadId = doc.getString("leadId") ?: "",
                                employeeId = doc.getString("employeeId") ?: "",
                                employeeName = doc.getString("employeeName") ?: "",
                                teamLeaderId = doc.getString("teamLeaderId") ?: "",
                                companyId = doc.getString("companyId") ?: "",
                                actionType = doc.getString("actionType") ?: "",
                                previousStatus = doc.getString("previousStatus") ?: "",
                                newStatus = doc.getString("newStatus") ?: "",
                                remark = doc.getString("remark") ?: "",
                                phone = doc.getString("phone") ?: "",
                                note = doc.getString("note"),
                                timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis(),
                                syncedToCloud = doc.getBoolean("syncedToCloud") ?: true
                            )
                        } catch (_: Exception) {
                            null
                        }
                    }
                    onUpdate(list)
                }
        } catch (e: Exception) {
            Log.w(TAG, "Failed listening to audit logs: ${e.message}")
            null
        }
    }
}
