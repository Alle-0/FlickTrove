package com.cinetrack.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.cinetrack.ui.utils.ActionFeedbackManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * Thin ViewModel that exposes ActionFeedbackManager to the UI layer.
 * Scoped at the Activity level so it's shared across all screens.
 */
@HiltViewModel
class UndoViewModel @Inject constructor(
    val actionFeedbackManager: ActionFeedbackManager
) : ViewModel()
