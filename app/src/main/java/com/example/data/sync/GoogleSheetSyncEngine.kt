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

    // Seed of real employees extracted directly from the 6 Google Sheet tabs
    val REAL_EMPLOYEES_SEED = listOf(
        // Team Cherry (8)
        Employee("emp_cherry_mahi", "Mahi singh", "Mahi singh", "comp_ind08", "tl_cherry", "dept_telecalling", "Telecalling"),
        Employee("emp_cherry_puja", "PUJA", "PUJA", "comp_ind08", "tl_cherry", "dept_telecalling", "Telecalling"),
        Employee("emp_cherry_rameez", "rameez zaheer", "rameez zaheer", "comp_ind08", "tl_cherry", "dept_telecalling", "Telecalling"),
        Employee("emp_cherry_aja", "AJA", "AJA", "comp_ind08", "tl_cherry", "dept_telecalling", "Telecalling"),
        Employee("emp_cherry_mohseen", "mohd mohseen saha", "mohd mohseen saha", "comp_ind08", "tl_cherry", "dept_telecalling", "Telecalling"),
        Employee("emp_cherry_ajay", "Ajay", "Ajay", "comp_ind08", "tl_cherry", "dept_telecalling", "Telecalling"),
        Employee("emp_cherry_sufiyan", "Sufiyan", "Sufiyan", "comp_ind08", "tl_cherry", "dept_telecalling", "Telecalling"),
        Employee("emp_cherry_priyanka", "Priyanka", "Priyanka", "comp_ind08", "tl_cherry", "dept_telecalling", "Telecalling"),

        // Team Musa (6)
        Employee("emp_musa_purnima", "Purnima", "Purnima", "comp_ind08", "tl_musa", "dept_telecalling", "Telecalling"),
        Employee("emp_musa_anjarul", "Mohd Anjarul", "Mohd Anjarul", "comp_ind08", "tl_musa", "dept_telecalling", "Telecalling"),
        Employee("emp_musa_salam", "Abdul salam", "Abdul salam", "comp_ind08", "tl_musa", "dept_telecalling", "Telecalling"),
        Employee("emp_musa_sreelekha", "Sreelekha", "Sreelekha", "comp_ind08", "tl_musa", "dept_telecalling", "Telecalling"),
        Employee("emp_musa_rajani", "rajani", "rajani", "comp_ind08", "tl_musa", "dept_telecalling", "Telecalling"),
        Employee("emp_musa_khan", "Khan", "Khan", "comp_ind08", "tl_musa", "dept_telecalling", "Telecalling"),

        // Team Zenni (6)
        Employee("emp_zenni_sangeeta", "Sangeeta Madhi", "Sangeeta Madhi", "comp_ind08", "tl_zenni", "dept_telecalling", "Telecalling"),
        Employee("emp_zenni_kashif", "Kashif", "Kashif", "comp_ind08", "tl_zenni", "dept_telecalling", "Telecalling"),
        Employee("emp_zenni_toukeer", "Toukeer", "Toukeer", "comp_ind08", "tl_zenni", "dept_telecalling", "Telecalling"),
        Employee("emp_zenni_saif", "M. Saif", "M. Saif", "comp_ind08", "tl_zenni", "dept_telecalling", "Telecalling"),
        Employee("emp_zenni_joyeb", "Mohd Joyeb", "Mohd Joyeb", "comp_ind08", "tl_zenni", "dept_telecalling", "Telecalling"),
        Employee("emp_zenni_anna", "anna", "anna", "comp_ind08", "tl_zenni", "dept_telecalling", "Telecalling"),

        // Team Jennifer (13)
        Employee("emp_jenn_shereen", "SHEREEN", "SHEREEN", "comp_ind15", "tl_jennifer", "dept_telecalling", "Telecalling"),
        Employee("emp_jenn_soni", "SONI", "SONI", "comp_ind15", "tl_jennifer", "dept_telecalling", "Telecalling"),
        Employee("emp_jenn_ritu", "RITU", "RITU", "comp_ind15", "tl_jennifer", "dept_telecalling", "Telecalling"),
        Employee("emp_jenn_payal", "PAYAL", "PAYAL", "comp_ind15", "tl_jennifer", "dept_telecalling", "Telecalling"),
        Employee("emp_jenn_siddarth", "SIDDARTH", "SIDDARTH", "comp_ind15", "tl_jennifer", "dept_telecalling", "Telecalling"),
        Employee("emp_jenn_payal2", "PAYAL 2", "PAYAL 2", "comp_ind15", "tl_jennifer", "dept_telecalling", "Telecalling"),
        Employee("emp_jenn_soni2", "soni 2", "soni 2", "comp_ind15", "tl_jennifer", "dept_telecalling", "Telecalling"),
        Employee("emp_jenn_ritu2", "ritu 2", "ritu 2", "comp_ind15", "tl_jennifer", "dept_telecalling", "Telecalling"),
        Employee("emp_jenn_dolly", "Dolly", "Dolly", "comp_ind15", "tl_jennifer", "dept_telecalling", "Telecalling"),
        Employee("emp_jenn_sidhant", "sidhant kar", "sidhant kar", "comp_ind15", "tl_jennifer", "dept_telecalling", "Telecalling"),
        Employee("emp_jenn_sneha", "Sneha", "Sneha", "comp_ind15", "tl_jennifer", "dept_telecalling", "Telecalling"),
        Employee("emp_jenn_anshika", "ANSHIKA", "ANSHIKA", "comp_ind15", "tl_jennifer", "dept_telecalling", "Telecalling"),
        Employee("emp_jenn_tanvi", "TANVI", "TANVI", "comp_ind15", "tl_jennifer", "dept_telecalling", "Telecalling"),

        // Team Venom (5)
        Employee("emp_venom_priyanka", "priyanka", "priyanka", "comp_ind15", "tl_venom", "dept_telecalling", "Telecalling"),
        Employee("emp_venom_sufiyan", "sufiyan", "sufiyan", "comp_ind15", "tl_venom", "dept_telecalling", "Telecalling"),
        Employee("emp_venom_payel", "Payel", "Payel", "comp_ind15", "tl_venom", "dept_telecalling", "Telecalling"),
        Employee("emp_venom_ranjit", "Ranjit", "Ranjit", "comp_ind15", "tl_venom", "dept_telecalling", "Telecalling"),
        Employee("emp_venom_vishakha", "Vishakha bagari", "Vishakha bagari", "comp_ind15", "tl_venom", "dept_telecalling", "Telecalling"),

        // Team Manish (6)
        Employee("emp_manish_ankit", "Ankit", "Ankit", "comp_ind15", "tl_manish", "dept_telecalling", "Telecalling"),
        Employee("emp_manish_ram", "RAM MISHRA", "RAM MISHRA", "comp_ind15", "tl_manish", "dept_telecalling", "Telecalling"),
        Employee("emp_manish_amit", "Amit", "Amit", "comp_ind15", "tl_manish", "dept_telecalling", "Telecalling"),
        Employee("emp_manish_sheroj", "sheroj", "sheroj", "comp_ind15", "tl_manish", "dept_telecalling", "Telecalling"),
        Employee("emp_manish_sanjay", "Sanjay", "Sanjay", "comp_ind15", "tl_manish", "dept_telecalling", "Telecalling"),
        Employee("emp_manish_rnjit", "Rnjit", "Rnjit", "comp_ind15", "tl_manish", "dept_telecalling", "Telecalling")
    )

    suspend fun fetchSheetTabs(sheetId: String): List<String> = withContext(Dispatchers.IO) {
        try {
            val url = "https://docs.google.com/spreadsheets/d/$sheetId/edit"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Android; Mobile; rv:109.0)")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return@withContext emptyList()

            val html = response.body?.string() ?: return@withContext emptyList()
            val pattern = Pattern.compile("docs-sheet-tab-caption\">([^<]+)</div>")
            val matcher = pattern.matcher(html)
            val tabs = mutableListOf<String>()

            while (matcher.find()) {
                val tab = matcher.group(1)?.trim() ?: ""
                if (tab.isNotBlank() &&
                    !tab.startsWith("sample", ignoreCase = true) &&
                    !tab.startsWith("report", ignoreCase = true)
                ) {
                    tabs.add(tab)
                }
            }
            tabs.distinct()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun fetchTabRows(sheetId: String, tabName: String): List<List<String>> = withContext(Dispatchers.IO) {
        try {
            val encodedTab = URLEncoder.encode(tabName, "UTF-8")
            val url = "https://docs.google.com/spreadsheets/d/$sheetId/gviz/tq?tqx=out:csv&sheet=$encodedTab"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0 (Android; Mobile; rv:109.0)")
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

        // Pre-scan to check if the tab contains any explicit date column
        var tabHasDates = false
        for (row in rawRows) {
            for (cell in row) {
                if (parseDateInfo(cell) != null) {
                    tabHasDates = true
                    break
                }
            }
            if (tabHasDates) break
        }

        for (row in rawRows) {
            if (row.isEmpty()) continue

            // Check if row is a header row
            val rowJoined = row.joinToString(" ").lowercase()
            if ((rowJoined.contains("date") && rowJoined.contains("phone")) ||
                rowJoined.contains("s.no") || rowJoined.contains("sr no") || rowJoined.contains("serial") ||
                (row.getOrNull(0)?.lowercase()?.contains("number") == true) ||
                (row.getOrNull(1)?.lowercase()?.contains("number") == true) ||
                (row.getOrNull(1)?.lowercase()?.contains("remark") == true)
            ) {
                continue
            }

            // Extract Date, Phone Number, and Remarks across cells
            var phone = ""
            var remarkRaw = ""
            var notesRaw = ""
            var phoneCellIndex = -1
            var rowDateStr = ""
            var isRowToday = !tabHasDates // If no dates in sheet at all, all rows belong to today's active batch

            // 1. Scan for Date cells first
            for (i in row.indices) {
                val dateInfo = parseDateInfo(row[i])
                if (dateInfo != null) {
                    rowDateStr = dateInfo.first
                    isRowToday = dateInfo.second
                    break
                }
            }

            // 2. Scan for Phone Number (skipping date cells)
            for (i in row.indices) {
                val cell = row[i].trim()
                if (parseDateInfo(cell) != null) continue // Skip date cells

                val digits = cell.filter { it.isDigit() }
                // Valid phone: 10 digits, or 12 starting with 91, or 11 starting with 0, or standard 8..15 digits
                if (digits.length in 8..15 && phone.isBlank()) {
                    // Avoid picking pure row index numbers (e.g. 1, 2, 3)
                    if (digits.length >= 10 || (digits.length >= 8 && !cell.contains("/") && !cell.contains("-"))) {
                        phone = digits
                        phoneCellIndex = i
                    }
                }
            }

            // If phone found, look for remark in subsequent cells
            if (phoneCellIndex != -1) {
                for (i in (phoneCellIndex + 1) until row.size) {
                    val cell = row[i].trim()
                    if (parseDateInfo(cell) != null) continue // Skip date cells
                    if (cell.isNotBlank() && remarkRaw.isBlank()) {
                        remarkRaw = cell
                    } else if (cell.isNotBlank() && notesRaw.isBlank()) {
                        notesRaw = cell
                    }
                }
            } else {
                // Fallback: check columns without date
                val col0 = row.getOrNull(0)?.trim() ?: ""
                val col1 = row.getOrNull(1)?.trim() ?: ""
                val col0Digits = col0.filter { it.isDigit() }
                val col1Digits = col1.filter { it.isDigit() }

                if (col1Digits.length >= 8 && parseDateInfo(col1) == null) {
                    phone = col1Digits
                    remarkRaw = row.getOrNull(2) ?: ""
                    notesRaw = row.getOrNull(3) ?: ""
                } else if (col0Digits.length >= 8 && parseDateInfo(col0) == null) {
                    phone = col0Digits
                    remarkRaw = row.getOrNull(1) ?: ""
                    notesRaw = row.getOrNull(2) ?: ""
                }
            }

            if (phone.isBlank()) continue

            // Normalize actual Google Sheet remark
            val normalizedRemark = RemarkConstants.normalize(remarkRaw)
            val hasRemark = normalizedRemark.isNotBlank() && !normalizedRemark.equals(RemarkConstants.PENDING, ignoreCase = true)

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

        return leads
    }
}
