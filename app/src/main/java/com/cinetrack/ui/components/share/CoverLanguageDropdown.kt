package com.cinetrack.ui.components.share

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.cinetrack.R
import com.cinetrack.ui.components.glass.hazeGlass
import com.cinetrack.ui.utils.ColorUtils
import com.cinetrack.ui.utils.bounceClick
import dev.chrisbanes.haze.HazeState

@Composable
internal fun CountPill(
    count: Int,
    isSelected: Boolean,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(
                if (isSelected) accentColor.copy(alpha = 0.24f)
                else Color.White.copy(alpha = 0.09f)
            )
            .border(
                0.6.dp,
                if (isSelected) accentColor.copy(alpha = 0.60f)
                else Color.White.copy(alpha = 0.14f),
                RoundedCornerShape(50)
            )
            .padding(horizontal = 7.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = count.toString(),
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
            color = if (isSelected) ColorUtils.lightenForText(accentColor, 1.35f) else Color.White.copy(alpha = 0.75f),
            lineHeight = 11.sp
        )
    }
}

@Composable
internal fun LanguageMenuItem(
    text: String,
    count: Int,
    isSelected: Boolean,
    accentColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(42.dp)
            .bounceClick(scaleDown = 0.98f, onClick = onClick)
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) accentColor.copy(alpha = 0.12f) else Color.Transparent)
            .padding(horizontal = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            fontSize = 13.5.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) ColorUtils.lightenForText(accentColor, 1.35f) else Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false)
        )
        Spacer(modifier = Modifier.width(12.dp))
        CountPill(
            count = count,
            isSelected = isSelected,
            accentColor = accentColor
        )
    }
}

/**
 * Floating glassmorphic language dropdown for cover/logo selection.
 */
@Composable
internal fun CoverLanguageDropdown(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    dropdownTopDp: Dp,
    coverHazeState: HazeState,
    currentSelectedLanguage: String,
    currentTotalCount: Int,
    languages: List<String>,
    accentColor: Color,
    getLanguageCount: (String) -> Int,
    onLanguageSelected: (String) -> Unit
) {
    if (expanded) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .zIndex(50f)
                .pointerInput(Unit) {
                    detectTapGestures { onDismissRequest() }
                }
        )
    }

    AnimatedVisibility(
        visible = expanded,
        enter = fadeIn(tween(180)) + scaleIn(
            initialScale = 0.95f,
            transformOrigin = TransformOrigin(0.5f, 0f),
            animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
        ),
        exit = fadeOut(tween(130)) + scaleOut(
            targetScale = 0.95f,
            transformOrigin = TransformOrigin(0.5f, 0f),
            animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .offset(y = dropdownTopDp)
            .zIndex(60f)
    ) {
        val scrollState = rememberScrollState()
        val canScrollUp = scrollState.canScrollBackward
        val canScrollDown = scrollState.canScrollForward

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .requiredHeightIn(max = 280.dp)
                .shadow(20.dp, RoundedCornerShape(16.dp))
                .hazeGlass(
                    state = coverHazeState,
                    shape = RoundedCornerShape(16.dp),
                    blurRadius = 24.dp,
                    containerColor = Color(0xFF141418).copy(alpha = 0.60f),
                    borderWidth = 1.dp,
                    borderColor = Color.White.copy(alpha = 0.18f)
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                    .drawWithContent {
                        drawContent()
                        if (canScrollUp || canScrollDown) {
                            val topStop = if (canScrollUp) 0.08f else 0f
                            val bottomStop = if (canScrollDown) 0.92f else 1f
                            drawRect(
                                brush = Brush.verticalGradient(
                                    0f to (if (canScrollUp) Color.Transparent else Color.Black),
                                    topStop to Color.Black,
                                    bottomStop to Color.Black,
                                    1f to (if (canScrollDown) Color.Transparent else Color.Black)
                                ),
                                blendMode = BlendMode.DstIn
                            )
                        }
                    }
                    .verticalScroll(scrollState)
                    .padding(vertical = 6.dp)
            ) {
                // Opzione "Tutte le lingue"
                val isAllSelected = currentSelectedLanguage == "all"
                LanguageMenuItem(
                    text = stringResource(R.string.share_cover_picker_all_languages),
                    count = currentTotalCount,
                    isSelected = isAllSelected,
                    accentColor = accentColor,
                    onClick = {
                        onLanguageSelected("all")
                        onDismissRequest()
                    }
                )

                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp),
                    color = Color.White.copy(alpha = 0.08f),
                    thickness = 0.5.dp
                )

                for (code in languages) {
                    val count = getLanguageCount(code)
                    val isCurrent = currentSelectedLanguage.equals(code, ignoreCase = true)
                    LanguageMenuItem(
                        text = getLanguageDisplayName(code),
                        count = count,
                        isSelected = isCurrent,
                        accentColor = accentColor,
                        onClick = {
                            onLanguageSelected(code)
                            onDismissRequest()
                        }
                    )
                }
            }
        }
    }
}
