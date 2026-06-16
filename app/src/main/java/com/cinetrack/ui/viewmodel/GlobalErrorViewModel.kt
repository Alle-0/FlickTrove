package com.cinetrack.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.cinetrack.ui.utils.GlobalErrorHandler
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * Thin ViewModel that exposes GlobalErrorHandler to the UI layer.
 * Used at the top level of the app (e.g. FlickTroveApp) to provide
 * the singleton instance to GlobalErrorToast without breaking Compose injection rules.
 */
@HiltViewModel
class GlobalErrorViewModel @Inject constructor(
    val globalErrorHandler: GlobalErrorHandler
) : ViewModel()
