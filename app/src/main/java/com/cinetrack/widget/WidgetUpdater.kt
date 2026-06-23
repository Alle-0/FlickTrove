package com.cinetrack.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

object WidgetUpdater {
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun update(context: Context) {
        scope.launch {
            try {
                val appWidgetManager = android.appwidget.AppWidgetManager.getInstance(context)
                val manager = GlanceAppWidgetManager(context)
                
                // Aggiorna FlickTroveWidget
                val widget1Component = android.content.ComponentName(context, FlickTroveWidgetReceiver::class.java)
                val widget1Ids = appWidgetManager.getAppWidgetIds(widget1Component)
                widget1Ids.forEach { id ->
                    try {
                        val glanceId = manager.getGlanceIdBy(id)
                        FlickTroveWidget().update(context, glanceId)
                    } catch (e: Exception) {}
                }
                
                // Aggiorna FlickTroveListWidget
                val widget2Component = android.content.ComponentName(context, FlickTroveListWidgetReceiver::class.java)
                val widget2Ids = appWidgetManager.getAppWidgetIds(widget2Component)
                widget2Ids.forEach { id ->
                    try {
                        val glanceId = manager.getGlanceIdBy(id)
                        FlickTroveListWidget().update(context, glanceId)
                    } catch (e: Exception) {}
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
