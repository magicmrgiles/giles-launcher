package com.giles.einklauncher.data.notes

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {

    @Query("SELECT * FROM note_lines ORDER BY position ASC")
    fun observeLines(): Flow<List<NoteLineEntity>>

    @Query("SELECT * FROM note_lines WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): NoteLineEntity?

    @Query("SELECT COALESCE(MAX(position), -1) FROM note_lines")
    suspend fun maxPosition(): Int

    @Insert
    suspend fun insert(line: NoteLineEntity): Long

    @Update
    suspend fun update(line: NoteLineEntity)

    @Query("DELETE FROM note_lines WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM note_lines")
    suspend fun clear()

    @Transaction
    suspend fun replaceAll(lines: List<NoteLineEntity>) {
        clear()
        lines.forEachIndexed { index, line ->
            insert(line.copy(id = 0, position = index))
        }
    }
}
