package com.bookreader.app

import android.app.Application
import com.bookreader.app.data.AppDatabase
import com.bookreader.app.data.SettingsRepository
import com.bookreader.app.importer.BookImporter
import com.bookreader.app.narration.Narrator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import com.bookreader.app.reader.OpenBooks
import org.readium.r2.shared.util.asset.AssetRetriever
import org.readium.r2.shared.util.http.DefaultHttpClient
import org.readium.r2.streamer.PublicationOpener
import org.readium.r2.streamer.parser.DefaultPublicationParser

/** Holds the app-wide objects. Small enough that no dependency-injection library is needed. */
class BookReaderApp : Application() {
    val database by lazy { AppDatabase.create(this) }
    val settings by lazy { SettingsRepository(this) }

    // Readium: reads EPUB files. PDFs are shown with Android's built-in PdfRenderer instead,
    // so no PDF engine is passed here (pdfFactory = null).
    private val httpClient by lazy { DefaultHttpClient() }
    val assetRetriever by lazy { AssetRetriever(contentResolver, httpClient) }
    val publicationOpener by lazy {
        PublicationOpener(
            publicationParser = DefaultPublicationParser(this, httpClient, assetRetriever, pdfFactory = null),
        )
    }

    val importer by lazy { BookImporter(this, database.books(), assetRetriever, publicationOpener) }
    val openBooks by lazy { OpenBooks(this) }

    /** Reads aloud. One for the whole app, shared by the reader screen and the playback service. */
    val narrator by lazy {
        Narrator(this).also { n ->
            n.onListeningPosition = { bookId, location ->
                appScope.launch { database.books().saveListenPosition(bookId, location) }
            }
        }
    }

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
}

val android.content.Context.app: BookReaderApp get() = applicationContext as BookReaderApp
