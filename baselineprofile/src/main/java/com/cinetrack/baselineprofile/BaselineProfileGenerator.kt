package com.cinetrack.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.Until
import java.util.regex.Pattern

@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {

    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun generate() {
        rule.collect(
            packageName = "com.cinetrack",
            profileBlock = {
                // Starts the app
                pressHome()
                startActivityAndWait()

                // Bypass the Login screen by entering as Guest
                val guestButtonPattern = Pattern.compile("(?i)Enter as Guest|Entra come Ospite|Continua come Ospite|Continue as Guest")
                val guestButton = device.wait(Until.findObject(By.text(guestButtonPattern)), 6000)
                if (guestButton != null) {
                    guestButton.click()
                    device.waitForIdle()
                    
                    // Wait for the confirm button in the modal to appear
                    val confirmPattern = Pattern.compile("(?i)Accept and Continue|Accetta e Continua|Accedi come Ospite|Sign in as Guest")
                    val confirmGuestButton = device.wait(Until.findObject(By.text(confirmPattern)), 6000)
                    if (confirmGuestButton != null) {
                        confirmGuestButton.click()
                        device.waitForIdle()
                    }
                }
                
                // Wait for the Home screen to appear by waiting for the Search button
                val searchPattern = Pattern.compile("(?i)Cerca|Search")
                val searchButton = device.wait(Until.findObject(By.desc(searchPattern)), 12000)

                // Scroll the feed using coordinates to avoid StaleObjectException on recomposed Compose items
                val midX = device.displayWidth / 2
                val startY = (device.displayHeight * 0.75).toInt()
                val endY = (device.displayHeight * 0.25).toInt()

                device.swipe(midX, startY, midX, endY, 25)
                device.waitForIdle()
                device.swipe(midX, endY, midX, startY, 25)
                device.waitForIdle()

                // Click Search FAB to open the search screen
                val currentSearchButton = searchButton ?: device.findObject(By.desc(searchPattern))
                if (currentSearchButton != null) {
                    currentSearchButton.click()
                    device.waitForIdle()

                    // Wait for the search input field and type a query
                    val searchField = device.wait(Until.findObject(By.clazz("android.widget.EditText")), 8000)
                    if (searchField != null) {
                        searchField.text = "Batman"
                        device.waitForIdle()
                        device.pressEnter()
                        device.waitForIdle()
                    }

                    // Wait a moment for search results to render and scroll through them
                    Thread.sleep(2500)
                    device.swipe(midX, startY, midX, endY, 25)
                    device.waitForIdle()
                }
            }
        )
    }
}

