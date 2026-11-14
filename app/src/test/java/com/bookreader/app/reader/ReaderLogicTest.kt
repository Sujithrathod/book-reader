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

    @Test fun autoScrollGetsFasterWithSpeed() {
        // Slowest is a relaxed reading pace; fastest is still followable.
        assertEquals(9f, autoScrollDpPerSecond(1), 0.001f)
        assertEquals(54f, autoScrollDpPerSecond(10), 0.001f)
        assertEquals(60, autoTurnSeconds(1))
        assertEquals(6, autoTurnSeconds(10))
        for (s in 1 until 10) {
            assert(autoScrollDpPerSecond(s + 1) > autoScrollDpPerSecond(s))
            assert(autoTurnSeconds(s + 1) < autoTurnSeconds(s))
        }
        // Out-of-range speeds are held to the ends.
        assertEquals(autoTurnSeconds(10), autoTurnSeconds(99))
        assertEquals(autoScrollDpPerSecond(1), autoScrollDpPerSecond(0), 0.001f)
    }

    @Test fun everyReadingThemeHasReadableText() {
        fun luminance(argb: Long): Double {
            fun ch(v: Long): Double { val c = v / 255.0; return if (c <= 0.03928) c / 12.92 else Math.pow((c + 0.055) / 1.055, 2.4) }
            return 0.2126 * ch((argb shr 16) and 0xFF) + 0.7152 * ch((argb shr 8) and 0xFF) + 0.0722 * ch(argb and 0xFF)
        }
        for (t in com.bookreader.app.data.ReadingTheme.entries) {
            val a = luminance(t.background); val b = luminance(t.text)
            val contrast = (maxOf(a, b) + 0.05) / (minOf(a, b) + 0.05)
            assert(contrast >= 7.0) { "${t.label}: contrast $contrast" }
            assertEquals(t.label, a < b, t.dark)
        }
    }
}
