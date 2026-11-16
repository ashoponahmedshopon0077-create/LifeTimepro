package com.example.export

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.data.model.AgeProfile
import com.example.data.model.Friendship
import com.example.data.model.GiftItem
import com.example.data.model.LifeMoment
import com.example.data.model.Relationship
import com.example.domain.DateCalculations
import java.io.File
import java.io.FileOutputStream

object ShareAndExportHelper {

    fun shareText(context: Context, subject: String, text: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, text)
        }
        context.startActivity(Intent.createChooser(intent, subject))
    }

    fun shareCounterCardImage(
        context: Context,
        title: String,
        headlineValue: String,
        subtitle: String,
        footer: String = "LifeTime – Age, Love & Memories"
    ) {
        val width = 1080
        val height = 1350
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val bgPaint = Paint().apply {
            shader = LinearGradient(
                0f, 0f, width.toFloat(), height.toFloat(),
                intArrayOf(
                    Color.parseColor("#880E38"),
                    Color.parseColor("#B31B4B"),
                    Color.parseColor("#C88A36")
                ),
                null,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        val cardPaint = Paint().apply {
            color = Color.argb(40, 255, 255, 255)
            isAntiAlias = true
        }
        canvas.drawRoundRect(RectF(72f, 120f, width - 72f, height - 120f), 64f, 64f, cardPaint)

        val titlePaint = Paint().apply {
            color = Color.parseColor("#FFE4BE")
            textSize = 52f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText(title, width / 2f, 340f, titlePaint)

        val bigPaint = Paint().apply {
            color = Color.WHITE
            textSize = 96f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        val lines = headlineValue.chunked(22)
        var y = 580f
        for (line in lines.take(3)) {
            canvas.drawText(line, width / 2f, y, bigPaint)
            y += 120f
        }

        val subPaint = Paint().apply {
            color = Color.parseColor("#FFF0F3")
            textSize = 42f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText(subtitle.take(44), width / 2f, 940f, subPaint)

        val footPaint = Paint().apply {
            color = Color.parseColor("#FFD9E2")
            textSize = 34f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        canvas.drawText(footer, width / 2f, height - 200f, footPaint)

        val sharedDir = File(context.cacheDir, "shared").apply { mkdirs() }
        val file = File(sharedDir, "lifetime_card_${System.currentTimeMillis()}.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        bitmap.recycle()

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, "$title — $headlineValue\n$subtitle")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share LifeTime Card"))
    }

    fun exportPdfReportAndShare(
        context: Context,
        userName: String,
        ages: List<AgeProfile>,
        relationships: List<Relationship>,
        friendships: List<Friendship>,
        moments: List<LifeMoment>,
        gifts: List<GiftItem>
    ) {
        val doc = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = doc.startPage(pageInfo)
        val canvas = page.canvas

        val headerPaint = Paint().apply {
            color = Color.parseColor("#B31B4B")
            textSize = 22f
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            isAntiAlias = true
        }
        val sectionPaint = Paint().apply {
            color = Color.parseColor("#880E38")
            textSize = 14f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            isAntiAlias = true
        }
        val bodyPaint = Paint().apply {
            color = Color.parseColor("#23191C")
            textSize = 11f
            typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
            isAntiAlias = true
        }

        var y = 48f
        canvas.drawText("LifeTime – Personal Timeline Report", 40f, y, headerPaint)
        y += 22f
        canvas.drawText("Prepared for $userName • Generated ${DateCalculations.formatDate(System.currentTimeMillis())}", 40f, y, bodyPaint)
        y += 28f

        fun drawSection(title: String, lines: List<String>) {
            if (y > 760f) return
            canvas.drawText(title, 40f, y, sectionPaint)
            y += 16f
            if (lines.isEmpty()) {
                canvas.drawText("  • No entries yet", 40f, y, bodyPaint)
                y += 16f
            } else {
                for (line in lines.take(6)) {
                    if (y > 790f) break
                    canvas.drawText("  • ${line.take(78)}", 40f, y, bodyPaint)
                    y += 15f
                }
            }
            y += 10f
        }

        drawSection("Age Profiles (${ages.size})", ages.map {
            val d = DateCalculations.calculateDuration(it.birthDateMillis)
            "${it.name} (${it.relation}): ${d.years}y ${d.months}m ${d.remainingDaysInMonth}d"
        })
        drawSection("Love & Relationships (${relationships.size})", relationships.map {
            val d = DateCalculations.calculateDuration(it.startDateMillis)
            "${it.partnerName}: ${d.years}y ${d.months}m (${d.totalDays} days)"
        })
        drawSection("Friendships (${friendships.size})", friendships.map {
            val d = DateCalculations.calculateDuration(it.startDateMillis)
            "${it.friendName}: ${d.years}y ${d.months}m"
        })
        drawSection("Important Moments & Memories (${moments.size})", moments.map {
            "${DateCalculations.formatDate(it.eventDateMillis)} — ${it.title} (${it.category})"
        })
        drawSection("Gifts (${gifts.size})", gifts.map {
            "${it.name} [${it.status}] — ${it.occasion} ($${it.budget})"
        })

        doc.finishPage(page)

        val reportsDir = File(context.filesDir, "reports").apply { mkdirs() }
        val pdfFile = File(reportsDir, "LifeTime_Report.pdf")
        FileOutputStream(pdfFile).use { out ->
            doc.writeTo(out)
        }
        doc.close()

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", pdfFile)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "LifeTime Personal Report")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Export LifeTime PDF Report"))
    }
}
