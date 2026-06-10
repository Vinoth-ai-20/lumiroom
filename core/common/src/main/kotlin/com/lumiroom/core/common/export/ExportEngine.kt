package com.lumiroom.core.common.export

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExportEngine @Inject constructor() {

    /**
     * Generates a basic PDF Report containing room analysis details.
     */
    fun exportToPdf(
        context: Context,
        roomName: String,
        healthScore: Int,
        budget: Double,
        suggestions: List<String>
    ): File {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 Size at 72 PPI
        val page = document.startPage(pageInfo)

        val canvas: Canvas = page.canvas
        val paint = Paint().apply {
            color = Color.BLACK
            textSize = 16f
        }

        var yPos = 50f
        canvas.drawText("Lumiroom Insight Report", 50f, yPos, paint)
        yPos += 40f
        
        paint.textSize = 12f
        canvas.drawText("Room Name: $roomName", 50f, yPos, paint)
        yPos += 20f
        canvas.drawText("Health Score: $healthScore / 100", 50f, yPos, paint)
        yPos += 20f
        canvas.drawText("Total Budget Used: ₹$budget", 50f, yPos, paint)
        yPos += 40f

        canvas.drawText("Layout Suggestions:", 50f, yPos, paint)
        yPos += 20f
        for (suggestion in suggestions) {
            canvas.drawText("• $suggestion", 70f, yPos, paint)
            yPos += 20f
        }

        document.finishPage(page)

        val file = File(context.cacheDir, "Lumiroom_Report_${System.currentTimeMillis()}.pdf")
        document.writeTo(FileOutputStream(file))
        document.close()

        return file
    }

    /**
     * Exports a simple JSON backup of the room metadata.
     * (In a real app, this would serialize the full entity graph)
     */
    fun exportToJsonBackup(context: Context, roomName: String, roomId: String): File {
        val backupData = mapOf(
            "roomId" to roomId,
            "roomName" to roomName,
            "exportDate" to System.currentTimeMillis().toString()
        )
        val jsonString = Json.encodeToString(backupData)
        
        val file = File(context.cacheDir, "Lumiroom_Backup_${System.currentTimeMillis()}.json")
        file.writeText(jsonString)
        return file
    }
}
