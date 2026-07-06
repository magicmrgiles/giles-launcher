package com.giles.einklauncher.data.apps

import kotlinx.serialization.Serializable

/**
 * A stable reference to a launchable activity. This — not a display label or icon — is
 * what we persist, so a pinned app keeps working across relabels and reboots.
 */
@Serializable
data class PinnedAppRef(
    val packageName: String,
    val activityName: String,
)

/** A launchable app resolved from PackageManager, ready to show and launch. */
data class AppInfo(
    val packageName: String,
    val activityName: String,
    val label: String,
) {
    val ref: PinnedAppRef get() = PinnedAppRef(packageName, activityName)

    /** One- or two-letter monogram used inside a grid tile. */
    val monogram: String
        get() = label.trim().firstOrNull()?.uppercase() ?: "?"
}
