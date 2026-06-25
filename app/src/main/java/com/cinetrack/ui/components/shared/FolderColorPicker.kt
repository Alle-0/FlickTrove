package com.cinetrack.ui.components.shared

import androidx.compose.ui.res.stringResource
import com.cinetrack.R

import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import android.graphics.Color.HSVToColor
import android.graphics.Color.colorToHSV
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.scaleIn
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
// Removed conflicting imports
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import com.cinetrack.ui.utils.bounceClick
import androidx.compose.ui.platform.LocalFocusManager
import com.cinetrack.util.toComposeColor
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

@Composable
fun FolderColorPicker(
    selectedColor: String,
    onColorSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val presets = listOf("#6366F1", "#EC4899", "#F59E0B", "#10B981", "#8B5CF6", "#EF4444")
    var isCustomMode by remember { mutableStateOf(!presets.contains(selectedColor.uppercase())) }
    
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.color_picker_title),
            style = MaterialTheme.typography.labelMedium,
            color = Color.White.copy(alpha = 0.4f),
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            presets.forEach { colorHex ->
                val color = colorHex.toComposeColor()
                val isSelected = !isCustomMode && selectedColor.uppercase() == colorHex.uppercase()
                
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(color)
                        .border(
                            width = if (isSelected) 4.dp else 1.dp,
                            color = if (isSelected) Color.White else color.copy(alpha = 0.2f),
                            shape = CircleShape
                        )
                        .bounceClick { 
                            isCustomMode = false
                            onColorSelected(colorHex)
                        }
                )
            }
            
            // Custom Color Button
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(
                        if (isCustomMode) selectedColor.toComposeColor()
                        else Color.White.copy(alpha = 0.1f)
                    )
                    .border(
                        width = if (isCustomMode) 4.dp else 1.dp,
                        color = if (isCustomMode) Color.White else Color.White.copy(alpha = 0.1f),
                        shape = CircleShape
                    )
                    .bounceClick { isCustomMode = true },
                contentAlignment = Alignment.Center
            ) {
                if (!isCustomMode) {
                    Icon(
                        imageVector = ImageVector.vectorResource(id = R.drawable.ic_plus),
                        contentDescription = "Custom Color",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
        
        AnimatedVisibility(
            visible = isCustomMode,
            enter = fadeIn() + scaleIn(initialScale = 0.95f),
            exit = fadeOut() + scaleOut(targetScale = 0.95f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                var localColor by remember { mutableStateOf(selectedColor) }
                var hexInput by remember { 
                    mutableStateOf(if (selectedColor.startsWith("#")) selectedColor.uppercase().removePrefix("#") else "6366F1") 
                }
                var isHexFocused by remember { mutableStateOf(false) }
                var isDragging by remember { mutableStateOf(false) }

                val focusManager = LocalFocusManager.current

                // Sync hex ← wheel: only when drag ends and hex field not focused
                androidx.compose.runtime.LaunchedEffect(isDragging, localColor) {
                    if (!isDragging && !isHexFocused && localColor.length == 7) {
                        hexInput = localColor.uppercase().removePrefix("#")
                    }
                }
                
                ColorWheel(
                    selectedColor = localColor,
                    onColorChanged = { localColor = it },
                    onInteractionStart = { 
                        isDragging = true
                        focusManager.clearFocus()
                    },
                    onInteractionEnd = { 
                        isDragging = false
                        onColorSelected(localColor)
                    },
                    modifier = Modifier.size(160.dp)
                )
                
                Spacer(Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier
                        .width(160.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.05f))
                        .border(
                            width = if (isHexFocused) 2.dp else 1.dp,
                            color = if (isHexFocused) Color.White else Color.White.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "#",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Black),
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
                                    val newHex = "#$clean"
                                    localColor = newHex
                                    onColorSelected(newHex)
                                }
                            }
                        },
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            color = Color.White,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Start,
                            letterSpacing = 1.sp
                        ),
                        cursorBrush = SolidColor(Color.White),
                        singleLine = true,
                        modifier = Modifier
                            .width(80.dp)
                            .padding(start = 4.dp)
                            .onFocusChanged { focusState ->
                                isHexFocused = focusState.isFocused
                                // When losing focus, sync hex display to current color
                                if (!focusState.isFocused && localColor.length == 7) {
                                    hexInput = localColor.uppercase().removePrefix("#")
                                }
                            }
                    )
                }
            }
        }
    }
}
