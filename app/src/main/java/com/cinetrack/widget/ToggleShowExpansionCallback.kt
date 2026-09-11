package com.cinetrack.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.updateAll

/**
 * Callback che gestisce il toggle espansione/collasso di una serie
 * nel widget Now Watching (fisarmonica).
 *
 * Se la serie è già espansa → collassa (expanded = -1).
 * Se è una serie diversa → espande quella nuova.
 */
class ToggleShowExpansionCallback : ActionCallback {

    companion object {
        val KEY_SHOW_ID = ActionParameters.Key<Long>("toggle_show_id")
        private const val PREFS_NAME = "flicktrove_widget_state"
        private const val KEY_EXPANDED = "expanded_now_watching_id"

        fun getExpandedShowId(context: Context): Long =
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getLong(KEY_EXPANDED, -1L)

        fun setExpandedShowId(context: Context, showId: Long) =
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit().putLong(KEY_EXPANDED, showId).apply()
    }

    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val showId = parameters[KEY_SHOW_ID] ?: return
        val current = getExpandedShowId(context)
        val newId = if (current == showId) -1L else showId
        setExpandedShowId(context, newId)
        FlickTroveNowWatchingWidget().updateAll(context)
    }
}
