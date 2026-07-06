package com.giles.einklauncher.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

/**
 * The single Preferences DataStore for the whole app. Both settings and the pinned-app
 * grid live here; the file is included in backup rules so it survives reboots/restores.
 * The NotificationListenerService reads from this same store (same process).
 */
val Context.launcherDataStore: DataStore<Preferences> by preferencesDataStore(name = "launcher_prefs")
