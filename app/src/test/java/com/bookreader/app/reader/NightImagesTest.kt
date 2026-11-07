package com.bookreader.app.reader

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NightImagesTest {
    private fun argb(a: Int, r: Int, g: Int, b: Int) = (a shl 24) or (r shl 16) or (g shl 8) or b

    @Test fun seeThroughPixelsLandOnWhite() {
        val px = intArrayOf(
            argb(0, 0, 0, 0),          // transparent background
            argb(255, 0, 0, 0),        // black line
            argb(128, 0, 0, 0),        // half-covered edge
            argb(255, 30, 90, 220),    // opaque blue
        )
        NightImages.onWhite(px)
        assertEquals(argb(255, 255, 255, 255), px[0])
        assertEquals(argb(255, 0, 0, 0), px[1])
        assertEquals(argb(255, 127, 127, 127), px[2])
        assertEquals(argb(255, 30, 90, 220), px[3])
    }

    @Test fun transparentChartTurnsDarkAtNight() {
        // Black lines on a see-through background must not be kept as a "dark picture".
        val w = 200
        val h = 150
        val px = IntArray(w * h) { argb(0, 0, 0, 0) }
        for (x in 20 until 180) px[100 * w + x] = argb(255, 0, 0, 0)
        NightImages.onWhite(px)
        NightPage.apply(px, w, h)
        assertEquals(argb(255, 0, 0, 0), px[10 * w + 10])        // background now black
        assertEquals(argb(255, 255, 255, 255), px[100 * w + 50]) // line now white
    }

    @Test fun pictureAddressesMatchBookHrefs() {
        val hrefs = listOf("OEBPS/c1.xhtml", "OEBPS/graph.png", "graph.png", "OEBPS/my%20photo.jpg")
        assertEquals("OEBPS/graph.png", NightImages.matchHref("https://readium/publication/OEBPS/graph.png", hrefs))
        assertEquals("OEBPS/my%20photo.jpg", NightImages.matchHref("https://readium/publication/OEBPS/my%20photo.jpg", hrefs))
        assertEquals("graph.png", NightImages.matchHref("https://readium/publication/graph.png", hrefs))
        assertEquals("OEBPS/graph.png", NightImages.matchHref("https://readium/publication/OEBPS/graph.png?x=1#top", hrefs))
        assertNull(NightImages.matchHref("https://readium/publication/OEBPS/other.png", hrefs))
    }
}
