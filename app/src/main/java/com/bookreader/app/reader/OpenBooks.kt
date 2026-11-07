package com.bookreader.app.reader

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.bookreader.app.BookFormat
import com.bookreader.app.app
import com.bookreader.app.data.BookEntity
import com.bookreader.app.data.bookFormat
import com.bookreader.app.data.ReaderSettings
import org.readium.r2.shared.publication.Publication
import org.readium.r2.shared.util.getOrElse
import java.io.File

/** A book that is open in the reader. EPUBs carry their parsed Readium publication. */
class ReaderSession(
    val book: BookEntity,
    val publication: Publication?,
    val initialSettings: ReaderSettings,
) {
    /** Latest saved location, used when the reader screen is re-created (e.g. on rotation). */
    @Volatile var lastLocation: String? = book.location

    /** Latest listening position (narration), separate from the reading position. */
    @Volatile var listenLocation: String? = book.listenLocation

    /** Current settings, readable from non-Compose code such as the EPUB selection menu. */
    @Volatile var currentSettings: ReaderSettings = initialSettings

    /** PDF: showing the reflowed text instead of page images (the "A" button). */
    var pdfReflow by mutableStateOf(false)

    /** Book language from its metadata (EPUB), used to pick a matching voice. */
    val language: String? get() = publication?.metadata?.languages?.firstOrNull()
}

/**
 * Keeps open books in memory between the library and the reader screen.
 * Readium needs the parsed publication before the reader screen is created,
 * so the library opens the book first, then starts the reader.
 */
class OpenBooks(private val context: Context) {
    private val sessions = mutableMapOf<Long, ReaderSession>()

    fun get(bookId: Long): ReaderSession? = synchronized(sessions) { sessions[bookId] }

    suspend fun open(book: BookEntity, settings: ReaderSettings): Result<ReaderSession> {
        get(book.id)?.let { return Result.success(it) }
        if (!File(book.filePath).exists()) {
            return Result.failure(IllegalStateException("The book file is missing. Please import it again."))
        }
        val publication = if (book.bookFormat == BookFormat.EPUB) {
            val app = context.app
            val asset = app.assetRetriever.retrieve(File(book.filePath)).getOrElse {
                return Result.failure(IllegalStateException("This EPUB could not be opened."))
            }
            app.publicationOpener.open(asset, allowUserInteraction = false).getOrElse {
                asset.close()
                return Result.failure(IllegalStateException("This EPUB could not be opened."))
            }
        } else null
        val session = ReaderSession(book, publication, settings)
        synchronized(sessions) { sessions[book.id] = session }
        return Result.success(session)
    }

    fun close(bookId: Long) {
        val session = synchronized(sessions) { sessions.remove(bookId) }
        session?.publication?.close()
    }
}
