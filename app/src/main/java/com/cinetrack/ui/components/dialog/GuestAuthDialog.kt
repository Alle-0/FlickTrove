package com.cinetrack.ui.components.dialog

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.Navigator
import com.cinetrack.R
import com.cinetrack.ui.components.glass.hazeGlass
import dev.chrisbanes.haze.HazeState

@Composable
fun GuestAuthDialog(
    showDialog: Boolean,
    onDismiss: () -> Unit,
    navigator: Navigator?,
    globalHazeState: HazeState
) {
    if (showDialog) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .widthIn(max = 400.dp)
                    .fillMaxWidth(0.85f)
                    .hazeGlass(state = globalHazeState, alpha = 1f, shape = RoundedCornerShape(32.dp))
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Text(
                        stringResource(R.string.account_guest_sync_title),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        stringResource(R.string.account_guest_sync_message),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Button(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.1f))
                        ) {
                            Text(stringResource(R.string.auth_guest_dialog_cancel), color = MaterialTheme.colorScheme.onSurface)
                        }
                        Button(
                            onClick = {
                                onDismiss()
                                val rootNav = navigator?.parent ?: navigator
                                rootNav?.push(com.cinetrack.ui.screens.LoginScreen())
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text(stringResource(R.string.auth_btn_login), color = MaterialTheme.colorScheme.onPrimary)
                        }
                    }
                }
            }
        }
    }
}
