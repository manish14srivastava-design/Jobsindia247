package com.example.data.sync

import com.example.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.BufferedReader
import java.io.StringReader
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

data class SyncResult(
    val teamLeaderId: String,
    val employeesCount: Int,
    val leadsCount: Int,
    val success: Boolean,
    val error: String? = null
)

object GoogleSheetSyncEngine {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    // Exactly the 2 real companies specified with verified official links
    val REAL_COMPANIES = listOf(
        Company(
            id = "comp_ind08",
            companyCode = "IND08",
            name = "SPIN101",
            officialLink = "https://spin101.game",
            industry = "Gaming & Entertainment"
        ),
        Company(
            id = "comp_ind15",
            companyCode = "IND15",
            name = "RUMMY77",
            officialLink = "http://allrummy77.com",
            industry = "Gaming & Entertainment"
        )
    )

    // Exactly the 6 real team leaders with their verified sheet IDs & URLs
    val REAL_TEAM_LEADERS = listOf(
        // SPIN101 (IND08)
        TeamLeader(
            id = "tl_cherry",
            name = "Team Cherry",
            companyId = "comp_ind08",
            departmentId = "dept_telecalling",
            sheetId = "101u_NJ7UZYDzDe2HR7dUgnwsEl0WHRkUeXdTuat3lvg",
            primaryGid = "637358966",
            sheetUrl = "https://docs.google.com/spreadsheets/d/101u_NJ7UZYDzDe2HR7dUgnwsEl0WHRkUeXdTuat3lvg/edit?gid=637358966#gid=637358966",
            detectedTabsCount = 8,
            syncEnabled = true,
            syncStatus = "SYNCED",
            lastSyncedAt = System.currentTimeMillis()
        ),
        TeamLeader(
            id = "tl_musa",
            name = "Team Musa",
            companyId = "comp_ind08",
            departmentId = "dept_telecalling",
            sheetId = "1DD8e724R61qz3pN5O2flgDq7xh2GaxlXTQyG7kCcaWM",
            primaryGid = "2092700234",
            sheetUrl = "https://docs.google.com/spreadsheets/d/1DD8e724R61qz3pN5O2flgDq7xh2GaxlXTQyG7kCcaWM/edit?gid=2092700234#gid=2092700234",
            detectedTabsCount = 6,
            syncEnabled = true,
            syncStatus = "SYNCED",
            lastSyncedAt = System.currentTimeMillis()
        ),
        TeamLeader(
            id = "tl_zenni",
            name = "Team Zenni",
            companyId = "comp_ind08",
            departmentId = "dept_telecalling",
            sheetId = "1AxRsmasqPAU5u_qfuUzqn6dw_fNQaj_BI-7MjU386Bg",
            primaryGid = "2014427505",
            sheetUrl = "https://docs.google.com/spreadsheets/d/1AxRsmasqPAU5u_qfuUzqn6dw_fNQaj_BI-7MjU386Bg/edit?gid=2014427505#gid=2014427505",
            detectedTabsCount = 6,
            syncEnabled = true,
            syncStatus = "SYNCED",
            lastSyncedAt = System.currentTimeMillis()
        ),

        // RUMMY77 (IND15)
        TeamLeader(
            id = "tl_jennifer",
            name = "Team Jennifer",
            companyId = "comp_ind15",
            departmentId = "dept_telecalling",
            sheetId = "1e32WYPhjhKZbZ6jia4auvBjHJu7tTN0SncFYHSVFpf8",
            primaryGid = "872791572",
            sheetUrl = "https://docs.google.com/spreadsheets/d/1e32WYPhjhKZbZ6jia4auvBjHJu7tTN0SncFYHSVFpf8/edit?gid=872791572#gid=872791572",
            detectedTabsCount = 13,
            syncEnabled = true,
            syncStatus = "SYNCED",
            lastSyncedAt = System.currentTimeMillis()
        ),
        TeamLeader(
            id = "tl_venom",
            name = "Team Venom",
            companyId = "comp_ind15",
            departmentId = "dept_telecalling",
            sheetId = "1ALginCz7NbR7zVNZDTuDq9vzpS6PCQc1Ff4eU0zklnM",
            primaryGid = "820005143",
            sheetUrl = "https://docs.google.com/spreadsheets/d/1ALginCz7NbR7zVNZDTuDq9vzpS6PCQc1Ff4eU0zklnM/edit?gid=820005143#gid=820005143",
            detectedTabsCount = 5,
            syncEnabled = true,
            syncStatus = "SYNCED",
            lastSyncedAt = System.currentTimeMillis()
        ),
        TeamLeader(
            id = "tl_manish",
            name = "Team Manish",
            companyId = "comp_ind15",
            departmentId = "dept_telecalling",
            sheetId = "18-Ev29CNf_kaEr438NYo6coGzBFUdE7ucGLpIbkKSk0",
            primaryGid = "345792563",
            sheetUrl = "https://docs.google.com/spreadsheets/d/18-Ev29CNf_kaEr438NYo6coGzBFUdE7ucGLpIbkKSk0/edit?gid=345792563#gid=345792563",
            detectedTabsCount = 6,
            syncEnabled = true,
            syncStatus = "SYNCED",
            lastSyncedAt = System.currentTimeMillis()
        )
    )

