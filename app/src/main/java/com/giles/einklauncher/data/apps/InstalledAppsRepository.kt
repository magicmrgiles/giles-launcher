package com.giles.einklauncher.data.apps

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Enumerates and launches installed apps via [PackageManager]. Enumeration relies on the
 * <queries> MAIN/LAUNCHER entry in the manifest — not the restricted QUERY_ALL_PACKAGES
 * permission — so it is Play-Store compliant.
 */
class InstalledAppsRepository(private val context: Context) {

    private val pm: PackageManager get() = context.packageManager

    /** Every launchable activity, sorted by label. Runs off the main thread. */
    suspend fun loadAll(): List<AppInfo> = withContext(Dispatchers.Default) {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val resolved: List<ResolveInfo> = queryLauncherActivities(intent)
        resolved
            .asSequence()
            .map { it.toAppInfo() }
            // Don't offer ourselves as a pinnable app.
            .filter { it.packageName != context.packageName }
            .distinctBy { it.packageName + "/" + it.activityName }
            .sortedBy { it.label.lowercase() }
            .toList()
    }

    private fun queryLauncherActivities(intent: Intent): List<ResolveInfo> =
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            pm.queryIntentActivities(
                intent,
                PackageManager.ResolveInfoFlags.of(0L),
            )
        } else {
            @Suppress("DEPRECATION")
            pm.queryIntentActivities(intent, 0)
        }

    private fun ResolveInfo.toAppInfo(): AppInfo = AppInfo(
        packageName = activityInfo.packageName,
        activityName = activityInfo.name,
        label = loadLabel(pm).toString(),
    )

    /** Resolve display labels for a set of stored refs, keyed by ref. */
    suspend fun resolveLabels(refs: List<PinnedAppRef>): Map<PinnedAppRef, String> =
        withContext(Dispatchers.Default) {
            val all = loadAll().associateBy { it.ref }
            refs.associateWith { ref ->
                all[ref]?.label
                    ?: runCatching {
                        pm.getApplicationLabel(pm.getApplicationInfo(ref.packageName, 0)).toString()
                    }.getOrNull()
                    ?: ref.packageName.substringAfterLast('.')
            }
        }

    /** True if the stored activity still resolves (app not uninstalled/disabled). */
    fun isLaunchable(ref: PinnedAppRef): Boolean = buildLaunchIntent(ref).let { intent ->
        (if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU)
            pm.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0L))
        else @Suppress("DEPRECATION") pm.queryIntentActivities(intent, 0)).isNotEmpty()
    }

    /**
     * Launch a pinned app in a new task (required — the launcher is not itself the caller's
     * task). Falls back to the package's default launch intent if the exact activity is
     * gone. Returns false if nothing could be launched.
     */
    fun launch(ref: PinnedAppRef): Boolean {
        val explicit = buildLaunchIntent(ref)
        if (tryStart(explicit)) return true
        val fallback = pm.getLaunchIntentForPackage(ref.packageName)?.apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return fallback != null && tryStart(fallback)
    }

    private fun buildLaunchIntent(ref: PinnedAppRef): Intent = Intent(Intent.ACTION_MAIN).apply {
        addCategory(Intent.CATEGORY_LAUNCHER)
        setClassName(ref.packageName, ref.activityName)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
    }

    private fun tryStart(intent: Intent): Boolean = runCatching {
        if (intent.resolveActivity(pm) == null) return false
        context.startActivity(intent)
        true
    }.getOrDefault(false)

    /**
     * Suggested first-run defaults: resolve well-known categories (dialer, messaging,
     * browser, email, camera, clock, maps, settings) to real activities on this device,
     * de-duplicate, and cap at [count]. Anything unresolved is simply skipped.
     */
    suspend fun suggestedDefaults(count: Int): List<PinnedAppRef> = withContext(Dispatchers.Default) {
        val probes: List<Intent> = listOf(
            Intent(Intent.ACTION_DIAL),
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_MESSAGING),
            Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com")),
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_EMAIL),
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_CALENDAR),
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_MAPS),
            Intent("android.media.action.IMAGE_CAPTURE"),
            Intent(android.provider.AlarmClock.ACTION_SHOW_ALARMS),
        )
        val seen = HashSet<String>()
        val result = ArrayList<PinnedAppRef>()
        for (probe in probes) {
            if (result.size >= count) break
            val ri = pm.resolveActivity(probe, 0) ?: continue
            val ai = ri.activityInfo ?: continue
            if (ai.packageName == context.packageName) continue
            if (seen.add(ai.packageName)) {
                // resolveActivity can return the system "resolver" — only pin real activities.
                if (!ai.name.contains("ResolverActivity", ignoreCase = true)) {
                    result.add(PinnedAppRef(ai.packageName, ai.name))
                }
            }
        }
        result.take(count)
    }
}
