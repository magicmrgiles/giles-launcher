package com.giles.einklauncher.data.apps

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.giles.einklauncher.data.launcherDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.nullable
import kotlinx.serialization.json.Json

/**
 * Persists the pinned-app grid as an ordered list of slots. Each slot is either a
 * [PinnedAppRef] or null (an empty "+" slot). Order is bottom-aligned in the UI, so the
 * list is stored in visual order and index 0 is the top-left slot.
 */
class PinnedAppsRepository(private val context: Context) {

    private val json = Json { ignoreUnknownKeys = true }
    private val slotSerializer = ListSerializer(PinnedAppRef.serializer().nullable)

    private object Keys {
        val PINNED = stringPreferencesKey("pinned_apps")
        val SEEDED = booleanPreferencesKey("pinned_seeded")
    }

    /** The current grid, or an empty list if nothing has been stored yet. */
    val pinned: Flow<List<PinnedAppRef?>> = context.launcherDataStore.data.map { p ->
        p[Keys.PINNED]?.let { runCatching { json.decodeFromString(slotSerializer, it) }.getOrNull() }
            ?: emptyList()
    }

    /** True once we've seeded first-run defaults, so we never re-seed over user edits. */
    suspend fun isSeeded(): Boolean = context.launcherDataStore.data.first()[Keys.SEEDED] ?: false

    suspend fun save(slots: List<PinnedAppRef?>) {
        context.launcherDataStore.edit { p ->
            p[Keys.PINNED] = json.encodeToString(slotSerializer, slots)
            p[Keys.SEEDED] = true
        }
    }

    /** Assign [ref] to a slot, growing the list with empty slots if needed. */
    suspend fun assign(index: Int, ref: PinnedAppRef) {
        val current = pinned.first().toMutableList()
        while (current.size <= index) current.add(null)
        current[index] = ref
        save(current)
    }

    /** Clear a slot back to empty ("+"). */
    suspend fun remove(index: Int) {
        val current = pinned.first().toMutableList()
        if (index in current.indices) {
            current[index] = null
            save(current)
        }
    }

    /**
     * Resize the grid. Growing prepends empty slots (new empties appear in the *top* row).
     * Shrinking keeps the bottom [count] slots and frees the rest — matching the spec's
     * "8→4 retains the bottom row" behaviour.
     */
    suspend fun resize(count: Int) {
        val current = pinned.first()
        val next = if (count >= current.size) {
            List(count - current.size) { null } + current
        } else {
            current.takeLast(count)
        }
        save(next)
    }
}
