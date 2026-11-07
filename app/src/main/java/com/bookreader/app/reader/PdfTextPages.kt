package com.bookreader.app.reader

import android.content.Context
import com.bookreader.app.narration.PdfSource
import com.bookreader.app.narration.PdfText
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

/** Reads a PDF's own outline (bookmarks) for the contents list and the chapter in the status line. */
internal object PdfOutline {
    private const val MAX_ENTRIES = 2000
    private const val MAX_DEPTH = 8

    suspend fun load(context: Context, file: File): List<OutlineEntry> = withContext(Dispatchers.IO) {
        runCatching {
            PDFBoxResourceLoader.init(context.applicationContext)
            PDDocument.load(file).use { doc ->
                val out = mutableListOf<OutlineEntry>()
                fun walk(items: Iterable<PDOutlineItem>, depth: Int) {
                    for (item in items) {
                        if (out.size >= MAX_ENTRIES) return
                        val title = item.title?.trim().orEmpty()
                        val page = runCatching { item.findDestinationPage(doc)?.let { doc.pages.indexOf(it) } }.getOrNull()
                        if (title.isNotEmpty() && page != null && page >= 0) out += OutlineEntry(title, depth, page)
                        if (depth < MAX_DEPTH) walk(item.children(), depth + 1)
                    }
                }
                doc.documentCatalog.documentOutline?.let { walk(it.children(), 0) }
                out
            }
        }.getOrDefault(emptyList())
    }
}

/**
 * Text of PDF pages for the reflow view, cleaned the same way as for reading aloud
 * (so a long-pressed paragraph matches what narration reads). Pages are extracted
 * when first shown and kept.
 */
internal class PdfTextPages(private val context: Context, private val file: File) : AutoCloseable {
    private val lock = Mutex()
    private var doc: PDDocument? = null // guarded by lock
    private val lines = HashMap<Int, List<String>>()
    private val paragraphs = HashMap<Int, List<String>>()
    @Volatile private var closed = false

    suspend fun paragraphs(page: Int): List<String> = withContext(Dispatchers.IO) {
        lock.withLock {
            try {
                paragraphs[page]?.let { return@withLock it }
                if (closed) return@withLock emptyList()
                val d = doc ?: run {
                    PDFBoxResourceLoader.init(context.applicationContext)
                    PDDocument.load(file)
                }.also { doc = it }
                fun linesOf(p: Int) = lines.getOrPut(p) { PdfSource.linesOf(d, p) }
                val result = runCatching {
                    PdfText.cleanPage(linesOf(page), if (page > 0) linesOf(page - 1) else null)
                }.getOrDefault(emptyList())
                paragraphs[page] = result
                result
            } finally {
                if (closed) release()
            }
        }
    }

    /** Safe while a page is being read: that read releases the file when it finishes. */
    override fun close() {
        closed = true
        if (lock.tryLock()) {
            try { release() } finally { lock.unlock() }
        }
    }

    private fun release() {
        runCatching { doc?.close() }
        doc = null
    }
}
