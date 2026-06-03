package com.cinetrack.widget

import android.content.Context
import com.cinetrack.domain.WidgetNotifier
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GlanceWidgetNotifier @Inject constructor(
    @ApplicationContext private val context: Context
) : WidgetNotifier {
    override fun notifyWidgetUpdated() {
        WidgetUpdater.update(context)
    }
}
