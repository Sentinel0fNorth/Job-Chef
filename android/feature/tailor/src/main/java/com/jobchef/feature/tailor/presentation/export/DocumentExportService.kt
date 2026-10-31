package com.jobchef.feature.tailor.presentation.export

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.google.gson.Gson
import com.jobchef.core.database.entity.BaseResumeEntity
import com.jobchef.core.database.entity.JobEntity
import com.jobchef.core.database.entity.TailoredApplicationEntity
import com.jobchef.core.network.model.ResumeContentDto
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

enum class ExportKind { RESUME, COVER_LETTER }

internal const val MAX_DRAFT_CHARACTERS = 50_000

data class DocumentPreviewLine(val text: String, val heading: Boolean = false)

class DocumentExportService(context: Context, private val gson: Gson) {
    private val appContext = context.applicationContext

    fun preview(
        kind: ExportKind,
        application: TailoredApplicationEntity,
        job: JobEntity
    ): List<DocumentPreviewLine> = documentBodyLines(kind, application, job)

    fun createPdf(
        kind: ExportKind,
        application: TailoredApplicationEntity,
        job: JobEntity,
        sourceResume: BaseResumeEntity,
        draftText: String? = null
    ): File {
        require(application.status == "READY") { "The generated application is not ready for export." }
        val directory = File(appContext.cacheDir, EXPORT_DIRECTORY).apply { mkdirs() }
        check(directory.isDirectory) { "Private export storage is unavailable." }
        val file = File(directory, "jobchef_${kind.name.lowercase()}_${UUID.randomUUID()}.pdf")
        val document = PdfDocument()
        try {
            val lines = documentLines(kind, application, job, sourceResume, draftText)
            drawDocument(document, lines)
            FileOutputStream(file).use(document::writeTo)
        } catch (error: Exception) {
            file.delete()
            throw error
        } finally {
            document.close()
        }
        return file
    }

