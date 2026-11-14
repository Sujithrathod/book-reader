package com.bookreader.app.narration

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PdfWordsTest {
    private fun word(text: String, left: Float, top: Float) = PdfWord(text, left, top, left + 0.08f, top + 0.02f)

    private val line = listOf(word("The", 0.10f, 0.20f), word("quick", 0.20f, 0.20f), word("fox", 0.30f, 0.20f), word("Next", 0.10f, 0.24f))

    @Test fun pressOnAWordFindsIt() {
        assertEquals(1, PdfWords.hit(line, 0.23f, 0.21f))
        assertEquals(3, PdfWords.hit(line, 0.12f, 0.25f))
    }

    @Test fun pressJustBesideAWordStillFindsIt() {
        assertEquals(2, PdfWords.hit(line, 0.39f, 0.21f))  // a little right of "fox"
        assertEquals(0, PdfWords.hit(line, 0.12f, 0.195f)) // a little above "The"
    }

    @Test fun pressOnEmptySpaceFindsNothing() {
        assertNull(PdfWords.hit(line, 0.80f, 0.80f))
        assertNull(PdfWords.hit(emptyList(), 0.5f, 0.5f))
    }

    @Test fun pressedWordIsFoundInTheReadText() {
        val page = "Chapter 2 The cat sat on the mat. The dog sat on the rug.".split(" ")
        val paragraphs = listOf("The cat sat on the mat.", "The dog sat on the rug.")
        // Second "sat" (page word 10) is in the second paragraph, not the first.
        assertEquals(1 to 8, PdfText.locateWord(paragraphs, page, 10))
        assertEquals(0 to 8, PdfText.locateWord(paragraphs, page, 4))
        assertEquals(1 to 0, PdfText.locateWord(paragraphs, page, 8))
    }

    @Test fun headerWordsAreNotInTheReadText() {
        val page = "Chapter 2 The cat sat on the mat.".split(" ")
        assertNull(PdfText.locateWord(listOf("The cat sat on the mat."), page, 0))
    }

    @Test fun hyphenatedAndLigatureWordsStillMatch() {
        // "exam-" + "ple" was joined when the page was cleaned; "ﬁne" uses a ligature on the page.
        val page = listOf("A", "ﬁne", "exam-", "ple", "of", "text")
        val paragraphs = listOf("A fine example of text")
        assertEquals(0 to 2, PdfText.locateWord(paragraphs, page, 1))
        assertEquals(0 to 7, PdfText.locateWord(paragraphs, page, 2))
        assertEquals(0 to 7, PdfText.locateWord(paragraphs, page, 3))
    }

    @Test fun pressOnABulletStartsAtTheNextWord() {
        val page = listOf("•", "First", "point")
        assertEquals(0 to 2, PdfText.locateWord(listOf("• First point"), page, 0))
    }

    @Test fun everyReadWordIsPlacedAndHeaderWordsAreNot() {
        val page = "Chapter 2 The cat sat on the mat. The dog sat on the rug.".split(" ")
        val paragraphs = listOf("The cat sat on the mat.", "The dog sat on the rug.")
        val at = PdfText.locateAll(paragraphs, page)
        assertNull(at[0])                 // "Chapter"
        assertNull(at[1])                 // "2"
        assertEquals(0 to 0, at[2])       // "The"
        assertEquals(0 to 19, at[7])      // "mat."
        assertEquals(1 to 0, at[8])       // second "The"
        assertEquals(1 to 19, at[13])     // "rug."
    }

    @Test fun wordsOnOneLineJoinIntoOneBar() {
        val bars = PdfWords.lines(line)
        assertEquals(2, bars.size)
        assertEquals("The quick fox", bars[0].text)
        assertEquals(0.10f, bars[0].left, 0.0001f)
        assertEquals(0.38f, bars[0].right, 0.0001f)
        assertEquals("Next", bars[1].text)
    }
}
