package com.cinetrack.ui.components.updates

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.cinetrack.R
import com.cinetrack.data.model.SocialNotification
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

@Composable
fun SocialNotificationCard(
    notification: SocialNotification,
    onClick: () -> Unit
) {
    val alpha = if (notification.isRead) 0.5f else 1f
    
    val dateText = notification.createdAt?.toDate()?.toInstant()
        ?.atZone(ZoneId.systemDefault())
        ?.toLocalDateTime()
        ?.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT).withLocale(Locale.getDefault()))
        ?: ""

    val actionText = if (notification.type == "like") {
        stringResource(R.string.updates_social_liked)
    } else {
        stringResource(R.string.updates_social_replied)
    }
    
    val iconRes = if (notification.type == "like") {
        R.drawable.ic_heart
    } else {
        R.drawable.ic_comment
    }
    
    val iconColor = if (notification.type == "like") {
        Color.Red
    } else {
        MaterialTheme.colorScheme.primary
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Immagine media (con iconcina in basso a destra)
        Box(
            modifier = Modifier.size(60.dp)
        ) {
            AsyncImage(
                model = notification.mediaImage,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.DarkGray)
            )
            
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 6.dp, y = 6.dp)
                    .size(24.dp)
                    .background(MaterialTheme.colorScheme.surface, CircleShape)
                    .padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(id = iconRes),
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = "${notification.senderName} $actionText su ${notification.mediaTitle}",
                color = Color.White.copy(alpha = alpha),
                fontSize = 14.sp,
                lineHeight = 18.sp,
                fontWeight = if (notification.isRead) FontWeight.Normal else FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = dateText,
                color = Color.White.copy(alpha = alpha * 0.6f),
                fontSize = 12.sp
            )
        }
        
        if (!notification.isRead) {
            Spacer(modifier = Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape)
            )
        }
    }
}
