package com.palmastro.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.palmastro.app.navigation.AppNavigation
import com.palmastro.app.ui.theme.PalmAstroTheme
import com.palmastro.data.repository.UserRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var userRepository: UserRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val hasProfile = runBlocking { userRepository.exists() }

        setContent {
            PalmAstroTheme {
                AppNavigation(hasProfile = hasProfile)
            }
        }
    }
}
