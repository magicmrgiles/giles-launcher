package com.giles.einklauncher

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.lifecycleScope
import com.giles.einklauncher.ui.LauncherRoot
import com.giles.einklauncher.ui.LauncherViewModel
import com.giles.einklauncher.ui.theme.EinkTheme
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: LauncherViewModel by viewModels()

    /** Emits whenever a HOME intent arrives while we're already running, so the UI resets. */
    private val homeReset = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    /** Bumped after a default-launcher role request returns, so the UI can re-check state. */
    private val roleResult = MutableStateFlow(0)

    private val roleRequestLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            roleResult.value += 1
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val settings by viewModel.settings.collectAsState()
            EinkTheme(themeMode = settings.themeMode) {
                LauncherRoot(
                    viewModel = viewModel,
                    homeReset = homeReset,
                    roleResultSignal = roleResult,
                    onRequestDefaultLauncher = ::requestDefaultLauncher,
                )
            }
        }
    }

    private fun requestDefaultLauncher() {
        val intent = DefaultLauncherHelper.requestIntent(this) ?: return
        runCatching { roleRequestLauncher.launch(intent) }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Pressing HOME while we're foregrounded should return to the home page and close
        // any open sheet — standard launcher behaviour.
        if (intent.hasCategory(Intent.CATEGORY_HOME)) {
            lifecycleScope.launch { homeReset.emit(Unit) }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.onResume()
    }
}
