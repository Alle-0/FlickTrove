package com.cinetrack.ui.components.stats

import com.cinetrack.R
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.layer.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.cinetrack.ui.utils.bounceClick
import com.cinetrack.util.buildTmdbImageUrl
import com.cinetrack.util.ImageType
import com.cinetrack.util.ImageQuality
import com.cinetrack.util.LocalImageQuality
import com.cinetrack.ui.viewmodel.CalculatedStats

// ════════════════════════════════════════════════════════════════════
// Wrapped Banner Pill Component
// ════════════════════════════════════════════════════════════════════

@Composable
fun WrappedBannerPill(
    stats: CalculatedStats,
    year: Int,
    graphicsLayer: androidx.compose.ui.graphics.layer.GraphicsLayer,
    isSharing: Boolean = false,
    onShare: () -> Unit
) {
    var expanded by rememberSaveable { mutableStateOf(false) }

    // Animate shape parameters for a smooth transition
    val cutoutRadius by animateDpAsState(
        targetValue = if (expanded) 16.dp else 10.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium),
        label = "cutout"
    )
    val cornerRadius by animateDpAsState(
        targetValue = if (expanded) 32.dp else 18.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium),
        label = "corner"
    )

    // Get theme colors in composable context
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val tertiary = MaterialTheme.colorScheme.tertiary

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .drawWithContent {
                // Draw the content normally on screen
                drawContent()
                
                // Record content for sharing at native screen density for maximum crispness
                graphicsLayer.record(
                    size = androidx.compose.ui.unit.IntSize(
                        size.width.toInt(), 
                        size.height.toInt()
                    )
                ) {
                    this@drawWithContent.drawContent()
                }
            }
            .graphicsLayer {
                // Use block version for compatibility
                clip = true
                shape = TicketShape(
                    cutoutRadius = cutoutRadius,
                    cornerRadius = cornerRadius
                )
            }
            .background(Color(0xFF0A0A0C))
            .drawBehind {
                // Use drawBehind for better compatibility and performance
                drawRect(color = Color(0xFF0A0A0C))
                
                // Nebula Mesh Effect
                drawRect(
                    Brush.radialGradient(
                        0.0f to primary.copy(alpha = 0.7f),
                        1.0f to Color.Transparent,
                        center = androidx.compose.ui.geometry.Offset(this.size.width * 0.15f, this.size.height * 0.2f),
                        radius = this.size.maxDimension * 0.85f
                    )
                )
                drawRect(
                    Brush.radialGradient(
                        0.0f to secondary.copy(alpha = 0.7f),
                        1.0f to Color.Transparent,
                        center = androidx.compose.ui.geometry.Offset(this.size.width * 0.85f, this.size.height * 0.45f),
                        radius = this.size.maxDimension * 0.75f
                    )
                )
                drawRect(
                    Brush.radialGradient(
                        0.0f to tertiary.copy(alpha = 0.75f),
                        1.0f to Color.Transparent,
                        center = androidx.compose.ui.geometry.Offset(this.size.width * 0.45f, this.size.height * 0.85f),
                        radius = this.size.maxDimension * 0.95f
                    )
                )
            }
            .border(
                width = 1.2.dp, 
                color = Color.White.copy(alpha = 0.2f), 
                shape = TicketShape(
                    cutoutRadius = cutoutRadius,
                    cornerRadius = cornerRadius
                )
            )
    ) {
        // Subtle animated background particles
        Canvas(modifier = Modifier.matchParentSize()) {
            val count = 20
            repeat(count) { i ->
                val x = (i * 15345.67f) % size.width
                val y = (i * 88765.43f) % size.height
                val alpha = 0.1f + (i % 5) * 0.02f
                drawCircle(
                    color = Color.White.copy(alpha = alpha),
                    radius = 2f + (i % 4),
                    center = androidx.compose.ui.geometry.Offset(x, y)
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 36.dp, vertical = if (expanded) 32.dp else 12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .bounceClick { expanded = !expanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {

                    Text(
                        text = stringResource(R.string.stats_wrapped, year),
                        style = (if (expanded) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.headlineSmall).copy(
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            letterSpacing = 1.5.sp
                        )
                    )
                }

                if (!isSharing) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(50)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (expanded) ImageVector.vectorResource(id = R.drawable.ic_left) else ImageVector.vectorResource(id = R.drawable.ic_right),
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }



            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn(tween(300)) + expandVertically(spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium)),
                exit = fadeOut(tween(200)) + shrinkVertically(spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium))
            ) {
                Column {
                    Spacer(Modifier.height(20.dp))
                    
                    // Main Hero Stat - Total Movies
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = (stats.moviesWatched + stats.tvWatched).toString(),
                            style = MaterialTheme.typography.displayLarge.copy(
                                fontWeight = FontWeight.Black,
                                fontSize = 72.sp,
                                color = Color.White,
                                letterSpacing = (-2).sp
                            )
                        )
                        Text(
                            text = stringResource(R.string.stats_titles_watched),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Black,
                                color = Color.White.copy(alpha = 0.6f),
                                letterSpacing = 2.sp,
                                fontSize = 10.sp
                            ),
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                    
                    Spacer(Modifier.height(28.dp))
                    
                    // Two Main Stats Row - Genere + Tempo
                    Row(
                        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        WrappedMainStat(stringResource(R.string.stats_top_genre), stats.topGenre ?: "---", ImageVector.vectorResource(id = R.drawable.ic_bacchetta), Modifier.weight(1f).fillMaxHeight())
                        WrappedMainStat(stringResource(R.string.stats_total_time_wrapped), stats.totalTimeFormatted, ImageVector.vectorResource(id = R.drawable.ic_clock), Modifier.weight(1f).fillMaxHeight())
                    }
                    
                    Spacer(Modifier.height(20.dp))
                    
                    // Highlight Box - Top Actor
                    val topActor = stats.topCast.firstOrNull()
                    if (topActor != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(TicketShape(cutoutRadius = 8.dp, cornerRadius = 24.dp))
                                .background(
                                    Brush.verticalGradient(
                                        0f to primary.copy(alpha = 0.3f),
                                        1f to secondary.copy(alpha = 0.1f)
                                    )
                                )
                                .border(1.2.dp, Color.White.copy(alpha = 0.15f), TicketShape(cutoutRadius = 8.dp, cornerRadius = 24.dp))
                                .padding(horizontal = 20.dp, vertical = 16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Actor Image
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(RoundedCornerShape(50))
                                        .background(Color.White.copy(alpha = 0.12f))
                                        .border(2.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(50)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (!topActor.profilePath.isNullOrEmpty()) {
                                        AsyncImage(
                                            model = buildTmdbImageUrl(topActor.profilePath, ImageType.PROFILE, LocalImageQuality.current),
                                            contentDescription = topActor.name,
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(RoundedCornerShape(50)),
                                            contentScale = ContentScale.Crop,
                                            onError = { /* fallback to icon */ }
                                        )
                                    } else {
                                        Icon(
                                            imageVector = ImageVector.vectorResource(id = R.drawable.ic_persona),
                                            contentDescription = null,
                                            tint = Color.White.copy(alpha = 0.5f),
                                            modifier = Modifier.size(32.dp)
                                        )
                                    }
                                }
                                
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stringResource(R.string.stats_year_star, year),
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.Black,
                                            color = Color.White.copy(alpha = 0.5f),
                                            letterSpacing = 1.5.sp,
                                            fontSize = 8.sp
                                        )
                                    )
                                    Text(
                                        text = topActor.name,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Black,
                                            color = Color.White,
                                            fontSize = 16.sp
                                        ),
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                
                                Icon(
                                    imageVector = ImageVector.vectorResource(id = R.drawable.ic_star),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                    
                    Spacer(Modifier.height(20.dp))
                    
                    // Secondary Stats - Film, Serie, Star Preferita, Regista Top
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            WrappedSmallStat(stringResource(R.string.stats_movies), stats.moviesWatched.toString(), ImageVector.vectorResource(id = R.drawable.ic_ciack), Modifier.weight(1f).fillMaxHeight())
                            WrappedSmallStat(stringResource(R.string.stats_series), stats.tvWatched.toString(), ImageVector.vectorResource(id = R.drawable.ic_tv), Modifier.weight(1.1f).fillMaxHeight())
                            WrappedSmallStat(stringResource(R.string.stats_top_directors), stats.topDirectors.firstOrNull()?.name ?: "---", ImageVector.vectorResource(id = R.drawable.ic_videocamera), Modifier.weight(1.9f).fillMaxHeight())
                        }
                    }

                    Spacer(Modifier.height(24.dp))
                    
                    // Branding
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                painter = androidx.compose.ui.res.painterResource(id = com.cinetrack.R.drawable.ic_launcher_foreground_vector),
                                contentDescription = "Logo",
                                tint = Color.Unspecified,
                                modifier = Modifier.size(28.dp).offset(y = (-1).dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "FLICKTROVE",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Black,
                                    color = Color.White,
                                    letterSpacing = 3.5.sp,
                                    fontSize = 12.sp
                                )
                            )
                        }
                        Text(
                            text = stringResource(R.string.stats_cinephile_legacy),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Light,
                                color = Color.White.copy(alpha = 0.5f),
                                letterSpacing = 2.sp,
                                fontSize = 8.sp
                            )
                        )
                    }

                    if (!isSharing) {
                        Spacer(Modifier.height(20.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = { onShare() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.White.copy(alpha = 0.25f),
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f)),
                                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                            ) {
                                Icon(ImageVector.vectorResource(id = R.drawable.ic_sparkle), null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(10.dp))
                                Text(stringResource(R.string.stats_share_card), style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black))
                            }
                        }
                    }
                }
            }
        }
    }
}

