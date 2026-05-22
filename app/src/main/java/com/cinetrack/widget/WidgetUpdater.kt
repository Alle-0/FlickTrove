package com.cinetrack.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

object WidgetUpdater {
    @OptIn(DelicateCoroutinesApi::class)
    fun update(context: Context) {
        GlobalScope.launch {
            try {
                FlickTroveWidget().updateAll(context)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
