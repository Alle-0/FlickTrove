package com.cinetrack.ui.screens

import androidx.compose.runtime.Composable
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.hilt.getViewModel
import com.cinetrack.ui.viewmodel.AuthViewModel

class LoginScreen : Screen {
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
