package com.bookreader.app.ui.library

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bookreader.app.app
import com.bookreader.app.data.BookEntity
import com.bookreader.app.data.LibraryQuery
import com.bookreader.app.data.LibrarySort
import com.bookreader.app.importer.ImportResult
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class LibraryViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application.app
    private val dao = app.database.books()

    val search = MutableStateFlow("")
    val sort = MutableStateFlow(LibrarySort.RECENT)
    val importing = MutableStateFlow(0)
    /** Show only this format ("EPUB", "PDF", "TXT"), or every book when null. */
    val format = MutableStateFlow<String?>(null)

    /** null while the database is loading, so the screen doesn't flash "empty". */
    val books: StateFlow<List<BookEntity>?> =
        combine(dao.observeAll(), search, sort, format) { all, q, s, f ->
            LibraryQuery.apply(if (f == null) all else all.filter { it.format == f }, q, s)
        }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** The unfinished book opened most recently, for the "Continue reading" card. */
    val continueBook: StateFlow<BookEntity?> =
        dao.observeAll()
            .map { all -> all.filter { it.lastOpenedAt != null && it.progress < 0.995 }.maxByOrNull { it.lastOpenedAt ?: 0L } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val totalBooks: StateFlow<Int> =
        dao.observeAll().map { it.size }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    private val _messages = Channel<String>(Channel.BUFFERED)
    val messages = _messages.receiveAsFlow()

    private val _openReader = Channel<Long>(Channel.BUFFERED)
    /** Emits a book id when the book is ready to show. */
    val openReader = _openReader.receiveAsFlow()

    fun import(uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            importing.value += uris.size
            val results = uris.map { uri -> app.importer.import(uri).also { importing.value -= 1 } }
            val ok = results.filterIsInstance<ImportResult.Imported>()
            val failed = results.filterIsInstance<ImportResult.Failed>()
            when {
                failed.isEmpty() && ok.size == 1 -> _messages.send("Added \"${ok[0].title}\"")
                failed.isEmpty() -> _messages.send("Added ${ok.size} books")
                else -> failed.forEach { _messages.send("${it.fileName}: ${it.reason}") }
            }
        }
    }

    fun open(book: BookEntity) {
        viewModelScope.launch {
            val settings = app.settings.settings.first()
            app.openBooks.open(book, settings)
                .onSuccess { _openReader.send(book.id) }
                .onFailure { _messages.send(it.message ?: "This book could not be opened.") }
        }
    }

    fun delete(book: BookEntity) {
        viewModelScope.launch {
            app.importer.delete(book)
            app.database.bookmarks().deleteForBook(book.id)
            _messages.send("Removed \"${book.title}\"")
        }
    }
}
