package com.cinetrack.ui.components.shared

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cinetrack.R
import com.cinetrack.data.local.entities.FolderEntity
import com.cinetrack.data.model.SharedFolderInviteDto
import com.cinetrack.data.repository.InviteDurationOption
import com.cinetrack.ui.utils.bounceClick
import dev.chrisbanes.haze.HazeState

@Composable
fun FolderShareModal(
    folder: FolderEntity,
    onDismiss: () -> Unit,
    onCreateInvite: (InviteDurationOption, (Result<SharedFolderInviteDto>) -> Unit) -> Unit,
    hazeState: HazeState? = null
) {
    val context = LocalContext.current
    var selectedOption by remember { mutableStateOf(InviteDurationOption.TWENTY_FOUR_HOURS) }
    var isLoading by remember { mutableStateOf(false) }
    var createdInvite by remember { mutableStateOf<SharedFolderInviteDto?>(null) }
    var copiedToast by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

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
                text = stringResource(R.string.folder_share_title),
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black),
                color = Color.White
            )
            ModalCloseButton(onClose = onDismiss)
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp, vertical = 8.dp)
        ) {
            Text(
                text = stringResource(R.string.folder_share_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.7f)
            )

            Spacer(Modifier.height(16.dp))

            if (createdInvite == null) {
                // Duration selection
                Text(
                    text = stringResource(R.string.folder_share_duration).uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    ),
                    color = Color.White.copy(alpha = 0.5f)
                )

                Spacer(Modifier.height(10.dp))

                val options = listOf(
                    InviteDurationOption.SINGLE_USE to stringResource(R.string.folder_invite_duration_single),
                    InviteDurationOption.TWENTY_FOUR_HOURS to stringResource(R.string.folder_invite_duration_24h),
                    InviteDurationOption.FORTY_EIGHT_HOURS to stringResource(R.string.folder_invite_duration_48h),
                    InviteDurationOption.SEVEN_DAYS to stringResource(R.string.folder_invite_duration_7d),
                    InviteDurationOption.ONE_MINUTE to stringResource(R.string.folder_invite_duration_1m)
                )

                options.forEach { (option, label) ->
                    val isSelected = selectedOption == option
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.5.dp)
                            .bounceClick(scaleDown = 0.98f) { selectedOption = option }
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) Color.White.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.04f))
                            .border(
                                1.dp,
                                if (isSelected) Color.White.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.08f),
                                RoundedCornerShape(12.dp)
                            )
                            .padding(horizontal = 14.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                fontSize = 14.sp
                            ),
                            color = if (isSelected) Color.White else Color.White.copy(alpha = 0.7f)
                        )
                        RadioButton(
                            selected = isSelected,
                            onClick = null,
                            colors = RadioButtonDefaults.colors(
                                selectedColor = Color.White,
                                unselectedColor = Color.White.copy(alpha = 0.3f)
                            ),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                if (errorMessage != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = errorMessage ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                Spacer(Modifier.height(14.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
                        .bounceClick(
                            enabled = !isLoading,
                            scaleDown = 0.96f
                        ) {
                            isLoading = true
                            errorMessage = null
                            onCreateInvite(selectedOption) { result ->
                                isLoading = false
                                result.onSuccess { invite ->
                                    createdInvite = invite
                                }.onFailure { err ->
                                    errorMessage = err.message ?: "Errore durante la creazione"
                                }
                            }
                        }
                        .clip(CircleShape)
                        .background(if (!isLoading) Color.White else Color.White.copy(alpha = 0.4f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = Color.Black,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.folder_invite_create_button),
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                        )
                    }
                }
            } else {
                // Invite successfully created view
                val invite = createdInvite!!
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color.White.copy(alpha = 0.08f))
                        .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(18.dp))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = stringResource(R.string.folder_invite_code_label).uppercase(),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            ),
                            color = Color.White.copy(alpha = 0.6f)
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = invite.code,
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = 3.sp,
                                fontFamily = FontFamily.Monospace
                            ),
                            color = Color.White
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Copy code button
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .bounceClick(scaleDown = 0.94f) {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("FlickTrove Invite Code", invite.code)
                                clipboard.setPrimaryClip(clip)
                                copiedToast = true
                            }
                            .clip(CircleShape)
                            .border(1.dp, Color.White.copy(alpha = 0.25f), CircleShape)
                            .background(Color.White.copy(alpha = 0.06f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = ImageVector.vectorResource(id = R.drawable.ic_link),
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = if (copiedToast) stringResource(R.string.folder_invite_copied) else stringResource(R.string.folder_invite_copy),
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold, color = Color.White)
                            )
                        }
                    }

                    // Share intent button
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .bounceClick(scaleDown = 0.94f) {
                                val shareText = "Unisciti alla mia cartella \"${folder.name}\" su FlickTrove!\nInserisci il codice: ${invite.code}"
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, shareText)
                                }
                                context.startActivity(Intent.createChooser(intent, "Condividi invito FlickTrove"))
                            }
                            .clip(CircleShape)
                            .background(Color.White),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = ImageVector.vectorResource(id = R.drawable.ic_share),
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.folder_invite_share),
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, color = Color.Black)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}
