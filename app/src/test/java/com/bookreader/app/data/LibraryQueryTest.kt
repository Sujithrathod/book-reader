package com.bookreader.app.data

import org.junit.Assert.assertEquals
import org.junit.Test

class LibraryQueryTest {
    private fun book(id: Long, title: String, author: String?, added: Long, opened: Long? = null) =
        BookEntity(id = id, title = title, author = author, format = "EPUB", filePath = "", coverPath = null, addedAt = added, lastOpenedAt = opened)

    private val books = listOf(
        book(1, "Moby Dick", "Herman Melville", added = 10),
        book(2, "emma", "Jane Austen", added = 30, opened = 100),
        book(3, "Notes", null, added = 20, opened = 200),
    )

    @Test fun searchMatchesTitleOrAuthorIgnoringCase() {
        assertEquals(listOf(1L), LibraryQuery.apply(books, "melv", LibrarySort.TITLE).map { it.id })
        assertEquals(listOf(2L), LibraryQuery.apply(books, "EMMA", LibrarySort.TITLE).map { it.id })
    }

    @Test fun sorts() {
        assertEquals(listOf(3L, 2L, 1L), LibraryQuery.apply(books, "", LibrarySort.RECENT).map { it.id })
        assertEquals(listOf(2L, 1L, 3L), LibraryQuery.apply(books, "", LibrarySort.TITLE).map { it.id })
        assertEquals(listOf(1L, 2L, 3L), LibraryQuery.apply(books, "", LibrarySort.AUTHOR).map { it.id })
        assertEquals(listOf(2L, 3L, 1L), LibraryQuery.apply(books, "", LibrarySort.ADDED).map { it.id })
    }
}
