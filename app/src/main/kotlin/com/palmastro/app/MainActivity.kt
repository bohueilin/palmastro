package com.palmastro.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.palmastro.app.navigation.AppNavigation
import com.palmastro.app.navigation.DeepLinkDestination
import com.palmastro.app.navigation.DeepLinkHandler
import com.palmastro.app.ui.onboarding.AppLanguage
import com.palmastro.app.ui.theme.PalmAstroTheme
import com.palmastro.data.repository.UserRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * AppCompatActivity (not ComponentActivity) so AppCompatDelegate.setApplicationLocales
 * works on pre-API-33 devices for the in-app language setting.
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    @Inject lateinit var userRepository: UserRepository

    private val pendingDeepLink = MutableStateFlow<DeepLinkDestination?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        pendingDeepLink.value = DeepLinkHandler.parse(intent)

        // Re-apply the persisted per-app language (no-op when unchanged).
        lifecycleScope.launch {
            userRepository.get()?.language?.let { AppLanguage.apply(it) }
        }

        setContent {
            PalmAstroTheme {
                var hasProfile by remember { mutableStateOf<Boolean?>(null) }
                val deepLink by pendingDeepLink.collectAsState()

                LaunchedEffect(Unit) {
                    hasProfile = userRepository.exists()
                }

                if (hasProfile != null) {
                    AppNavigation(
                        hasProfile = hasProfile!!,
                        deepLink = deepLink,
                        onDeepLinkConsumed = { pendingDeepLink.value = null },
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize())
                }
            }
        }
    }

    // launchMode="singleTask": subsequent deep links arrive here instead of onCreate.
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        DeepLinkHandler.parse(intent)?.let { pendingDeepLink.value = it }
    }
}
