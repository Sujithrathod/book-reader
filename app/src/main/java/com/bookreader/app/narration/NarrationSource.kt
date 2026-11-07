package com.bookreader.app.narration

import android.content.Context
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.publication.services.content.Content
import org.readium.r2.shared.publication.services.content.content
import java.io.Closeable
import java.io.File

/**
 * One block of text to read: a paragraph (TXT, PDF) or a text element (EPUB).
 * [position] says where it is, so the reader can highlight it and the app can
 * save the listening position.
 */
data class TextBlock(val text: String, val position: BlockPosition)

sealed interface BlockPosition {
    /** Saved form: "para:12@40" means paragraph 12, character 40. */
    fun encode(offset: Int): String

    data class Txt(val paragraph: Int) : BlockPosition {
        override fun encode(offset: Int) = "para:$paragraph@$offset"
    }

    data class Pdf(val page: Int, val paragraph: Int) : BlockPosition {
        override fun encode(offset: Int) = "pdfpage:$page.$paragraph@$offset"
    }

    /** [locator] is Readium's locator of the EPUB element, used for highlighting and saving. */
    data class Epub(val locator: Locator) : BlockPosition {
        override fun encode(offset: Int) = JSONObject().apply {
            put("locator", locator.toJSON())
            put("offset", offset)
        }.toString()
    }
}

/**
 * Supplies text blocks in reading order, one at a time, so a long book is never
 * loaded into speech memory all at once. [first] is the block to start in and the
 * character offset of the word to start at.
 */
interface NarrationSource : Closeable {
    /** Shown when [first] finds nothing to read. */
    val emptyMessage: String get() = "There's no readable text here."

    suspend fun first(): Pair<TextBlock, Int>?
    suspend fun next(): TextBlock?
    override fun close() {}
}

// ---------- TXT ----------

class TxtSource(
    private val paragraphs: List<String>,
    private val startParagraph: Int,
    private val startOffset: Int,
) : NarrationSource {
    private var index = startParagraph

    override suspend fun first(): Pair<TextBlock, Int>? {
        var i = startParagraph.coerceAtLeast(0)
        while (i < paragraphs.size && paragraphs[i].isBlank()) i++
        if (i >= paragraphs.size) return null
        index = i
        return TextBlock(paragraphs[i], BlockPosition.Txt(i)) to if (i == startParagraph) startOffset else 0
    }

    override suspend fun next(): TextBlock? {
        index++
        while (index < paragraphs.size && paragraphs[index].isBlank()) index++
        return paragraphs.getOrNull(index)?.let { TextBlock(it, BlockPosition.Txt(index)) }
    }
}

// ---------- PDF ----------

/**
 * Pulls text from a PDF page by page with PdfBox-Android. Pages are read in the
 * order their text is stored (better for two-column layouts than sorting by
 * position); repeated headers, footers and page numbers are dropped.
 * Scanned pages have no text and are skipped.
 */
