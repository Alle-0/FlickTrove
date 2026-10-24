package com.cinetrack.ui.components.shared

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cinetrack.R
import com.cinetrack.data.local.entities.FolderEntity
import com.cinetrack.data.model.SharedFolderDto
import com.cinetrack.data.model.SharedFolderInviteDto
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.KeyboardCapitalization
import com.cinetrack.ui.utils.bounceClick
import dev.chrisbanes.haze.HazeState

@Composable
fun FolderJoinDialog(
    initialCode: String = "",
    onDismiss: () -> Unit,
    onCheckCode: (String, (Result<Pair<SharedFolderInviteDto, SharedFolderDto>>) -> Unit) -> Unit,
    onAcceptInvite: (String, (Result<FolderEntity>) -> Unit) -> Unit,
    hazeState: HazeState? = null
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var codeInput by remember(initialCode) { mutableStateOf(sanitizeInviteCode(initialCode)) }
    var isChecking by remember { mutableStateOf(false) }
    var isJoining by remember { mutableStateOf(false) }
    var invitePreview by remember { mutableStateOf<Pair<SharedFolderInviteDto, SharedFolderDto>?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val focusRequester = remember { androidx.compose.ui.focus.FocusRequester() }
    var isFocused by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (invitePreview == null && initialCode.isBlank()) {
            kotlinx.coroutines.delay(250)
            focusRequester.requestFocus()
        }
    }

    LaunchedEffect(initialCode) {
        val clean = sanitizeInviteCode(initialCode)
        if (clean.isNotBlank() && invitePreview == null && !isChecking) {
            val fullCode = "FT-$clean"
            isChecking = true
            errorMessage = null
            onCheckCode(fullCode) { res ->
                isChecking = false
                res.onSuccess { preview ->
                    invitePreview = preview
                }.onFailure { err ->
                    errorMessage = err.message ?: context.getString(R.string.folder_join_invalid_code)
                }
            }
        }
    }

    FlickTroveModal(
        isVisible = true,
        onDismissRequest = onDismiss,
        hazeState = hazeState,
        maxWidth = 440.dp,
        maxWidthFraction = 0.92f,
        contentPadding = PaddingValues(0.dp)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 22.dp, end = 16.dp, top = 22.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.folder_join_title),
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black),
                color = Color.White
            )
            ModalCloseButton(onClose = onDismiss)
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp)
        ) {
            val buttonShape = RoundedCornerShape(16.dp)

            if (invitePreview == null) {
                Text(
                    text = stringResource(R.string.folder_join_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.padding(horizontal = 6.dp)
                )

                Spacer(Modifier.height(16.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.05f))
                        .border(
                            width = 1.dp,
                            color = if (errorMessage != null) MaterialTheme.colorScheme.error
                            else if (isFocused) Color.White
                            else Color.White.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            focusRequester.requestFocus()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "FT-",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.sp
                            ),
                            color = Color.White.copy(alpha = 0.45f)
                        )

                        BasicTextField(
                            value = codeInput,
                            onValueChange = { input ->
                                codeInput = sanitizeInviteCode(input)
                                errorMessage = null
                            },
                            modifier = Modifier
                                .focusRequester(focusRequester)
                                .onFocusChanged { isFocused = it.isFocused },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.headlineSmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 2.sp,
                                color = Color.White
                            ),
                            cursorBrush = SolidColor(Color.White),
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Characters,
                                autoCorrectEnabled = false
                            ),
                            decorationBox = { innerTextField ->
                                Box(contentAlignment = Alignment.CenterStart) {
                                    if (codeInput.isEmpty()) {
                                        Text(
                                            text = "7K9P2",
                                            style = MaterialTheme.typography.headlineSmall.copy(
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Bold,
                                                letterSpacing = 2.sp
                                            ),
                                            color = Color.White.copy(alpha = 0.25f)
                                        )
                                    }
                                    innerTextField()
                                }
                            }
                        )
                    }
                }

                if (errorMessage != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = errorMessage ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(Modifier.height(14.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .bounceClick(
                            enabled = codeInput.isNotBlank() && !isChecking,
                            scaleDown = 0.96f
                        ) {
                            val clean = codeInput.trim().uppercase()
                            if (clean.isBlank()) return@bounceClick
                            val fullCode = "FT-$clean"
                            isChecking = true
                            errorMessage = null
                            onCheckCode(fullCode) { result ->
                                isChecking = false
                                result.onSuccess { pair ->
                                    invitePreview = pair
                                }.onFailure { err ->
                                    errorMessage = when (err.message) {
                                        "INVITE_EXPIRED" -> context.getString(R.string.folder_join_expired)
                                        "INVITE_REVOKED" -> context.getString(R.string.folder_join_revoked)
                                        "INVITE_MAX_USES_REACHED" -> context.getString(R.string.folder_join_max_uses)
                                        else -> context.getString(R.string.folder_join_invalid_code)
                                    }
                                }
                            }
                        }
                        .clip(buttonShape)
                        .background(
                            if (codeInput.isNotBlank() && !isChecking) Color.White else Color.White.copy(alpha = 0.35f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isChecking) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = Color.Black,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.folder_join_check),
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                        )
                    }
                }
            } else {
                // Verified preview state
                val (invite, folder) = invitePreview!!

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.White.copy(alpha = 0.08f))
                        .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
                        .padding(18.dp)
                ) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = ImageVector.vectorResource(id = R.drawable.ic_cartella),
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                            Spacer(Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = folder.title,
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White
                                )
                                Text(
                                    text = stringResource(R.string.folder_join_invitation_from, folder.ownerName),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.65f)
                                )
                            }
                        }

                        if (!folder.description.isNullOrBlank()) {
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = folder.description,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.75f)
                            )
                        }

                        Spacer(Modifier.height(14.dp))
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White.copy(alpha = 0.06f))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = ImageVector.vectorResource(id = R.drawable.ic_clock),
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.6f),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "${folder.items.size} titoli inclusi",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }
                }

                if (errorMessage != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = errorMessage ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .bounceClick(scaleDown = 0.94f) { invitePreview = null }
                            .clip(buttonShape)
                            .border(1.dp, Color.White.copy(alpha = 0.25f), buttonShape)
                            .background(Color.White.copy(alpha = 0.06f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.settings_cancel),
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1.4f)
                            .height(44.dp)
                            .bounceClick(
                                enabled = !isJoining,
                                scaleDown = 0.96f
                            ) {
                                isJoining = true
                                errorMessage = null
                                onAcceptInvite(invite.code) { result ->
                                    isJoining = false
                                    result.onSuccess {
                                        onDismiss()
                                    }.onFailure { err ->
                                        errorMessage = when (err.message) {
                                            "MEMBER_LIMIT_REACHED" -> context.getString(R.string.folder_join_member_limit)
                                            else -> err.message ?: "Errore durante l'adesione"
                                        }
                                    }
                                }
                            }
                            .clip(buttonShape)
                            .background(if (!isJoining) Color.White else Color.White.copy(alpha = 0.4f)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isJoining) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = Color.Black,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = stringResource(R.string.folder_join_accept),
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun sanitizeInviteCode(input: String): String {
    var raw = input.trim().uppercase()
    if (raw.contains("CODE=")) {
        raw = raw.substringAfter("CODE=").substringBefore("&")
    }
    if (raw.startsWith("FT-") || raw.startsWith("FT_") || raw.startsWith("FT:")) {
        raw = raw.substring(3)
    } else if (raw.startsWith("FT") && raw.length > 2 && !raw[2].isLetterOrDigit()) {
        raw = raw.substring(3)
    }
    return raw.filter { it.isLetterOrDigit() }.take(8)
}

@Composable
private fun contextErrorMessage(resId: Int): String {
    return stringResource(resId)
}
