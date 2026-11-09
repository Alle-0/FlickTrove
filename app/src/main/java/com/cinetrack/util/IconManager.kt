package com.cinetrack.util

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager

object IconManager {

    private const val ALIAS_TEAL = "com.cinetrack.MainActivityTeal"
    private const val ALIAS_PINK = "com.cinetrack.MainActivityPink"
    private const val ALIAS_PURPLE = "com.cinetrack.MainActivityPurple"
    private const val ALIAS_AMBER = "com.cinetrack.MainActivityAmber"
    private const val ALIAS_BLUE = "com.cinetrack.MainActivityBlue"
    private const val ALIAS_HALLOWEEN = "com.cinetrack.MainActivityHalloween"
    private const val ALIAS_CHRISTMAS = "com.cinetrack.MainActivityChristmas"
    private const val ALIAS_VALENTINE = "com.cinetrack.MainActivityValentine"

    fun isHalloweenSeason(): Boolean {
        val cal = java.util.Calendar.getInstance()
        val month = cal.get(java.util.Calendar.MONTH) // Calendar.OCTOBER = 9, Calendar.NOVEMBER = 10
        val day = cal.get(java.util.Calendar.DAY_OF_MONTH)
        return (month == java.util.Calendar.OCTOBER && day >= 10) ||
               (month == java.util.Calendar.NOVEMBER && day <= 5)
    }

    fun isChristmasSeason(): Boolean {
        val cal = java.util.Calendar.getInstance()
        val month = cal.get(java.util.Calendar.MONTH) // Calendar.DECEMBER = 11, Calendar.JANUARY = 0
        val day = cal.get(java.util.Calendar.DAY_OF_MONTH)
        return (month == java.util.Calendar.DECEMBER && day >= 1) ||
               (month == java.util.Calendar.JANUARY && day <= 6)
    }

    fun isValentineSeason(): Boolean {
        val cal = java.util.Calendar.getInstance()
        val month = cal.get(java.util.Calendar.MONTH) // Calendar.FEBRUARY = 1
        val day = cal.get(java.util.Calendar.DAY_OF_MONTH)
        return month == java.util.Calendar.FEBRUARY && day in 7..16
    }

    private const val PREFS_ICON = "app_icon_prefs"
    private const val KEY_LAST_REGULAR_ALIAS = "last_regular_alias"

    private val regularAliases = setOf(
        ALIAS_TEAL,
        ALIAS_PINK,
        ALIAS_PURPLE,
        ALIAS_AMBER,
        ALIAS_BLUE
    )

