package com.cinetrack.ui.screens

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import cafe.adriel.voyager.hilt.getViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.core.screen.uniqueScreenKey
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.cinetrack.ui.viewmodel.AuthState
import com.cinetrack.ui.viewmodel.AuthViewModel
import com.cinetrack.ui.viewmodel.SettingsViewModel

class SplashScreen : Screen {
    override val key: ScreenKey = uniqueScreenKey

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val authViewModel: AuthViewModel = getViewModel()
        val settingsViewModel: SettingsViewModel = getViewModel()
        
        val authState by authViewModel.authState.collectAsStateWithLifecycle()
        val showAppEntryAnimation by settingsViewModel.showAppEntryAnimation.collectAsStateWithLifecycle()
        val defaultStartTab by settingsViewModel.defaultStartTab.collectAsStateWithLifecycle()
        
        var animationFinished by remember { mutableStateOf(false) }

        LaunchedEffect(authState, animationFinished, showAppEntryAnimation) {
            if (animationFinished || !showAppEntryAnimation) {
                when (authState) {
                    is AuthState.Authenticated, is AuthState.Anonymous -> {
                        navigator.replaceAll(MainScreen(defaultStartTab))
                    }
                    is AuthState.Unauthenticated, is AuthState.Error -> {
                        navigator.replaceAll(LoginScreen())
                    }
                    else -> {}
                }
            }
        }

        if (showAppEntryAnimation) {
            LogoAnimationScreen(
                hazeState = null,
                onAnimationEnd = {
                    animationFinished = true
                }
            )
        } else {
            // Un contenitore vuoto o con lo stesso sfondo in modo da non far sfarfallare nulla 
            // mentre si aspetta che AuthState venga risolto (se è in caricamento)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            )
        }
    }
}
