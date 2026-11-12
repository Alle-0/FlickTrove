package com.cinetrack.ui.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/**
 * Mirrors directional graphics horizontally when the active layout direction is RTL.
 */
@Composable
fun Modifier.mirrorInRtl(): Modifier {
    return if (LocalLayoutDirection.current == LayoutDirection.Rtl) {
        this.scale(scaleX = -1f, scaleY = 1f)
    } else {
        this
    }
}

/**
 * Returns [ltrSpacing] for LTR scripts, or `0.sp` when rendering Arabic/RTL scripts
 * so cursive glyph connections and ligatures are never broken.
 */
@Composable
fun rtlSafeLetterSpacing(ltrSpacing: TextUnit): TextUnit {
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val isArabic = LocalConfiguration.current.locales.let { !it.isEmpty && it[0]?.language == "ar" }
    return if (isRtl || isArabic) 0.sp else ltrSpacing
}

/**
 * Converts between a logical segment index (`0 until count`) and its physical left-to-right
 * visual slot inside an RTL-mirrored `Row`. Negative indices are returned unchanged.
 */
fun rtlMirrorIndex(index: Int, count: Int, isRtl: Boolean): Int {
    if (index < 0 || count <= 0) return index
    return if (isRtl) (count - 1 - index).coerceIn(0, count - 1) else index.coerceIn(0, count - 1)
}

/**
 * Returns `1` in LTR or `-1` in RTL so physical pixel `slideInHorizontally` / `slideOutHorizontally`
 * transitions follow the active layout direction (entering from start-side, exiting toward start-side on back).
 */
@Composable
fun rtlSlideDirection(): Int = if (LocalLayoutDirection.current == LayoutDirection.Rtl) -1 else 1
