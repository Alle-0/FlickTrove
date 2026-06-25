package com.cinetrack.ui.utils

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

data class ErrorEvent(
    val message: UiText,
    val retryFn: (suspend () -> Unit)? = null,
    val id: String = java.util.UUID.randomUUID().toString()
)

/**
 * GlobalErrorHandler
 * Gestore globale degli errori architetturali.
 * Consente un instradamento centralizzato dei fallimenti, intercettabili a livello globale.
 */
@Singleton
class GlobalErrorHandler @Inject constructor() {
    private val _errors = MutableSharedFlow<ErrorEvent>(
        replay = 0,
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val errors: SharedFlow<ErrorEvent> = _errors.asSharedFlow()

    fun emitError(message: UiText, retryFn: (suspend () -> Unit)? = null) {
        val success = _errors.tryEmit(ErrorEvent(message, retryFn))
        android.util.Log.e("GlobalErrorHandler", "Emitted error, success: $success")
    }
}
