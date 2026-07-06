package com.giles.einklauncher.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

/**
 * Returns a key that increments every time the host reaches ON_RESUME. Use it with
 * `remember(resumeKey) { ... }` to recompute state that can change while the app is
 * backgrounded (default-launcher status, notification-access grant, etc.).
 */
@Composable
fun rememberResumeKey(): Int {
    val owner = LocalLifecycleOwner.current
    var key by remember { mutableIntStateOf(0) }
    DisposableEffect(owner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) key++
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    return key
}
