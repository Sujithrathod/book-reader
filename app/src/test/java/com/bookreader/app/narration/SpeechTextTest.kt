package com.bookreader.app.narration

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class SpeechTextTest {
    private val text = "Call me Ishmael. Some years ago, never mind how long precisely, I went to sea. It was cold."

    private fun spoken(segs: List<Segment>) = segs.map { text.substring(it.start, it.end) }

    @Test fun wordStartMovesBackInsideAWord() {
        val i = text.indexOf("Ishmael") + 3
        assertEquals(text.indexOf("Ishmael"), SpeechText.wordStart(text, i))
    }

    @Test fun wordStartMovesForwardFromSpaceOrPunctuation() {
        assertEquals(text.indexOf("Some"), SpeechText.wordStart(text, text.indexOf(". Some")))
        assertEquals(text.length, SpeechText.wordStart(text, text.length - 1)) // final "." has no word after it
    }

    @Test fun sentences() {
        val s = spoken(SpeechText.sentences(text, Locale.ENGLISH))
        assertEquals(listOf("Call me Ishmael.", "Some years ago, never mind how long precisely, I went to sea.", "It was cold."), s)
    }

    @Test fun firstSegmentStartsAtTheExactWordNotTheSentence() {
        val segs = SpeechText.segmentsFrom(text, text.indexOf("never") + 2, Locale.ENGLISH)
        assertEquals(listOf("never mind how long precisely, I went to sea.", "It was cold."), spoken(segs))
    }

    @Test fun startingOnTheFirstWordGivesWholeSentences() {
        assertEquals(3, SpeechText.segmentsFrom(text, 0, Locale.ENGLISH).size)
    }

    @Test fun repeatedWordUsesTheChosenOccurrence() {
        val t = "The cat sat. The cat ran."
        val second = t.lastIndexOf("cat")
        val segs = SpeechText.segmentsFrom(t, second, Locale.ENGLISH)
        assertEquals("cat ran.", t.substring(segs[0].start, segs[0].end))
        assertEquals(1, segs.size)
    }

    @Test fun longSentencesAreSplitWithoutLosingText() {
        val long = (1..120).joinToString(" ") { "word$it" } + "."
        val segs = SpeechText.sentences(long, Locale.ENGLISH, maxLength = 100)
        assertTrue(segs.size > 1)
        assertTrue(segs.all { it.end - it.start <= 100 })
        assertEquals(long.replace(" ", ""), segs.joinToString("") { long.substring(it.start, it.end) }.replace(" ", ""))
    }

    @Test fun findSelectionUsesContextToPickTheRightOccurrence() {
        val t = "The cat sat.\n  The   cat ran away."
        val idx = SpeechText.findSelection(t, before = "The cat sat. The ", highlight = "cat")
        assertEquals(t.lastIndexOf("cat"), idx)
    }

    @Test fun findSelectionFallsBackToTheWordAlone() {
        assertEquals(4, SpeechText.findSelection("The whale.", before = "something else entirely ", highlight = "whale"))
        assertNull(SpeechText.findSelection("The whale.", before = null, highlight = "shark"))
    }
}