class PdfSource(
    private val context: Context,
    private val file: File,
    private val startPage: Int,
    private val startParagraph: Int = 0,
    private val startOffset: Int = 0,
) : NarrationSource {
    private var doc: PDDocument? = null
    private val document: PDDocument get() = doc ?: error("PDF not opened")
    private var page = startPage
    private var queue = ArrayDeque<TextBlock>()
    private var previousLines: List<String>? = null

    override val emptyMessage: String =
        "No readable text was found from this page on. If this PDF is scanned (photos of pages), " +
            "reading it aloud needs text recognition, which isn't available yet."

    private fun linesOf(pageIndex: Int): List<String> = linesOf(document, pageIndex)

    /** Loads the next page that has readable text into the queue. Returns false at the end. */
    private suspend fun loadFrom(pageIndex: Int): Boolean = withContext(Dispatchers.IO) {
        var p = pageIndex
        while (p < document.numberOfPages) {
            val lines = linesOf(p)
            val paras = PdfText.cleanPage(lines, previousLines)
            previousLines = lines
            if (paras.isNotEmpty()) {
                page = p
                queue = ArrayDeque(paras.mapIndexed { i, t -> TextBlock(t, BlockPosition.Pdf(p, i)) })
                return@withContext true
            }
            p++
        }
        false
    }

    override suspend fun first(): Pair<TextBlock, Int>? {
        // Opening a PDF can take a moment, so it happens here, off the main thread.
        withContext(Dispatchers.IO) {
            PDFBoxResourceLoader.init(context.applicationContext)
            doc = PDDocument.load(file)
        }
        if (startPage > 0) previousLines = withContext(Dispatchers.IO) { linesOf(startPage - 1) }
        if (!loadFrom(startPage.coerceAtLeast(0))) return null
        val sameStartPage = page == startPage
        repeat(if (sameStartPage) startParagraph else 0) { if (queue.size > 1) queue.removeFirst() }
        val block = queue.removeFirst()
        return block to if (sameStartPage && (block.position as BlockPosition.Pdf).paragraph == startParagraph) startOffset else 0
    }

    override suspend fun next(): TextBlock? {
        if (queue.isEmpty() && !loadFrom(page + 1)) return null
        return queue.removeFirst()
    }

    override fun close() {
        // May race with a page being read on another thread; that read then fails and narration ends cleanly.
        runCatching { doc?.close() }
        doc = null
    }

    companion object {
        /** One page's text lines; blank lines mark paragraph breaks. Also used by the reflow view. */
        fun linesOf(document: PDDocument, pageIndex: Int): List<String> {
            val stripper = PDFTextStripper()
            stripper.setStartPage(pageIndex + 1)
            stripper.setEndPage(pageIndex + 1)
            stripper.setSortByPosition(false)
            stripper.setParagraphEnd("\n\n")
            return stripper.getText(document).lines()
        }

        /** True if none of the first pages has any text, which usually means a scanned PDF. */
        suspend fun looksScanned(context: Context, file: File, pagesToCheck: Int = 5): Boolean = withContext(Dispatchers.IO) {
            PDFBoxResourceLoader.init(context.applicationContext)
            PDDocument.load(file).use { doc ->
                val end = minOf(doc.numberOfPages, pagesToCheck)
                if (end == 0) return@use true
                val stripper = PDFTextStripper()
                stripper.setStartPage(1)
                stripper.setEndPage(end)
                val text = stripper.getText(doc)
                text.isBlank()
            }
        }
    }
}

// ---------- EPUB ----------

/**
 * Reads an EPUB through Readium's content iterator, which yields the book's text
 * element by element (paragraphs, headings, list items) with a locator for each.
 *
 * To start at a long-pressed word, the iterator starts at the visible page and
 * looks for the element that contains the selected word and the text just before it.
 */
class EpubSource(
    private val publication: Publication,
    private val start: Locator,
    private val selectedWord: String?,
    private val textBefore: String?,
    private val startOffsetInElement: Int? = null,
) : NarrationSource {
    private var iterator: Content.Iterator? = null

    private suspend fun nextText(): Content.TextElement? {
        val it = iterator ?: return null
        while (true) {
            val el = it.nextOrNull() ?: return null
            if (el is Content.TextElement && el.text.isNotBlank()) return el
        }
    }

    override suspend fun first(): Pair<TextBlock, Int>? {
        val content = publication.content(start) ?: return null
        iterator = content.iterator()
        val word = selectedWord?.trim()?.takeIf { it.isNotEmpty() }
        if (word == null) {
            val el = nextText() ?: return null
            return TextBlock(el.text, BlockPosition.Epub(el.locator)) to (startOffsetInElement ?: 0)
        }
        // Search forward (within a reasonable distance) for the element holding the selection.
        var checked = 0
        while (checked < 400) {
            val el = nextText() ?: break
            checked++
            // Only look inside the chapter that holds the selection.
            if (el.locator.href.toString() != start.href.toString()) break
            val at = SpeechText.findSelection(el.text, textBefore, word)
            if (at != null) return TextBlock(el.text, BlockPosition.Epub(el.locator)) to at
        }
        // Not found: start at the top of the visible page rather than guessing.
        iterator = content.iterator()
        val el = nextText() ?: return null
        return TextBlock(el.text, BlockPosition.Epub(el.locator)) to 0
    }

    override suspend fun next(): TextBlock? =
        nextText()?.let { TextBlock(it.text, BlockPosition.Epub(it.locator)) }
}