    // Verified real employee tab names mapped per Team Leader Google Sheet
    val KNOWN_REAL_TABS_BY_TL_ID = mapOf(
        "tl_cherry" to listOf("Mahi singh", "PUJA", "rameez zaheer", "AJA", "mohd mohseen saha", "Ajay", "Sufiyan", "Priyanka"),
        "tl_musa" to listOf("Purnima", "Mohd Anjarul", "Abdul salam", "Sreelekha", "rajani", "Khan", "Tanusri Mukherjee"),
        "tl_zenni" to listOf("Sangeeta Madhi", "Kashif", "Toukeer", "M. Saif", "Mohd Joyeb", "anna"),
        "tl_jennifer" to listOf("SHEREEN", "SONI", "RITU", "PAYAL", "SIDDARTH", "PAYAL 2", "soni 2", "ritu 2", "Dolly", "sidhant kar", "Sneha", "ANSHIKA", "TANVI"),
        "tl_venom" to listOf("priyanka", "sufiyan", "Payel", "Ranjit", "Vishakha bagari"),
        "tl_manish" to listOf("Ankit", "RAM MISHRA", "Amit", "sheroj", "Sanjay", "Rnjit ", "Payel")
    )

    fun getAllInitialRealEmployees(): List<Employee> {
        val list = mutableListOf<Employee>()
        REAL_TEAM_LEADERS.forEach { tl ->
            val tabs = KNOWN_REAL_TABS_BY_TL_ID[tl.id] ?: emptyList()
            tabs.forEach { tabName ->
                list.add(
                    Employee(
                        id = "emp_${tl.id}_${tabName.trim().replace(" ", "_").lowercase()}",
                        name = tabName.trim(),
                        employeeTabName = tabName.trim(),
                        companyId = tl.companyId,
                        teamLeaderId = tl.id,
                        departmentId = tl.departmentId,
                        department = "Telecalling"
                    )
                )
            }
        }
        return list
    }

    // Dynamic tab & employee loader from connected Team Leader Google Sheets
    suspend fun fetchEmployeesForTeamLeader(tl: TeamLeader): List<Employee> = withContext(Dispatchers.IO) {
        val tabs = fetchSheetTabs(tl.sheetId, tl.id)
        val validTabs = if (tabs.isNotEmpty()) tabs else (KNOWN_REAL_TABS_BY_TL_ID[tl.id] ?: emptyList())
        validTabs.map { tabName ->
            Employee(
                id = "emp_${tl.id}_${tabName.trim().replace(" ", "_").lowercase()}",
                name = tabName.trim(),
                employeeTabName = tabName.trim(),
                companyId = tl.companyId,
                teamLeaderId = tl.id,
                departmentId = tl.departmentId,
                department = "Telecalling"
            )
        }
    }

