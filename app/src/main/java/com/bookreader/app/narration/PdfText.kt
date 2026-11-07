package com.bookreader.app.narration

/**
 * Cleans text extracted from one PDF page so it reads naturally aloud:
 * drops running headers and footers, bare page numbers, and joins words
 * hyphenated across line ends.
 */
object PdfText {

    private val pageNumber = Regex("""^\s*(page\s*)?\d{1,4}(\s*(of|/)\s*\d{1,4})?\s*$""", RegexOption.IGNORE_CASE)

    /** A line with its digits blanked, so "Chapter 2 · 14" and "Chapter 2 · 15" compare equal. */
    private fun shape(line: String) = line.trim().replace(Regex("\\d+"), "#").lowercase()

    /**
     * @param lines the page's lines, in reading order
     * @param previous the previous page's raw lines, used to spot repeated headers and footers
     * @return paragraphs ready to speak
     */
    fun cleanPage(lines: List<String>, previous: List<String>?): List<String> {
        var body = lines.map { it.trimEnd() }.dropWhile { it.isBlank() }.dropLastWhile { it.isBlank() }
        if (previous != null) {
            val prev = previous.filter { it.isNotBlank() }
            val prevTop = prev.firstOrNull()?.let(::shape)
            val prevBottom = prev.lastOrNull()?.let(::shape)
            if (body.isNotEmpty() && body.first().length <= 80 && shape(body.first()) == prevTop) body = body.drop(1)
            if (body.isNotEmpty() && body.last().length <= 80 && shape(body.last()) == prevBottom) body = body.dropLast(1)
        }
        body = body.filterNot { pageNumber.matches(it) }

        // Blank lines (PDFBox paragraph breaks) separate paragraphs; other line breaks are wraps.
        val paragraphs = mutableListOf<String>()
        val current = StringBuilder()
        fun flush() {
            val p = current.toString().replace(Regex("\\s+"), " ").trim()
            if (p.isNotEmpty()) paragraphs += p
            current.clear()
        }
        for (line in body) {
            val t = line.trim()
            if (t.isEmpty()) { flush(); continue }
            if (current.endsWith("-") && current.length > 1 && current[current.length - 2].isLetter() && t.first().isLowerCase()) {
                current.setLength(current.length - 1) // "exam-" + "ple" -> "example"
                current.append(t)
            } else {
                if (current.isNotEmpty()) current.append(' ')
                current.append(t)
            }
        }
        flush()
        return paragraphs
    }
}
