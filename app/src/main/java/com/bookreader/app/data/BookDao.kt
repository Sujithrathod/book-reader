package com.bookreader.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BookDao {
    @Query("SELECT * FROM books")
    fun observeAll(): Flow<List<BookEntity>>

    @Query("SELECT * FROM books WHERE id = :id")
    suspend fun get(id: Long): BookEntity?

    @Insert
    suspend fun insert(book: BookEntity): Long

    @Query("UPDATE books SET location = :location, progress = :progress WHERE id = :id")
    suspend fun savePosition(id: Long, location: String, progress: Double)

    @Query("UPDATE books SET listenLocation = :location WHERE id = :id")
    suspend fun saveListenPosition(id: Long, location: String)

    @Query("UPDATE books SET lastOpenedAt = :time WHERE id = :id")
    suspend fun markOpened(id: Long, time: Long)

    @Query("DELETE FROM books WHERE id = :id")
    suspend fun delete(id: Long)
}
