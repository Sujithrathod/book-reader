package com.bookreader.app.text

import org.junit.Assert.assertEquals
import org.junit.Test

class PlainTextTest {
    @Test fun decodesUtf8AndSkipsBom() {
        val bytes = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()) + "café".toByteArray(Charsets.UTF_8)
        assertEquals("café", PlainText.decode(bytes))
    }

    @Test fun fallsBackToWindows1252() {
        val bytes = byteArrayOf('c'.code.toByte(), 'a'.code.toByte(), 'f'.code.toByte(), 0xE9.toByte())
        assertEquals("café", PlainText.decode(bytes))
    }

    @Test fun joinsHardWrappedLinesIntoParagraphs() {
        val text = "Call me Ishmael. Some years\nago, never mind how long.\r\n\r\nIt is a way I have.\n"
        assertEquals(
            listOf("Call me Ishmael. Some years ago, never mind how long.", "It is a way I have."),
            PlainText.paragraphs(text),
        )
    }

    @Test fun eachLineIsAParagraphWhenThereAreNoBlankLines() {
        assertEquals(listOf("One.", "Two.", "Three."), PlainText.paragraphs("One.\nTwo.\n  Three.  "))
    }

    @Test fun findsChapterHeadings() {
        val paras = listOf("PREFACE", "Some text.", "Chapter 1. Loomings", "Text.", "IV.", "Chapter of accidents is a long sentence that keeps going and going well past sixty characters.")
        assertEquals(
            listOf(TextChapter("PREFACE", 0), TextChapter("Chapter 1. Loomings", 2), TextChapter("IV.", 4)),
            PlainText.chapters(paras),
        )
    }

    @Test fun titleFromFileName() {
        assertEquals("moby dick 1851", PlainText.titleFromFileName("moby_dick-1851.txt"))
        assertEquals("Untitled", PlainText.titleFromFileName(".txt"))
    }
}