    suspend fun fetchSheetTabs(sheetId: String, tlId: String? = null): List<String> = withContext(Dispatchers.IO) {
        try {
            val url = "https://docs.google.com/spreadsheets/d/$sheetId/edit"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext if (tlId != null) KNOWN_REAL_TABS_BY_TL_ID[tlId] ?: emptyList() else emptyList()
            }

            val html = response.body?.string() ?: return@withContext if (tlId != null) KNOWN_REAL_TABS_BY_TL_ID[tlId] ?: emptyList() else emptyList()
            val tabs = mutableListOf<String>()

            // Pattern 1: Array pattern in Google Sheet bootstrap data
            val arrayPattern = Pattern.compile("\\[\"([^\"]+)\",\\d+,\\d+,\\d+,\\d+,\\d+,\\d+,\\d+,\\d+\\]")
            val arrayMatcher = arrayPattern.matcher(html)
            while (arrayMatcher.find()) {
                val tab = arrayMatcher.group(1)?.trim() ?: ""
                if (isValidTabName(tab)) {
                    tabs.add(tab)
                }
            }

            // Pattern 2: JSON name property pattern
            if (tabs.isEmpty()) {
                val jsonPattern = Pattern.compile("\"name\":\"([^\"]+)\",\"sheetId\"")
                val jsonMatcher = jsonPattern.matcher(html)
                while (jsonMatcher.find()) {
                    val tab = jsonMatcher.group(1)?.trim() ?: ""
                    if (isValidTabName(tab)) {
                        tabs.add(tab)
                    }
                }
            }

            // Pattern 3: Legacy HTML Caption pattern
            if (tabs.isEmpty()) {
                val legacyPattern = Pattern.compile("docs-sheet-tab-caption\">([^<]+)</div>")
                val legacyMatcher = legacyPattern.matcher(html)
                while (legacyMatcher.find()) {
                    val tab = legacyMatcher.group(1)?.trim() ?: ""
                    if (isValidTabName(tab)) {
                        tabs.add(tab)
                    }
                }
            }

            val distinctTabs = tabs.distinct()
            if (distinctTabs.isNotEmpty()) {
                distinctTabs
            } else if (tlId != null) {
                KNOWN_REAL_TABS_BY_TL_ID[tlId] ?: emptyList()
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            if (tlId != null) KNOWN_REAL_TABS_BY_TL_ID[tlId] ?: emptyList() else emptyList()
        }
    }

    private fun isValidTabName(tab: String): Boolean {
        if (tab.isBlank() || tab.length > 50) return false
        val lower = tab.lowercase()
        return !lower.startsWith("sample") &&
                !lower.startsWith("report") &&
                !lower.startsWith("summary") &&
                !lower.contains("template")
    }

