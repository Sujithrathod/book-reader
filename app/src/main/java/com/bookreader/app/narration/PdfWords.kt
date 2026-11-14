package com.bookreader.app.narration

import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import com.tom_roush.pdfbox.text.TextPosition
import kotlin.math.abs
import kotlin.math.max

/**
 * One word on a PDF page and where it sits, as fractions of the page (0..1 from the
 * top-left corner), so a long-press on the page picture can be matched to a word.
 */
data class PdfWord(val text: String, val left: Float, val top: Float, val right: Float, val bottom: Float)

object PdfWords {
    /** The page's words, in the same order as the text that is read aloud. */
    fun of(document: PDDocument, pageIndex: Int): List<PdfWord> {
        val stripper = WordStripper()
        stripper.setStartPage(pageIndex + 1)
        stripper.setEndPage(pageIndex + 1)
        stripper.setSortByPosition(false)
        stripper.getText(document)
        return stripper.words
    }

    /**
     * The word at ([x], [y]) (page fractions): the one under the finger, or else the nearest
     * one within a small distance, since a fingertip rarely lands exactly on a word.
     */
    fun hit(words: List<PdfWord>, x: Float, y: Float): Int? {
        var best: Int? = null
        var bestDistance = Float.MAX_VALUE
        words.forEachIndexed { i, w ->
            val dx = max(0f, max(w.left - x, x - w.right))
            val dy = max(0f, max(w.top - y, y - w.bottom))
            // Lines are close together, so being off vertically counts for more.
            val d = dx + dy * 2f
            if (d < bestDistance) { bestDistance = d; best = i }
        }
        return best?.takeIf { bestDistance <= REACH }
    }

    private const val REACH = 0.03f

    /**
     * Joins neighbouring words on the same line into one box per line, so a highlighted
     * sentence is drawn as a few smooth bars instead of one box per word.
     */
    fun lines(words: List<PdfWord>): List<PdfWord> {
        val out = ArrayList<PdfWord>()
        for (w in words) {
            val last = out.lastOrNull()
            val sameLine = last != null && abs(last.top - w.top) < (last.bottom - last.top) * 0.5f && w.left >= last.left
            if (last != null && sameLine) {
                out[out.size - 1] = PdfWord(last.text + " " + w.text, last.left, minOf(last.top, w.top), max(last.right, w.right), max(last.bottom, w.bottom))
            } else out += w
        }
        return out
    }

    private class WordStripper : PDFTextStripper() {
        val words = ArrayList<PdfWord>()

        override fun writeString(text: String, textPositions: List<TextPosition>) {
            val sb = StringBuilder()
            var left = 0f; var top = 0f; var right = 0f; var bottom = 0f
            fun flush() {
                if (sb.isNotEmpty()) words += PdfWord(sb.toString(), left, top, right, bottom)
                sb.clear()
            }
            for (p in textPositions) {
                val u = p.unicode ?: continue
                if (u.isBlank()) { flush(); continue }
                val pw = p.pageWidth.takeIf { it > 0f } ?: continue
                val ph = p.pageHeight.takeIf { it > 0f } ?: continue
                val height = max(abs(p.heightDir), p.fontSizeInPt * 0.7f)
                val l = p.xDirAdj / pw
                val r = (p.xDirAdj + p.widthDirAdj) / pw
                val t = (p.yDirAdj - height) / ph
                val b = (p.yDirAdj + height * 0.3f) / ph
                if (sb.isEmpty()) { left = l; top = t; right = r; bottom = b } else {
                    left = minOf(left, l); top = minOf(top, t); right = maxOf(right, r); bottom = maxOf(bottom, b)
                }
                sb.append(u)
            }
            flush()
            super.writeString(text, textPositions)
        }
    }
}
