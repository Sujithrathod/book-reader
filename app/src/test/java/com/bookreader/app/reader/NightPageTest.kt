package com.bookreader.app.reader

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NightPageTest {
    private val w = 600
    private val h = 800
    private val white = rgb(255, 255, 255)
    private val black = rgb(0, 0, 0)
    private val darkFigure = rgb(26, 26, 31)

    private fun rgb(r: Int, g: Int, b: Int) = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
    private fun page() = IntArray(w * h) { white }
    private fun IntArray.fill(l: Int, t: Int, r: Int, b: Int, c: Int) { for (y in t until b) for (x in l until r) this[y * w + x] = c }
    private fun IntArray.at(x: Int, y: Int) = this[y * w + x]
    private fun channels(c: Int) = Triple((c shr 16) and 0xFF, (c shr 8) and 0xFF, c and 0xFF)

    /** Day/night pairs measured from Moon+ Reader screenshots (compressed, so allow a few levels). */
    @Test fun lightPagesInvertLikeMoonReader() {
        val px = page()
        px.fill(100, 100, 400, 104, rgb(13, 12, 202))    // blue chart line
        px.fill(100, 200, 140, 260, rgb(85, 94, 135))    // blue-grey bird
        NightPage.apply(px, w, h)
        assertEquals(black, px.at(10, 10))               // white paper turns black
        fun near(c: Int, r: Int, g: Int, b: Int) {
            val (cr, cg, cb) = channels(c)
            assertTrue("got $cr,$cg,$cb", kotlin.math.abs(cr - r) <= 8 && kotlin.math.abs(cg - g) <= 8 && kotlin.math.abs(cb - b) <= 8)
        }
        near(px.at(200, 102), 239, 241, 46)              // blue line turns yellow
        near(px.at(120, 230), 170, 162, 123)             // bird turns khaki
    }

    @Test fun darkFiguresStayDarkAndTheirEdgesStayClean() {
        val px = page()
        px.fill(60, 200, 540, 400, darkFigure)           // figure on a dark background
        px.fill(100, 240, 260, 252, white)               // white label inside it
        px.fill(400, 300, 440, 340, rgb(242, 191, 51))   // yellow circle inside it
        px.fill(60, 450, 540, 452, black)                // text line under it
        NightPage.apply(px, w, h)

        assertEquals(darkFigure, px.at(61, 201))          // figure corner kept
        assertEquals(darkFigure, px.at(538, 398))
        assertEquals(white, px.at(150, 245))              // its label kept white
        assertEquals(rgb(242, 191, 51), px.at(420, 320))  // its colours kept
        assertEquals(black, px.at(59, 199))               // paper just outside turns black (no white halo)
        assertEquals(black, px.at(300, 405))
        assertEquals(white, px.at(300, 451))              // text under it turns white
    }

    @Test fun boldTextAndTableLinesAreNotMistakenForPictures() {
        val px = page()
        for (line in 0 until 6) {                         // big bold heading: letter blocks with gaps
            val y = 100 + line * 30
            var x = 40
            while (x < 560) { px.fill(x, y, x + 14, y + 20, black); x += 22 }
        }
        px.fill(40, 400, 560, 403, black)                 // table rule
        NightPage.apply(px, w, h)
        assertEquals(white, px.at(45, 105))               // bold letters inverted to white
        assertEquals(white, px.at(300, 401))              // rule inverted to white
        assertEquals(black, px.at(300, 600))
    }

    @Test fun aWholeDarkSlideStaysAsItIs() {
        val px = IntArray(w * h) { darkFigure }
        px.fill(100, 100, 300, 120, white)                // title on the slide
        NightPage.apply(px, w, h)
        assertEquals(darkFigure, px.at(5, 5))
        assertEquals(white, px.at(200, 110))
    }
}
