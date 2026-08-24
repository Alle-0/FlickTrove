package com.cinetrack.ui.components.stats

import android.graphics.RectF
import android.graphics.Region
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.cinetrack.R
import kotlinx.collections.immutable.ImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class CountryPathData(
    val id: String,
    val composePath: Path,
    val bounds: Rect,
    val androidBounds: RectF,
    val region: Region
)

@Composable
fun WorldHeatMap(
    countryCounts: ImmutableList<Pair<String, Int>>,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    var parsedPaths by remember { mutableStateOf<List<CountryPathData>>(emptyList()) }
    var worldBounds by remember { mutableStateOf(Rect.Zero) }

    val countsMap = remember(countryCounts) { countryCounts.toMap() }
    val maxCount = remember(countryCounts) { countryCounts.maxOfOrNull { it.second } ?: 1 }

    var scale by remember { mutableFloatStateOf(1f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }
    
    var selectedCountry by remember { mutableStateOf<String?>(null) }
    var selectedCount by remember { mutableStateOf(0) }
    var lastTapOffset by remember { mutableStateOf<Offset?>(null) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.Default) {
            var minX = Float.MAX_VALUE
            var minY = Float.MAX_VALUE
            var maxX = Float.MIN_VALUE
            var maxY = Float.MIN_VALUE

            val paths = WorldPaths.mapNotNull { (id, pathString) ->
                try {
                    val path = PathParser().parsePathString(pathString).toPath()
                    val bounds = path.getBounds()
                    
                    val androidPath = path.asAndroidPath()
                    val rectF = RectF()
                    androidPath.computeBounds(rectF, true)
                    
                    val region = Region()
                    region.setPath(
                        androidPath,
                        Region(rectF.left.toInt() - 1, rectF.top.toInt() - 1, rectF.right.toInt() + 1, rectF.bottom.toInt() + 1)
                    )

                    if (bounds.left < minX) minX = bounds.left
                    if (bounds.top < minY) minY = bounds.top
                    if (bounds.right > maxX) maxX = bounds.right
                    if (bounds.bottom > maxY) maxY = bounds.bottom

                    CountryPathData(id, path, bounds, rectF, region)
                } catch (e: Exception) {
                    null
                }
            }
            worldBounds = Rect(minX, minY, maxX, maxY)
            parsedPaths = paths
        }
    }

    Box(modifier = modifier) {
        if (parsedPaths.isNotEmpty()) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .clipToBounds()
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale = (scale * zoom).coerceIn(1f, 10f)
                            if (scale > 1f) {
                                panOffset += pan
                            } else {
                                panOffset = Offset.Zero
                            }
                        }
                    }
                    .pointerInput(Unit) {
                        detectTapGestures { tapOffset ->
                            lastTapOffset = tapOffset
                        }
                    }
            ) {
                if (worldBounds.width == 0f || worldBounds.height == 0f) return@Canvas

                val canvasWidth = size.width
                val canvasHeight = size.height

                val scaleX = canvasWidth / worldBounds.width
                val scaleY = canvasHeight / worldBounds.height
                val baseScale = minOf(scaleX, scaleY) * 0.95f

                val mapDrawWidth = worldBounds.width * baseScale
                val mapDrawHeight = worldBounds.height * baseScale
                val startX = (canvasWidth - mapDrawWidth) / 2f
                val startY = (canvasHeight - mapDrawHeight) / 2f

                // Hit testing
                lastTapOffset?.let { tap ->
                    lastTapOffset = null
                    
                    val pivotX = canvasWidth / 2f
                    val pivotY = canvasHeight / 2f
                    
                    val s1x = tap.x - panOffset.x
                    val s1y = tap.y - panOffset.y
                    
                    val s2x = (s1x - pivotX) / scale + pivotX
                    val s2y = (s1y - pivotY) / scale + pivotY
                    
                    val s3x = s2x - startX
                    val s3y = s2y - startY
                    
                    val s4x = s3x / baseScale
                    val s4y = s3y / baseScale
                    
                    val normalizedX = s4x + worldBounds.left
                    val normalizedY = s4y + worldBounds.top
                    
                    val xInt = normalizedX.toInt()
                    val yInt = normalizedY.toInt()

                    var found = false
                    for (country in parsedPaths) {
                        if (country.androidBounds.contains(normalizedX, normalizedY)) {
                            if (country.region.contains(xInt, yInt)) {
                                selectedCountry = country.id
                                selectedCount = countsMap[country.id] ?: 0
                                found = true
                                break
                            }
                        }
                    }
                    if (!found) {
                        selectedCountry = null
                    }
                }

                withTransform({
                    translate(left = panOffset.x, top = panOffset.y)
                    scale(scaleX = scale, scaleY = scale, pivot = Offset(canvasWidth / 2f, canvasHeight / 2f))
                    translate(left = startX, top = startY)
                    scale(scaleX = baseScale, scaleY = baseScale, pivot = Offset.Zero)
                    translate(left = -worldBounds.left, top = -worldBounds.top)
                }) {
                    parsedPaths.forEach { country ->
                        val count = countsMap[country.id] ?: 0
                        
                        val fillAlpha = if (count > 0) {
                            0.3f + (0.7f * (count.toFloat() / maxCount.toFloat()))
                        } else {
                            0.05f
                        }
                        
                        val fillColor = if (count > 0) accentColor.copy(alpha = fillAlpha) else Color.White.copy(alpha = fillAlpha)

                        drawPath(
                            path = country.composePath,
                            color = fillColor,
                            style = Fill
                        )
                        
                        drawPath(
                            path = country.composePath,
                            color = Color.Black.copy(alpha = 0.2f),
                            style = Stroke(width = 0.5f / baseScale / scale)
                        )
                        
                        if (selectedCountry == country.id) {
                            drawPath(
                                path = country.composePath,
                                color = Color.White,
                                style = Stroke(width = 2f / baseScale / scale)
                            )
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = selectedCountry != null,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp)
            ) {
                selectedCountry?.let { countryCode ->
                    val name = java.util.Locale.Builder().setLanguage("en").setRegion(countryCode).build().displayCountry
                    Box(
                        modifier = Modifier
                            .background(Color(0xFF1E1E1E).copy(alpha = 0.9f), RoundedCornerShape(12.dp))
                            .border(1.dp, accentColor.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = name,
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (selectedCount > 0) stringResource(R.string.stats_map_movies, selectedCount) else stringResource(R.string.stats_map_no_movies),
                                color = accentColor,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}
