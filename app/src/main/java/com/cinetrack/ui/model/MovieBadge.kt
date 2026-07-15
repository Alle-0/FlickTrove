package com.cinetrack.ui.model

import androidx.compose.runtime.Composable

enum class MovieBadge(val text: String, val colorValue: Long) {
    NEW("NEW", 0xFFFF007F), // NeonPink
    MASTERPIECE("MASTERPIECE", 0xFFFFD700),
    BEST("BEST", 0xFF00E5FF),
    HOT("HOT", 0xFFFFC107), // AccentYellow
    WOW("WOW", 0xFF00E676), // NeonTeal
    HIDDEN_GEM("HIDDEN GEM", 0xFF00E676),
    DIVISIVE("DIVISIVE", 0xFFFF9800),
    BLOCKBUSTER("BLOCKBUSTER", 0xFF6200EA),
    INDIE("INDIE", 0xFFAED581),
    VINTAGE("VINTAGE", 0xFFBCAAA4),
    CLASSIC("CLASSIC", 0xFF8D6E63),
    CULT("CULT", 0xFF9C27B0),
    EPIC("EPIC", 0xFFFF5722),
    QUICK("QUICK", 0xFFC6FF00),
    BINGE("BINGE", 0xFF00BCD4),
    SNACK("SNACK", 0xFFC6FF00),
    HORROR("HORROR", 0xFFE53935),
    THRILLER("THRILLER", 0xFF651FFF),
    ANIMAZIONE("ANIMAZIONE", 0xFFFF9800),
    SCI_FI("SCI-FI", 0xFF2962FF),
    COMEDY("COMEDY", 0xFFFFEA00),
    DOCU("DOCU", 0xFF9E9E9E),
    FAMILY("FAMILY", 0xFF81D4FA)
}

@Composable
fun MovieBadge.getLocalizedText(): String {
    return this.text
}
