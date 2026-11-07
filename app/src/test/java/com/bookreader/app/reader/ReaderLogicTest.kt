package com.bookreader.app.reader

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReaderLogicTest {
    @Test fun middleOpensMenuAndEdgesTurnPages() {
        assertEquals(TapZone.MENU, tapZone(500f, 1000f, 1000f, 2000f, edgesTurnPages = true))
        assertEquals(TapZone.PREVIOUS, tapZone(100f, 1000f, 1000f, 2000f, edgesTurnPages = true))
        assertEquals(TapZone.NEXT, tapZone(900f, 1000f, 1000f, 2000f, edgesTurnPages = true))
    }

    @Test fun topAndBottomOfTheMiddleOpenTheMenuInsteadOfScrolling() {
        assertEquals(TapZone.MENU, tapZone(500f, 100f, 1000f, 2000f, edgesTurnPages = true))
        assertEquals(TapZone.MENU, tapZone(500f, 1950f, 1000f, 2000f, edgesTurnPages = true))
        assertEquals(TapZone.NEXT, tapZone(900f, 100f, 1000f, 2000f, edgesTurnPages = true))
        assertEquals(TapZone.PREVIOUS, tapZone(100f, 1950f, 1000f, 2000f, edgesTurnPages = true))
    }

    @Test fun everyTapOpensMenuWhenEdgeTurningIsOff() {
        assertEquals(TapZone.MENU, tapZone(900f, 1900f, 1000f, 2000f, edgesTurnPages = false))
    }

    @Test fun longTitlesKeepTheirStartAndEnd() {
        assertEquals("Short", middleEllipsis("Short", 20))
        val cut = middleEllipsis("From Large Language Models to Foundation Models", 30)
        assertEquals(30, cut.length)
        assertEquals(true, cut.startsWith("From Large Lan") && cut.endsWith("ation Models") && "…" in cut)
    }

    @Test fun chapterIsTheLastEntryStartingOnOrBeforeThePage() {
        val outline = listOf(
            OutlineEntry("Preface", 0, 0),
            OutlineEntry("Chapter 1", 0, 3),
            OutlineEntry("1.1 Tokens", 1, 3),
            OutlineEntry("Chapter 2", 0, 10),
        )
        assertEquals("Preface", chapterAt(outline, 2))
        assertEquals("1.1 Tokens", chapterAt(outline, 3))
        assertEquals("1.1 Tokens", chapterAt(outline, 9))
        assertEquals("Chapter 2", chapterAt(outline, 50))
        assertNull(chapterAt(outline.drop(1), 1))
    }
}
