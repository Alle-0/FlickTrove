package com.cinetrack.ui.screens

import androidx.compose.runtime.Composable
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.core.screen.uniqueScreenKey
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.hilt.getViewModel
import com.cinetrack.ui.viewmodel.AuthViewModel

class LoginScreen : Screen {
    override val key: ScreenKey = uniqueScreenKey

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val viewModel = getViewModel<AuthViewModel>()
        
        AuthScreen(
            viewModel = viewModel,
            onLoginSuccess = {
                navigator.replaceAll(MainScreen())
            }
        )
    }
}
