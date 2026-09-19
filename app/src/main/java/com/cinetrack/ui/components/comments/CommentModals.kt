package com.cinetrack.ui.components.comments

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cinetrack.R
import com.cinetrack.data.model.AppComment
import com.cinetrack.ui.components.shared.FlickTroveModal
import com.cinetrack.ui.utils.bounceClick
import dev.chrisbanes.haze.HazeState

@Composable
fun CommentReportModal(
    comment: AppComment?,
    onDismiss: () -> Unit,
    onReport: (category: String) -> Unit,
    hazeState: HazeState
) {
    FlickTroveModal(
        isVisible = comment != null,
        onDismissRequest = onDismiss,
        hazeState = hazeState
    ) {
        if (comment != null) {
            Text(
                text = stringResource(R.string.comment_report_title),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.comment_report_subtitle),
                color = Color.White.copy(0.8f)
            )
            Spacer(modifier = Modifier.height(24.dp))

            val reportButtonModifier = Modifier
                .fillMaxWidth()
                .background(Color.Red.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                .border(1.dp, Color.Red.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                .padding(vertical = 14.dp)

            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .bounceClick { onReport("SPOILER") }
                        .then(reportButtonModifier),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        stringResource(R.string.comment_report_spoiler),
                        color = Color.Red,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Box(
                    modifier = Modifier
                        .bounceClick { onReport("INAPPROPRIATE_CONTENT") }
                        .then(reportButtonModifier),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        stringResource(R.string.comment_report_inappropriate_content),
                        color = Color.Red,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Box(
                    modifier = Modifier
                        .bounceClick { onReport("INAPPROPRIATE_USER") }
                        .then(reportButtonModifier),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        stringResource(R.string.comment_report_inappropriate_user),
                        color = Color.Red,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Box(
                    modifier = Modifier
                        .bounceClick { onDismiss() }
                        .fillMaxWidth()
                        .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        stringResource(R.string.comment_cancel_btn),
                        color = Color.White.copy(alpha = 0.8f),
                        fontWeight = FontWeight.SemiBold
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
            Text(
                stringResource(R.string.comment_delete_title),
                color = Color.White,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                stringResource(R.string.comment_delete_subtitle),
                color = Color.White.copy(0.8f)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .weight(1f)
                        .bounceClick { onDismiss() },
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White.copy(alpha = 0.08f),
                        contentColor = Color.White.copy(alpha = 0.85f)
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.12f))
                ) {
                    Text(
                        text = stringResource(R.string.comment_cancel_btn),
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Button(
                    onClick = onConfirm,
                    modifier = Modifier
                        .weight(1f)
                        .bounceClick { onConfirm() },
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Red.copy(alpha = 0.15f),
                        contentColor = Color(0xFFFF5252)
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.Red.copy(alpha = 0.3f))
                ) {
                    Text(
                        text = stringResource(R.string.comment_delete_title),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
