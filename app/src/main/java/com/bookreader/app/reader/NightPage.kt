package com.bookreader.app.reader

/**
 * Night mode for PDF pages, working on the page image's ARGB pixels.
 *
 * Like Moon+ Reader, the page is colour-inverted: white paper turns black, black text white,
 * blue chart lines yellow. A plain inversion would also turn pictures that are already dark
 * (figures drawn on a dark background, dark photos, dark slides) light, so those are found
 * first and left as they are. The night page is then dark everywhere.
 */
object NightPage {
    /** A rectangle in pixels; [right] and [bottom] are exclusive. */
    data class Box(val left: Int, val top: Int, val right: Int, val bottom: Int)

    /** Cells darker than this (average brightness 0..255) can be part of a dark picture. */
    private const val DARK_CELL = 90
    /** Picture edges are trimmed while a whole edge row or column is brighter than this. */
    private const val LIGHT_EDGE = 128

    fun invert(argb: Int): Int = (argb and 0xFF000000.toInt()) or (argb.inv() and 0x00FFFFFF)

    private fun brightness(c: Int): Int =
        (((c shr 16) and 0xFF) * 299 + ((c shr 8) and 0xFF) * 587 + (c and 0xFF) * 114) / 1000

    /** Turns a day page into its night version, in place. */
    fun apply(px: IntArray, w: Int, h: Int) {
        val keep = darkPictures(px, w, h)
        val kept = BooleanArray(w)
        for (y in 0 until h) {
            kept.fill(false)
            for (b in keep) if (y >= b.top && y < b.bottom) for (x in b.left until b.right) kept[x] = true
            val row = y * w
            for (x in 0 until w) if (!kept[x]) px[row + x] = invert(px[row + x])
        }
    }

    /**
     * Large dark areas of the page. The page is split into small cells; neighbouring dark
     * cells are grouped, and groups big enough to be a picture (not a line of bold text or a
     * table rule) are returned, with edges trimmed to the picture's own border.
     */
    fun darkPictures(px: IntArray, w: Int, h: Int): List<Box> {
        if (w <= 0 || h <= 0) return emptyList()
        val cell = maxOf(4, w / 120)
        val gw = (w + cell - 1) / cell
        val gh = (h + cell - 1) / cell
        val dark = BooleanArray(gw * gh)
        for (gy in 0 until gh) for (gx in 0 until gw) {
            var sum = 0L
            var n = 0
            var y = gy * cell
            while (y < minOf(h, (gy + 1) * cell)) {
                var x = gx * cell
                while (x < minOf(w, (gx + 1) * cell)) {
                    sum += brightness(px[y * w + x]); n++
                    x += 2
                }
                y += 2
            }
            dark[gy * gw + gx] = n > 0 && sum / n < DARK_CELL
        }

        val boxes = mutableListOf<Box>()
        val seen = BooleanArray(gw * gh)
        val stack = IntArray(gw * gh)
        for (start in dark.indices) {
            if (!dark[start] || seen[start]) continue
            var top = 0
            stack[top++] = start
            seen[start] = true
            var minX = gw; var maxX = -1; var minY = gh; var maxY = -1; var count = 0
            while (top > 0) {
                val i = stack[--top]
                val cx = i % gw
                val cy = i / gw
                count++
                if (cx < minX) minX = cx
                if (cx > maxX) maxX = cx
                if (cy < minY) minY = cy
                if (cy > maxY) maxY = cy
                fun visit(nx: Int, ny: Int) {
                    if (nx < 0 || ny < 0 || nx >= gw || ny >= gh) return
                    val j = ny * gw + nx
                    if (dark[j] && !seen[j]) { seen[j] = true; stack[top++] = j }
                }
                visit(cx - 1, cy); visit(cx + 1, cy); visit(cx, cy - 1); visit(cx, cy + 1)
            }
            val bigEnough = count >= 24 &&
                (maxX - minX + 1) * cell >= w * 0.08 &&
                (maxY - minY + 1) * cell >= h * 0.03
            if (!bigEnough) continue
            // Grow by one cell (edge cells that were only partly dark), then trim light edges.
            val box = trim(
                px, w,
                Box(
                    maxOf(0, (minX - 1) * cell), maxOf(0, (minY - 1) * cell),
                    minOf(w, (maxX + 2) * cell), minOf(h, (maxY + 2) * cell),
                ),
            )
            if (box.right > box.left && box.bottom > box.top) boxes += box
        }
        return boxes
    }

    private fun trim(px: IntArray, w: Int, b: Box): Box {
        var (l, t, r, btm) = b
        fun rowMean(y: Int): Int { var s = 0L; for (x in l until r) s += brightness(px[y * w + x]); return (s / maxOf(1, r - l)).toInt() }
        fun colMean(x: Int): Int { var s = 0L; for (y in t until btm) s += brightness(px[y * w + x]); return (s / maxOf(1, btm - t)).toInt() }
        while (t < btm && rowMean(t) > LIGHT_EDGE) t++
        while (btm > t && rowMean(btm - 1) > LIGHT_EDGE) btm--
        while (l < r && colMean(l) > LIGHT_EDGE) l++
        while (r > l && colMean(r - 1) > LIGHT_EDGE) r--
        return Box(l, t, r, btm)
    }
}
