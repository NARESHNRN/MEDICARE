package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.db.VerifiedMedicineEntity
import com.example.data.model.ExpiryStatus
import com.example.service.MedicineVerificationEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("MediVoice", appName)
  }

  @Test
  fun `verify medicine verification engine detects expiry and strength`() {
    val sampleDb = listOf(
      VerifiedMedicineEntity(
        id = "med_1",
        brandName = "Dolo 650",
        genericName = "Paracetamol",
        standardStrength = "650 mg",
        manufacturer = "Micro Labs Ltd",
        commonUses = "Pain and fever",
        warnings = "Max 4000mg daily",
        agePrecautions = "Adults & elderly",
        pregnancyStatus = "Safe at recommended doses",
        commonSideEffects = "Nausea",
        seriousSideEffects = "Liver damage",
        storage = "Store below 30C",
        scheduleCategory = "OTC"
      )
    )

    val ocrText = "Dolo 650 Paracetamol Tablets IP 650mg B.No ML-9482 EXP AUG 2027 Schedule H"
    val result = MedicineVerificationEngine.verifyMedicine(ocrText, sampleDb)

    assertEquals("Dolo 650", result.medicineName)
    assertEquals("650MG", result.strength)
    assertEquals(ExpiryStatus.NOT_EXPIRED, result.expiryStatus)
    assertNotNull(result.parsedExpiryDate)
  }

  @Test
  fun `verify expired medicine alerts properly`() {
    val sampleDb = listOf(
      VerifiedMedicineEntity(
        id = "med_2",
        brandName = "Crocin 500",
        genericName = "Paracetamol",
        standardStrength = "500 mg",
        manufacturer = "GSK Pharmaceuticals",
        commonUses = "Pain and fever",
        warnings = "Do not overdose",
        agePrecautions = "Adults and elderly",
        pregnancyStatus = "Compatible",
        commonSideEffects = "Mild upset",
        seriousSideEffects = "Liver issue",
        storage = "Store below 25C",
        scheduleCategory = "OTC"
      )
    )

    val expiredOcrText = "Crocin 500 Paracetamol 500mg EXP JAN 2025"
    val result = MedicineVerificationEngine.verifyMedicine(expiredOcrText, sampleDb)

    assertEquals(ExpiryStatus.EXPIRED, result.expiryStatus)
  }
}
