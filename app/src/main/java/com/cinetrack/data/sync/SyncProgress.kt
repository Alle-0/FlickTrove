package com.cinetrack.data.sync

import com.cinetrack.ui.utils.UiText

data class SyncProgress(
    val message: UiText,
    val progress: Float?
)
