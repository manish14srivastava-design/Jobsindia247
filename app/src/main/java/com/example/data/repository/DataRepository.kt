package com.example.data.repository

import com.example.data.model.ActivityRecord
import com.example.data.model.Lead
import com.example.data.model.LeadPriority
import com.example.data.model.QuickRemark
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * DataRepository implementation directly interacting with Firebase Firestore SDK.
 * Handles CRUD operations and real-time streaming for 'leads', 'activities', and 'quickRemarks'
 * without mock layers.
 */
class DataRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    companion object {
        const val COLLECTION_LEADS = "leads"
        const val COLLECTION_ACTIVITIES = "activities"
        const val COLLECTION_QUICK_REMARKS = "quickRemarks"
    }

    // ==========================================
    // LEADS CRUD OPERATIONS
    // ==========================================

    /**
     * Create or overwrite a lead in Firestore.
     */
    suspend fun createLead(lead: Lead): Result<Unit> = runCatching {
        val leadId = lead.id.ifBlank { firestore.collection(COLLECTION_LEADS).document().id }
        val leadToSave = if (lead.id.isBlank()) lead.copy(id = leadId) else lead
        firestore.collection(COLLECTION_LEADS)
            .document(leadId)
            .set(leadToDocumentMap(leadToSave), SetOptions.merge())
            .await()
    }

    /**
     * Batch insert or update multiple leads.
     */
    suspend fun batchCreateOrUpdateLeads(leads: List<Lead>): Result<Unit> = runCatching {
        if (leads.isEmpty()) return@runCatching
        val batch = firestore.batch()
        for (lead in leads) {
            val leadId = lead.id.ifBlank { firestore.collection(COLLECTION_LEADS).document().id }
            val leadToSave = if (lead.id.isBlank()) lead.copy(id = leadId) else lead
            val docRef = firestore.collection(COLLECTION_LEADS).document(leadId)
            batch.set(docRef, leadToDocumentMap(leadToSave), SetOptions.merge())
        }
        batch.commit().await()
    }

    /**
     * Read a single lead by its unique ID.
     */
    suspend fun getLeadById(leadId: String): Result<Lead?> = runCatching {
        val snapshot = firestore.collection(COLLECTION_LEADS)
            .document(leadId)
            .get()
            .await()
        if (snapshot.exists()) documentToLead(snapshot) else null
    }

    /**
     * Read all leads, ordered by Google Sheet original sequence (sourceRowIndex).
     */
    suspend fun getAllLeads(): Result<List<Lead>> = runCatching {
        val snapshot = firestore.collection(COLLECTION_LEADS)
            .orderBy("sourceRowIndex", Query.Direction.ASCENDING)
            .get()
            .await()
        snapshot.documents.mapNotNull { documentToLead(it) }
    }

    /**
     * Read leads assigned to an employee.
     */
    suspend fun getLeadsByEmployee(employeeId: String): Result<List<Lead>> = runCatching {
        val snapshot = firestore.collection(COLLECTION_LEADS)
            .whereEqualTo("assignedEmployeeId", employeeId)
            .get()
            .await()
        snapshot.documents.mapNotNull { documentToLead(it) }
            .sortedBy { it.sourceRowIndex }
    }

    /**
     * Read leads by status (e.g., INTERESTED, SUCCESSFUL, PENDING).
     */
    suspend fun getLeadsByStatus(status: String): Result<List<Lead>> = runCatching {
        val snapshot = firestore.collection(COLLECTION_LEADS)
            .whereEqualTo("status", status)
            .get()
            .await()
        snapshot.documents.mapNotNull { documentToLead(it) }
            .sortedBy { it.sourceRowIndex }
    }

    /**
     * Update specific fields of a Lead document.
     */
    suspend fun updateLeadFields(leadId: String, updates: Map<String, Any?>): Result<Unit> = runCatching {
        val finalUpdates = updates.toMutableMap()
        finalUpdates["updatedAt"] = System.currentTimeMillis()
        firestore.collection(COLLECTION_LEADS)
            .document(leadId)
            .update(finalUpdates)
            .await()
    }

    /**
     * Update entire Lead document.
     */
    suspend fun updateLead(lead: Lead): Result<Unit> = runCatching {
        val updatedLead = lead.copy(updatedAt = System.currentTimeMillis())
        firestore.collection(COLLECTION_LEADS)
            .document(lead.id)
            .set(leadToDocumentMap(updatedLead), SetOptions.merge())
            .await()
    }

    /**
     * Delete a lead document by ID.
     */
    suspend fun deleteLead(leadId: String): Result<Unit> = runCatching {
        firestore.collection(COLLECTION_LEADS)
            .document(leadId)
            .delete()
            .await()
    }

    /**
     * Real-time stream of all leads.
     */
    fun streamAllLeads(): Flow<List<Lead>> = callbackFlow {
        val listener = firestore.collection(COLLECTION_LEADS)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val leads = snapshot?.documents?.mapNotNull { documentToLead(it) }
                    ?.sortedBy { it.sourceRowIndex }
                    ?: emptyList()
                trySend(leads)
            }
        awaitClose { listener.remove() }
    }

    /**
     * Real-time stream of leads for a specific employee.
     */
    fun streamLeadsByEmployee(employeeId: String): Flow<List<Lead>> = callbackFlow {
        val listener = firestore.collection(COLLECTION_LEADS)
            .whereEqualTo("assignedEmployeeId", employeeId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val leads = snapshot?.documents?.mapNotNull { documentToLead(it) }
                    ?.sortedBy { it.sourceRowIndex }
                    ?: emptyList()
                trySend(leads)
            }
        awaitClose { listener.remove() }
    }

    // ==========================================
    // ACTIVITIES CRUD OPERATIONS
    // ==========================================

    /**
     * Create an activity audit record.
     */
    suspend fun createActivity(activity: ActivityRecord): Result<Unit> = runCatching {
        val activityId = activity.id.ifBlank { firestore.collection(COLLECTION_ACTIVITIES).document().id }
        val activityToSave = if (activity.id.isBlank()) activity.copy(id = activityId) else activity
        firestore.collection(COLLECTION_ACTIVITIES)
            .document(activityId)
            .set(activityToDocumentMap(activityToSave), SetOptions.merge())
            .await()
    }

    /**
     * Read an activity record by ID.
     */
    suspend fun getActivityById(activityId: String): Result<ActivityRecord?> = runCatching {
        val snapshot = firestore.collection(COLLECTION_ACTIVITIES)
            .document(activityId)
            .get()
            .await()
        if (snapshot.exists()) documentToActivity(snapshot) else null
    }

    /**
     * Read recent activities across the organization (e.g., for supervisory dashboards).
     */
    suspend fun getRecentActivities(limit: Long = 100): Result<List<ActivityRecord>> = runCatching {
        val snapshot = firestore.collection(COLLECTION_ACTIVITIES)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(limit)
            .get()
            .await()
        snapshot.documents.mapNotNull { documentToActivity(it) }
    }

    /**
     * Read activities for a specific employee.
     */
    suspend fun getActivitiesByEmployee(employeeId: String, limit: Long = 100): Result<List<ActivityRecord>> = runCatching {
        val snapshot = firestore.collection(COLLECTION_ACTIVITIES)
            .whereEqualTo("employeeId", employeeId)
            .get()
            .await()
        snapshot.documents.mapNotNull { documentToActivity(it) }
            .sortedByDescending { it.timestamp }
            .take(limit.toInt())
    }

    /**
     * Read activities for a specific lead.
     */
    suspend fun getActivitiesByLead(leadId: String): Result<List<ActivityRecord>> = runCatching {
        val snapshot = firestore.collection(COLLECTION_ACTIVITIES)
            .whereEqualTo("leadId", leadId)
            .get()
            .await()
        snapshot.documents.mapNotNull { documentToActivity(it) }
            .sortedByDescending { it.timestamp }
    }

    /**
     * Update an activity record.
     */
    suspend fun updateActivity(activity: ActivityRecord): Result<Unit> = runCatching {
        firestore.collection(COLLECTION_ACTIVITIES)
            .document(activity.id)
            .set(activityToDocumentMap(activity), SetOptions.merge())
            .await()
    }

    /**
     * Delete an activity record.
     */
    suspend fun deleteActivity(activityId: String): Result<Unit> = runCatching {
        firestore.collection(COLLECTION_ACTIVITIES)
            .document(activityId)
            .delete()
            .await()
    }

    /**
     * Real-time stream of recent activities.
     */
    fun streamRecentActivities(limit: Long = 100): Flow<List<ActivityRecord>> = callbackFlow {
        val listener = firestore.collection(COLLECTION_ACTIVITIES)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(limit)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val activities = snapshot?.documents?.mapNotNull { documentToActivity(it) } ?: emptyList()
                trySend(activities)
            }
        awaitClose { listener.remove() }
    }

    // ==========================================
    // QUICK REMARKS CRUD OPERATIONS
    // ==========================================

    /**
     * Create or update a quick remark template.
     */
    suspend fun createQuickRemark(remark: QuickRemark): Result<Unit> = runCatching {
        val remarkId = remark.id.ifBlank { firestore.collection(COLLECTION_QUICK_REMARKS).document().id }
        val remarkToSave = if (remark.id.isBlank()) remark.copy(id = remarkId) else remark
        firestore.collection(COLLECTION_QUICK_REMARKS)
            .document(remarkId)
            .set(quickRemarkToDocumentMap(remarkToSave), SetOptions.merge())
            .await()
    }

    /**
     * Batch insert or sync quick remarks.
     */
    suspend fun batchSetQuickRemarks(remarks: List<QuickRemark>): Result<Unit> = runCatching {
        if (remarks.isEmpty()) return@runCatching
        val batch = firestore.batch()
        for (remark in remarks) {
            val remarkId = remark.id.ifBlank { firestore.collection(COLLECTION_QUICK_REMARKS).document().id }
            val remarkToSave = if (remark.id.isBlank()) remark.copy(id = remarkId) else remark
            val docRef = firestore.collection(COLLECTION_QUICK_REMARKS).document(remarkId)
            batch.set(docRef, quickRemarkToDocumentMap(remarkToSave), SetOptions.merge())
        }
        batch.commit().await()
    }

    /**
     * Read a quick remark by ID.
     */
    suspend fun getQuickRemarkById(remarkId: String): Result<QuickRemark?> = runCatching {
        val snapshot = firestore.collection(COLLECTION_QUICK_REMARKS)
            .document(remarkId)
            .get()
            .await()
        if (snapshot.exists()) documentToQuickRemark(snapshot) else null
    }

    /**
     * Read all active quick remarks, sorted by displayOrder.
     */
    suspend fun getAllQuickRemarks(): Result<List<QuickRemark>> = runCatching {
        val snapshot = firestore.collection(COLLECTION_QUICK_REMARKS)
            .get()
            .await()
        snapshot.documents.mapNotNull { documentToQuickRemark(it) }
            .sortedBy { it.displayOrder }
    }

    /**
     * Update quick remark.
     */
    suspend fun updateQuickRemark(remark: QuickRemark): Result<Unit> = runCatching {
        firestore.collection(COLLECTION_QUICK_REMARKS)
            .document(remark.id)
            .set(quickRemarkToDocumentMap(remark), SetOptions.merge())
            .await()
    }

    /**
     * Delete a quick remark.
     */
    suspend fun deleteQuickRemark(remarkId: String): Result<Unit> = runCatching {
        firestore.collection(COLLECTION_QUICK_REMARKS)
            .document(remarkId)
            .delete()
            .await()
    }

    /**
     * Real-time stream of all quick remarks.
     */
    fun streamQuickRemarks(): Flow<List<QuickRemark>> = callbackFlow {
        val listener = firestore.collection(COLLECTION_QUICK_REMARKS)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val remarks = snapshot?.documents?.mapNotNull { documentToQuickRemark(it) }
                    ?.sortedBy { it.displayOrder }
                    ?: emptyList()
                trySend(remarks)
            }
        awaitClose { listener.remove() }
    }

    // ==========================================
    // MAPPERS: Object <-> Firestore Map
    // ==========================================

    private fun leadToDocumentMap(lead: Lead): Map<String, Any?> {
        return mapOf(
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
            "createdAt" to lead.createdAt,
            "updatedAt" to lead.updatedAt
        )
    }

    private fun documentToLead(doc: DocumentSnapshot): Lead? {
        return try {
            val priorityStr = doc.getString("priority") ?: "NORMAL"
            val priority = try {
                LeadPriority.valueOf(priorityStr)
            } catch (e: Exception) {
                LeadPriority.NORMAL
            }
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
                priority = priority,
                currentRemark = doc.getString("currentRemark") ?: "",
                previousRemark = doc.getString("previousRemark")?.takeIf { it.isNotBlank() },
                nextFollowupAt = doc.getLong("nextFollowupAt")?.takeIf { it > 0L },
                notes = doc.getString("notes")?.takeIf { it.isNotBlank() },
                callCount = (doc.getLong("callCount") ?: 0L).toInt(),
                lastCalledAt = doc.getLong("lastCalledAt")?.takeIf { it > 0L },
                linkSent = doc.getBoolean("linkSent") ?: false,
                linkSentAt = doc.getLong("linkSentAt")?.takeIf { it > 0L },
                messageTemplateId = doc.getString("messageTemplateId")?.takeIf { it.isNotBlank() },
                createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis(),
                updatedAt = doc.getLong("updatedAt") ?: System.currentTimeMillis()
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun activityToDocumentMap(activity: ActivityRecord): Map<String, Any?> {
        return mapOf(
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
    }

    private fun documentToActivity(doc: DocumentSnapshot): ActivityRecord? {
        return try {
            ActivityRecord(
                id = doc.getString("id") ?: doc.id,
                employeeId = doc.getString("employeeId") ?: "",
                employeeName = doc.getString("employeeName") ?: "",
                leadId = doc.getString("leadId") ?: "",
                phone = doc.getString("phone") ?: "",
                remark = doc.getString("remark") ?: "",
                note = doc.getString("note")?.takeIf { it.isNotBlank() },
                followUpAt = doc.getLong("followUpAt")?.takeIf { it > 0L },
                linkSent = doc.getBoolean("linkSent") ?: false,
                departmentId = doc.getString("departmentId") ?: "",
                teamLeaderId = doc.getString("teamLeaderId") ?: "",
                timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun quickRemarkToDocumentMap(remark: QuickRemark): Map<String, Any?> {
        return mapOf(
            "id" to remark.id,
            "label" to remark.label,
            "colorHex" to remark.colorHex,
            "requiresFollowup" to remark.requiresFollowup,
            "displayOrder" to remark.displayOrder,
            "isActive" to remark.isActive
        )
    }

    private fun documentToQuickRemark(doc: DocumentSnapshot): QuickRemark? {
        return try {
            QuickRemark(
                id = doc.getString("id") ?: doc.id,
                label = doc.getString("label") ?: "",
                colorHex = doc.getString("colorHex") ?: "#10E57A",
                requiresFollowup = doc.getBoolean("requiresFollowup") ?: false,
                displayOrder = (doc.getLong("displayOrder") ?: 0L).toInt(),
                isActive = doc.getBoolean("isActive") ?: true
            )
        } catch (e: Exception) {
            null
        }
    }
}
