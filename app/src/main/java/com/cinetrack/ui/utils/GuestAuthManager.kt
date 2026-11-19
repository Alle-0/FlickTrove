package com.cinetrack.ui.utils

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

data class GuestAuthDialogConfig(
    val titleRes: Int? = null,
    val messageRes: Int? = null
)

/**
 * GuestAuthManager
 * App-level singleton that broadcasts guest authentication prompts across the app.
 * Decouples individual screens and modals from injecting SettingsViewModel solely for auth checks.
 */
@Singleton
class GuestAuthManager @Inject constructor() {
    private val _events = MutableSharedFlow<GuestAuthDialogConfig>(
        replay = 0,
        extraBufferCapacity = 16,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val events: SharedFlow<GuestAuthDialogConfig> = _events.asSharedFlow()

    fun triggerGuestAuthDialog(
        @StringRes titleRes: Int? = null,
        @StringRes messageRes: Int? = null
    ) {
        _events.tryEmit(GuestAuthDialogConfig(titleRes, messageRes))
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface GuestAuthEntryPoint {
    fun guestAuthManager(): GuestAuthManager
}

@Composable
fun rememberGuestAuthManager(): GuestAuthManager? {
    val context = LocalContext.current
    return remember(context) {
        runCatching {
            EntryPointAccessors.fromApplication(
                context.applicationContext,
                GuestAuthEntryPoint::class.java
            ).guestAuthManager()
        }.getOrNull()
    }
}
