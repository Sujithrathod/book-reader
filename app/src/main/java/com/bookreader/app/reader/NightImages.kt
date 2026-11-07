package com.bookreader.app.reader

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import org.readium.r2.navigator.epub.EpubNavigatorFragment
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Publication
import java.io.ByteArrayOutputStream

/**
 * Night mode for EPUB pictures, so they look like PDF pages at night ([NightPage]): white
 * charts turn dark, pictures that are already dark stay as they are.
 *
 * The pictures live in Readium's web page, so each one is read from the book, turned into its
 * night version here and swapped into the page. Until the swap lands, Readium's own CSS
 * inversion covers the picture; switching back to day puts the original back.
 */
internal object NightImages {
    /** Longest side of a processed picture; bigger ones are scaled down to keep the page light. */
    private const val MAX_SIDE = 1600

    /** Night versions as data URLs, keyed by the picture's href. Sized in characters (~16 MB). */
    private val cache = object : LruCache<String, String>(8 * 1024 * 1024) {
        override fun sizeOf(key: String, value: String) = value.length
    }

    /** Marks every picture with its day address and lists the ones not yet swapped. */
    private const val LIST_JS = """(function(){var out=[];var a=document.querySelectorAll('img');
for(var i=0;i<a.length;i++){var im=a[i];if(!im.hasAttribute('data-day-src'))im.setAttribute('data-day-src',im.src);
if(!im.hasAttribute('data-night'))out.push(im.getAttribute('data-day-src'));}return out;})()"""

    /** The inline !important filter beats Readium's CSS inversion, which would undo the swap. */
    private const val SWAP_JS = """(function(src,data){var a=document.querySelectorAll('img');
for(var i=0;i<a.length;i++){var im=a[i];if(im.getAttribute('data-day-src')===src&&!im.hasAttribute('data-night')){
im.src=data;im.setAttribute('data-night','');im.style.setProperty('filter','none','important');}}})"""

    private const val RESTORE_JS = """(function(){var a=document.querySelectorAll('img[data-night]');
for(var i=0;i<a.length;i++){var im=a[i];im.src=im.getAttribute('data-day-src');im.removeAttribute('data-night');
im.style.removeProperty('filter');}})()"""

    /** Brings the pictures of the visible chapter in line with [night]. */
    suspend fun sync(nav: EpubNavigatorFragment, publication: Publication, night: Boolean) {
        if (!night) {
            nav.evaluateJavascript(RESTORE_JS)
            return
        }
        val srcs = nav.evaluateJavascript(LIST_JS)?.let { runCatching { JSONArray(it) }.getOrNull() } ?: return
        val links = publication.resources + publication.readingOrder
        val hrefs = links.map { it.url().toString() }
        for (i in 0 until srcs.length()) {
            val src = srcs.optString(i)
            val href = matchHref(src, hrefs) ?: continue
            val data = nightDataUrl(publication, links[hrefs.indexOf(href)]) ?: continue
            nav.evaluateJavascript("$SWAP_JS(${JSONObject.quote(src)},${JSONObject.quote(data)})")
        }
    }

    private suspend fun nightDataUrl(publication: Publication, link: Link): String? {
        val key = link.url().toString()
        cache.get(key)?.let { return it }
        val data = withContext(Dispatchers.Default) {
            val resource = publication.get(link) ?: return@withContext null
            val bytes = try { resource.read().getOrNull() } finally { resource.close() } ?: return@withContext null
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
            var sample = 1
            while (maxOf(bounds.outWidth, bounds.outHeight) / sample > MAX_SIDE) sample *= 2
            // SVG and broken pictures don't decode; Readium's CSS inversion stays on those.
            val bitmap = BitmapFactory.decodeByteArray(
                bytes, 0, bytes.size,
                BitmapFactory.Options().apply { inSampleSize = sample; inPreferredConfig = Bitmap.Config.ARGB_8888 },
            ) ?: return@withContext null
            val w = bitmap.width
            val h = bitmap.height
            val px = IntArray(w * h)
            bitmap.getPixels(px, 0, w, 0, 0, w, h)
            bitmap.recycle()
            onWhite(px)
            NightPage.apply(px, w, h)
            val night = Bitmap.createBitmap(px, w, h, Bitmap.Config.ARGB_8888)
            // Photos stay JPEG to keep the page small; charts and drawings stay sharp as PNG.
            val jpeg = bounds.outMimeType == "image/jpeg"
            val out = ByteArrayOutputStream()
            night.compress(if (jpeg) Bitmap.CompressFormat.JPEG else Bitmap.CompressFormat.PNG, 90, out)
            night.recycle()
            "data:image/${if (jpeg) "jpeg" else "png"};base64," + Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
        } ?: return null
        cache.put(key, data)
        return data
    }

    /**
     * Lays see-through pixels over white, as the day page shows them. Without this a transparent
     * background would count as a dark picture and stay black, hiding black lines drawn on it.
     */
    fun onWhite(px: IntArray) {
        for (i in px.indices) {
            val c = px[i]
            val a = (c ushr 24) and 0xFF
            if (a == 0xFF) continue
            fun mix(v: Int) = (v * a + 255 * (255 - a)) / 255
            px[i] = (0xFF shl 24) or (mix((c shr 16) and 0xFF) shl 16) or (mix((c shr 8) and 0xFF) shl 8) or mix(c and 0xFF)
        }
    }

    /**
     * The book href that a picture's web address points to, e.g. "OEBPS/graph.png" for
     * "https://readium/publication/OEBPS/graph.png". The longest match wins, so "a/b.png"
     * isn't taken for "x/a/b.png".
     */
    fun matchHref(src: String, hrefs: List<String>): String? {
        val path = decodedPath(src.substringBefore('#').substringBefore('?'))
        return hrefs
            .filter { h -> decodedPath(h.substringBefore('#')).let { path == it || path.endsWith("/$it") } }
            .maxByOrNull { it.length }
    }

    private fun decodedPath(s: String): String =
        runCatching { java.net.URI(s).path }.getOrNull()?.trimStart('/') ?: s.trimStart('/')
}
