package com.cinetrack.ui.components

import androidx.compose.ui.res.stringResource
import com.cinetrack.R
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.cinetrack.util.ImageQuality

@Composable
fun ThemeSelectionDialog(
    current: String,
    accentColor: Color,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    val options = listOf(
        "System" to "Sistema",
        "AMOLED" to "AMOLED (Nero assoluto)",
        "Dark" to "Scuro (Premium Dark)",
        "Light" to "Chiaro"
    )
    
    GenericSelectionDialog(
        title = "Tema App",
        icon = Icons.Rounded.DarkMode,
        options = options,
        current = current,
        accentColor = accentColor,
        onDismiss = onDismiss,
        onSelect = onSelect
    )
}

@Composable
fun LanguageSelectionDialog(
    current: String,
    accentColor: Color,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    val options = listOf(
        "it-IT" to "Italiano",
        "en-US" to "English (US)",
        "en-GB" to "English (UK)",
        "es-ES" to "Español",
        "fr-FR" to "Français",
        "de-DE" to "Deutsch",
        "ja-JP" to "日本語",
        "ko-KR" to "한국어"
    )
    
    GenericSelectionDialog(
        title = "Lingua Contenuti",
        icon = Icons.Rounded.Language,
        options = options,
        current = current,
        accentColor = accentColor,
        onDismiss = onDismiss,
        onSelect = onSelect
    )
}

@Composable
fun ImageQualitySelectionDialog(
    current: ImageQuality,
    accentColor: Color,
    onDismiss: () -> Unit,
    onSelect: (ImageQuality) -> Unit
) {
    val options = listOf(
        ImageQuality.LOW to "Bassa (Risparmio Dati)",
        ImageQuality.MEDIUM to "Media (Bilanciata)",
        ImageQuality.HIGH to "Alta (Miglior Qualità)"
    )
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF1C1C1E)
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.HighQuality,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = stringResource(R.string.settings_quality),
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                options.forEach { (optionValue, label) ->
                    val isSelected = current == optionValue
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) accentColor.copy(alpha = 0.15f) else Color.Transparent)
                            .clickable { onSelect(optionValue) }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = { onSelect(optionValue) },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = accentColor,
                                unselectedColor = Color.White.copy(alpha = 0.5f)
                            )
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (isSelected) accentColor else Color.White
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.action_cancel), color = Color.White.copy(alpha = 0.7f))
                    }
                }
            }
        }
    }
}

@Composable
fun <T> GenericSelectionDialog(
    title: String,
    icon: ImageVector,
    options: List<Pair<T, String>>,
    current: T,
    accentColor: Color,
    onDismiss: () -> Unit,
    onSelect: (T) -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF1C1C1E)
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                options.forEach { (optionValue, label) ->
                    val isSelected = current == optionValue
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) accentColor.copy(alpha = 0.15f) else Color.Transparent)
                            .clickable { onSelect(optionValue) }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = { onSelect(optionValue) },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = accentColor,
                                unselectedColor = Color.White.copy(alpha = 0.5f)
                            )
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (isSelected) accentColor else Color.White
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.action_cancel), color = Color.White.copy(alpha = 0.7f))
                    }
                }
            }
        }
    }
}

@Composable
fun TextSizeSelectionDialog(
    current: Float,
    accentColor: Color,
    onDismiss: () -> Unit,
    onSelect: (Float) -> Unit
) {
    val options = listOf(
        0.8f to "Piccolo",
        1.0f to "Predefinito",
        1.2f to "Grande"
    )
    
    GenericSelectionDialog(
        title = "Dimensione testo titoli",
        icon = Icons.Rounded.FormatSize,
        options = options,
        current = current,
        accentColor = accentColor,
        onDismiss = onDismiss,
        onSelect = onSelect
    )
}
