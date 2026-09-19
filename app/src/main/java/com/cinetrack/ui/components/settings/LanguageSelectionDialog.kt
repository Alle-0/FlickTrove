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
fun LanguageSelectionDialog(
    current: String,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit,
    accentColor: Color,
    vibrationEnabled: Boolean
) {
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    val context = androidx.compose.ui.platform.LocalContext.current

    Column(
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.settings_language),
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
            color = Color.White
        )
        Spacer(modifier = Modifier.height(16.dp))
        
        val options = listOf(
            "system" to stringResource(R.string.settings_language_system),
            "en" to stringResource(R.string.settings_language_en),
            "it" to stringResource(R.string.settings_language_it),
            "es" to stringResource(R.string.settings_language_es),
            "fr" to stringResource(R.string.settings_language_fr),
            "de" to stringResource(R.string.settings_language_de),
            "pt" to stringResource(R.string.settings_language_pt),
            "ru" to stringResource(R.string.settings_language_ru),
            "hi" to stringResource(R.string.settings_language_hi),
            "ja" to stringResource(R.string.settings_language_ja),
            "ko" to stringResource(R.string.settings_language_ko),
            "zh" to stringResource(R.string.settings_language_zh),
            "id" to stringResource(R.string.settings_language_id),
            "tr" to stringResource(R.string.settings_language_tr)
        )
        
        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 420.dp)
                .verticalFadingEdges(scrollState, topEdgeHeight = 24.dp, bottomEdgeHeight = 28.dp)
                .verticalScroll(scrollState)
        ) {
            options.forEach { (value, label) ->
                val isSelected = current == value
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .bounceClick {
                            if (vibrationEnabled) com.cinetrack.util.VibrationHelper.vibrateTick(context)
                            onSelect(value)
                        }
                        .background(
                            color = if (isSelected) accentColor else Color.White.copy(alpha = 0.05f),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .clip(RoundedCornerShape(16.dp))
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal),
                        color = if (isSelected) Color(0xFF1E1E1E) else Color.White,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(20.dp))
        SettingsDialogCancelButton(
            text = stringResource(R.string.settings_cancel),
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

