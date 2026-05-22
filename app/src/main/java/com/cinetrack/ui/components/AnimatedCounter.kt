package com.cinetrack.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle

@Composable
fun AnimatedCounter(
    targetValue: Int,
    modifier: Modifier = Modifier,
    style: TextStyle = TextStyle.Default,
    color: Color = Color.White,
    onComplete: () -> Unit = {}
) {
    val haptic = LocalHapticFeedback.current
    var count by remember { mutableIntStateOf(0) }
    
    // Animation to increment count
    LaunchedEffect(targetValue) {
        if (count == targetValue) return@LaunchedEffect
        
        // Simple linear interpolation to reach targetValue
        val start = count
        val duration = 1000L
        val startTime = System.currentTimeMillis()
        
        while (System.currentTimeMillis() - startTime < duration) {
            val progress = (System.currentTimeMillis() - startTime).toFloat() / duration
            count = (start + (targetValue - start) * progress).toInt()
            kotlinx.coroutines.delay(16)
        }
        count = targetValue
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        onComplete()
    }

    val countString = count.toString()
    
    Row(modifier = modifier) {
        countString.forEachIndexed { index, char ->
            val digit = remember(char) { char }
            AnimatedContent(
                targetState = digit,
                transitionSpec = {
                    if (targetState > initialState) {
                        slideInVertically { it } + fadeIn() togetherWith slideOutVertically { -it } + fadeOut()
                    } else {
                        slideInVertically { -it } + fadeIn() togetherWith slideOutVertically { it } + fadeOut()
                    }.using(SizeTransform(clip = false))
                },
                label = "Digit$index"
            ) { targetChar ->
                Text(
                    text = targetChar.toString(),
                    style = style,
                    color = color,
                    softWrap = false
                )
            }
        }
    }
}
