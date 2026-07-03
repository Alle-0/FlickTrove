package com.cinetrack.ui.components.detail

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.cinetrack.R
import com.cinetrack.ui.components.shared.FlickTroveModal
import com.cinetrack.ui.utils.bounceClick
import dev.chrisbanes.haze.HazeState

@Composable
fun DetailTranslationPromptModal(
    showTranslationPrompt: Pair<Long, String>?,
    onDismiss: () -> Unit,
    onTranslate: (commentId: Long, text: String, requireWifi: Boolean) -> Unit,
    hazeState: HazeState,
    accentColor: Color
) {
    var rememberedPromptData by remember { mutableStateOf<Pair<Long, String>?>(null) }
    LaunchedEffect(showTranslationPrompt) {
        if (showTranslationPrompt != null) {
            rememberedPromptData = showTranslationPrompt
        }
    }

    FlickTroveModal(
        isVisible = showTranslationPrompt != null,
        onDismissRequest = onDismiss,
        hazeState = hazeState
    ) {
        val promptData = showTranslationPrompt ?: rememberedPromptData
        if (promptData != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(id = R.drawable.ic_x),
                    contentDescription = stringResource(R.string.settings_close),
                    tint = Color.White.copy(alpha = 0.65f),
                    modifier = Modifier
                        .size(18.dp)
                        .bounceClick(scaleDown = 0.9f) { onDismiss() }
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Icon(
                imageVector = ImageVector.vectorResource(id = R.drawable.ic_traduzione),
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(48.dp).padding(bottom = 16.dp)
            )
            Text(
                stringResource(R.string.detail_translate_comment),
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                stringResource(R.string.detail_translate_download_prompt),
                color = Color.White.copy(alpha = 0.8f),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            Spacer(modifier = Modifier.height(24.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        onTranslate(promptData.first, promptData.second, false)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accentColor.copy(alpha = 0.2f),
                        contentColor = accentColor
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.detail_translate_yes_cellular), fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = {
                        onTranslate(promptData.first, promptData.second, true)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White.copy(alpha = 0.05f),
                        contentColor = Color.White.copy(alpha = 0.8f)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.detail_translate_no_wifi))
                }

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.detail_translate_not_now), color = Color.White.copy(alpha = 0.65f))
                }
            }
        }
    }
}
