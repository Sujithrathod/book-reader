package com.bookreader.app.text

import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.Charset
import java.nio.charset.CodingErrorAction

/** A chapter heading found in a plain-text book, pointing at a paragraph index. */
data class TextChapter(val title: String, val paragraphIndex: Int)

/** A plain-text book split into paragraphs, ready to show and (in step 3) to read aloud. */
data class PlainTextBook(val paragraphs: List<String>, val chapters: List<TextChapter>)

object PlainText {

    /**
     * Decodes bytes as UTF-8 (skipping a byte-order mark). If the bytes are not
     * valid UTF-8, falls back to Windows-1252, the most common legacy encoding
     * for English text files, so the book still opens instead of failing.
     */
    fun decode(bytes: ByteArray): String {
        val start = if (bytes.size >= 3 &&
            bytes[0] == 0xEF.toByte() && bytes[1] == 0xBB.toByte() && bytes[2] == 0xBF.toByte()
        ) 3 else 0
        val buffer = ByteBuffer.wrap(bytes, start, bytes.size - start)
        return try {
            Charsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(buffer)
                .toString()
        } catch (e: CharacterCodingException) {
            String(bytes, start, bytes.size - start, Charset.forName("windows-1252"))
        }
    }

    /**
     * Splits text into paragraphs. Blank lines separate paragraphs; inside a
     * paragraph, hard line breaks (common in Project Gutenberg files) are joined
     * with spaces. If the file has no blank lines at all, every line is a paragraph.
     */
    fun paragraphs(text: String): List<String> {
        val normalized = text.replace("\r\n", "\n").replace('\r', '\n')
        val hasBlankLines = Regex("\n[ \t]*\n").containsMatchIn(normalized)
        val blocks = if (hasBlankLines) normalized.split(Regex("\n[ \t]*\n+")) else normalized.split('\n')
        return blocks
            .map { block -> block.lines().joinToString(" ") { it.trim() }.replace(Regex(" {2,}"), " ").trim() }
            .filter { it.isNotEmpty() }
    }

    private val headingPattern = Regex(
        """^(chapter|part|book|section|prologue|epilogue|preface|introduction|contents)\b.*""" +
            """|^([IVXLC]+|\d{1,3})\.?$""",
        RegexOption.IGNORE_CASE,
    )

    /** Short lines that look like "Chapter 3", "PART TWO", "Prologue" or "IV." become chapters. */
    fun chapters(paragraphs: List<String>): List<TextChapter> =
        paragraphs.mapIndexedNotNull { index, p ->
            if (p.length <= 60 && headingPattern.matches(p)) TextChapter(p, index) else null
        }

    fun parse(bytes: ByteArray): PlainTextBook {
        val paragraphs = paragraphs(decode(bytes))
        return PlainTextBook(paragraphs, chapters(paragraphs))
    }

    /** Title guess from a file name: "moby_dick-1851.txt" becomes "moby dick 1851". */
    fun titleFromFileName(name: String): String =
        name.substringBeforeLast('.').replace(Regex("[_\\-]+"), " ").trim().ifEmpty { "Untitled" }
}