    fun renderPreviewPages(
        kind: ExportKind,
        application: TailoredApplicationEntity,
        job: JobEntity,
        sourceResume: BaseResumeEntity,
        draftText: String? = null
    ): List<Bitmap> {
        val file = createPdf(kind, application, job, sourceResume, draftText)
        return try {
            ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { descriptor ->
                val renderer = PdfRenderer(descriptor)
                try {
                    require(renderer.pageCount in 1..MAX_PREVIEW_PAGES) {
                        "The generated document has too many pages to preview on this device."
                    }
                    val bitmaps = mutableListOf<Bitmap>()
                    try {
                        for (index in 0 until renderer.pageCount) {
                            val page = renderer.openPage(index)
                            try {
                                val bitmap = Bitmap.createBitmap(
                                    page.width, page.height, Bitmap.Config.ARGB_8888
                                )
                                bitmap.eraseColor(android.graphics.Color.WHITE)
                                page.render(bitmap, null, Matrix(), PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                                bitmaps += bitmap
                            } finally {
                                page.close()
                            }
                        }
                        bitmaps
                    } catch (error: Exception) {
                        bitmaps.forEach(Bitmap::recycle)
                        throw error
                    }
                } finally {
                    renderer.close()
                }
            }
        } finally {
            file.delete()
        }
    }

    fun copyTo(contentResolver: ContentResolver, destination: Uri, source: File) {
        val output = contentResolver.openOutputStream(destination, "w")
            ?: error("The selected document destination cannot be opened.")
        output.use { stream -> source.inputStream().use { input -> input.copyTo(stream) } }
    }

    private fun documentLines(
        kind: ExportKind,
        application: TailoredApplicationEntity,
        job: JobEntity,
        sourceResume: BaseResumeEntity,
        draftText: String? = null
    ): List<DocumentPreviewLine> {
        require(application.status == "READY") { "The generated application is not ready for export." }
        val identity = buildList {
            sourceResume.contactName?.takeIf(String::isNotBlank)?.let {
                add(DocumentPreviewLine(it, true))
            }
            listOfNotNull(
                sourceResume.contactEmail?.takeIf(String::isNotBlank),
                sourceResume.contactPhone?.takeIf(String::isNotBlank),
                sourceResume.contactLocation?.takeIf(String::isNotBlank)
            ).forEach { add(DocumentPreviewLine(it)) }
            if (application.demoMode) {
                add(DocumentPreviewLine("SYNTHETIC DEMO OUTPUT — NOT FOR APPLICATION USE", true))
            }
        }
        val body = draftText?.let(::draftLines) ?: documentBodyLines(kind, application, job)
        return identity + body
    }

    private fun documentBodyLines(
        kind: ExportKind,
        application: TailoredApplicationEntity,
        job: JobEntity
    ): List<DocumentPreviewLine> {
        require(application.status == "READY") { "The generated application is not ready for export." }
        val lines = mutableListOf<DocumentPreviewLine>()
        if (kind == ExportKind.RESUME) {
            val json = application.resumeContentJson
                ?: error("The backend did not return generated resume content.")
            val content = gson.fromJson(json, ResumeContentDto::class.java)
                ?: error("Generated resume content could not be read.")
            lines += DocumentPreviewLine(content.headline, true)
            lines += DocumentPreviewLine(job.title, true)
            lines += DocumentPreviewLine(job.company)
            lines += DocumentPreviewLine("SUMMARY", true)
            lines += DocumentPreviewLine(content.summary)
            if (content.skills.isNotEmpty()) {
                lines += DocumentPreviewLine("SKILLS", true)
                lines += DocumentPreviewLine(content.skills.joinToString(" · "))
            }
            content.experience.forEach { experience ->
                lines += DocumentPreviewLine("EXPERIENCE", true)
                lines += DocumentPreviewLine(listOf(experience.role, experience.employer, experience.dates)
                    .filter(String::isNotBlank).joinToString(" · "))
                experience.bullets.forEach { bullet -> lines += DocumentPreviewLine("• $bullet") }
            }
            if (content.education.isNotEmpty()) {
                lines += DocumentPreviewLine("EDUCATION", true)
                content.education.forEach { education ->
                    lines += DocumentPreviewLine(
                        listOf(education.qualification, education.institution, education.dates)
                            .filter(String::isNotBlank).joinToString(" · ")
                    )
                }
            }
        } else {
            lines += DocumentPreviewLine("Application for ${job.title}", true)
            lines += DocumentPreviewLine(job.company)
            lines += DocumentPreviewLine("")
            val letter = application.coverLetter?.takeIf(String::isNotBlank)
                ?: error("The backend did not return a generated cover letter.")
            letter.lines().forEach { paragraph -> lines += DocumentPreviewLine(paragraph) }
        }
        return lines
    }

    private fun draftLines(text: String): List<DocumentPreviewLine> {
        require(text.isNotBlank() && text.length <= MAX_DRAFT_CHARACTERS) {
            "The local draft is empty or exceeds the supported character limit."
        }
        return text.lines().mapIndexed { index, line ->
            DocumentPreviewLine(line, heading = index == 0)
        }
    }

    private fun drawDocument(document: PdfDocument, lines: List<DocumentPreviewLine>) {
        val regular = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.BLACK
            textSize = BODY_TEXT_SIZE
            typeface = Typeface.create(Typeface.SERIF, Typeface.NORMAL)
        }
        val bold = Paint(regular).apply {
            textSize = HEADING_TEXT_SIZE
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
        }
        val pageWidth = PAGE_WIDTH
        val pageHeight = PAGE_HEIGHT
        val margin = PAGE_MARGIN
        val contentWidth = pageWidth - 2 * margin
        val regularLineHeight = regular.fontSpacing
        var pageNumber = 0
        var page = newPage(document, ++pageNumber, pageWidth, pageHeight)
        var canvas = page.canvas
        var y = margin.toFloat()

        fun finishPage() {
            val footer = Paint(regular).apply { textSize = FOOTER_TEXT_SIZE; color = android.graphics.Color.DKGRAY }
            canvas.drawText("Job Chef · Page $pageNumber", margin.toFloat(), (pageHeight - margin / 2f), footer)
            document.finishPage(page)
        }

        for (line in lines) {
            val paint = if (line.heading) bold else regular
            val parts = wrap(line.text, paint, contentWidth.toFloat())
            if (parts.isEmpty()) {
                y += regularLineHeight * 0.6f
                continue
            }
            for (part in parts) {
                val lineHeight = paint.fontSpacing
                if (y + lineHeight > pageHeight - margin) {
                    finishPage()
                    page = newPage(document, ++pageNumber, pageWidth, pageHeight)
                    canvas = page.canvas
                    y = margin.toFloat()
                }
                canvas.drawText(part, margin.toFloat(), y, paint)
                y += lineHeight
            }
            y += if (line.heading) regularLineHeight * 0.25f else regularLineHeight * 0.12f
        }
        finishPage()
    }

    private fun newPage(document: PdfDocument, pageNumber: Int, width: Int, height: Int): PdfDocument.Page =
        document.startPage(PdfDocument.PageInfo.Builder(width, height, pageNumber).create())

    private fun wrap(text: String, paint: Paint, maxWidth: Float): List<String> {
        if (text.isBlank()) return emptyList()
        val result = mutableListOf<String>()
        var remaining = text.trim()
        while (remaining.isNotEmpty()) {
            val count = paint.breakText(remaining, true, maxWidth, null).coerceAtLeast(1)
            var end = count.coerceAtMost(remaining.length)
            if (end < remaining.length) {
                val lastSpace = remaining.lastIndexOf(' ', end - 1)
                if (lastSpace > 0) end = lastSpace
            }
            result += remaining.substring(0, end).trim()
            remaining = remaining.substring(end).trimStart()
        }
        return result
    }

    private companion object {
        const val EXPORT_DIRECTORY = "jobchef_exports"
        const val MAX_PREVIEW_PAGES = 40
        const val PAGE_WIDTH = 612
        const val PAGE_HEIGHT = 792
        const val PAGE_MARGIN = 48
        const val BODY_TEXT_SIZE = 11f
        const val HEADING_TEXT_SIZE = 12f
        const val FOOTER_TEXT_SIZE = 8f
    }
}
