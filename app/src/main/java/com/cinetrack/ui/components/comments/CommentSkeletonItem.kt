package com.cinetrack.ui.components.comments

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.cinetrack.R

@Composable
fun CommentSkeletonItem(
    index: Int,
    brush: Brush
) {
    val isReply = index == 1
    val isMediaComment = index == 2
    val isLongComment = index == 3

    val startPadding = if (isReply) 36.dp else 16.dp
    val avatarSize = if (isReply) 30.dp else 36.dp

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = startPadding, end = 16.dp, top = 10.dp, bottom = 10.dp)
            .drawBehind {
                if (isReply) {
                    val lineX = (-16.dp).toPx()
                    val branchY = 18.dp.toPx()
                    drawLine(
                        color = Color.White.copy(alpha = 0.12f),
                        start = Offset(lineX, -14.dp.toPx()),
                        end = Offset(lineX, branchY),
                        strokeWidth = 2.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = Color.White.copy(alpha = 0.12f),
                        start = Offset(lineX, branchY),
                        end = Offset(-4.dp.toPx(), branchY),
                        strokeWidth = 2.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
            }
    ) {
        // Avatar
        Box(
            modifier = Modifier
                .size(avatarSize)
                .clip(CircleShape)
                .background(brush)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            // Header row: Username + Pill Badge + Right timestamp
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                val nameWidth = when (index % 4) {
                    0 -> 76.dp
                    1 -> 64.dp
                    2 -> 88.dp
                    else -> 70.dp
                }
                Box(
                    modifier = Modifier
                        .size(width = nameWidth, height = 12.dp)
                        .clip(CircleShape)
                        .background(brush)
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Origin Badge Pill (CircleShape matching FlickTrove design standard)
                Box(
                    modifier = Modifier
                        .size(width = if (index % 2 == 0) 66.dp else 54.dp, height = 18.dp)
                        .clip(CircleShape)
                        .background(brush)
                )

                Spacer(modifier = Modifier.weight(1f))

                // Timestamp
                Box(
                    modifier = Modifier
                        .size(width = 36.dp, height = 10.dp)
                        .clip(CircleShape)
                        .background(brush)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Content
            if (isMediaComment) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.42f)
                        .height(12.dp)
                        .clip(CircleShape)
                        .background(brush)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.75f)
                        .height(130.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(brush),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_image),
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.15f),
                        modifier = Modifier.size(28.dp)
                    )
                }
            } else if (isLongComment) {
                Box(modifier = Modifier.fillMaxWidth(0.96f).height(12.dp).clip(CircleShape).background(brush))
                Spacer(modifier = Modifier.height(6.dp))
                Box(modifier = Modifier.fillMaxWidth(0.86f).height(12.dp).clip(CircleShape).background(brush))
                Spacer(modifier = Modifier.height(6.dp))
                Box(modifier = Modifier.fillMaxWidth(0.45f).height(12.dp).clip(CircleShape).background(brush))
            } else {
                val w1 = if (isReply) 0.88f else 0.92f
                val w2 = if (isReply) 0.52f else 0.65f
                Box(modifier = Modifier.fillMaxWidth(w1).height(12.dp).clip(CircleShape).background(brush))
                Spacer(modifier = Modifier.height(6.dp))
                Box(modifier = Modifier.fillMaxWidth(w2).height(12.dp).clip(CircleShape).background(brush))
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action Footer Row: Reply pill + Star with like count + Flag
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 38.dp, height = 12.dp)
                        .clip(CircleShape)
                        .background(brush)
                )

                Spacer(modifier = Modifier.width(16.dp))

                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(brush)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Box(
                    modifier = Modifier
                        .size(width = 16.dp, height = 10.dp)
                        .clip(CircleShape)
                        .background(brush)
                )

                Spacer(modifier = Modifier.width(16.dp))

                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(brush)
                )
            }
        }
    }
}
