package com.bookreader.app.importer

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import com.bookreader.app.BookFormat
import com.bookreader.app.data.BookDao
import com.bookreader.app.data.BookEntity
import com.bookreader.app.text.PlainText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.readium.r2.shared.publication.services.cover
import org.readium.r2.shared.util.asset.AssetRetriever
import org.readium.r2.shared.util.getOrElse
import org.readium.r2.streamer.PublicationOpener
import java.io.File
import java.util.UUID

sealed interface ImportResult {
    data class Imported(val title: String) : ImportResult
    data class Failed(val fileName: String, val reason: String) : ImportResult
}

/**
 * Copies a book chosen in the system file picker into the app's private storage,
 * reads its title, author and cover, and saves it to the library.
 * Nothing leaves the phone.
 */
class BookImporter(
    private val context: Context,
    private val dao: BookDao,
    private val assetRetriever: AssetRetriever,
    private val publicationOpener: PublicationOpener,
) {
    private val booksDir get() = File(context.filesDir, "books").apply { mkdirs() }
    private val coversDir get() = File(context.filesDir, "covers").apply { mkdirs() }

    suspend fun import(uri: Uri): ImportResult = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val (name, mime) = queryNameAndType(uri)
        val displayName = name ?: "book"
        val format = BookFormat.detect(name, mime)
            ?: return@withContext ImportResult.Failed(displayName, "Only EPUB, PDF and TXT files are supported.")

        val id = UUID.randomUUID().toString()
        val file = File(booksDir, "$id.${format.extensions.first()}")
        try {
            resolver.openInputStream(uri)?.use { input -> file.outputStream().use { input.copyTo(it) } }
                ?: return@withContext ImportResult.Failed(displayName, "The file could not be read.")
        } catch (e: Exception) {
            file.delete()
            return@withContext ImportResult.Failed(displayName, "The file could not be copied: ${e.message}")
        }

        val meta = when (format) {
            BookFormat.EPUB -> readEpub(file, id)
            BookFormat.PDF -> readPdf(file, id, displayName)
            BookFormat.TXT -> readTxt(file, displayName)
        }
        if (meta is Meta.Error) {
            file.delete()
            return@withContext ImportResult.Failed(displayName, meta.reason)
        }
        meta as Meta.Ok
        dao.insert(
            BookEntity(
                title = meta.title,
                author = meta.author,
                format = format.name,
                filePath = file.absolutePath,
                coverPath = meta.coverPath,
                addedAt = System.currentTimeMillis(),
            )
        )
        ImportResult.Imported(meta.title)
    }

    /** Removes the book's files and its library entry. */
    suspend fun delete(book: BookEntity) = withContext(Dispatchers.IO) {
        File(book.filePath).delete()
        book.coverPath?.let { File(it).delete() }
        dao.delete(book.id)
    }

    private sealed interface Meta {
        data class Ok(val title: String, val author: String?, val coverPath: String?) : Meta
        data class Error(val reason: String) : Meta
    }

    private suspend fun readEpub(file: File, id: String): Meta {
        val asset = assetRetriever.retrieve(file).getOrElse {
            return Meta.Error("This EPUB could not be opened. It may be damaged.")
        }
        val publication = publicationOpener.open(asset, allowUserInteraction = false).getOrElse {
            asset.close()
            return Meta.Error("This EPUB could not be opened. It may be damaged or copy-protected (DRM).")
        }
        try {
            val title = publication.metadata.title?.takeIf { it.isNotBlank() } ?: file.nameWithoutExtension
            val author = publication.metadata.authors.joinToString(", ") { it.name }.ifBlank { null }
            val cover = runCatching { publication.cover() }.getOrNull()?.let { saveCover(it, id) }
            return Meta.Ok(title, author, cover)
        } finally {
            publication.close()
        }
    }

    private fun readPdf(file: File, id: String, displayName: String): Meta {
        return try {
            ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY).use { pfd ->
                PdfRenderer(pfd).use { renderer ->
                    if (renderer.pageCount == 0) return Meta.Error("This PDF has no pages.")
                    val cover = renderer.openPage(0).use { page ->
                        val width = 400
                        val height = (width.toFloat() * page.height / page.width).toInt().coerceIn(1, 1200)
                        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                        bitmap.eraseColor(Color.WHITE)
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        saveCover(bitmap, id)
                    }
                    Meta.Ok(PlainText.titleFromFileName(displayName), null, cover)
                }
            }
        } catch (e: SecurityException) {
            Meta.Error("This PDF is password-protected, which isn't supported yet.")
        } catch (e: Exception) {
            Meta.Error("This PDF could not be opened. It may be damaged.")
        }
    }

    private fun readTxt(file: File, displayName: String): Meta {
        if (file.length() == 0L) return Meta.Error("This text file is empty.")
        return Meta.Ok(PlainText.titleFromFileName(displayName), null, null)
    }

    private fun saveCover(bitmap: Bitmap, id: String): String? = runCatching {
        val out = File(coversDir, "$id.jpg")
        out.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 85, it) }
        out.absolutePath
    }.getOrNull()

    private fun queryNameAndType(uri: Uri): Pair<String?, String?> {
        val resolver = context.contentResolver
        val name = runCatching {
            resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                if (c.moveToFirst()) c.getString(0) else null
            }
        }.getOrNull() ?: uri.lastPathSegment
        return name to resolver.getType(uri)
    }
}
