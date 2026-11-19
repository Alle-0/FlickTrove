package com.cinetrack.ui.components.comments

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cinetrack.R
import com.cinetrack.ui.components.shared.ModalCloseButton
import com.cinetrack.ui.components.shared.MorphGlassModal
import dev.chrisbanes.haze.HazeState

/**
 * Modal dialog showing markdown syntax formatting, spoiler tags, media mentions,
 * and writing etiquette guidelines for CommsUni comments.
 */
@Composable
fun WritingTipsModal(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    hazeState: HazeState,
    accentColor: Color,
    triggerBounds: Rect? = null
) {
    MorphGlassModal(
        isVisible = isVisible,
        onDismissRequest = onDismiss,
        triggerBounds = triggerBounds,
        hazeState = hazeState,
        targetMaxWidth = 420.dp,
        targetWidthFraction = 0.90f,
        targetCornerRadius = 32.dp
    ) { contentAlpha ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .graphicsLayer { alpha = contentAlpha }
                .padding(horizontal = 22.dp, vertical = 22.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.comment_writing_tips_title),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge
                )
                ModalCloseButton(onClick = onDismiss)
            }

            Spacer(modifier = Modifier.height(18.dp))

            WritingTipCodeRow(
                syntax = "**word**",
                description = stringResource(R.string.comment_tip_bold),
                descriptionFontWeight = FontWeight.Bold
            )
            WritingTipCodeRow(
                syntax = "*word*",
                description = stringResource(R.string.comment_tip_italic),
                descriptionFontStyle = FontStyle.Italic
            )
            WritingTipCodeRow(
                syntax = "~~word~~",
                description = stringResource(R.string.comment_tip_strikethrough),
                descriptionTextDecoration = TextDecoration.LineThrough
            )
            WritingTipCodeRow(
                syntax = "- list",
                description = stringResource(R.string.comment_tip_list)
            )
            WritingTipCodeRow(
                syntax = "> text",
                description = stringResource(R.string.comment_tip_quote)
            )
            WritingTipCodeRow(
                syntax = stringResource(R.string.comment_tip_media_tag_syntax),
                description = stringResource(R.string.comment_tip_media_tag),
                syntaxColor = accentColor,
                isPillSyntax = true
            )

            Spacer(modifier = Modifier.height(4.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
            Spacer(modifier = Modifier.height(12.dp))

            WritingTipIconRow(
                iconRes = R.drawable.ic_image,
                description = stringResource(R.string.comment_tip_attachments)
            )
            WritingTipIconRow(
                iconRes = R.drawable.ic_link,
                description = stringResource(R.string.comment_tip_links)
            )
            WritingTipIconRow(
                iconRes = R.drawable.ic_eye_off,
                description = stringResource(R.string.comment_tip_spoiler)
            )
            WritingTipInfoRow(
                description = stringResource(R.string.comment_tip_max_chars, 8000)
            )
        }
    }
}

@Composable
private fun WritingTipCodeRow(
    syntax: String,
    description: String,
    syntaxColor: Color = Color.White.copy(alpha = 0.90f),
    isPillSyntax: Boolean = false,
    descriptionFontWeight: FontWeight = FontWeight.Normal,
    descriptionFontStyle: FontStyle = FontStyle.Normal,
    descriptionTextDecoration: TextDecoration? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.width(100.dp),
            contentAlignment = Alignment.Center
        ) {
            if (isPillSyntax) {
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(syntaxColor.copy(alpha = 0.16f))
                        .padding(horizontal = 10.dp, vertical = 2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = syntax,
                        color = syntaxColor,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            } else {
                Text(
                    text = syntax,
                    color = syntaxColor,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp,
                        letterSpacing = 0.2.sp
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = description,
            color = Color.White.copy(alpha = 0.78f),
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = descriptionFontWeight,
                fontStyle = descriptionFontStyle,
                textDecoration = descriptionTextDecoration
            ),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun WritingTipIconRow(
    iconRes: Int,
    description: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.width(100.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.72f),
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = description,
            color = Color.White.copy(alpha = 0.78f),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun WritingTipInfoRow(
    description: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.width(100.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Info,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.72f),
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = description,
            color = Color.White.copy(alpha = 0.78f),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
    }
}
