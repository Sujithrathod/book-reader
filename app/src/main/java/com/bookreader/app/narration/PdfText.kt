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

    /** Letters and digits only, so "ﬁrst," on the page matches "first" in the cleaned text. */
    private fun key(word: String) =
        java.text.Normalizer.normalize(word, java.text.Normalizer.Form.NFKC).lowercase().filter { it.isLetterOrDigit() }

    /**
     * Finds a word of the page in the cleaned paragraphs that are read aloud.
     * [pageWords] are the page's words in reading order and [index] the chosen one. Cleaning
     * drops headers and joins hyphenated words, so the word is matched together with its
     * neighbours rather than by counting.
     * @return (paragraph, character offset), or null if the word isn't part of the read text
     */
    fun locateWord(paragraphs: List<String>, pageWords: List<String>, index: Int): Pair<Int, Int>? =
        WordLocator(paragraphs, pageWords).locate(index, skipToNextWord = true)

    /**
     * Where every word of the page sits in the read text: (paragraph, character offset), or null
     * for words that aren't read (headers, page numbers). Used to highlight the words being read
     * on the page picture.
     */
    fun locateAll(paragraphs: List<String>, pageWords: List<String>): List<Pair<Int, Int>?> {
        val locator = WordLocator(paragraphs, pageWords)
        return pageWords.indices.map { locator.locate(it, skipToNextWord = false) }
    }

    private class WordLocator(paragraphs: List<String>, pageWords: List<String>) {
        private class Token(val paragraph: Int, val offset: Int, val key: String)

        private val raw = ArrayList<String>()
        /** For each page word, its index in [raw], or -1 for a bare dash or bullet. */
        private val rawIndex = IntArray(pageWords.size)
        private val tokens = ArrayList<Token>()

        init {
            pageWords.forEachIndexed { i, w ->
                val k = key(w)
                rawIndex[i] = if (k.isEmpty()) -1 else raw.size.also { raw += k }
            }
            paragraphs.forEachIndexed { p, text ->
                Regex("\\S+").findAll(text).forEach { m -> key(m.value).takeIf { it.isNotEmpty() }?.let { tokens += Token(p, m.range.first, it) } }
            }
        }

        fun locate(index: Int, skipToNextWord: Boolean): Pair<Int, Int>? {
            var at = rawIndex.getOrNull(index) ?: return null
            if (at < 0) {
                // A press on a bare dash or bullet starts at the next real word.
                if (!skipToNextWord) return null
                at = (index until rawIndex.size).firstOrNull { rawIndex[it] >= 0 }?.let { rawIndex[it] } ?: return null
            }
            val target = raw[at]
            var best: Token? = null
            var bestScore = -1.0
            tokens.forEachIndexed { j, t ->
                val exact = t.key == target
                // "exam-" + "ple" was joined into "example".
                val part = !exact && target.length >= 2 && (t.key.startsWith(target) || t.key.endsWith(target))
                if (!exact && !part) return@forEachIndexed
                var score = if (exact) 0.5 else 0.0
                for (d in -4..4) {
                    if (d == 0) continue
                    val a = raw.getOrNull(at + d) ?: continue
                    val b = tokens.getOrNull(j + d)?.key ?: continue
                    if (a == b) score += 1.0
                }
                // Among equal matches, prefer the one at a similar place on the page.
                score -= kotlin.math.abs(at.toDouble() / raw.size - j.toDouble() / tokens.size) * 0.4
                if (score > bestScore) { bestScore = score; best = t }
            }
            // A lone match with no matching neighbour is most likely a header word that also
            // appears in the text; only a long-press may settle for it.
            if (!skipToNextWord && bestScore < 1.0 && raw.size > 3) return null
            return best?.let { it.paragraph to it.offset }
        }
    }
}
