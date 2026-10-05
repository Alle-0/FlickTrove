package com.cinetrack.ui.components.comments

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cinetrack.R
import com.cinetrack.data.model.AppComment
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import com.cinetrack.ui.components.shared.FlickTroveModal
import com.cinetrack.ui.components.shared.MorphGlassModal
import com.cinetrack.ui.components.shared.ModalBackButton
import com.cinetrack.ui.components.shared.ModalCloseButton
import com.cinetrack.ui.utils.bounceClick
import dev.chrisbanes.haze.HazeState
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.layout.ContentScale
import java.util.Locale

private enum class CommentModalStep {
    MENU,
    REASONS,
    TV_TIME_OWNERSHIP,
    OTHER_DETAIL
}

private val MEDIA_REGEX = Regex("!\\[.*?\\]\\((.*?)\\)")

@Composable
fun CommentContextPreviewCard(
    comment: AppComment,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isEffectivelyDeleted = comment.isEffectivelyDeleted
    val avatarBgColor = remember(comment.userDisplayName, comment.originSlug) {
        if (comment.userDisplayName.isNotBlank()) {
            val hue = (comment.userDisplayName.fold(0) { acc, c -> acc * 31 + c.code }.and(0x7FFFFFFF) % 360).toFloat()
            Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, 0.45f, 0.35f)))
        } else {
            Color(0xFF2A2A2A)
        }
    }
    var isImageError by remember(comment.userAvatarUrl) { mutableStateOf(false) }

    val cleanPreviewText = remember(comment.text) {
        comment.text
            .replace(MEDIA_REGEX, "")
            .replace(Regex("[#*_`~]"), "")
            .trim()
            .ifBlank {
                if (comment.attachedMedia.isNotEmpty() || comment.text.contains("![")) "[Media]"
                else "..."
            }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .border(1.dp, Color.White.copy(alpha = 0.09f), RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Avatar (32.dp, CircleShape)
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(avatarBgColor),
                contentAlignment = Alignment.Center
            ) {
                val initial = comment.userDisplayName.trim().firstOrNull()?.uppercaseChar()
                if (initial != null && initial.isLetterOrDigit()) {
                    Text(
                        text = initial.toString(),
                        color = Color.White.copy(alpha = 0.9f),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                    )
                } else {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_persona),
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.size(16.dp)
                    )
                }

                val safeAvatarUrl = comment.userAvatarUrl.trim()
                if (safeAvatarUrl.isNotBlank() && !isImageError) {
                    val modelData: Any = if (safeAvatarUrl.startsWith("data:image", ignoreCase = true) && safeAvatarUrl.contains("base64,")) {
                        try {
                            val base64Data = safeAvatarUrl.substringAfter("base64,")
                            android.util.Base64.decode(base64Data, android.util.Base64.DEFAULT)
                        } catch (e: Exception) {
                            safeAvatarUrl
                        }
                    } else {
                        safeAvatarUrl
                    }

                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(modelData)
                            .crossfade(true)
                            .build(),
                        contentDescription = comment.userDisplayName,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        onError = { isImageError = true }
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Body Column
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (isEffectivelyDeleted) stringResource(R.string.comment_deleted)
                               else comment.userDisplayName.ifBlank { stringResource(R.string.comment_anonymous_user) },
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    if (comment.originSlug.isNotBlank()) {
                        Spacer(modifier = Modifier.width(6.dp))

                        val parsedColor = comment.originColor?.let {
                            try {
                                Color(android.graphics.Color.parseColor(it))
                            } catch (e: Exception) {
                                null
                            }
                        }
                        val originColor = parsedColor ?: run {
                            val hue = (comment.originSlug.fold(0) { acc, c -> acc * 31 + c.code }
                                .and(0x7FFFFFFF) % 360).toFloat()
                            Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, 0.65f, 0.9f)))
                        }
                        val originIconUrl = comment.originIcon?.takeIf { it.isNotBlank() }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .background(originColor.copy(alpha = 0.18f), CircleShape)
                                .border(1.dp, originColor.copy(alpha = 0.4f), CircleShape)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            if (comment.originSlug.equals("flicktrove", ignoreCase = true)) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_flicktrove_logo),
                                    contentDescription = "FlickTrove",
                                    tint = Color.Unspecified,
                                    modifier = Modifier.size(width = 12.dp, height = 9.dp)
                                )
                            } else if (!originIconUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = originIconUrl,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(11.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                )
                            } else {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_world),
                                    contentDescription = null,
                                    tint = originColor,
                                    modifier = Modifier.size(10.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = comment.originName.ifBlank {
                                    comment.originSlug.replaceFirstChar { char ->
                                        if (char.isLowerCase()) char.titlecase(Locale.ROOT) else char.toString()
                                    }
                                },
                                color = originColor,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 8.5.sp),
                                maxLines = 1
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = if (isEffectivelyDeleted) stringResource(R.string.comment_deleted) else cleanPreviewText,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    ),
                    color = Color.White.copy(alpha = 0.65f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun CommentReportModal(
    comment: AppComment?,
    onDismiss: () -> Unit,
    onReport: (category: String, detail: String?) -> Unit,
    onBlockUser: ((comment: AppComment) -> Unit)? = null,
    triggerBounds: Rect? = null,
    hazeState: HazeState
) {
    // Keep last non-null comment so content stays visible during the exit fade-out animation
    var displayComment by remember { mutableStateOf(comment) }
    var currentStep by remember { mutableStateOf(CommentModalStep.MENU) }
    var otherReasonText by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(comment) {
        if (comment != null) {
            displayComment = comment
            currentStep = CommentModalStep.MENU
            otherReasonText = ""
        }
    }

    // Intercept back gesture if user is in a sub-step
    BackHandler(enabled = comment != null && currentStep != CommentModalStep.MENU) {
        currentStep = when (currentStep) {
            CommentModalStep.OTHER_DETAIL -> CommentModalStep.REASONS
            else -> CommentModalStep.MENU
        }
    }

    MorphGlassModal(
        isVisible = comment != null,
        onDismissRequest = onDismiss,
        triggerBounds = triggerBounds,
        hazeState = hazeState,
        targetMaxWidth = 420.dp
    ) {
        val c = displayComment ?: return@MorphGlassModal
        val canBlock = onBlockUser != null && c.userId.isNotBlank()

        val isTvTime = remember(c.originSlug, c.originName, c.id, c.userDisplayName) {
            c.originSlug.contains("tvtime", ignoreCase = true) ||
            c.originSlug.contains("tv-time", ignoreCase = true) ||
            c.originSlug.contains("tv_time", ignoreCase = true) ||
            c.originName.contains("tv time", ignoreCase = true) ||
            c.originName.contains("tvtime", ignoreCase = true) ||
            c.originName.contains("refugees", ignoreCase = true) ||
            c.id.startsWith("tvtime", ignoreCase = true) ||
            c.id.startsWith("tvt", ignoreCase = true) ||
            c.userId.contains("tvtime", ignoreCase = true) ||
            c.userDisplayName.matches(Regex("^[a-z]+_[a-z]+\\.[0-9a-f]+$"))
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp, vertical = 24.dp)
        ) {
            AnimatedContent(
                targetState = currentStep,
                transitionSpec = {
                    val isGoingBack = (initialState == CommentModalStep.OTHER_DETAIL && targetState == CommentModalStep.REASONS) ||
                            (targetState == CommentModalStep.MENU)
                    if (isGoingBack) {
                        (slideInHorizontally { -it / 3 } + fadeIn(tween(200)))
                            .togetherWith(slideOutHorizontally { it / 3 } + fadeOut(tween(150)))
                    } else {
                        (slideInHorizontally { it / 3 } + fadeIn(tween(200)))
                            .togetherWith(slideOutHorizontally { -it / 3 } + fadeOut(tween(150)))
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize(),
                label = "CommentModalStepTransition"
            ) { step ->
            when (step) {
                CommentModalStep.MENU -> {
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Header with Title and Unified Close Button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Spacer(modifier = Modifier.size(32.dp))
                            Text(
                                text = stringResource(R.string.comment_action_menu_title),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleLarge,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.weight(1f)
                            )
                            ModalCloseButton(onClick = onDismiss)
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Comment Preview Card
                        CommentContextPreviewCard(comment = c)

                        Spacer(modifier = Modifier.height(16.dp))

                        Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // 1. TV Time Archive Ownership Option (Only for TV Time comments)
                            if (isTvTime) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp)
                                        .bounceClick { currentStep = CommentModalStep.TV_TIME_OWNERSHIP }
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(Color(0xFF2DD4BF).copy(alpha = 0.12f))
                                        .border(1.dp, Color(0xFF2DD4BF).copy(alpha = 0.28f), RoundedCornerShape(16.dp))
                                        .padding(horizontal = 16.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f, fill = false)
                                        ) {
                                            Icon(
                                                painter = painterResource(id = R.drawable.ic_persona),
                                                contentDescription = null,
                                                tint = Color(0xFF2DD4BF),
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Text(
                                                text = stringResource(R.string.comment_action_tvtime_ownership),
                                                color = Color.White.copy(alpha = 0.95f),
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_right),
                                            contentDescription = null,
                                            tint = Color(0xFF2DD4BF).copy(alpha = 0.60f),
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }

                            // 2. Report Action Button (Morphs to pure violation report reasons)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .bounceClick { currentStep = CommentModalStep.REASONS }
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color.White.copy(alpha = 0.07f))
                                    .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
                                    .padding(horizontal = 16.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f, fill = false)
                                    ) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_flag),
                                            contentDescription = null,
                                            tint = Color.White.copy(alpha = 0.85f),
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = stringResource(R.string.comment_action_report),
                                            color = Color.White.copy(alpha = 0.95f),
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_right),
                                        contentDescription = null,
                                        tint = Color.White.copy(alpha = 0.35f),
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }

                            // 3. Block Author Action (Red Button)
                            if (canBlock) {
                                val displayName = c.userDisplayName.ifBlank { "User" }
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp)
                                        .bounceClick {
                                            onBlockUser?.invoke(c)
                                        }
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(Color(0xFFFF3B30).copy(alpha = 0.12f))
                                        .border(1.dp, Color(0xFFFF3B30).copy(alpha = 0.28f), RoundedCornerShape(16.dp))
                                        .padding(horizontal = 16.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.weight(1f, fill = false)
                                        ) {
                                            Icon(
                                                painter = painterResource(id = R.drawable.ic_block),
                                                contentDescription = null,
                                                tint = Color(0xFFFF6E6E),
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Text(
                                                text = stringResource(R.string.comment_block_user_action, displayName),
                                                color = Color(0xFFFF6E6E),
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_right),
                                            contentDescription = null,
                                            tint = Color(0xFFFF6E6E).copy(alpha = 0.50f),
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                CommentModalStep.REASONS -> {
                    // Pure violation report reasons mapped to CommsUni categories
                    val reasons = remember {
                        listOf(
                            "spoiler" to R.string.comment_report_reason_spoiler,
                            "abuse" to R.string.comment_report_reason_abuse,
                            "spam" to R.string.comment_report_reason_spam,
                            "sexual" to R.string.comment_report_reason_sexual,
                            "illegal" to R.string.comment_report_reason_illegal,
                            "other" to R.string.comment_report_reason_other
                        )
                    }

                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Header with Back Button, Title, and Unified Close Button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ModalBackButton(onClick = { currentStep = CommentModalStep.MENU })
                            Text(
                                text = stringResource(R.string.comment_report_title),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleLarge,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.weight(1f)
                            )
                            ModalCloseButton(onClick = onDismiss)
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Comment Preview Card
                        CommentContextPreviewCard(comment = c)

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = stringResource(R.string.comment_report_subtitle),
                            color = Color.White.copy(0.65f),
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 420.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            reasons.forEach { (key, strRes) ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .bounceClick {
                                            if (key == "other") {
                                                currentStep = CommentModalStep.OTHER_DETAIL
                                            } else {
                                                onReport(key, null)
                                            }
                                        }
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(Color.White.copy(alpha = 0.06f))
                                        .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(16.dp))
                                        .padding(horizontal = 16.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = stringResource(strRes),
                                            color = Color.White.copy(alpha = 0.90f),
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_right),
                                            contentDescription = null,
                                            tint = Color.White.copy(alpha = 0.35f),
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                CommentModalStep.OTHER_DETAIL -> {
                    val isValid = otherReasonText.trim().isNotEmpty()
                    val focusManager = LocalFocusManager.current
                    val keyboardController = LocalSoftwareKeyboardController.current

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                                keyboardController?.hide()
                                focusManager.clearFocus()
                            }
                    ) {
                        // Header with Back Button, Title, and Unified Close Button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ModalBackButton(onClick = { currentStep = CommentModalStep.REASONS })
                            Text(
                                text = stringResource(R.string.comment_report_other_title),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleLarge,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.weight(1f)
                            )
                            ModalCloseButton(onClick = onDismiss)
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Comment Preview Card
                        CommentContextPreviewCard(comment = c)

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = stringResource(R.string.comment_report_other_subtitle),
                            color = Color.White.copy(0.65f),
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = otherReasonText,
                            onValueChange = { if (it.length <= 500) otherReasonText = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 120.dp, max = 180.dp),
                            placeholder = {
                                Text(
                                    text = stringResource(R.string.comment_report_other_placeholder),
                                    color = Color.White.copy(alpha = 0.35f),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color.White.copy(alpha = 0.22f),
                                unfocusedBorderColor = Color.White.copy(alpha = 0.10f),
                                cursorColor = Color.White,
                                focusedContainerColor = Color.White.copy(alpha = 0.05f),
                                unfocusedContainerColor = Color.White.copy(alpha = 0.04f)
                            ),
                            shape = RoundedCornerShape(16.dp),
                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                color = Color.White,
                                lineHeight = 22.sp
                            )
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "${otherReasonText.length}/500",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (otherReasonText.length >= 500) Color(0xFFFF5252) else Color.White.copy(alpha = 0.35f),
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.End
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        // Action Buttons Row (Rule 14: Horizontal Row, 16.dp radius)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp)
                                    .bounceClick { currentStep = CommentModalStep.REASONS }
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color.White.copy(alpha = 0.08f))
                                    .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
                                    .padding(horizontal = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = stringResource(R.string.comment_cancel_btn),
                                    color = Color.White.copy(alpha = 0.85f),
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp)
                                    .bounceClick(enabled = isValid) {
                                        if (isValid) {
                                            onReport("other", otherReasonText.trim())
                                        }
                                    }
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(
                                        if (isValid) Color(0xFFFF3B30).copy(alpha = 0.22f)
                                        else Color.White.copy(alpha = 0.04f)
                                    )
                                    .border(
                                        1.dp,
                                        if (isValid) Color(0xFFFF3B30).copy(alpha = 0.5f)
                                        else Color.White.copy(alpha = 0.08f),
                                        RoundedCornerShape(16.dp)
                                    )
                                    .padding(horizontal = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = stringResource(R.string.comment_report_other_submit),
                                    color = if (isValid) Color(0xFFFF6E6E) else Color.White.copy(alpha = 0.35f),
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                CommentModalStep.TV_TIME_OWNERSHIP -> {
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Header with Back Button, Title, and Unified Close Button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ModalBackButton(onClick = { currentStep = CommentModalStep.MENU })
                            Text(
                                text = stringResource(R.string.comment_tvtime_ownership_title),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleLarge,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.weight(1f)
                            )
                            ModalCloseButton(onClick = onDismiss)
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Comment Preview Card
                        CommentContextPreviewCard(comment = c)

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = stringResource(R.string.comment_tvtime_ownership_subtitle),
                            color = Color.White.copy(0.65f),
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            // 1. Claim ownership option
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .bounceClick { onReport("mine_claim", null) }
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color.White.copy(alpha = 0.07f))
                                    .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
                                    .padding(horizontal = 16.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f, fill = false)
                                    ) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_tick),
                                            contentDescription = null,
                                            tint = Color(0xFF2DD4BF),
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = stringResource(R.string.comment_report_reason_mine_claim),
                                            color = Color.White.copy(alpha = 0.95f),
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_right),
                                        contentDescription = null,
                                        tint = Color.White.copy(alpha = 0.35f),
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }

                            // 2. Hide archived comment option
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .bounceClick { onReport("mine_hide", null) }
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color.White.copy(alpha = 0.07f))
                                    .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
                                    .padding(horizontal = 16.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f, fill = false)
                                    ) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_closed_eye),
                                            contentDescription = null,
                                            tint = Color.White.copy(alpha = 0.70f),
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = stringResource(R.string.comment_report_reason_mine_hide),
                                            color = Color.White.copy(alpha = 0.95f),
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_right),
                                        contentDescription = null,
                                        tint = Color.White.copy(alpha = 0.35f),
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
}

