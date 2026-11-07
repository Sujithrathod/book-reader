package com.bookreader.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One imported book. The file is a private copy inside the app's storage.
 *
 * [location] is where the reader stopped, in a format-specific form:
 * EPUB: a Readium Locator as JSON; PDF: "page:<index>"; TXT: "para:<index>".
 * [listenLocation] uses the narration format, see BlockPosition.encode.
 */
@Entity(tableName = "books")
data class BookEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val author: String?,
    val format: String,
    val filePath: String,
    val coverPath: String?,
    val addedAt: Long,
    val lastOpenedAt: Long? = null,
    val location: String? = null,
    val progress: Double = 0.0,
    /** Where reading aloud stopped; kept separate from the visual reading position. */
    val listenLocation: String? = null,
)

/** The book's format as an enum. Kept outside the entity so Room doesn't treat it as a column. */
val BookEntity.bookFormat: com.bookreader.app.BookFormat get() = com.bookreader.app.BookFormat.valueOf(format)