    suspend fun fetchTabRows(sheetId: String, tabName: String): List<List<String>> = withContext(Dispatchers.IO) {
        try {
            val encodedTab = URLEncoder.encode(tabName.trim(), "UTF-8")
            val url = "https://docs.google.com/spreadsheets/d/$sheetId/gviz/tq?tqx=out:csv&sheet=$encodedTab"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return@withContext emptyList()

            val csvContent = response.body?.string() ?: return@withContext emptyList()
            parseCsv(csvContent)
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun parseCsv(csvText: String): List<List<String>> {
        val result = mutableListOf<List<String>>()
        val reader = BufferedReader(StringReader(csvText))
        var line: String? = reader.readLine()

        while (line != null) {
            if (line.isNotBlank()) {
                val row = parseCsvLine(line)
                if (row.isNotEmpty()) {
                    result.add(row)
                }
            }
            line = reader.readLine()
        }
        return result
    }

    private fun parseCsvLine(line: String): List<String> {
        val tokens = mutableListOf<String>()
        var inQuotes = false
        val sb = StringBuilder()

        for (i in line.indices) {
            val c = line[i]
            if (c == '\"') {
                inQuotes = !inQuotes
            } else if (c == ',' && !inQuotes) {
                tokens.add(sb.toString().trim())
                sb.setLength(0)
            } else {
                sb.append(c)
            }
        }
        tokens.add(sb.toString().trim())
        return tokens
    }

    /**
     * Parses raw rows into ordered Leads, preserving the exact 1-indexed source row order (1, 2, 3... 300)
     * and strictly identifying today's current date numbers vs past archive numbers.
     */
    fun parseLeadsFromTabRows(
        rawRows: List<List<String>>,
        companyId: String,
        tlId: String,
        empId: String,
        sheetId: String,
        tabName: String
    ): List<Lead> {
        val leads = mutableListOf<Lead>()
        var sourceOrder = 1

        val todayCal = java.util.Calendar.getInstance()
        val todayDay = todayCal.get(java.util.Calendar.DAY_OF_MONTH)
        val todayMonth = todayCal.get(java.util.Calendar.MONTH) + 1 // 1-12
        val todayYear = todayCal.get(java.util.Calendar.YEAR)
        val todayYear2D = todayYear % 100
        val todayFormatted = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault()).format(java.util.Date())

        val monthMap = mapOf(
            "jan" to 1, "january" to 1,
            "feb" to 2, "february" to 2,
            "mar" to 3, "march" to 3,
            "apr" to 4, "april" to 4,
            "may" to 5,
            "jun" to 6, "june" to 6,
            "jul" to 7, "july" to 7,
            "aug" to 8, "august" to 8,
            "sep" to 9, "sept" to 9, "september" to 9,
            "oct" to 10, "october" to 10,
            "nov" to 11, "november" to 11,
            "dec" to 12, "december" to 12
        )

        fun parseDateInfo(cell: String): Pair<String, Boolean>? {
            return try {
                val s = cell.trim()
                if (s.isEmpty() || s.length > 30) return null

                // 1. yyyy-MM-dd or yyyy/MM/dd
                val ymdMatch = Pattern.compile("^(\\d{4})[-/](\\d{1,2})[-/](\\d{1,2})$").matcher(s)
                if (ymdMatch.find()) {
                    val y = ymdMatch.group(1)?.toIntOrNull() ?: 0
                    val m = ymdMatch.group(2)?.toIntOrNull() ?: 0
                    val d = ymdMatch.group(3)?.toIntOrNull() ?: 0
                    val isToday = (d == todayDay && m == todayMonth && (y == todayYear || y == 0))
                    return Pair(s, isToday)
                }

                // 2. dd/MM/yyyy or dd-MM-yyyy or dd.MM.yyyy
                val dmyMatch = Pattern.compile("^(\\d{1,2})[-/.](\\d{1,2})[-/.](\\d{2,4})$").matcher(s)
                if (dmyMatch.find()) {
                    var d = dmyMatch.group(1)?.toIntOrNull() ?: 0
                    var m = dmyMatch.group(2)?.toIntOrNull() ?: 0
                    val y = dmyMatch.group(3)?.toIntOrNull() ?: 0
                    if (d <= 12 && m > 12) {
                        val tmp = d; d = m; m = tmp // MM/dd/yyyy swap
                    }
                    val yMatch = (y == todayYear || y == todayYear2D || y == 0)
                    val isToday = (d == todayDay && m == todayMonth && yMatch)
                    return Pair(s, isToday)
                }

                // 3. dd-MMM-yyyy or dd MMM or dd-MMM
                val dMmmMatch = Pattern.compile("^(\\d{1,2})[- /]([a-zA-Z]{3,9})(?:[- /](\\d{2,4}))?$").matcher(s)
                if (dMmmMatch.find()) {
                    val d = dMmmMatch.group(1)?.toIntOrNull() ?: 0
                    val mName = dMmmMatch.group(2)?.lowercase() ?: ""
                    val m = monthMap[mName] ?: 0
                    val y = dMmmMatch.group(3)?.toIntOrNull() ?: 0
                    val yMatch = (y == 0 || y == todayYear || y == todayYear2D)
                    val isToday = (d == todayDay && m == todayMonth && yMatch)
                    return Pair(s, isToday)
                }

                // 4. MMM dd, yyyy or MMM dd
                val mmmDMatch = Pattern.compile("^([a-zA-Z]{3,9})[- /](\\d{1,2})(?:[- /,]+(\\d{2,4}))?$").matcher(s)
                if (mmmDMatch.find()) {
                    val mName = mmmDMatch.group(1)?.lowercase() ?: ""
                    val m = monthMap[mName] ?: 0
                    val d = mmmDMatch.group(2)?.toIntOrNull() ?: 0
                    val y = mmmDMatch.group(3)?.toIntOrNull() ?: 0
                    val yMatch = (y == 0 || y == todayYear || y == todayYear2D)
                    val isToday = (d == todayDay && m == todayMonth && yMatch)
                    return Pair(s, isToday)
                }

                // 5. dd/MM or dd-MM
                val dmShortMatch = Pattern.compile("^(\\d{1,2})[-/](\\d{1,2})$").matcher(s)
                if (dmShortMatch.find()) {
                    val d = dmShortMatch.group(1)?.toIntOrNull() ?: 0
                    val m = dmShortMatch.group(2)?.toIntOrNull() ?: 0
                    val isToday = (d == todayDay && m == todayMonth)
                    return Pair(s, isToday)
                }

                null
            } catch (_: Exception) {
                null
            }
        }

        // Header detection
        var headerRowIndex = -1
        var phoneCol = -1
        var remarkCol = -1
        var dateCol = -1
        var nameCol = -1
        var notesCol = -1

        for (rIdx in 0 until minOf(5, rawRows.size)) {
            val r = rawRows[rIdx]
            var hasPhoneHeader = false
            var hasRemarkHeader = false

            for (cIdx in r.indices) {
                val header = r[cIdx].trim().lowercase()
                if (header.contains("phone") || header.contains("mobile") || header.contains("contact") ||
                    header == "number" || header == "numbers" || header == "no." || header == "no"
                ) {
                    phoneCol = cIdx
                    hasPhoneHeader = true
                }
                if (header.contains("remark") || header.contains("status") || header.contains("feedback") ||
                    header.contains("disposition") || header == "result" || header.contains("calling")
                ) {
                    remarkCol = cIdx
                    hasRemarkHeader = true
                }
                if (header.contains("date") || header == "dt" || header == "day") {
                    dateCol = cIdx
                }
                if (header.contains("name") || header.contains("customer") || header.contains("client")) {
                    nameCol = cIdx
                }
                if (header.contains("note") || header.contains("comment") || header.contains("sub remark")) {
                    notesCol = cIdx
                }
            }

            if (hasPhoneHeader || hasRemarkHeader || r.any { it.trim().equals("s.no", ignoreCase = true) || it.trim().equals("sr no", ignoreCase = true) }) {
                headerRowIndex = rIdx
                break
            }
        }

        // Pre-scan to check if the tab contains any explicit date column
        var tabHasDates = false
        val scanStart = if (headerRowIndex >= 0) headerRowIndex + 1 else 0
        for (rIdx in scanStart until rawRows.size) {
            val row = rawRows[rIdx]
            for (cell in row) {
                if (parseDateInfo(cell) != null) {
                    tabHasDates = true
                    break
                }
            }
            if (tabHasDates) break
        }

        for (rIdx in scanStart until rawRows.size) {
            val row = rawRows[rIdx]
            if (row.isEmpty()) continue

            // Extract Date, Phone Number, and Remarks across cells
            var phone = ""
            var remarkRaw = ""
            var notesRaw = ""
            var rowDateStr = ""
            var isRowToday = !tabHasDates // If no dates in sheet at all, all rows belong to today's active batch

            // 1. Scan for Date cells
            if (dateCol != -1 && dateCol < row.size) {
                val dInfo = parseDateInfo(row[dateCol])
                if (dInfo != null) {
                    rowDateStr = dInfo.first
                    isRowToday = dInfo.second
                }
            }
            if (rowDateStr.isBlank()) {
                for (cell in row) {
                    val dInfo = parseDateInfo(cell)
                    if (dInfo != null) {
                        rowDateStr = dInfo.first
                        isRowToday = dInfo.second
                        break
                    }
                }
            }

            // 2. Scan for Phone Number (skipping date cells)
            if (phoneCol != -1 && phoneCol < row.size) {
                val digits = row[phoneCol].filter { it.isDigit() }
                if (digits.length in 8..15) {
                    phone = digits
                }
            }
            if (phone.isBlank()) {
                for (cell in row) {
                    val cellTrim = cell.trim()
                    if (parseDateInfo(cellTrim) != null) continue
                    val digits = cellTrim.filter { it.isDigit() }
                    if (digits.length in 8..15) {
                        if (digits.length >= 10 || (!cellTrim.contains("/") && !cellTrim.contains("-"))) {
                            phone = digits
                            break
                        }
                    }
                }
            }

            if (phone.isBlank()) continue

            // 3. Scan for Remark
            if (remarkCol != -1 && remarkCol < row.size) {
                val cell = row[remarkCol].trim()
                if (RemarkConstants.isValidRemark(cell)) {
                    remarkRaw = cell
                }
            }
            if (remarkRaw.isBlank()) {
                for (cell in row) {
                    val cellTrim = cell.trim()
                    if (parseDateInfo(cellTrim) != null) continue
                    val digits = cellTrim.filter { it.isDigit() }
                    if (digits.length >= 8) continue // skip phone number
                    if (RemarkConstants.isValidRemark(cellTrim)) {
                        remarkRaw = cellTrim
                        break
                    }
                }
            }

            // 4. Notes / Link
            if (notesCol != -1 && notesCol < row.size) {
                notesRaw = row[notesCol].trim()
            }

            // Normalize actual Google Sheet remark
            val normalizedRemark = RemarkConstants.normalize(remarkRaw)
            val hasRemark = RemarkConstants.isValidRemark(normalizedRemark)

            val status = when {
                normalizedRemark.equals(RemarkConstants.INTERESTED, ignoreCase = true) -> "INTERESTED"
                normalizedRemark.equals(RemarkConstants.SUCCESSFUL, ignoreCase = true) ||
                        normalizedRemark.equals(RemarkConstants.DONE, ignoreCase = true) -> "SUCCESSFUL"
                normalizedRemark.equals(RemarkConstants.CALLBACK, ignoreCase = true) -> "CALLBACK"
                normalizedRemark.equals(RemarkConstants.FOLLOW_UP, ignoreCase = true) -> "FOLLOW_UP"
                hasRemark -> "COMPLETED"
                else -> "PENDING"
            }

            val leadId = "lead_${companyId}_${tlId}_${empId}_row_$sourceOrder"
            val isLinkSent = notesRaw.contains("link", ignoreCase = true) ||
                    remarkRaw.contains("link", ignoreCase = true) ||
                    normalizedRemark.equals(RemarkConstants.INTERESTED, ignoreCase = true) ||
                    status == "SUCCESSFUL"

            leads.add(
                Lead(
                    id = leadId,
                    customerName = "Customer #$sourceOrder",
                    phone = phone,
                    companyId = companyId,
                    departmentId = "dept_telecalling",
                    teamLeaderId = tlId,
                    assignedEmployeeId = empId,
                    sheetId = sheetId,
                    sheetTabName = tabName,
                    sourceRowIndex = sourceOrder,
                    status = status,
                    priority = if (sourceOrder <= 15) LeadPriority.HIGH else LeadPriority.NORMAL,
                    currentRemark = if (hasRemark) normalizedRemark else "",
                    notes = notesRaw.ifBlank { null },
                    linkSent = isLinkSent,
                    linkSentAt = if (isLinkSent && hasRemark) System.currentTimeMillis() else null,
                    callCount = if (hasRemark) 1 else 0,
                    lastCalledAt = if (hasRemark) System.currentTimeMillis() else null,
                    dateStr = if (rowDateStr.isNotBlank()) rowDateStr else todayFormatted,
                    isToday = isRowToday,
                    updatedAt = System.currentTimeMillis()
                )
            )
            sourceOrder++
        }

        // If tab had dates but none matched calendar today, mark the latest batch date as active current data
        if (tabHasDates && leads.none { it.isToday } && leads.isNotEmpty()) {
            val latestDate = leads.map { it.dateStr }.filter { it.isNotBlank() }.lastOrNull()
            if (latestDate != null) {
                for (i in leads.indices) {
                    if (leads[i].dateStr == latestDate) {
                        leads[i] = leads[i].copy(isToday = true)
                    }
                }
            }
        }

        return leads
    }
}
