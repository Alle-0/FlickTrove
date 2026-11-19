package com.cinetrack.ui.components.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cinetrack.ui.theme.PrimaryTeal

/**
 * Premium animated input text field with floating label, animated borders,
 * icons, password masking, and error status feedback.
 */
@Composable
fun PremiumAuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    errorText: String? = null,
    isPassword: Boolean = false,
    enabled: Boolean = true,
    trailingIcon: (@Composable () -> Unit)? = null
) {
    var isFocused by remember { mutableStateOf(false) }
    val isFloating = isFocused || value.isNotEmpty()
    val density = LocalDensity.current

    val labelOffsetY by animateDpAsState(
        targetValue = if (isFloating) 3.dp else 39.dp,
        animationSpec = spring(
            stiffness = Spring.StiffnessMediumLow,
            dampingRatio = Spring.DampingRatioLowBouncy
        ),
        label = "LabelY"
    )
    val labelOffsetX by animateDpAsState(
        targetValue = if (isFloating) 20.dp else 52.dp,
        animationSpec = spring(
            stiffness = Spring.StiffnessMediumLow,
            dampingRatio = Spring.DampingRatioLowBouncy
        ),
        label = "LabelX"
    )
    val labelScale by animateFloatAsState(
        targetValue = if (isFloating) 0.75f else 1f,
        animationSpec = spring(
            stiffness = Spring.StiffnessMediumLow,
            dampingRatio = Spring.DampingRatioLowBouncy
        ),
        label = "LabelScale"
    )
    val labelColor by animateColorAsState(
        targetValue = when {
            isError -> Color(0xFFE57373)
            isFocused -> PrimaryTeal
            isFloating -> Color.White.copy(alpha = 0.9f)
            else -> Color.White.copy(alpha = 0.7f)
        },
        label = "LabelColor"
    )
    val borderColor by animateColorAsState(
        targetValue = when {
            isError -> Color(0xFFE57373).copy(alpha = 0.7f)
            isFocused || value.isNotEmpty() -> PrimaryTeal.copy(alpha = 0.8f)
            else -> Color.White.copy(alpha = 0.25f)
        },
        label = "BorderColor"
    )
    val containerColor by animateColorAsState(
        targetValue = if (isFocused) Color.White.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.04f),
        label = "ContainerColor"
    )

    Box(modifier = modifier.fillMaxWidth()) {
        // Field container
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 20.dp)
                .height(58.dp)
                .background(color = containerColor, shape = CircleShape)
                .border(1.5.dp, borderColor, CircleShape)
                .padding(horizontal = 20.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxSize()
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = when {
                        isError -> Color(0xFFE57373)
                        isFocused || value.isNotEmpty() -> PrimaryTeal
                        else -> Color.White.copy(alpha = 0.6f)
                    },
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier
                        .weight(1f)
                        .onFocusChanged { isFocused = it.isFocused },
                    textStyle = TextStyle(
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    cursorBrush = SolidColor(PrimaryTeal),
                    visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
                    enabled = enabled,
                    singleLine = true
                )
                if (trailingIcon != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    trailingIcon()
                }
            }
        }

        // Error text below the field
        AnimatedVisibility(
            visible = isError && !errorText.isNullOrEmpty(),
            enter = scaleIn(initialScale = 0.95f) + fadeIn(),
            exit = scaleOut(targetScale = 0.95f) + fadeOut(),
            modifier = Modifier.padding(top = 82.dp, start = 16.dp)
        ) {
            Text(
                text = errorText ?: "",
                color = Color(0xFFE57373),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }

        // Animated Floating Label
        Text(
            text = label,
            color = labelColor,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            modifier = Modifier.graphicsLayer {
                scaleX = labelScale
                scaleY = labelScale
                translationX = with(density) { labelOffsetX.toPx() }
                translationY = with(density) { labelOffsetY.toPx() }
                transformOrigin = TransformOrigin(0f, 0f)
            }
        )
    }
}
