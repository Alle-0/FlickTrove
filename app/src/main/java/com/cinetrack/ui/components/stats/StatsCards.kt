package com.cinetrack.ui.components.stats

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cinetrack.R
import com.cinetrack.ui.utils.bounceClick

// ════════════════════════════════════════════════════════════════════
// Section Header
// ════════════════════════════════════════════════════════════════════

@Composable
fun StatsSectionHeader(
    icon: ImageVector,
    title: String,
    count: Int?,
    actionLabel: String? = null,
    onActionClick: (() -> Unit)? = null
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 14.dp)
    ) {
        // Icon inside a rounded pill box
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(14.dp))
        Text(
            title,
            color = MaterialTheme.colorScheme.primary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.5.sp,
            modifier = Modifier.weight(1f)
        )
        
        if (actionLabel != null && onActionClick != null) {
            Row(
                modifier = Modifier
                    .bounceClick { onActionClick() }
                    .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = actionLabel,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary,
                    letterSpacing = 1.sp
                )
                Spacer(Modifier.width(4.dp))
                Icon(
                    imageVector = if (actionLabel.contains("MENO", ignoreCase = true)) ImageVector.vectorResource(id = R.drawable.ic_left) else ImageVector.vectorResource(id = R.drawable.ic_right),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(14.dp)
                )
            }
        } else if (count != null) {
            CountingText(target = count, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), fontSize = 11.sp, fontWeight = FontWeight.Black)
        }
    }
}

// ════════════════════════════════════════════════════════════════════
// Mini stat card
// ════════════════════════════════════════════════════════════════════

@Composable
fun MiniStatCard(
    label: String,
    value: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .statsCard(RoundedCornerShape(24.dp))
    ) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
            Column {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        value,
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-0.5).sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(label, color = Color.White.copy(alpha = 0.3f), fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
            }
        }
    }
}

// ════════════════════════════════════════════════════════════════════
// Animated counting number
// ════════════════════════════════════════════════════════════════════

@Composable
fun CountingText(
    target: Int,
    color: Color,
    fontSize: androidx.compose.ui.unit.TextUnit,
    fontWeight: FontWeight,
    letterSpacing: androidx.compose.ui.unit.TextUnit = 0.sp,
    modifier: Modifier = Modifier,
    suffix: String? = null,
    suffixFontSize: androidx.compose.ui.unit.TextUnit = fontSize,
    textAlign: TextAlign? = null
) {
    var currentValue by rememberSaveable { mutableStateOf(0) }
    LaunchedEffect(target) {
        currentValue = target
    }

    val animatedValue by animateIntAsState(
        targetValue = currentValue,
        animationSpec = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
        label = "count"
    )
    if (suffix != null) {
        Text(
            text = buildAnnotatedString {
                withStyle(SpanStyle(color = color, fontSize = fontSize, fontWeight = fontWeight, letterSpacing = letterSpacing)) {
                    append(animatedValue.toString())
                }
                withStyle(SpanStyle(color = color.copy(alpha = 0.5f), fontSize = suffixFontSize, fontWeight = FontWeight.Medium)) {
                    append(" $suffix")
                }
            },
            modifier = modifier.wrapContentSize(unbounded = true),
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Visible,
            textAlign = textAlign,
            lineHeight = fontSize
        )
    } else {
        Text(
            text = animatedValue.toString(),
            color = color,
            fontSize = fontSize,
            fontWeight = fontWeight,
            letterSpacing = letterSpacing,
            modifier = modifier,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Visible,
            textAlign = textAlign,
            lineHeight = fontSize
        )
    }
}
