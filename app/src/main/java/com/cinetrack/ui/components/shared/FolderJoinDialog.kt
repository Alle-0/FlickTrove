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
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import com.cinetrack.ui.utils.bounceClick
import com.cinetrack.util.toComposeColor
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
                val folderColor = remember(folder.color) {
                    folder.color.toComposeColor(Color(0xFFE5A93C))
                }
                val topItems = remember(folder.items) {
                    folder.items.filter { !it.posterPath.isNullOrBlank() }.take(3)
                }

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
                            if (topItems.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .size(52.dp)
                                        .clip(RoundedCornerShape(50))
                                        .background(folderColor.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = ImageVector.vectorResource(id = R.drawable.ic_cartella),
                                        contentDescription = null,
                                        tint = folderColor,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .width(40.dp + ((topItems.size - 1) * 22).dp)
                                        .height(60.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    topItems.forEachIndexed { index, item ->
                                        val centerIndex = (topItems.size - 1) / 2f
                                        val currentRotation = if (topItems.size <= 1) 0f else {
                                            val maxRotation = 15f
                                            val rotationStep = (maxRotation * 2) / (topItems.size - 1)
                                            -maxRotation + (index * rotationStep)
                                        }
                                        val xOffset = (index - centerIndex) * 14f

                                        Box(
                                            modifier = Modifier
                                                .offset(x = xOffset.dp)
                                                .size(width = 40.dp, height = 60.dp)
                                                .graphicsLayer {
                                                    rotationZ = currentRotation
                                                    transformOrigin = TransformOrigin(0.5f, 1.1f)
                                                }
                                                .clip(RoundedCornerShape(10.dp))
                                                .border(1.5.dp, Color(0xFF13151A), RoundedCornerShape(10.dp))
                                                .zIndex((topItems.size - index).toFloat())
                                        ) {
                                            AsyncImage(
                                                model = "https://image.tmdb.org/t/p/w200${item.posterPath}",
                                                contentDescription = null,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(Modifier.width(16.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (topItems.isNotEmpty()) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(folderColor)
                                        )
                                        Spacer(Modifier.width(8.dp))
                                    }
                                    Text(
                                        text = folder.title,
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = (-0.3).sp
                                        ),
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = stringResource(R.string.folder_join_invitation_from, folder.ownerName),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.65f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        if (!folder.description.isNullOrBlank()) {
                            Spacer(Modifier.height(14.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.White.copy(alpha = 0.05f))
                                    .border(0.5.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                                    .padding(horizontal = 12.dp, vertical = 10.dp)
                            ) {
                                Text(
                                    text = folder.description,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        lineHeight = 20.sp
                                    ),
                                    color = Color.White.copy(alpha = 0.8f),
                                    maxLines = 4,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
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
                                imageVector = ImageVector.vectorResource(id = R.drawable.ic_cartella),
                                contentDescription = null,
                                tint = folderColor,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "${folder.items.size} titoli inclusi",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.85f)
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
