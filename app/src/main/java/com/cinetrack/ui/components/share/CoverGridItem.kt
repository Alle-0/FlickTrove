package com.cinetrack.ui.components.share

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.cinetrack.R
import com.cinetrack.data.api.ImageItem
import com.cinetrack.ui.components.shared.shimmerEffect
import com.cinetrack.ui.utils.ColorUtils
import com.cinetrack.ui.utils.bounceClick
import com.cinetrack.util.ImageQuality
import com.cinetrack.util.ImageType
import com.cinetrack.util.VibrationHelper
import com.cinetrack.util.buildTmdbImageUrl

@Composable
internal fun CoverGridItem(
    item: ImageItem,
    isSelected: Boolean,
    isPosterTab: Boolean,
    isLogoTab: Boolean,
    itemAspectRatio: Float,
    accentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val imageUrl = remember(item.filePath) {
        val imageType = when {
            isPosterTab -> ImageType.POSTER
            isLogoTab -> ImageType.LOGO
            else -> ImageType.BACKDROP
        }
        buildTmdbImageUrl(item.filePath, imageType, ImageQuality.HIGH)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(itemAspectRatio)
            .bounceClick(scaleDown = 0.95f) {
                VibrationHelper.vibrateTick(context)
                onClick()
            }
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF1E1E24))
            .border(
                width = if (isSelected) 2.5.dp else 1.dp,
                color = if (isSelected) accentColor else Color.White.copy(alpha = 0.10f),
                shape = RoundedCornerShape(12.dp)
            )
    ) {
        SubcomposeAsyncImage(
            model = ImageRequest.Builder(context)
                .data(imageUrl)
                .crossfade(true)
                .build(),
            contentDescription = null,
            contentScale = if (isLogoTab) ContentScale.Fit else ContentScale.Crop,
            modifier = if (isLogoTab) {
                Modifier
                    .fillMaxSize()
                    .padding(14.dp)
            } else {
                Modifier.fillMaxSize()
            },
            loading = {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF1E1E24))
                        .shimmerEffect()
                )
            }
        )

        // Top-right selection badge
        if (isSelected) {
            Box(
                modifier = Modifier
                    .padding(6.dp)
                    .align(Alignment.TopEnd)
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(accentColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_tick),
                    contentDescription = stringResource(R.string.share_cover_picker_selected),
                    tint = ColorUtils.contentColorForAccent(accentColor),
                    modifier = Modifier.size(13.dp)
                )
            }
        }
    }
}
