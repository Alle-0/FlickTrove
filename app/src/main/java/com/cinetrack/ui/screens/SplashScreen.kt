package com.cinetrack.ui.screens

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import cafe.adriel.voyager.hilt.getViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.cinetrack.ui.viewmodel.AuthState
import com.cinetrack.ui.viewmodel.AuthViewModel

class SplashScreen : Screen {
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val authViewModel: AuthViewModel = getViewModel()
        val authState by authViewModel.authState.collectAsStateWithLifecycle()
        var animationFinished by remember { mutableStateOf(false) }

        LaunchedEffect(authState, animationFinished) {
            if (animationFinished) {
                when (authState) {
                    is AuthState.Authenticated, is AuthState.Anonymous -> {
                        navigator.replaceAll(MainScreen())
                    }
                    is AuthState.Unauthenticated, is AuthState.Error -> {
                        navigator.replaceAll(LoginScreen())
                    }
                    else -> {}
                }
            }
        }

        LogoAnimationScreen(
            hazeState = null,
            onAnimationEnd = {
                animationFinished = true
            }
        )
    }
}
