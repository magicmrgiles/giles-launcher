package com.giles.einklauncher.data.notes

import androidx.room.Entity
import androidx.room.PrimaryKey

/** The four line kinds a note line can take. */
enum class LineType { TEXT, CHECKBOX, BULLET, NUMBER }

/**
 * One line of the single continuous note. [position] gives visual order; character
 * formatting is stored per line as independent flags. Numbered lines auto-increment within
 * consecutive runs at render time, so no number is stored.
 */
@Entity(tableName = "note_lines")
data class NoteLineEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val position: Int,
    val type: LineType,
    val text: String,
    val done: Boolean = false,
    val bold: Boolean = false,
    val italic: Boolean = false,
    val underline: Boolean = false,
    val strike: Boolean = false,
)
