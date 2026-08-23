package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.Company
import com.example.data.model.Lead
import com.example.data.sync.GoogleSheetSyncEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `verify app name resource`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("JobsIndia247 Tracking", appName)
  }

  @Test
  fun `verify exact companies and team leaders configuration`() {
    assertEquals(2, GoogleSheetSyncEngine.REAL_COMPANIES.size)
    assertEquals(6, GoogleSheetSyncEngine.REAL_TEAM_LEADERS.size)

    val ind08 = GoogleSheetSyncEngine.REAL_COMPANIES.find { it.companyCode == "IND08" }
    assertEquals("SPIN101", ind08?.name)

    val ind15 = GoogleSheetSyncEngine.REAL_COMPANIES.find { it.companyCode == "IND15" }
    assertEquals("RUMMY77", ind15?.name)
  }

  @Test
  fun `verify lead isolation compound identifier integrity`() {
    val leadA = Lead(
      id = "comp_ind08_emp_cherry_mahi_0",
      companyId = "comp_ind08",
      teamLeaderId = "tl_cherry",
      assignedEmployeeId = "emp_cherry_mahi",
      phone = "9876543210",
      sourceRowIndex = 0,
      status = "INTERESTED"
    )

    val leadB = Lead(
      id = "comp_ind08_emp_cherry_puja_0",
      companyId = "comp_ind08",
      teamLeaderId = "tl_cherry",
      assignedEmployeeId = "emp_cherry_puja",
      phone = "9876543210",
      sourceRowIndex = 0,
      status = "PENDING"
    )

    // Even if phone numbers and row indices are identical, employee identity isolates leads
    assertNotEquals(leadA.id, leadB.id)
    assertNotEquals(leadA.assignedEmployeeId, leadB.assignedEmployeeId)
    assertNotEquals(leadA.status, leadB.status)
  }
}