fun shareBitmap(context: android.content.Context, bitmap: android.graphics.Bitmap, title: String) {
    try {
        val cachePath = java.io.File(context.cacheDir, "images")
        if (!cachePath.exists()) cachePath.mkdirs()
        
        val imageFile = java.io.File(cachePath, "wrapped_stats_${System.currentTimeMillis()}.png")
        val stream = java.io.FileOutputStream(imageFile)
        
        // Convert hardware bitmap to software before compressing
        val softwareBitmap = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O && bitmap.config == android.graphics.Bitmap.Config.HARDWARE) {
            bitmap.copy(android.graphics.Bitmap.Config.ARGB_8888, false)
        } else {
            bitmap
        }
        
        softwareBitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, stream)
        stream.flush()
        stream.close()

        val contentUri = androidx.core.content.FileProvider.getUriForFile(
            context, 
            "${context.packageName}.fileprovider", 
            imageFile
        )

        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(android.content.Intent.EXTRA_STREAM, contentUri)
            putExtra(android.content.Intent.EXTRA_SUBJECT, title)
            putExtra(android.content.Intent.EXTRA_TEXT, context.getString(R.string.stats_share_text, title))
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        
        val chooser = android.content.Intent.createChooser(intent, context.getString(R.string.stats_share_chooser_title))
        chooser.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    } catch (e: Exception) {
        e.printStackTrace()
        throw e // Rethrow to allow fallback to text sharing in the caller
    }
}

