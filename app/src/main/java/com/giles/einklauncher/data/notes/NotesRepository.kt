package com.giles.einklauncher.data.notes

import android.content.Context
import kotlinx.coroutines.flow.Flow

/** Line-level formatting flags carried from the editor toolbar. */
data class LineFormat(
    val bold: Boolean = false,
    val italic: Boolean = false,
    val underline: Boolean = false,
    val strike: Boolean = false,
)

/** CRUD over the single continuous note, backed by Room. */
class NotesRepository(context: Context) {

    private val dao = NotesDatabase.get(context).noteDao()

    val lines: Flow<List<NoteLineEntity>> = dao.observeLines()

    suspend fun appendLine(type: LineType, text: String, format: LineFormat) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        val position = dao.maxPosition() + 1
        dao.insert(
            NoteLineEntity(
                position = position,
                type = type,
                text = trimmed,
                done = false,
                bold = format.bold,
                italic = format.italic,
                underline = format.underline,
                strike = format.strike,
            )
        )
    }

    suspend fun toggleCheckbox(id: Long) {
        val line = dao.getById(id) ?: return
        if (line.type != LineType.CHECKBOX) return
        dao.update(line.copy(done = !line.done))
    }

    suspend fun updateLine(line: NoteLineEntity) = dao.update(line)

    suspend fun deleteLine(id: Long) = dao.deleteById(id)

    suspend fun clearAll() = dao.clear()
}
