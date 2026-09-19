package com.cinetrack.ui.components.comments

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.cinetrack.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun LiquidStarIcon(isLiked: Boolean, accentColor: Color, modifier: Modifier = Modifier) {
    val fillAnim = remember { androidx.compose.animation.core.Animatable(if (isLiked) 1f else 0f) }
    val splashAnim = remember { androidx.compose.animation.core.Animatable(0f) }
    val waveAnim = remember { androidx.compose.animation.core.Animatable(0f) }
    var isFirstComposition by remember { mutableStateOf(true) }

    LaunchedEffect(isLiked) {
        if (isFirstComposition) {
            isFirstComposition = false
            return@LaunchedEffect
        }
        
        if (isLiked) {
            launch {
                waveAnim.animateTo(
                    targetValue = 1f,
                    animationSpec = androidx.compose.animation.core.tween(durationMillis = 500, easing = androidx.compose.animation.core.LinearEasing)
                )
            }
            launch {
                fillAnim.animateTo(
                    targetValue = 1f,
                    animationSpec = androidx.compose.animation.core.tween(durationMillis = 400, easing = androidx.compose.animation.core.FastOutSlowInEasing)
                )
            }
            launch {
                delay(300)
                splashAnim.animateTo(
                    targetValue = 1f,
                    animationSpec = androidx.compose.animation.core.tween(durationMillis = 300, easing = androidx.compose.animation.core.LinearOutSlowInEasing)
                )
                splashAnim.snapTo(0f)
                waveAnim.snapTo(0f)
            }
        } else {
            fillAnim.snapTo(0f)
            splashAnim.snapTo(0f)
            waveAnim.snapTo(0f)
        }
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Icon(
            painter = painterResource(id = R.drawable.ic_star),
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.5f),
            modifier = Modifier.matchParentSize()
        )
        
        if (fillAnim.value > 0f) {
            Icon(
                painter = painterResource(id = R.drawable.ic_star_piena),
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier
                    .matchParentSize()
                    .drawWithContent {
                        val path = Path()
                        val width = this.size.width
                        val height = this.size.height
                        
                        val fillHeight = height * fillAnim.value
                        val waterY = height - fillHeight
                        
                        if (fillAnim.value < 1f) {
                            val waveHeight = 1.5.dp.toPx()
                            val wavePhase = waveAnim.value * Math.PI * 4 
                            
                            path.moveTo(0f, height)
                            path.lineTo(0f, waterY)
                            
                            for (x in 0..width.toInt() step 2) {
                                val yOffset = kotlin.math.sin((x / width) * Math.PI * 2 + wavePhase).toFloat() * waveHeight
                                path.lineTo(x.toFloat(), waterY + yOffset)
                            }
                            
                            path.lineTo(width, waterY)
                            path.lineTo(width, height)
                            path.close()
                            
                            clipPath(path) {
                                this@drawWithContent.drawContent()
                            }
                        } else {
                            this@drawWithContent.drawContent()
                        }
                    }
            )
        }

        if (splashAnim.value > 0f && splashAnim.value < 1f) {
            Canvas(modifier = Modifier.fillMaxSize().scale(2f)) {
                val center = androidx.compose.ui.geometry.Offset(this.size.width / 2, this.size.height / 2)
                val maxRadius = this.size.width / 2
                
                for (i in 0 until 5) {
                    val angle = (i * (360f / 5) - 90f) * (Math.PI / 180f).toFloat()
                    val distance = maxRadius * (0.5f + splashAnim.value * 0.8f)
                    
                    val dropCenter = androidx.compose.ui.geometry.Offset(
                        x = center.x + kotlin.math.cos(angle.toDouble()).toFloat() * distance,
                        y = center.y + kotlin.math.sin(angle.toDouble()).toFloat() * distance
                    )
                    
                    val dropRadius = (2.dp.toPx()) * (1f - splashAnim.value)
                    
                    drawCircle(
                        color = accentColor.copy(alpha = 1f - splashAnim.value),
                        radius = dropRadius,
                        center = dropCenter
                    )
                }
            }
        }
    }
}
