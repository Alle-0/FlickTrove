package com.cinetrack.ui.components

import com.cinetrack.util.buildTmdbImageUrl
import com.cinetrack.util.ImageType
import com.cinetrack.util.ImageQuality
import com.cinetrack.util.LocalImageQuality
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import androidx.compose.material3.Icon
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import com.cinetrack.R

@Composable
fun CastMemberComponent(id: Int, name: String, character: String, profilePath: String?, onClick: (Int) -> Unit) {
    Column(
        modifier = Modifier
            .width(85.dp)
            .clickable { onClick(id) }
            .padding(end = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            modifier = Modifier.size(56.dp),
            shape = CircleShape,
            color = Color.White.copy(alpha = 0.05f),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                val initials = androidx.compose.runtime.remember(name) {
                    name.split(" ")
                        .filter { it.isNotBlank() }
                        .mapNotNull { it.firstOrNull()?.toString() }
                        .take(2)
                        .joinToString("")
                        .uppercase()
                }
                if (initials.isNotEmpty()) {
                    Text(
                        text = initials,
                        color = Color.White.copy(alpha = 0.4f),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Icon(
                        imageVector = ImageVector.vectorResource(id = R.drawable.ic_persona),
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.3f),
                        modifier = Modifier.size(24.dp)
                    )
                }

                if (!profilePath.isNullOrBlank()) {
                    AsyncImage(
                        model = buildTmdbImageUrl(profilePath, ImageType.PROFILE, LocalImageQuality.current),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = name,
            color = Color.White,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            lineHeight = 14.sp,
            modifier = Modifier.height(28.dp)
        )
        
        Text(
            text = character,
            color = Color.White.copy(alpha = 0.5f),
            fontSize = 10.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}
