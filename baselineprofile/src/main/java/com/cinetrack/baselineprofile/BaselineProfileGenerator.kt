package com.cinetrack.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.uiautomator.By

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

                // Optional: You can simulate user interactions here
                // We first need to bypass the Login screen if it appears
                // Wait for the guest button to appear (timeout 5 seconds)
                val guestButton = device.wait(androidx.test.uiautomator.Until.findObject(By.text("Continua come Ospite")), 5000)
                if (guestButton != null) {
                    guestButton.click()
                    device.waitForIdle()
                    
                    // Wait for the confirm button in the modal to appear (timeout 5 seconds)
                    val confirmGuestButton = device.wait(androidx.test.uiautomator.Until.findObject(By.text("Accedi come Ospite")), 5000)
                    if (confirmGuestButton != null) {
                        confirmGuestButton.click()
                        device.waitForIdle()
                    }
                }
                
                // Wait a bit for the Home to load
                device.wait(androidx.test.uiautomator.Until.hasObject(By.desc("Search")), 10000)

                // We click the Search button to precompile the SearchScreen and related components
                val searchButton = device.findObject(By.desc("Search"))
                if (searchButton != null) {
                    searchButton.click()
                    device.waitForIdle()
                    
                    // Digita una query di ricerca per far apparire le MovieCard
                    val searchField = device.wait(androidx.test.uiautomator.Until.findObject(By.textContains("Cerca")), 5000)
                    if (searchField != null) {
                        searchField.text = "Batman"
                        device.waitForIdle()
                        device.pressEnter()
                        device.waitForIdle()
                    }
                    
                    // Aspettiamo che carichi i risultati della ricerca (le MovieCard)
                    val scrollableList = device.wait(androidx.test.uiautomator.Until.findObject(By.scrollable(true)), 10000)
                    if (scrollableList != null) {
                        scrollableList.setGestureMargin(device.displayWidth / 5)
                        // Scorriamo per forzare il rendering e la pre-compilazione di più MovieCard
                        scrollableList.scroll(androidx.test.uiautomator.Direction.DOWN, 1f)
                        device.waitForIdle()
                    }
                }
            }
        )
    }
}