    private fun getLastRegularAlias(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_ICON, Context.MODE_PRIVATE)
        return prefs.getString(KEY_LAST_REGULAR_ALIAS, ALIAS_TEAL) ?: ALIAS_TEAL
    }

    private fun saveLastRegularAlias(context: Context, alias: String) {
        if (alias in regularAliases) {
            context.getSharedPreferences(PREFS_ICON, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_LAST_REGULAR_ALIAS, alias)
                .apply()
        }
    }

    private fun isAliasEnabled(packageManager: PackageManager, context: Context, alias: String): Boolean {
        return try {
            val state = packageManager.getComponentEnabledSetting(ComponentName(context, alias))
            state == PackageManager.COMPONENT_ENABLED_STATE_ENABLED ||
                    (state == PackageManager.COMPONENT_ENABLED_STATE_DEFAULT && alias == ALIAS_TEAL)
        } catch (_: Exception) {
            false
        }
    }

    fun updateAppIcon(
        context: Context,
        colorName: String,
        isDynamicIconEnabled: Boolean,
        isSeasonalIconEnabled: Boolean = true,
        restartApp: Boolean = true
    ) {
        val packageManager = context.packageManager
        
        val aliases = listOf(
            ALIAS_TEAL,
            ALIAS_PINK,
            ALIAS_PURPLE,
            ALIAS_AMBER,
            ALIAS_BLUE,
            ALIAS_HALLOWEEN,
            ALIAS_CHRISTMAS,
            ALIAS_VALENTINE
        )
        
        val activeAlias = when {
            isSeasonalIconEnabled && isHalloweenSeason() -> ALIAS_HALLOWEEN
            isSeasonalIconEnabled && isChristmasSeason() -> ALIAS_CHRISTMAS
            isSeasonalIconEnabled && isValentineSeason() -> ALIAS_VALENTINE
            !isDynamicIconEnabled -> {
                // If dynamic icon is disabled, keep the currently active icon without changing it.
                // Exception: if a seasonal icon was active and is no longer valid, fallback to the last active regular icon.
                val isSeasonalCurrentlyActive = isAliasEnabled(packageManager, context, ALIAS_HALLOWEEN) ||
                        isAliasEnabled(packageManager, context, ALIAS_CHRISTMAS) ||
                        isAliasEnabled(packageManager, context, ALIAS_VALENTINE)
                if (isSeasonalCurrentlyActive) {
                    getLastRegularAlias(context)
                } else {
                    for (regular in regularAliases) {
                        if (isAliasEnabled(packageManager, context, regular)) {
                            saveLastRegularAlias(context, regular)
                            break
                        }
                    }
                    return
                }
            }
            colorName == "Pink" -> ALIAS_PINK
            colorName == "Purple" -> ALIAS_PURPLE
            colorName == "Amber" -> ALIAS_AMBER
            colorName == "Blue" -> ALIAS_BLUE
            colorName == "Teal" -> ALIAS_TEAL
            colorName.startsWith("#") -> getClosestPresetAlias(colorName)
            else -> ALIAS_TEAL
        }

        if (activeAlias in regularAliases) {
            saveLastRegularAlias(context, activeAlias)
        }

        // Check if the alias is already enabled to avoid unnecessary restarts
        val isCurrentlyEnabled = isAliasEnabled(packageManager, context, activeAlias)
        
        if (isCurrentlyEnabled) {
            return
        }

        // Enable the active alias
        packageManager.setComponentEnabledSetting(
            ComponentName(context, activeAlias),
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP
        )

        // Disable all other aliases
        for (alias in aliases) {
            if (alias != activeAlias) {
                packageManager.setComponentEnabledSetting(
                    ComponentName(context, alias),
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP
                )
            }
        }

        if (restartApp) {
            // Restart the app cleanly to apply the new icon
            val intent = android.content.Intent(context, com.cinetrack.MainActivity::class.java).apply {
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK)
                action = android.content.Intent.ACTION_MAIN
                addCategory(android.content.Intent.CATEGORY_LAUNCHER)
            }
            context.startActivity(intent)
        }
    }

    private val presetColors = mapOf(
        "Pink" to android.graphics.Color.parseColor("#FF4081"),
        "Purple" to android.graphics.Color.parseColor("#BB86FC"),
        "Amber" to android.graphics.Color.parseColor("#FFB300"),
        "Blue" to android.graphics.Color.parseColor("#00B0FF"),
        "Teal" to android.graphics.Color.parseColor("#2DD4BF")
    )

    private fun getClosestPresetAlias(hexColor: String): String {
        return try {
            val customColor = android.graphics.Color.parseColor(hexColor)
            val r1 = android.graphics.Color.red(customColor)
            val g1 = android.graphics.Color.green(customColor)
            val b1 = android.graphics.Color.blue(customColor)

            var minDistance = Double.MAX_VALUE
            var closestAlias = ALIAS_TEAL

            for ((name, colorValue) in presetColors) {
                val r2 = android.graphics.Color.red(colorValue)
                val g2 = android.graphics.Color.green(colorValue)
                val b2 = android.graphics.Color.blue(colorValue)

                val distance = Math.sqrt(
                    Math.pow((r1 - r2).toDouble(), 2.0) +
                    Math.pow((g1 - g2).toDouble(), 2.0) +
                    Math.pow((b1 - b2).toDouble(), 2.0)
                )

                if (distance < minDistance) {
                    minDistance = distance
                    closestAlias = when (name) {
                        "Pink" -> ALIAS_PINK
                        "Purple" -> ALIAS_PURPLE
                        "Amber" -> ALIAS_AMBER
                        "Blue" -> ALIAS_BLUE
                        else -> ALIAS_TEAL
                    }
                }
            }
            closestAlias
        } catch (e: Exception) {
            ALIAS_TEAL
        }
    }
}
