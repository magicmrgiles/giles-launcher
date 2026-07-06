package com.giles.einklauncher

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings

/**
 * Helpers for becoming the default launcher. On API 29+ this is the RoleManager ROLE_HOME
 * flow; on older devices we fall back to the system home-picker settings screen, which
 * surfaces the standard "Complete action using" behaviour.
 */
object DefaultLauncherHelper {

    /** True if this app is the current default home / launcher. */
    fun isDefault(context: Context): Boolean {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val resolve = context.packageManager.resolveActivity(intent, 0)
        return resolve?.activityInfo?.packageName == context.packageName
    }

    /**
     * Returns an intent that asks the user to make us the default launcher, or null if the
     * platform offers no such intent. On API 29+ this is the ROLE_HOME request dialog; on
     * older devices it is the home-settings screen.
     */
    fun requestIntent(context: Context): Intent? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = context.getSystemService(RoleManager::class.java)
            if (roleManager != null &&
                roleManager.isRoleAvailable(RoleManager.ROLE_HOME) &&
                !roleManager.isRoleHeld(RoleManager.ROLE_HOME)
            ) {
                return roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME)
            }
            return null
        }
        // Pre-Q fallback: open the system's home-app picker.
        return Intent(Settings.ACTION_HOME_SETTINGS)
    }
}
