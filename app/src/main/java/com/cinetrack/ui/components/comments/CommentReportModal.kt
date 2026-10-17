package com.cinetrack.ui.components.comments

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cinetrack.R
import com.cinetrack.data.model.AppComment
import com.cinetrack.ui.components.shared.FlickTroveModal
import com.cinetrack.ui.components.shared.ModalBackButton
import com.cinetrack.ui.components.shared.ModalCloseButton
import com.cinetrack.ui.components.shared.MorphGlassModal
import com.cinetrack.ui.utils.bounceClick
import dev.chrisbanes.haze.HazeState

private enum class CommentModalStep {
    MENU,
    REASONS,
    TV_TIME_OWNERSHIP,
    OTHER_DETAIL
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

    FlickTroveModal(
        isVisible = comment != null,
        onDismissRequest = onDismiss,
        hazeState = hazeState
    ) {
        val c = displayComment ?: return@FlickTroveModal
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
            modifier = Modifier.fillMaxWidth()
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

                        Spacer(modifier = Modifier.height(10.dp))

                        // Comment Preview Card
                        CommentContextPreviewCard(comment = c)

                        Spacer(modifier = Modifier.height(14.dp))

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
                                            onBlockUser(c)
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

                        Spacer(modifier = Modifier.height(10.dp))

                        // Comment Preview Card
                        CommentContextPreviewCard(comment = c)

                        Spacer(modifier = Modifier.height(12.dp))

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

                        Spacer(modifier = Modifier.height(10.dp))

                        // Comment Preview Card
                        CommentContextPreviewCard(comment = c)

                        Spacer(modifier = Modifier.height(12.dp))

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

                        Spacer(modifier = Modifier.height(10.dp))

                        // Comment Preview Card
                        CommentContextPreviewCard(comment = c)

                        Spacer(modifier = Modifier.height(12.dp))

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

