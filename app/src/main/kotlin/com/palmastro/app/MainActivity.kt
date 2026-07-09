package com.palmastro.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.palmastro.app.navigation.AppNavigation
import com.palmastro.app.ui.theme.PalmAstroTheme
import com.palmastro.data.repository.UserRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var userRepository: UserRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            PalmAstroTheme {
                var hasProfile by remember { mutableStateOf<Boolean?>(null) }

                LaunchedEffect(Unit) {
                    hasProfile = userRepository.exists()
                }

                if (hasProfile != null) {
                    AppNavigation(hasProfile = hasProfile!!)
                } else {
                    Box(modifier = Modifier.fillMaxSize())
                }
            }
        }
    }
}
