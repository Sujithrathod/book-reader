package com.bookreader.app.reader

/** What a tap on the page does: turn back, show or hide the menu, or turn forward. */
enum class TapZone { PREVIOUS, MENU, NEXT }

/**
 * Tap areas: the left side goes to the previous page, the right side to the next page,
 * and the middle column, top to bottom, opens the menu. With [edgesTurnPages] off, every
 * tap opens the menu.
 */
@Suppress("UNUSED_PARAMETER")
fun tapZone(x: Float, y: Float, width: Float, height: Float, edgesTurnPages: Boolean): TapZone {
    if (!edgesTurnPages || width <= 0f) return TapZone.MENU
    val fx = x / width
    return when {
        fx < 0.3f -> TapZone.PREVIOUS
        fx > 0.7f -> TapZone.NEXT
        else -> TapZone.MENU
    }
}

/** Shortens [text] to [maxChars] by cutting out its middle: "From Language…nguage Models". */
fun middleEllipsis(text: String, maxChars: Int): String {
    if (text.length <= maxChars) return text
    if (maxChars < 3) return text.take(maxChars.coerceAtLeast(0))
    val keep = maxChars - 1
    val head = (keep + 1) / 2
    return text.take(head).trimEnd() + "…" + text.takeLast(keep - head).trimStart()
}

/** A chapter in a PDF's own outline (bookmarks), pointing at a page index. */
data class OutlineEntry(val title: String, val depth: Int, val page: Int)

/** The chapter that [page] belongs to: the last outline entry starting on or before it. */
fun chapterAt(entries: List<OutlineEntry>, page: Int): String? {
    var best: OutlineEntry? = null
    for (e in entries) if (e.page <= page && (best == null || e.page >= best.page)) best = e
    return best?.title
}

/** Auto scroll in a continuous view: how fast the text glides, for speed 1 (slow) to 10 (fast). */
fun autoScrollDpPerSecond(speed: Int): Float = 4f + speed.coerceIn(1, 10) * 5f

/** Auto scroll in a paged view: seconds a page stays up before it turns, for speed 1 to 10. */
fun autoTurnSeconds(speed: Int): Int = 66 - speed.coerceIn(1, 10) * 6
