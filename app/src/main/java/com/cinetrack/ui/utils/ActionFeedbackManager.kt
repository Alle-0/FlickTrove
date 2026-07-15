package com.cinetrack.ui.utils

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton
import com.cinetrack.ui.components.common.UndoToast

data class UndoAction(
    val message: UiText,
    val undoFn: (suspend () -> Unit)?
)

/**
 * ActionFeedbackManager
 * App-level singleton that broadcasts "undo toast" events.
 * Any ViewModel injects this and calls emit() after a user mutation.
 * MainActivity observes events and shows the UndoToast composable.
 */
@Singleton
class ActionFeedbackManager @Inject constructor() {
    private val _events = MutableSharedFlow<UndoAction>(
        replay = 0,
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val events: SharedFlow<UndoAction> = _events.asSharedFlow()

    fun emit(message: UiText, undoFn: (suspend () -> Unit)? = null) {
        val success = _events.tryEmit(UndoAction(message, undoFn))
        android.util.Log.d("ActionFeedbackManager", "Emitted toast, success: $success")
    }
}
