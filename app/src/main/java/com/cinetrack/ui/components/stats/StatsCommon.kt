package com.cinetrack.ui.components.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import com.cinetrack.R
import com.cinetrack.ui.viewmodel.CalculatedStats
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

// ──────────────────────────────────────────────────────────────────────
// Custom Stats Card Modifier (No Blur)
// ──────────────────────────────────────────────────────────────────────
fun Modifier.statsCard(
    shape: Shape = RoundedCornerShape(20.dp)
): Modifier = this.then(
    Modifier
        .background(Color.White.copy(alpha = 0.06f), shape)
        .border(1.dp, Color.White.copy(alpha = 0.15f), shape)
        .clip(shape)
)

class TicketShape(
    private val cutoutRadius: Dp,
    private val cornerRadius: Dp = 32.dp
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val path = Path().apply {
            val rPx = with(density) { cutoutRadius.toPx() }.coerceAtMost(size.height / 3.5f)
            val crPx = with(density) { cornerRadius.toPx() }.coerceAtMost(size.height / 2.2f)
            
            // Further safety: ensure corner + cutout don't overlap with a small gap
            val safeRPx = if (crPx + rPx > size.height / 2f) {
                (size.height / 2f - crPx).coerceAtLeast(size.height * 0.05f)
            } else {
                rPx
            }
            
            // Start top-left
            moveTo(crPx, 0f)
            lineTo(size.width - crPx, 0f)
            // Top-right corner
            arcTo(
                rect = Rect(size.width - 2 * crPx, 0f, size.width, 2 * crPx),
                startAngleDegrees = 270f,
                sweepAngleDegrees = 90f,
                forceMoveTo = false
            )
            
            // Right edge cutout
            lineTo(size.width, size.height / 2f - safeRPx)
            arcTo(
                rect = Rect(size.width - safeRPx, size.height / 2f - safeRPx, size.width + safeRPx, size.height / 2f + safeRPx),
                startAngleDegrees = 270f,
                sweepAngleDegrees = -180f,
                forceMoveTo = false
            )
            
            lineTo(size.width, size.height - crPx)
            // Bottom-right corner
            arcTo(
                rect = Rect(size.width - 2 * crPx, size.height - 2 * crPx, size.width, size.height),
                startAngleDegrees = 0f,
                sweepAngleDegrees = 90f,
                forceMoveTo = false
            )
            
            lineTo(crPx, size.height)
            // Bottom-left corner
            arcTo(
                rect = Rect(0f, size.height - 2 * crPx, 2 * crPx, size.height),
                startAngleDegrees = 90f,
                sweepAngleDegrees = 90f,
                forceMoveTo = false
            )
            
            // Left edge cutout
            lineTo(0f, size.height / 2f + safeRPx)
            arcTo(
                rect = Rect(-safeRPx, size.height / 2f - safeRPx, safeRPx, size.height / 2f + safeRPx),
                startAngleDegrees = 90f,
                sweepAngleDegrees = -180f,
                forceMoveTo = false
            )
            
            lineTo(0f, crPx)
            // Top-left corner
            arcTo(
                rect = Rect(0f, 0f, 2 * crPx, 2 * crPx),
                startAngleDegrees = 180f,
                sweepAngleDegrees = 90f,
                forceMoveTo = false
            )
            close()
        }
        return Outline.Generic(path)
    }
}

// ── Helper Share Function ───────────────────────────────────────────
fun shareStats(context: android.content.Context, stats: CalculatedStats, title: String) {
    val message = buildString {
        append(context.getString(R.string.stats_share_line_1, title))
        append(context.getString(R.string.stats_share_line_2, stats.moviesWatched, stats.tvWatched))
        append(context.getString(R.string.stats_share_line_3, stats.totalTimeFormatted))
        
        val topGenre = stats.genreCounts.firstOrNull()?.first
        if (topGenre != null) {
            append(context.getString(R.string.stats_share_line_genre, topGenre))
        }
        
        val topActor = stats.topCast.firstOrNull()?.name
        if (topActor != null) {
            append(context.getString(R.string.stats_share_line_actor, topActor))
        }

        val topDirector = stats.topDirectors.firstOrNull()?.name
        if (topDirector != null) {
            append(context.getString(R.string.stats_share_line_director, topDirector))
        }

        val topDecade = stats.decadeCounts.maxByOrNull { it.second }?.first
        if (topDecade != null) {
            append(context.getString(R.string.stats_share_line_decade, topDecade))
        }

        append(context.getString(R.string.stats_share_line_outro))
    }

    val sendIntent = android.content.Intent().apply {
        action = android.content.Intent.ACTION_SEND
        putExtra(android.content.Intent.EXTRA_TEXT, message)
        type = "text/plain"
    }
    val chooser = android.content.Intent.createChooser(sendIntent, context.getString(R.string.stats_share_title_msg))
    chooser.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(chooser)
}
