package com.cinetrack.util

import android.net.Uri
import com.cinetrack.R

sealed class LinkSecurityCheck {
    data class Safe(val cleanUrl: String, val host: String) : LinkSecurityCheck()
    data class Blocked(val reasonResId: Int) : LinkSecurityCheck()
}

object LinkSecurityUtils {

    // Regex to detect direct IPv4 / IPv6 addresses
    private val IPV4_REGEX = Regex("^\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}$")
    private val SUSPICIOUS_EXTENSIONS = listOf(".apk", ".exe", ".scr", ".bat", ".vbs", ".cmd", ".msi", ".jar")

    fun verifyUrl(rawUrl: String): LinkSecurityCheck {
        val trimmed = rawUrl.trim()
        if (trimmed.isBlank()) {
            return LinkSecurityCheck.Blocked(R.string.link_security_malformed)
        }

        val parsed = try {
            Uri.parse(trimmed)
        } catch (_: Exception) {
            return LinkSecurityCheck.Blocked(R.string.link_security_malformed)
        }

        val scheme = parsed.scheme?.lowercase() ?: ""
        if (scheme != "http" && scheme != "https") {
            return LinkSecurityCheck.Blocked(R.string.link_security_invalid_scheme)
        }

        // Deceptive userinfo check: e.g. https://google.com@evil.com
        if (!parsed.userInfo.isNullOrBlank()) {
            return LinkSecurityCheck.Blocked(R.string.link_security_deceptive)
        }

        val host = parsed.host?.lowercase() ?: ""
        if (host.isBlank()) {
            return LinkSecurityCheck.Blocked(R.string.link_security_malformed)
        }

        // Direct IP address check (common in phishing/scam servers)
        if (IPV4_REGEX.matches(host) || (host.startsWith("[") && host.endsWith("]"))) {
            return LinkSecurityCheck.Blocked(R.string.link_security_ip_blocked)
        }

        // Punycode check (homograph phishing attacks like xn--...)
        if (host.startsWith("xn--") || host.contains(".xn--")) {
            return LinkSecurityCheck.Blocked(R.string.link_security_punycode)
        }

        // Dangerous direct file downloads
        val path = parsed.path?.lowercase() ?: ""
        if (SUSPICIOUS_EXTENSIONS.any { path.endsWith(it) }) {
            return LinkSecurityCheck.Blocked(R.string.link_security_dangerous_file)
        }

        // Clean host for UI display: strip "www."
        val displayHost = if (host.startsWith("www.")) host.removePrefix("www.") else host

        return LinkSecurityCheck.Safe(cleanUrl = trimmed, host = displayHost)
    }
}