@Composable
private fun WrappedMainStat(label: String, value: String, icon: ImageVector, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(Color.White.copy(alpha = 0.12f))
            .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(18.dp))
            .padding(horizontal = 12.dp, vertical = 14.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxHeight()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Black,
                        color = Color.White.copy(alpha = 0.6f),
                        letterSpacing = 0.5.sp,
                        fontSize = 9.sp,
                        lineHeight = 11.sp
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    fontSize = 16.sp,
                    letterSpacing = (-0.5).sp,
                    lineHeight = 18.sp
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun WrappedSmallStat(label: String, value: String, icon: ImageVector, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(14.dp))
            .padding(horizontal = 10.dp, vertical = 12.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.size(12.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Black,
                        color = Color.White.copy(alpha = 0.5f),
                        letterSpacing = 0.5.sp,
                        fontSize = 9.sp,
                        lineHeight = 11.sp
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    fontSize = when {
                        value.length > 25 -> 10.sp
                        value.length > 18 -> 11.sp
                        value.length > 12 -> 12.sp
                        else -> 14.sp
                    },
                    lineHeight = 14.sp
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun WrappedMiniStat(label: String, value: String, icon: ImageVector, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(Color.White.copy(alpha = 0.18f))
            .border(1.2.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(24.dp))
            .drawBehind {
                // Glass shine effect
                drawRect(
                    brush = Brush.linearGradient(
                        0f to Color.Transparent,
                        0.5f to Color.White.copy(alpha = 0.03f),
                        1f to Color.Transparent,
                        start = androidx.compose.ui.geometry.Offset(0f, 0f),
                        end = androidx.compose.ui.geometry.Offset(size.width, size.height)
                    )
                )
            }
            .padding(14.dp)
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Black,
                        color = Color.White.copy(alpha = 0.5f),
                        letterSpacing = 1.2.sp,
                        fontSize = 9.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    fontSize = if (value.length > 12) 14.sp else 18.sp,
                    letterSpacing = (-0.5).sp
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
