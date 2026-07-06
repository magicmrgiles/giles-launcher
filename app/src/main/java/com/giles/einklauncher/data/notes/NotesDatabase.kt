package com.giles.einklauncher.data.notes

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters

class LineTypeConverters {
    @TypeConverter
    fun toLineType(value: String): LineType = LineType.valueOf(value)

    @TypeConverter
    fun fromLineType(type: LineType): String = type.name
}

@Database(entities = [NoteLineEntity::class], version = 1, exportSchema = false)
@TypeConverters(LineTypeConverters::class)
abstract class NotesDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao

    companion object {
        @Volatile private var instance: NotesDatabase? = null

        fun get(context: Context): NotesDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                NotesDatabase::class.java,
                "notes.db",
            ).build().also { instance = it }
        }
    }
}
