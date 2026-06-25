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

    fun updateAppIcon(context: Context, colorName: String, isDynamicIconEnabled: Boolean) {
        if (!isDynamicIconEnabled) {
            return
        }

        val packageManager = context.packageManager
        
        val aliases = listOf(ALIAS_TEAL, ALIAS_PINK, ALIAS_PURPLE, ALIAS_AMBER, ALIAS_BLUE)
        
        val activeAlias = when {
            colorName == "Pink" -> ALIAS_PINK
            colorName == "Purple" -> ALIAS_PURPLE
            colorName == "Amber" -> ALIAS_AMBER
            colorName == "Blue" -> ALIAS_BLUE
            colorName == "Teal" -> ALIAS_TEAL
            colorName.startsWith("#") -> getClosestPresetAlias(colorName)
            else -> ALIAS_TEAL
        }

        // Check if the alias is already enabled to avoid unnecessary restarts
        val isCurrentlyEnabled = packageManager.getComponentEnabledSetting(
            ComponentName(context, activeAlias)
        ) == PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        
        if (isCurrentlyEnabled) {
            return
        }

        // Schedule an app restart because changing the component setting will kill the app
        val restartIntent = android.content.Intent(android.content.Intent.ACTION_MAIN).apply {
            component = ComponentName(context, activeAlias)
            addCategory(android.content.Intent.CATEGORY_LAUNCHER)
            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        val pendingIntent = android.app.PendingIntent.getActivity(
            context,
            1234,
            restartIntent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
        alarmManager.set(
            android.app.AlarmManager.RTC_WAKEUP,
            System.currentTimeMillis() + 1500, // 1.5 second delay
            pendingIntent
        )

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
        // Clear tasks from Recent Apps to prevent black screen when user taps old instance
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
        try {
            for (appTask in activityManager.appTasks) {
                if (appTask.taskInfo.baseIntent?.component?.packageName == context.packageName) {
                    appTask.finishAndRemoveTask()
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("IconManager", "Error clearing tasks", e)
        }

        // Slight delay to allow OS to remove task, then kill process
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            android.os.Process.killProcess(android.os.Process.myPid())
            System.exit(0)
        }, 300)
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
