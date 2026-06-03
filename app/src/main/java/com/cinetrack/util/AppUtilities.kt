package com.cinetrack.util

import android.content.Context
import coil.imageLoader
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppUtilities @Inject constructor(
    @ApplicationContext private val context: Context
) {
    @OptIn(coil.annotation.ExperimentalCoilApi::class)
    suspend fun clearImageCache() = withContext(Dispatchers.IO) {
        context.imageLoader.diskCache?.clear()
        context.imageLoader.memoryCache?.clear()
    }

    fun updateAppIcon(colorName: String, isDynamicIconEnabled: Boolean) {
        IconManager.updateAppIcon(context, colorName, isDynamicIconEnabled)
    }
}
