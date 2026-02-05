package com.app.str.utils

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Environment
import com.app.str.data.model.SalarySlipResponse
import com.app.str.data.model.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class PdfGenerator {
    
    companion object {
        private const val PAGE_WIDTH = 595
        private const val PAGE_HEIGHT = 842
        private const val MARGIN = 50
        
        suspend fun generateSalarySlipPdf(
            context: Context,
            salarySlip: SalarySlipResponse,
            employeeName: String,
            designation: String,
            city: String,
            state: String,
            dateOfJoining: String,
            employeeId: String
        ): Result<File> = withContext(Dispatchers.IO) {
            try {
                val pdfDocument = PdfDocument()
                val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
                val page = pdfDocument.startPage(pageInfo)
                val canvas = page.canvas
                
                drawSalarySlipContent(canvas, salarySlip, employeeName, designation, city, state, dateOfJoining, employeeId)
                
                pdfDocument.finishPage(page)
                
                // Save to Downloads folder
                val downloadsFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val fileName = "SalarySlip_${salarySlip.month}_${salarySlip.year}_${System.currentTimeMillis()}.pdf"
                val file = File(downloadsFolder, fileName)
                
                FileOutputStream(file).use { fos ->
                    pdfDocument.writeTo(fos)
                }
                
                pdfDocument.close()
                Result.Success(file)
            } catch (e: Exception) {
                Result.Error("Failed to generate PDF: ${e.message}")
            }
        }
        
        private fun drawSalarySlipContent(
            canvas: Canvas,
            salarySlip: SalarySlipResponse,
            employeeName: String,
            designation: String,
            city: String,
            state: String,
            dateOfJoining: String,
            employeeId: String
        ) {
            val regularPaint = Paint().apply {
                isAntiAlias = true
                textSize = 10f
                color = Color.BLACK
            }
            
            val boldPaint = Paint().apply {
                isAntiAlias = true
                textSize = 10f
                color = Color.BLACK
                typeface = Typeface.DEFAULT_BOLD
            }
            
            val headerPaint = Paint().apply {
                isAntiAlias = true
                textSize = 12f
                color = Color.BLACK
                typeface = Typeface.DEFAULT_BOLD
            }

            val companyPaint = Paint().apply {
                isAntiAlias = true
                textSize = 18f
                color = Color.BLACK
                typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD_ITALIC)
            }

            val titlePaint = Paint().apply {
                isAntiAlias = true
                textSize = 14f
                color = Color.BLACK
                typeface = Typeface.DEFAULT_BOLD
            }

            val netPayPaint = Paint().apply {
                isAntiAlias = true
                textSize = 20f
                color = Color.BLACK
                typeface = Typeface.DEFAULT_BOLD
            }

            // Light gray color for table headers
            val tableBgPaint = Paint().apply {
                color = Color.LTGRAY
            }
            
            var yPosition = MARGIN + 25f
            
            // Draw outer border
            val borderPaint = Paint().apply {
                color = Color.BLACK
                style = Paint.Style.STROKE
                strokeWidth = 2f
            }
            canvas.drawRect(MARGIN.toFloat() - 5f, MARGIN.toFloat() - 5f, 
                PAGE_WIDTH - MARGIN.toFloat() + 5f, PAGE_HEIGHT - MARGIN.toFloat() + 5f, borderPaint)
            
            // Company Header - Centered vertically in header area
            val companyName = "Shree Taj Realtor"
            val companyWidth = companyPaint.measureText(companyName)
            canvas.drawText(companyName, (PAGE_WIDTH - companyWidth) / 2f, yPosition, companyPaint)
            yPosition += 25f
            
            // Company Tagline - Centered below company name
            val taglinePaint = Paint().apply {
                isAntiAlias = true
                textSize = 12f
                color = Color.DKGRAY
                typeface = Typeface.create(Typeface.SERIF, Typeface.ITALIC)
            }
            val tagline = "\"The Sign of Truth & Trust\""
            val taglineWidth = taglinePaint.measureText(tagline)
            canvas.drawText(tagline, (PAGE_WIDTH - taglineWidth) / 2f, yPosition, taglinePaint)
            yPosition += 25f
            
            // Draw header separator line
            canvas.drawLine(MARGIN.toFloat(), yPosition, PAGE_WIDTH - MARGIN.toFloat(), yPosition, borderPaint)
            yPosition += 20f
            
            // Payslip Title - Centered
            val monthNames = arrayOf(
                "January", "February", "March", "April", "May", "June",
                "July", "August", "September", "October", "November", "December"
            )
            val monthName = monthNames[salarySlip.month - 1]
            val titleText = "Payslip for the month of $monthName ${salarySlip.year}"
            val titleWidth = titlePaint.measureText(titleText)
            canvas.drawText(titleText, (PAGE_WIDTH - titleWidth) / 2f, yPosition, titlePaint)
            yPosition += 30f
            
            // Draw another separator line
            canvas.drawLine(MARGIN.toFloat(), yPosition, PAGE_WIDTH - MARGIN.toFloat(), yPosition, borderPaint)
            yPosition += 20f
            
            // EMPLOYEE PAY SUMMARY Header
            canvas.drawText("EMPLOYEE PAY SUMMARY", MARGIN.toFloat(), yPosition, headerPaint)
            yPosition += 25f
            
            // Employee Details Section (Left Side)
            val leftColumnX = MARGIN.toFloat()
            val rightColumnX = PAGE_WIDTH - MARGIN - 180f
            val detailsStartY = yPosition
            
            canvas.drawText("Employee Name", leftColumnX, yPosition, regularPaint)
            canvas.drawText(": $employeeName, $employeeId", leftColumnX + 80f, yPosition, regularPaint)
            yPosition += 18f
            
            canvas.drawText("Designation", leftColumnX, yPosition, regularPaint)
            canvas.drawText(": $designation", leftColumnX + 80f, yPosition, regularPaint)
            yPosition += 18f
            
            canvas.drawText("Date of Joining", leftColumnX, yPosition, regularPaint)
            canvas.drawText(": $dateOfJoining", leftColumnX + 80f, yPosition, regularPaint)
            yPosition += 18f
            
            canvas.drawText("Pay Period", leftColumnX, yPosition, regularPaint)
            canvas.drawText(": $monthName ${salarySlip.year}", leftColumnX + 80f, yPosition, regularPaint)
            yPosition += 18f
            
            canvas.drawText("Pay Date", leftColumnX, yPosition, regularPaint)
            canvas.drawText(": ${SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())}", leftColumnX + 80f, yPosition, regularPaint)
            
            // Employee Net Pay (Right Side)
            val netPayBoxY = detailsStartY - 10f
            canvas.drawText("Employee Net Pay", rightColumnX, netPayBoxY + 15f, headerPaint)
            canvas.drawText("₹${String.format("%.2f", salarySlip.netSalary)}", rightColumnX, netPayBoxY + 40f, netPayPaint)
            canvas.drawText("Paid Days : ${salarySlip.presentDays} | LOP Days : ${salarySlip.unpaidAbsences}", rightColumnX, netPayBoxY + 60f, regularPaint)
            canvas.drawText("Target: ₹${String.format("%.2f", salarySlip.targetArea)} | Sales: ₹${String.format("%.2f", salarySlip.salesSum)}", rightColumnX, netPayBoxY + 75f, regularPaint)
            
            yPosition += 40f
            
            // Draw separator line before table
            canvas.drawLine(MARGIN.toFloat(), yPosition, PAGE_WIDTH - MARGIN.toFloat(), yPosition, borderPaint)
            yPosition += 20f
            
            // Table Headers with background
            val tableHeaderY = yPosition

            // Draw header backgrounds with wider earnings YTD column
            canvas.drawRect(MARGIN.toFloat(), tableHeaderY - 15f, MARGIN + 120f, tableHeaderY + 5f, tableBgPaint)
            canvas.drawRect(MARGIN + 120f, tableHeaderY - 15f, MARGIN + 180f, tableHeaderY + 5f, tableBgPaint)
            canvas.drawRect(MARGIN + 180f, tableHeaderY - 15f, MARGIN + 250f, tableHeaderY + 5f, tableBgPaint)
            canvas.drawRect(MARGIN + 250f, tableHeaderY - 15f, MARGIN + 350f, tableHeaderY + 5f, tableBgPaint)
            canvas.drawRect(MARGIN + 350f, tableHeaderY - 15f, MARGIN + 430f, tableHeaderY + 5f, tableBgPaint)
            canvas.drawRect(MARGIN + 430f, tableHeaderY - 15f, PAGE_WIDTH - MARGIN.toFloat(), tableHeaderY + 5f, tableBgPaint)
            
            // Draw table borders with wider earnings YTD column
            canvas.drawLine(MARGIN.toFloat(), tableHeaderY - 15f, PAGE_WIDTH - MARGIN.toFloat(), tableHeaderY - 15f, borderPaint)
            canvas.drawLine(MARGIN.toFloat(), tableHeaderY + 5f, PAGE_WIDTH - MARGIN.toFloat(), tableHeaderY + 5f, borderPaint)
            canvas.drawLine(MARGIN.toFloat(), tableHeaderY - 15f, MARGIN.toFloat(), tableHeaderY + 5f, borderPaint)
            canvas.drawLine(MARGIN + 120f, tableHeaderY - 15f, MARGIN + 120f, tableHeaderY + 5f, borderPaint)
            canvas.drawLine(MARGIN + 180f, tableHeaderY - 15f, MARGIN + 180f, tableHeaderY + 5f, borderPaint)
            canvas.drawLine(MARGIN + 250f, tableHeaderY - 15f, MARGIN + 250f, tableHeaderY + 5f, borderPaint)
            canvas.drawLine(MARGIN + 350f, tableHeaderY - 15f, MARGIN + 350f, tableHeaderY + 5f, borderPaint)
            canvas.drawLine(MARGIN + 430f, tableHeaderY - 15f, MARGIN + 430f, tableHeaderY + 5f, borderPaint)
            canvas.drawLine(PAGE_WIDTH - MARGIN.toFloat(), tableHeaderY - 15f, PAGE_WIDTH - MARGIN.toFloat(), tableHeaderY + 5f, borderPaint)
            
            // Table Headers Text with wider earnings YTD column
            canvas.drawText("EARNINGS", MARGIN + 5f, tableHeaderY, boldPaint)
            canvas.drawText("AMOUNT", MARGIN + 125f, tableHeaderY, boldPaint)
            canvas.drawText("YTD", MARGIN + 200f, tableHeaderY, boldPaint)
            canvas.drawText("DEDUCTIONS", MARGIN + 255f, tableHeaderY, boldPaint)
            canvas.drawText("AMOUNT", MARGIN + 355f, tableHeaderY, boldPaint)
            canvas.drawText("YTD", MARGIN + 440f, tableHeaderY, boldPaint)
            
            yPosition = tableHeaderY + 25f
            
            // Table Data Rows
            val rowHeight = 18f
            
            // Use only actual data from API response
            var deductionRowY = yPosition
            
            // Basic Salary Row (from API monthlySalary: 35000.0)
            canvas.drawText("Basic", MARGIN + 5f, yPosition, regularPaint)
            canvas.drawText("₹${String.format("%.2f", salarySlip.monthlySalary)}", MARGIN + 125f, yPosition, regularPaint)
            canvas.drawText("₹${String.format("%.2f", salarySlip.monthlySalary)}", MARGIN + 200f, yPosition, regularPaint)
            
            // First deduction: Absence Deduction (from API: 14807.65)
            canvas.drawText("Absence ", MARGIN + 255f, deductionRowY, regularPaint)
            canvas.drawText("₹${String.format("%.2f", salarySlip.absenceDeduction)}", MARGIN + 355f, deductionRowY, regularPaint)
            canvas.drawText("₹${String.format("%.2f", salarySlip.absenceDeduction)}", MARGIN + 440f, deductionRowY, regularPaint)
            deductionRowY += rowHeight
            yPosition += rowHeight
            
            // Only show other earnings/deductions if they exist in API and have values > 0
            
            // Half Day Deduction - only if > 0
            if (salarySlip.halfDayDeduction > 0) {
                canvas.drawText("Half Day (${salarySlip.halfDayCount})", MARGIN + 255f, deductionRowY, regularPaint)
                canvas.drawText("₹${String.format("%.2f", salarySlip.halfDayDeduction)}", MARGIN + 355f, deductionRowY, regularPaint)
                canvas.drawText("₹${String.format("%.2f", salarySlip.halfDayDeduction)}", MARGIN + 440f, deductionRowY, regularPaint)
                deductionRowY += rowHeight
            }
            
            // Target Penalty (from API: 10000.0) - only if > 0
            if (salarySlip.targetPenalty > 0) {
                canvas.drawText("Target Penalty", MARGIN + 255f, deductionRowY, regularPaint)
                canvas.drawText("₹${String.format("%.2f", salarySlip.targetPenalty)}", MARGIN + 355f, deductionRowY, regularPaint)
                canvas.drawText("₹${String.format("%.2f", salarySlip.targetPenalty)}", MARGIN + 440f, deductionRowY, regularPaint)
                deductionRowY += rowHeight
            }
            
            // Advance to next position for any additional earnings rows if needed
            // Since API only has basic salary as main earning, we'll just show that
            
            // Draw separator line
            canvas.drawLine(MARGIN.toFloat(), yPosition + 5f, PAGE_WIDTH - MARGIN.toFloat(), yPosition + 5f, borderPaint)
            yPosition += 15f
            
            // Totals Row with background
            canvas.drawRect(MARGIN.toFloat(), yPosition - 10f, MARGIN + 220f, yPosition + 10f, tableBgPaint)
            canvas.drawRect(MARGIN + 220f, yPosition - 10f, PAGE_WIDTH - MARGIN.toFloat(), yPosition + 10f, tableBgPaint)
            
            canvas.drawText("Gross Earnings", MARGIN + 5f, yPosition, boldPaint)
            canvas.drawText("₹${String.format("%.2f", salarySlip.grossSalary)}", MARGIN + 125f, yPosition, boldPaint)
            canvas.drawText("Total Deductions", MARGIN + 255f, yPosition, boldPaint)
            canvas.drawText("₹${String.format("%.2f", salarySlip.totalDeduction)}", MARGIN + 355f, yPosition, boldPaint)
            
            yPosition += 40f
            
            // Draw separator line
            canvas.drawLine(MARGIN.toFloat(), yPosition, PAGE_WIDTH - MARGIN.toFloat(), yPosition, borderPaint)
            yPosition += 20f
            
            // NET PAY Section Header with background
            canvas.drawRect(MARGIN.toFloat(), yPosition - 10f, MARGIN + 220f, yPosition + 10f, tableBgPaint)
            canvas.drawRect(MARGIN + 420f, yPosition - 10f, PAGE_WIDTH - MARGIN.toFloat(), yPosition + 10f, tableBgPaint)
            
            canvas.drawText("NET PAY", MARGIN + 5f, yPosition, boldPaint)
            canvas.drawText("AMOUNT", MARGIN + 425f, yPosition, boldPaint)
            yPosition += 25f
            
            // Draw line
            canvas.drawLine(MARGIN.toFloat(), yPosition, PAGE_WIDTH - MARGIN.toFloat(), yPosition, borderPaint)
            yPosition += 15f
            
            // Net pay calculations
            canvas.drawText("Gross Earnings", MARGIN + 5f, yPosition, regularPaint)
            canvas.drawText("₹${String.format("%.2f", salarySlip.grossSalary)}", MARGIN + 425f, yPosition, regularPaint)
            yPosition += 18f
            
            canvas.drawText("Total Deductions", MARGIN + 5f, yPosition, regularPaint)
            canvas.drawText("(-) ₹${String.format("%.2f", salarySlip.totalDeduction)}", MARGIN + 425f, yPosition, regularPaint)
            yPosition += 25f
            
            // Draw thick line for total
            val thickLinePaint = Paint().apply {
                color = Color.BLACK
                strokeWidth = 2f
            }
            canvas.drawLine(MARGIN.toFloat(), yPosition, PAGE_WIDTH - MARGIN.toFloat(), yPosition, thickLinePaint)
            yPosition += 15f
            
            canvas.drawText("Total Net Payable", MARGIN + 5f, yPosition, boldPaint)
            canvas.drawText("₹${String.format("%.2f", salarySlip.netSalary)}", MARGIN + 425f, yPosition, boldPaint)
            yPosition += 30f
            
            // Amount in words - Centered with company font size
            val amountInWords = if (salarySlip.netSalary > 0) convertAmountToWords(salarySlip.netSalary) else "Indian Rupee Zero Only"
            val amountText = "Total Net Payable ₹${String.format("%.2f", salarySlip.netSalary)} ($amountInWords)"
            val amountTextWidth = companyPaint.measureText(amountText)
            canvas.drawText(amountText, (PAGE_WIDTH - amountTextWidth) / 2f, yPosition, companyPaint)
            yPosition += 20f
            
            // Formula - Centered
            val formulaText = "**Total Net Payable = Gross Earnings - Total Deductions"
            val formulaTextWidth = regularPaint.measureText(formulaText)
            canvas.drawText(formulaText, (PAGE_WIDTH - formulaTextWidth) / 2f, yPosition, regularPaint)
            yPosition += 40f
            
            // Footer - Centered
            val footerText = "This document has been automatically generated by Shree Taj Realtor CRM; therefore, a signature is not required."
            val footerTextWidth = regularPaint.measureText(footerText)
            canvas.drawText(footerText, (PAGE_WIDTH - footerTextWidth) / 2f, yPosition, regularPaint)
        }
        
        private fun convertAmountToWords(amount: Double): String {
            val intAmount = amount.toInt()
            return when {
                intAmount == 0 -> "Indian Rupee Only"
                intAmount >= 100000 -> {
                    val lakhs = intAmount / 100000
                    val thousands = (intAmount % 100000) / 1000
                    val hundreds = (intAmount % 1000) / 100
                    when {
                        thousands > 0 -> "Indian Rupee ${getNumberInWords(lakhs)} Lakh ${getNumberInWords(thousands)} Thousand Only"
                        hundreds > 0 -> "Indian Rupee ${getNumberInWords(lakhs)} Lakh ${getNumberInWords(hundreds)} Hundred Only"
                        else -> "Indian Rupee ${getNumberInWords(lakhs)} Lakh Only"
                    }
                }
                intAmount >= 1000 -> {
                    val thousands = intAmount / 1000
                    val hundreds = (intAmount % 1000) / 100
                    if (hundreds > 0) {
                        "Indian Rupee ${getNumberInWords(thousands)} Thousand ${getNumberInWords(hundreds)} Hundred Only"
                    } else {
                        "Indian Rupee ${getNumberInWords(thousands)} Thousand Only"
                    }
                }
                intAmount >= 100 -> "Indian Rupee ${getNumberInWords(intAmount / 100)} Hundred Only"
                else -> "Indian Rupee ${getNumberInWords(intAmount)} Only"
            }
        }
        
        private fun getNumberInWords(number: Int): String {
            val ones = arrayOf("", "One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine")
            val tens = arrayOf("", "", "Twenty", "Thirty", "Forty", "Fifty", "Sixty", "Seventy", "Eighty", "Ninety")
            val teens = arrayOf("Ten", "Eleven", "Twelve", "Thirteen", "Fourteen", "Fifteen", "Sixteen", "Seventeen", "Eighteen", "Nineteen")
            
            return when {
                number < 10 -> ones[number]
                number < 20 -> teens[number - 10]
                number < 100 -> {
                    val ten = number / 10
                    val one = number % 10
                    if (one == 0) tens[ten] else "${tens[ten]} ${ones[one]}"
                }
                else -> number.toString()
            }
        }
        
        private fun drawTableRow(canvas: Canvas, x: Float, y: Float, label: String, value: String, paint: Paint) {
            canvas.drawText(label, x, y, paint)
            canvas.drawText(value, PAGE_WIDTH - MARGIN - 100f, y, paint)
        }
    }
}