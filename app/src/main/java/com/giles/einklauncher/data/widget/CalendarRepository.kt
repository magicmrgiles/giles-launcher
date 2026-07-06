package com.giles.einklauncher.data.widget

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar

/** A single calendar instance in the widget's window. */
data class CalendarEvent(
    val id: Long,
    val title: String,
    val begin: Long,
    val end: Long,
    val allDay: Boolean,
)

/**
 * Queries [CalendarContract.Instances] for upcoming events across all calendars. Requires
 * the READ_CALENDAR runtime permission; every method degrades to an empty list when the
 * permission is absent so callers can simply hide the row.
 */
class CalendarRepository(private val context: Context) {

    fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) ==
            PackageManager.PERMISSION_GRANTED

    /** The single next event starting from now (within the next 24h). */
    suspend fun nextEvent(): CalendarEvent? =
        upcomingToday().firstOrNull() ?: nextInWindow(hoursAhead = 36).firstOrNull()

    /** Remaining events for the rest of today, ordered by start time. */
    suspend fun restOfToday(): List<CalendarEvent> = upcomingToday()

    private suspend fun upcomingToday(): List<CalendarEvent> = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val endOfDay = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis
        queryInstances(now, endOfDay)
    }

    private suspend fun nextInWindow(hoursAhead: Int): List<CalendarEvent> =
        withContext(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            queryInstances(now, now + hoursAhead * 60L * 60L * 1000L)
        }

    private fun queryInstances(start: Long, end: Long): List<CalendarEvent> {
        if (!hasPermission()) return emptyList()

        val projection = arrayOf(
            CalendarContract.Instances.EVENT_ID,
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END,
            CalendarContract.Instances.ALL_DAY,
        )

        val builder = CalendarContract.Instances.CONTENT_URI.buildUpon()
        ContentUris.appendId(builder, start)
        ContentUris.appendId(builder, end)

        return runCatching {
            context.contentResolver.query(
                builder.build(),
                projection,
                null,
                null,
                CalendarContract.Instances.BEGIN + " ASC",
            )?.use { c ->
                val out = ArrayList<CalendarEvent>()
                val idIx = c.getColumnIndexOrThrow(CalendarContract.Instances.EVENT_ID)
                val titleIx = c.getColumnIndexOrThrow(CalendarContract.Instances.TITLE)
                val beginIx = c.getColumnIndexOrThrow(CalendarContract.Instances.BEGIN)
                val endIx = c.getColumnIndexOrThrow(CalendarContract.Instances.END)
                val allDayIx = c.getColumnIndexOrThrow(CalendarContract.Instances.ALL_DAY)
                while (c.moveToNext()) {
                    val begin = c.getLong(beginIx)
                    // Skip instances that already ended.
                    if (c.getLong(endIx) < System.currentTimeMillis()) continue
                    out.add(
                        CalendarEvent(
                            id = c.getLong(idIx),
                            title = c.getString(titleIx)?.takeIf { it.isNotBlank() } ?: "(No title)",
                            begin = begin,
                            end = c.getLong(endIx),
                            allDay = c.getInt(allDayIx) == 1,
                        )
                    )
                }
                out
            } ?: emptyList()
        }.getOrDefault(emptyList())
    }
}
