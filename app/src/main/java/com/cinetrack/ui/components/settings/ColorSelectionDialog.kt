package com.cinetrack.ui.components.settings

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.scrollBy
import kotlinx.coroutines.isActive
import kotlinx.coroutines.delay
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.input.pointer.pointerInput
import com.cinetrack.ui.components.common.FlickTroveSwitch
import kotlin.math.roundToInt
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarOutline
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import com.cinetrack.util.VibrationHelper
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cinetrack.R
import com.cinetrack.ui.components.glass.hazeGlass
import com.cinetrack.ui.components.shared.ColorWheel
import com.cinetrack.ui.theme.*
import com.cinetrack.ui.utils.bounceClick
import com.cinetrack.ui.utils.premiumScrollbar
import com.cinetrack.ui.utils.verticalFadingEdges
import com.cinetrack.util.toComposeColor
import dev.chrisbanes.haze.HazeState
import com.cinetrack.ui.viewmodel.SettingsViewModel

@Composable
fun ColorSelectionDialog(
    current: String,
    onDismiss: () -> Unit,
    onSelect: (String, Offset) -> Unit
) {
    val focusManager = LocalFocusManager.current
    var tempSelectedColor by remember { mutableStateOf(current) }
    var isCustomMode by remember { mutableStateOf(current.startsWith("#")) }

    val previewAccentColor = remember(tempSelectedColor) {
        try {
            if (tempSelectedColor.startsWith("#")) tempSelectedColor.toComposeColor()
            else when(tempSelectedColor) {
                "Pink" -> NeonPink
                "Purple" -> NeonPurple
                "Amber" -> NeonAmber
                "Blue" -> NeonBlue
                else -> NeonTeal
            }
        } catch(e: Exception) { NeonTeal }
    }

    Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateContentSize(animationSpec = tween(400))
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.settings_interface_color),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                        color = Color.White
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Unified Presets Row with "+" for Custom
                    val presets = listOf(
                        "Teal" to NeonTeal,
                        "Pink" to NeonPink,
                        "Purple" to NeonPurple,
                        "Amber" to NeonAmber,
                        "Blue" to NeonBlue
                    )
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(24.dp))
                            .background(Color.White.copy(alpha = 0.05f))
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.Top
                    ) {
                        presets.forEach { (name, color) ->
                            val isSelected = !isCustomMode && tempSelectedColor == name
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .bounceClick { 
                                            isCustomMode = false
                                            tempSelectedColor = name
                                        }
                                        .clip(CircleShape)
                                        .background(if (isSelected) Color.White.copy(alpha = 0.1f) else Color.Transparent),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(color)
                                            .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                                    )
                                }
                                
                                Spacer(modifier = Modifier.height(4.dp))
                                
                                Text(
                                    text = name,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 10.sp
                                    ),
                                    color = if (isSelected) color else Color.White.copy(alpha = 0.4f)
                                )
                            }
                        }

                        // The "+" Button for Custom mode
                        val customSelected = isCustomMode
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .bounceClick { isCustomMode = true }
                                    .clip(CircleShape)
                                    .background(if (customSelected) Color.White.copy(alpha = 0.2f) else Color.Transparent)
                                    .border(
                                        width = if (customSelected) 2.dp else 0.dp,
                                        color = if (customSelected) Color.White else Color.Transparent,
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = ImageVector.vectorResource(id = R.drawable.ic_plus),
                                    contentDescription = stringResource(R.string.settings_custom),
                                    tint = if (customSelected) Color.White else Color.White.copy(alpha = 0.5f),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(4.dp))
                            
                            Text(
                                text = stringResource(R.string.settings_custom),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = if (customSelected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 10.sp
                                ),
                                color = if (customSelected) Color.White else Color.White.copy(alpha = 0.4f)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Animated Custom Section
                    AnimatedVisibility(
                        visible = isCustomMode,
                        enter = scaleIn(initialScale = 0.95f, animationSpec = tween(400)) + fadeIn(animationSpec = tween(400)),
                        exit = scaleOut(targetScale = 0.95f, animationSpec = tween(400)) + fadeOut(animationSpec = tween(400))
                    ) {
                        // Custom Color Section (Wheel + Hex)
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            var hexInput by remember(current) { 
                                mutableStateOf(if (current.startsWith("#")) current.uppercase() else "#00BCD4") 
                            }
                            var isHexFocused by remember { mutableStateOf(false) }
                            var isDragging by remember { mutableStateOf(false) }

                            // Sync hex ← wheel: only when drag ends and hex field not focused
                            LaunchedEffect(isDragging, tempSelectedColor) {
                                if (!isDragging && !isHexFocused && tempSelectedColor.length == 7) {
                                    hexInput = tempSelectedColor.uppercase()
                                }
                            }
                            
                            ColorWheel(
                                selectedColor = tempSelectedColor,
                                onColorChanged = { 
                                    tempSelectedColor = it
                                },
                                onInteractionStart = { 
                                    isDragging = true
                                    focusManager.clearFocus()
                                },
                                onInteractionEnd = { 
                                    isDragging = false
                                },
                                modifier = Modifier
                                    .size(200.dp)
                                    .padding(16.dp)
                            )
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            // Hex Input
                            Row(
                                modifier = Modifier
                                    .width(160.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.White.copy(alpha = 0.05f))
                                    .border(
                                        width = if (isHexFocused) 2.dp else 1.dp,
                                        color = if (isHexFocused) previewAccentColor else Color.White.copy(alpha = 0.1f),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "#",
                                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Black),
                                    color = Color.White.copy(alpha = 0.3f)
                                )
                                BasicTextField(
                                    value = if (hexInput.startsWith("#")) hexInput.substring(1) else hexInput,
                                    onValueChange = { newValue ->
                                        if (newValue.length <= 6) {
                                            val clean = newValue.uppercase().filter { it.isDigit() || it in 'A'..'F' }
                                            hexInput = clean
                                            // Update wheel only when we have a full valid hex
                                            if (clean.length == 6) {
                                                tempSelectedColor = "#$clean"
                                            }
                                        }
                                    },
                                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Start,
                                        letterSpacing = 1.sp
                                    ),
                                    cursorBrush = SolidColor(previewAccentColor),
                                    singleLine = true,
                                    modifier = Modifier
                                        .width(80.dp)
                                        .padding(start = 4.dp)
                                        .onFocusChanged { focusState ->
                                            isHexFocused = focusState.isFocused
                                            // When losing focus, sync hex display to current color
                                            if (!focusState.isFocused && tempSelectedColor.length == 7) {
                                                hexInput = tempSelectedColor.uppercase().removePrefix("#")
                                            }
                                        }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(28.dp))
                    
                    // Track the screen-space centre of the Conferma button
                    val confirmButtonCenter = remember { arrayOf(Offset.Zero) }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SettingsDialogCancelButton(
                            text = stringResource(R.string.settings_cancel),
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f)
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .onGloballyPositioned { coords ->
                                    val pos = coords.positionInWindow()
                                    confirmButtonCenter[0] = Offset(
                                        x = pos.x + coords.size.width / 2f,
                                        y = pos.y + coords.size.height / 2f
                                    )
                                }
                                .bounceClick {
                                    onSelect(tempSelectedColor, confirmButtonCenter[0])
                                    onDismiss()
                                }
                                .clip(RoundedCornerShape(16.dp))
                                .background(previewAccentColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.settings_confirm),
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = Color.Black
                            )
                        }
                    }
                }
}

