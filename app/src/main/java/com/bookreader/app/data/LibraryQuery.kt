package com.bookreader.app.data

enum class LibrarySort(val label: String) {
    RECENT("Recently read"),
    TITLE("Title"),
    AUTHOR("Author"),
    ADDED("Date added"),
}

/** Filters by title or author (case-insensitive) and sorts the library. */
object LibraryQuery {
    fun apply(books: List<BookEntity>, search: String, sort: LibrarySort): List<BookEntity> {
        val q = search.trim()
        val filtered = if (q.isEmpty()) books else books.filter {
            it.title.contains(q, ignoreCase = true) || (it.author?.contains(q, ignoreCase = true) == true)
        }
        return when (sort) {
            LibrarySort.RECENT -> filtered.sortedWith(
                compareByDescending<BookEntity> { it.lastOpenedAt ?: 0L }.thenByDescending { it.addedAt }
            )
            LibrarySort.TITLE -> filtered.sortedBy { it.title.lowercase() }
            LibrarySort.AUTHOR -> filtered.sortedWith(
                compareBy<BookEntity> { it.author == null }.thenBy { it.author?.lowercase() }.thenBy { it.title.lowercase() }
            )
            LibrarySort.ADDED -> filtered.sortedByDescending { it.addedAt }
        }
    }
}
