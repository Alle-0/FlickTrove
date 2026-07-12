package com.cinetrack.util

import com.cinetrack.BuildConfig
import com.cinetrack.data.api.GithubRelease
import com.cinetrack.data.api.GithubService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

data class AppUpdateInfo(
    val isUpdateAvailable: Boolean = false,
    val latestVersion: String = "",
    val currentVersion: String = BuildConfig.VERSION_NAME,
    val releaseNotes: String = "",
    val htmlUrl: String = "https://github.com/Alle-0/FlickTrove/releases/latest"
)

@Singleton
class AppUpdateManager @Inject constructor(
    private val githubService: GithubService
) {
    private val _updateInfo = MutableStateFlow<AppUpdateInfo?>(null)
    val updateInfo: StateFlow<AppUpdateInfo?> = _updateInfo.asStateFlow()

    private var hasCheckedThisSession = false
    private val _dismissedVersionThisSession = MutableStateFlow<String?>(null)
    val dismissedVersionThisSession: StateFlow<String?> = _dismissedVersionThisSession.asStateFlow()

    suspend fun checkForUpdates(force: Boolean = false): AppUpdateInfo? {
        if (hasCheckedThisSession && !force && _updateInfo.value != null) {
            return _updateInfo.value
        }
        return try {
            val release = githubService.getLatestRelease()
            val latestTag = release.tagName
            val isNewer = isNewerVersion(latestTag, BuildConfig.VERSION_NAME)
            val info = AppUpdateInfo(
                isUpdateAvailable = isNewer,
                latestVersion = latestTag.removePrefix("v").removePrefix("V"),
                currentVersion = BuildConfig.VERSION_NAME,
                releaseNotes = extractAppNotes(release.body),
                htmlUrl = release.htmlUrl
            )
            _updateInfo.value = info
            hasCheckedThisSession = true
            info
        } catch (e: Exception) {
            _updateInfo.value
        }
    }

    fun dismissUpdateForSession(version: String) {
        _dismissedVersionThisSession.value = version
    }

    fun isDismissedForSession(version: String): Boolean {
        return _dismissedVersionThisSession.value == version
    }

    companion object {
        fun extractAppNotes(rawBody: String?): String {
            if (rawBody.isNullOrBlank()) return ""
            val startTag = "<!-- APP_NOTES_START -->"
            val endTag = "<!-- APP_NOTES_END -->"
            val startIndex = rawBody.indexOf(startTag, ignoreCase = true)
            val endIndex = rawBody.indexOf(endTag, ignoreCase = true)
            return if (startIndex != -1 && endIndex != -1 && endIndex > startIndex) {
                rawBody.substring(startIndex + startTag.length, endIndex).trim()
            } else {
                rawBody.trim()
            }
        }

        fun isNewerVersion(latest: String, current: String): Boolean {
            val cleanLatest = latest.removePrefix("v").removePrefix("V").trim()
            val cleanCurrent = current.removePrefix("v").removePrefix("V").trim()
            val latestParts = cleanLatest.split(".").map { it.toIntOrNull() ?: 0 }
            val currentParts = cleanCurrent.split(".").map { it.toIntOrNull() ?: 0 }

            val maxLen = maxOf(latestParts.size, currentParts.size)
            for (i in 0 until maxLen) {
                val l = latestParts.getOrElse(i) { 0 }
                val c = currentParts.getOrElse(i) { 0 }
                if (l > c) return true
                if (l < c) return false
            }
            return false
        }
    }
}
