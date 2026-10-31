package com.jobchef.feature.resume.presentation.vault

import android.content.Context
import android.net.Uri
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import java.util.UUID

class ResumeImportProcessor(context: Context) {
    private val appContext = context.applicationContext

    fun import(uri: Uri): ResumeDraft {
        val descriptor = appContext.contentResolver.openFileDescriptor(uri, "r")
            ?: throw IllegalArgumentException("The selected PDF cannot be opened.")
        val fileSize = descriptor.use { it.statSize }
        if (fileSize <= 0L || fileSize > MAX_PDF_BYTES) {
            throw IllegalArgumentException("Choose a text-based PDF no larger than 10 MB.")
        }

        PDFBoxResourceLoader.init(appContext)
        val sourceText = appContext.contentResolver.openInputStream(uri)?.use { input ->
            PDDocument.load(input).use { document ->
                if (document.isEncrypted) {
                    throw IllegalArgumentException("Encrypted PDFs are not supported. Export an unlocked copy and try again.")
                }
                if (document.numberOfPages !in 1..MAX_PDF_PAGES) {
                    throw IllegalArgumentException("The PDF must contain between 1 and 20 pages.")
                }
                PDFTextStripper().getText(document)
            }
        }?.trim().orEmpty()
        if (sourceText.length < MIN_EXTRACTED_CHARS) {
            throw IllegalArgumentException("No usable text was found. Scanned PDFs and OCR are not supported.")
        }
        if (sourceText.length > MAX_RESUME_CHARS) {
            throw IllegalArgumentException("The extracted text exceeds the 50,000 character limit.")
        }

        val title = uri.lastPathSegment?.substringAfterLast('/')?.takeIf(String::isNotBlank)
            ?: "Imported resume"
        val contacts = ResumeTextSanitizer.extractContacts(sourceText)
        return ResumeDraft(
            id = UUID.randomUUID().toString(),
            title = title.removeSuffix(".pdf").take(120),
            sourceUri = uri.toString(),
            sourceText = sourceText,
            sanitizedText = contacts.sanitizedText,
            contactName = contacts.name,
            contactEmail = contacts.email,
            contactPhone = contacts.phone,
            contactLocation = contacts.location
        )
    }

    fun sanitizeReviewedText(text: String): String = ResumeTextSanitizer.extractContacts(text).sanitizedText

    companion object {
        private const val MAX_PDF_BYTES = 10L * 1024 * 1024
        private const val MAX_PDF_PAGES = 20
        private const val MAX_RESUME_CHARS = 50_000
        private const val MIN_EXTRACTED_CHARS = 80
    }
}

internal object ResumeTextSanitizer {
    fun extractContacts(text: String): ContactExtraction {
        var sanitized = text
        val email = EMAIL.find(text)?.value
        val phone = PHONE.find(text)?.value
        val locationMatch = ADDRESS.find(text)
        val location = locationMatch?.groupValues?.getOrNull(1)?.trim()
        val name = text.lineSequence().firstOrNull { it.isNotBlank() }?.trim()
            ?.takeIf { it.length <= 80 && NAME.matches(it) && it.split(WHITESPACE).size in 2..4 }

        if (!email.isNullOrBlank()) sanitized = sanitized.replace(email, CONTACT_REDACTION)
        if (!phone.isNullOrBlank()) sanitized = sanitized.replace(phone, CONTACT_REDACTION)
        if (!location.isNullOrBlank()) sanitized = sanitized.replace(location, CONTACT_REDACTION)
        if (!name.isNullOrBlank()) sanitized = sanitized.replaceFirst(name, CONTACT_REDACTION)
        sanitized = sanitized.replace(EMAIL, CONTACT_REDACTION).replace(PHONE, CONTACT_REDACTION)

        return ContactExtraction(sanitized, name, email, phone, location)
    }

    internal data class ContactExtraction(
        val sanitizedText: String,
        val name: String?,
        val email: String?,
        val phone: String?,
        val location: String?
    )

    private const val CONTACT_REDACTION = "[CONTACT REDACTED]"
    private val WHITESPACE = Regex("\\s+")
    private val EMAIL = Regex("(?i)[a-z0-9._%+-]+@[a-z0-9.-]+\\.[a-z]{2,}")
    private val PHONE = Regex(
        "(?<!\\d)(?:(?:\\+?91[\\s.-]?)?(?:[6-9]\\d{4}[\\s.-]?\\d{5}|[6-9]\\d{2}[\\s.-]?\\d{3}[\\s.-]?\\d{4})|(?:\\+?1[\\s.-]?)?(?:\\(?\\d{3}\\)?[\\s.-]?)?\\d{3}[\\s.-]?\\d{4})(?!\\d)"
    )
    private val ADDRESS = Regex("(?im)^\\s*(?:address|location)\\s*:\\s*(.+)$")
    private val NAME = Regex("^[\\p{L}][\\p{L} .'-]+$")
}

data class ResumeDraft(
    val id: String,
    val title: String,
    val sourceUri: String,
    val sourceText: String,
    val sanitizedText: String,
    val contactName: String?,
    val contactEmail: String?,
    val contactPhone: String?,
    val contactLocation: String?
)