@Composable
fun CommentReportModal(
    comment: AppComment?,
    onDismiss: () -> Unit,
    onReport: (category: String) -> Unit,
    onBlockUser: ((comment: AppComment) -> Unit)? = null,
    triggerBounds: Rect? = null,
    hazeState: HazeState
) = CommentReportModal(
    comment = comment,
    onDismiss = onDismiss,
    onReport = { category, _ -> onReport(category) },
    onBlockUser = onBlockUser,
    triggerBounds = triggerBounds,
    hazeState = hazeState
)

@Composable
fun CommentBlockModal(
    comment: AppComment?,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    hazeState: HazeState
) {
    FlickTroveModal(
        isVisible = comment != null,
        onDismissRequest = onDismiss,
        hazeState = hazeState
    ) {
        if (comment != null) {
            val displayName = comment.userDisplayName.ifBlank { "User" }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.comment_block_user_title),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge
                )
                ModalCloseButton(onClick = onDismiss)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.comment_block_user_subtitle, displayName),
                color = Color.White.copy(0.70f),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .bounceClick { onDismiss() }
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.08f))
                        .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.comment_cancel_btn),
                        color = Color.White.copy(alpha = 0.85f),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .bounceClick { onConfirm() }
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFFFF3B30).copy(alpha = 0.22f))
                        .border(1.dp, Color(0xFFFF3B30).copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.comment_block_user_confirm),
                        color = Color(0xFFFF5252),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun CommentDeleteModal(
    comment: AppComment?,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    hazeState: HazeState
) {
    FlickTroveModal(
        isVisible = comment != null,
        onDismissRequest = onDismiss,
        hazeState = hazeState
    ) {
        if (comment != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.comment_delete_title),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge
                )
                ModalCloseButton(onClick = onDismiss)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.comment_delete_subtitle),
                color = Color.White.copy(0.70f),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .bounceClick { onDismiss() }
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.08f))
                        .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.comment_cancel_btn),
                        color = Color.White.copy(alpha = 0.85f),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .bounceClick { onConfirm() }
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFFFF3B30).copy(alpha = 0.22f))
                        .border(1.dp, Color(0xFFFF3B30).copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.comment_delete_title),
                        color = Color(0xFFFF5252),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun ExternalLinkWarningModal(
    url: String?,
    host: String,
    onDismiss: () -> Unit,
    onConfirm: (url: String) -> Unit,
    hazeState: HazeState
) {
    FlickTroveModal(
        isVisible = !url.isNullOrBlank(),
        onDismissRequest = onDismiss,
        hazeState = hazeState
    ) {
        if (!url.isNullOrBlank()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(androidx.compose.foundation.shape.CircleShape)
                            .background(Color(0xFF2DD4BF).copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_external_link),
                            contentDescription = null,
                            tint = Color(0xFF2DD4BF),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Text(
                        text = stringResource(R.string.external_link_warning_title),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge
                    )
                }
                ModalCloseButton(onClick = onDismiss)
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = stringResource(R.string.external_link_warning_desc),
                color = Color.White.copy(0.70f),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Domain & URL Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = 0.05f))
                    .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
                    .padding(14.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_world),
                            contentDescription = null,
                            tint = Color(0xFF2DD4BF),
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = host.ifBlank { "Website" },
                            color = Color.White,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = url,
                        color = Color.White.copy(alpha = 0.50f),
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Action Buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .bounceClick { onDismiss() }
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.08f))
                        .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.comment_cancel_btn),
                        color = Color.White.copy(alpha = 0.85f),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Box(
                    modifier = Modifier
                        .weight(1.2f)
                        .height(50.dp)
                        .bounceClick { onConfirm(url) }
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF2DD4BF))
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = stringResource(R.string.external_link_warning_continue),
                            color = Color.Black,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            painter = painterResource(id = R.drawable.ic_right),
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CommsUniInfoModal(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    hazeState: HazeState,
    triggerBounds: Rect? = null
) {
    val context = LocalContext.current
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
                .graphicsLayer { alpha = contentAlpha }
                .padding(horizontal = 22.dp, vertical = 24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.commsuni_info_title),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge
                )
                ModalCloseButton(onClick = onDismiss)
            }

            Spacer(modifier = Modifier.height(18.dp))

            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.08f))
                        .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_commsuni_logo),
                        contentDescription = "CommsUni",
                        modifier = Modifier.size(44.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.commsuni_info_desc),
                color = Color.White.copy(alpha = 0.85f),
                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .bounceClick {
                        try {
                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://commsuni.tv"))
                            context.startActivity(intent)
                        } catch (_: Exception) {}
                    }
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF2DD4BF))
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "commsuni.tv",
                        color = Color.Black,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        painter = painterResource(id = R.drawable.ic_right),
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

