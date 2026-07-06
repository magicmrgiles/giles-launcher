package com.giles.einklauncher.notifications

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings

/** Helpers for checking and requesting the special notification-listener access. */
object NotificationAccess {

    fun isEnabled(context: Context): Boolean {
        val component = ComponentName(context, LauncherNotificationListenerService::class.java)
        val flat = Settings.Secure.getString(
            context.contentResolver,
            "enabled_notification_listeners",
        ) ?: return false
        return flat.split(':').any {
            val cn = ComponentName.unflattenFromString(it)
            cn != null && cn == component
        }
    }

    /** Opens the system "Notification access" screen — the only way to grant this access. */
    fun openSettings(context: Context) {
        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
    }
}
