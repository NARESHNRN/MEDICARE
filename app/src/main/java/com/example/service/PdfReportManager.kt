package com.example.service

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.data.model.MedicineAnalysisResult
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object PdfReportManager {

    fun generateMedicineReportPdf(
        context: Context,
        result: MedicineAnalysisResult,
        userName: String
    ): File? {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 size in points
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        val titlePaint = Paint().apply {
            color = Color.rgb(0, 91, 96) // Deep Teal
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val sectionHeaderPaint = Paint().apply {
            color = Color.rgb(0, 54, 58)
            textSize = 14f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val bodyPaint = Paint().apply {
            color = Color.rgb(30, 41, 59)
            textSize = 10.5f
            typeface = Typeface.DEFAULT
        }

        val alertPaint = Paint().apply {
            color = Color.rgb(180, 40, 20)
            textSize = 10.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val disclaimerPaint = Paint().apply {
            color = Color.rgb(100, 116, 139)
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
        }

        var y = 50f
        val left = 45f
        val right = 550f

        // Header
        canvas.drawText("MediVoice - Medicine Accessibility Report", left, y, titlePaint)
        y += 20f
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMMM yyyy, HH:mm"))
        canvas.drawText("Generated for: $userName | Date: $timestamp", left, y, disclaimerPaint)
        y += 25f

        // Separator Line
        val linePaint = Paint().apply {
            color = Color.rgb(203, 213, 225)
            strokeWidth = 1.5f
        }
        canvas.drawLine(left, y, right, y, linePaint)
        y += 25f

        // 1. Medicine Core Identification
        canvas.drawText("1. IDENTIFIED MEDICINE DETAILS", left, y, sectionHeaderPaint)
        y += 18f
        canvas.drawText("Medicine Name: ${result.medicineName}", left, y, bodyPaint)
        y += 15f
        canvas.drawText("Active Ingredient: ${result.activeIngredient}", left, y, bodyPaint)
        y += 15f
        canvas.drawText("Strength / Dosage: ${result.strength}", left, y, bodyPaint)
        y += 15f
        canvas.drawText("Manufacturer: ${result.manufacturer}", left, y, bodyPaint)
        y += 15f
        canvas.drawText("Batch / Lot Number: ${result.batchNumber}", left, y, bodyPaint)
        y += 22f

        // 2. Expiry Status
        canvas.drawText("2. EXPIRY VERIFICATION STATUS", left, y, sectionHeaderPaint)
        y += 18f
        val expiryDateTxt = result.expiryDateString ?: "Not Detected on Scanned Strip"
        canvas.drawText("Expiry Date Printed: $expiryDateTxt", left, y, bodyPaint)
        y += 15f
        val expPaint = if (result.expiryStatus.name == "EXPIRED") alertPaint else bodyPaint
        canvas.drawText("Status: ${result.expiryMessage}", left, y, expPaint)
        y += 22f

        // 3. Clinical Indications & Guidance
        canvas.drawText("3. CLINICAL USES & GENERAL PURPOSE", left, y, sectionHeaderPaint)
        y += 18f
        for (use in result.generalUses.take(3)) {
            canvas.drawText("• $use", left + 10, y, bodyPaint)
            y += 14f
        }
        y += 10f

        // 4. Special Populations (Age & Pregnancy)
        canvas.drawText("4. SPECIAL POPULATIONS & PRECAUTIONS", left, y, sectionHeaderPaint)
        y += 18f
        canvas.drawText("Age Guidance: ${result.ageGuidelines.take(85)}", left, y, bodyPaint)
        y += 15f
        canvas.drawText("Pregnancy / Breastfeeding: ${result.pregnancyBreastfeeding.take(85)}", left, y, bodyPaint)
        y += 22f

        // 5. Warnings & Side Effects
        canvas.drawText("5. IMPORTANT WARNINGS & SIDE EFFECTS", left, y, sectionHeaderPaint)
        y += 18f
        for (warning in result.importantWarnings.take(2)) {
            canvas.drawText("• Warning: ${warning.take(80)}", left + 10, y, alertPaint)
            y += 14f
        }
        canvas.drawText("Common Side Effects: ${result.commonSideEffects.take(4).joinToString(", ")}", left, y, bodyPaint)
        y += 15f
        canvas.drawText("Storage: ${result.storageInstructions}", left, y, bodyPaint)
        y += 25f

        // 6. Verification Status & Source Separation
        canvas.drawText("6. VERIFICATION EVIDENCE & AUDIT TRAIL", left, y, sectionHeaderPaint)
        y += 18f
        canvas.drawText("Confidence Level: ${result.verificationEvidence.confidenceLevel} (${(result.verificationEvidence.overallScore * 100).toInt()}%)", left, y, bodyPaint)
        y += 15f
        canvas.drawText("Package-Derived Data: Verbatim text from medicine blister foil.", left, y, bodyPaint)
        y += 15f
        canvas.drawText("Verified Reference: ${result.verifiedReferenceSource}", left, y, bodyPaint)
        y += 30f

        // 7. Mandatory Legal Healthcare Disclaimer
        canvas.drawLine(left, y, right, y, linePaint)
        y += 18f
        canvas.drawText("HEALTHCARE SAFETY DISCLAIMER:", left, y, sectionHeaderPaint)
        y += 15f
        val disclaimer1 = "This application provides medicine information and accessibility assistance."
        val disclaimer2 = "It does not replace advice from a doctor or pharmacist. Never change or stop prescribed"
        val disclaimer3 = "medication without direct consultation with a licensed healthcare practitioner."
        canvas.drawText(disclaimer1, left, y, disclaimerPaint)
        y += 13f
        canvas.drawText(disclaimer2, left, y, disclaimerPaint)
        y += 13f
        canvas.drawText(disclaimer3, left, y, disclaimerPaint)

        pdfDocument.finishPage(page)

        return try {
            val reportsDir = File(context.cacheDir, "reports").apply { mkdirs() }
            val file = File(reportsDir, "MediVoice_Report_${System.currentTimeMillis()}.pdf")
            val outputStream = FileOutputStream(file)
            pdfDocument.writeTo(outputStream)
            pdfDocument.close()
            outputStream.close()
            file
        } catch (e: Exception) {
            e.printStackTrace()
            pdfDocument.close()
            null
        }
    }

    fun sharePdf(context: Context, pdfFile: File) {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                pdfFile
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, "Share Medicine Report"))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
