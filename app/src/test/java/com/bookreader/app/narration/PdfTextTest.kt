package com.bookreader.app.narration

import org.junit.Assert.assertEquals
import org.junit.Test

class PdfTextTest {
    @Test fun dropsRepeatedHeaderFooterAndPageNumbers() {
        val prev = listOf("Team Handbook · Chapter 2", "Some text.", "Page 14 of 30")
        val page = listOf("Team Handbook · Chapter 2", "We meet every Monday at", "nine. Bring your exam-", "ples.", "", "New paragraph here.", "15", "Page 15 of 30")
        assertEquals(
            listOf("We meet every Monday at nine. Bring your examples.", "New paragraph here."),
            PdfText.cleanPage(page, prev),
        )
    }

    @Test fun keepsFirstPageHeadingWithoutPreviousPage() {
        assertEquals(listOf("Introduction Hello there."), PdfText.cleanPage(listOf("Introduction", "Hello there."), null))
    }

    @Test fun keepsRealHyphenatedNames() {
        assertEquals(listOf("Jean- Paul arrived."), PdfText.cleanPage(listOf("Jean-", "Paul arrived."), null))
    }
}
