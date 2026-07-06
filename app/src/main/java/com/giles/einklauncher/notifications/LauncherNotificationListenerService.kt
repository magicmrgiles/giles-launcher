package com.giles.einklauncher.notifications

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.giles.einklauncher.data.apps.PinnedAppsRepository
import com.giles.einklauncher.data.settings.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * When the notification filter setting is on, suppresses notifications from any package
 * that is not currently pinned to the home grid.
 *
 * Enabling this listener is a *special access* the user grants from system settings
 * (Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS) — it is never a runtime permission
 * dialog. Play Store review implications: apps requesting notification-listener access must
 * have a core feature that genuinely needs it and complete the Play Console declaration;
 * "notification management/filtering" is an accepted use, but the app can be rejected if the
 * access looks incidental. That is why the toggle is opt-in and does nothing until the user
 * explicitly grants access.
 */
class LauncherNotificationListenerService : NotificationListenerService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Volatile private var filterEnabled = false
    @Volatile private var pinnedPackages: Set<String> = emptySet()
    @Volatile private var connected = false

    override fun onListenerConnected() {
        super.onListenerConnected()
        connected = true

        val settings = SettingsRepository(applicationContext)
        val pinned = PinnedAppsRepository(applicationContext)

        // Keep our cached view of the filter setting + pinned packages current, and
        // re-sweep the shade whenever either changes.
        scope.launch {
            combine(settings.settings, pinned.pinned) { s, slots ->
                s.notificationFilterEnabled to slots.filterNotNull().map { it.packageName }.toSet()
            }.collect { (enabled, packages) ->
                filterEnabled = enabled
                pinnedPackages = packages
                if (connected) sweepActive()
            }
        }
    }

    override fun onListenerDisconnected() {
        connected = false
        super.onListenerDisconnected()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        sbn ?: return
        if (shouldSuppress(sbn)) {
            runCatching { cancelNotification(sbn.key) }
        }
    }

    /** Re-evaluate everything currently in the shade (e.g. after the setting toggles on). */
    private fun sweepActive() {
        if (!filterEnabled) return
        val active = runCatching { activeNotifications }.getOrNull() ?: return
        active.forEach { sbn ->
            if (shouldSuppress(sbn)) runCatching { cancelNotification(sbn.key) }
        }
    }

    private fun shouldSuppress(sbn: StatusBarNotification): Boolean {
        if (!filterEnabled) return false
        if (sbn.packageName == applicationContext.packageName) return false
        if (sbn.packageName in pinnedPackages) return false
        // Leave ongoing/foreground-service notifications alone (calls, media, downloads).
        val flags = sbn.notification?.flags ?: 0
        if (flags and Notification.FLAG_ONGOING_EVENT != 0) return false
        if (flags and Notification.FLAG_FOREGROUND_SERVICE != 0) return false
        return true
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
