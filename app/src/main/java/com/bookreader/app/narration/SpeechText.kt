package com.bookreader.app.narration

import java.text.BreakIterator
import java.util.Locale

/** A piece of text to speak, as a character range [start, end) inside a block of text. */
data class Segment(val start: Int, val end: Int)

/** Pure text logic for narration: words, sentences, and where speech starts. */
object SpeechText {

    private fun isWordChar(c: Char) = c.isLetterOrDigit() || c == '\'' || c == '’' || c == '-'

    /**
     * Start of the word at [offset]. If the offset is on a space or punctuation,
     * moves forward to the next word. Returns text.length if there is no word after it.
     */
    fun wordStart(text: String, offset: Int): Int {
        var i = offset.coerceIn(0, text.length)
        if (i < text.length && isWordChar(text[i])) {
            while (i > 0 && isWordChar(text[i - 1])) i--
            return i
        }
        while (i < text.length && !isWordChar(text[i])) i++
        return i
    }

    /** End (exclusive) of the word starting at or after [offset]. */
    fun wordEnd(text: String, offset: Int): Int {
        var i = wordStart(text, offset)
        while (i < text.length && isWordChar(text[i])) i++
        return i
    }

    /**
     * Sentence ranges covering the text, trimmed of surrounding spaces. Very long
     * sentences are split at a space so highlighting and pausing stay precise.
     */
    fun sentences(text: String, locale: Locale = Locale.getDefault(), maxLength: Int = 300): List<Segment> {
        val it = BreakIterator.getSentenceInstance(locale)
        it.setText(text)
        val out = mutableListOf<Segment>()
        var start = it.first()
        var end = it.next()
        while (end != BreakIterator.DONE) {
            splitLong(text, trim(text, start, end), maxLength, out)
            start = end
            end = it.next()
        }
        return out
    }

    private fun trim(text: String, s: Int, e: Int): Segment {
        var a = s
        var b = e
        while (a < b && text[a].isWhitespace()) a++
        while (b > a && text[b - 1].isWhitespace()) b--
        return Segment(a, b)
    }

    private fun splitLong(text: String, seg: Segment, max: Int, out: MutableList<Segment>) {
        if (seg.start >= seg.end) return
        var s = seg.start
        while (seg.end - s > max) {
            var cut = text.lastIndexOf(' ', s + max)
            if (cut <= s) cut = s + max
            out += trim(text, s, cut)
            s = cut
        }
        val last = trim(text, s, seg.end)
        if (last.start < last.end) out += last
    }

    /**
     * The segments to speak when starting at [offset]: the first one begins at the
     * word under the offset (not at the start of its sentence), then whole sentences follow.
     */
    fun segmentsFrom(text: String, offset: Int, locale: Locale = Locale.getDefault()): List<Segment> {
        val start = wordStart(text, offset)
        if (start >= text.length) return emptyList()
        val all = sentences(text, locale)
        val first = all.indexOfFirst { start < it.end }
        if (first < 0) return emptyList()
        val head = all[first].copy(start = maxOf(all[first].start, start))
        return listOf(head) + all.drop(first + 1)
    }

    /**
     * Finds where a selection sits inside a longer text. Readium reports a selection
     * as the selected word plus up to 50 characters before it; whitespace may differ
     * from the text the content iterator returns, so spaces are compared loosely.
     * Returns the offset of the selected word in [text], or null if it isn't there.
     */
    fun findSelection(text: String, before: String?, highlight: String): Int? {
        val hl = highlight.trim()
        if (hl.isEmpty()) return null
        val (norm, map) = normalize(text)
        val nHl = normalize(hl).first
        val nBefore = normalize(before.orEmpty()).first.takeLast(40)
        if (nBefore.isNotBlank()) {
            val idx = norm.indexOf(nBefore + nHl).takeIf { it >= 0 }
                ?: norm.indexOf(nBefore.trimEnd() + " " + nHl).takeIf { it >= 0 }
                ?: norm.indexOf(nBefore.trimEnd() + nHl).takeIf { it >= 0 }
            if (idx != null) {
                val hlIdx = norm.indexOf(nHl, idx + nBefore.trimEnd().length)
                if (hlIdx >= 0) return map[hlIdx]
            }
        }
        // Fall back to the first exact match of the word itself.
        val only = norm.indexOf(nHl)
        return if (only >= 0) map[only] else null
    }

    /** Collapses runs of whitespace to one space; returns the text and a map back to original indexes. */
    private fun normalize(s: String): Pair<String, IntArray> {
        val sb = StringBuilder()
        val map = ArrayList<Int>()
        var lastSpace = false
        s.forEachIndexed { i, c ->
            if (c.isWhitespace()) {
                if (!lastSpace) { sb.append(' '); map += i }
                lastSpace = true
            } else {
                sb.append(c); map += i; lastSpace = false
            }
        }
        return sb.toString() to map.toIntArray()
    }
}
