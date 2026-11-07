package com.bookreader.app

/**
 * The book formats the app accepts. Detection uses the MIME type the file
 * picker reports first, then falls back to the file extension, because many
 * file managers report EPUB and TXT files as "application/octet-stream".
 */
enum class BookFormat(val extensions: Set<String>, val mimeTypes: Set<String>) {
    EPUB(setOf("epub"), setOf("application/epub+zip")),
    PDF(setOf("pdf"), setOf("application/pdf")),
    TXT(setOf("txt", "text"), setOf("text/plain"));

    companion object {
        /** MIME types to pass to the system file picker. */
        val pickerMimeTypes: Array<String> =
            (entries.flatMap { it.mimeTypes } + "application/octet-stream").toTypedArray()

        fun detect(fileName: String?, mimeType: String?): BookFormat? {
            val mime = mimeType?.lowercase()?.substringBefore(';')?.trim()
            entries.firstOrNull { mime != null && mime in it.mimeTypes }?.let { return it }
            val ext = fileName?.substringAfterLast('.', missingDelimiterValue = "")?.lowercase()
            if (ext.isNullOrEmpty()) return null
            return entries.firstOrNull { ext in it.extensions }
        }
    }
}
