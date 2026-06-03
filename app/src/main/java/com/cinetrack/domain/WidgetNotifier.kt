package com.cinetrack.domain

/**
 * Interface to trigger widget updates without exposing Android Context or UI framework dependencies
 * to the Data or Domain layers.
 */
interface WidgetNotifier {
    fun notifyWidgetUpdated()
}
