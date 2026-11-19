package com.cinetrack.ui.components.search

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cinetrack.R
import com.cinetrack.ui.utils.bounceClick
import com.cinetrack.ui.utils.mirrorInRtl

/**
 * Centered button at the bottom of search results to discover more trending items.
 */
@Composable
fun DiscoverMoreTrendingButton(
    onClick: () -> Unit
) {
    val accentColor = MaterialTheme.colorScheme.primary
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp, bottom = 28.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .bounceClick(onClick = onClick)
                .background(
                    color = accentColor.copy(alpha = 0.12f),
                    shape = CircleShape
                )
                .border(
                    width = 1.dp,
                    color = accentColor.copy(alpha = 0.35f),
                    shape = CircleShape
                )
                .padding(vertical = 8.dp, horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = stringResource(R.string.search_discover_more),
                color = accentColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.width(6.dp))
            Icon(
                imageVector = ImageVector.vectorResource(id = R.drawable.ic_right),
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(13.dp).mirrorInRtl()
            )
        }
    }
}

/**
 * End-of-carousel card prompting the user to discover more trending titles.
 */
@Composable
fun DiscoverMoreTrendingCard(
    cardWidth: Dp,
    columns: Int,
    onClick: () -> Unit
) {
    val accentColor = MaterialTheme.colorScheme.primary
    if (columns == 1) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(108.dp)
                .bounceClick(onClick = onClick)
                .background(
                    color = Color(0xFF191924),
                    shape = RoundedCornerShape(12.dp)
                )
                .border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            accentColor.copy(alpha = 0.5f),
                            accentColor.copy(alpha = 0.12f)
                        )
                    ),
                    shape = RoundedCornerShape(12.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(accentColor.copy(alpha = 0.16f), CircleShape)
                        .border(1.dp, accentColor.copy(alpha = 0.45f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(id = R.drawable.ic_right),
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(18.dp).mirrorInRtl()
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Text(
                    text = stringResource(R.string.search_discover_more),
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    } else {
        val cardHeight = cardWidth * 1.5f
        Box(
            modifier = Modifier
                .width(cardWidth)
                .height(cardHeight)
                .bounceClick(onClick = onClick)
                .background(
                    color = Color(0xFF191924),
                    shape = RoundedCornerShape(12.dp)
                )
                .border(
                    width = 1.2.dp,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            accentColor.copy(alpha = 0.55f),
                            accentColor.copy(alpha = 0.1f)
                        )
                    ),
                    shape = RoundedCornerShape(12.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(accentColor.copy(alpha = 0.18f), CircleShape)
                        .border(1.5.dp, accentColor.copy(alpha = 0.5f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = ImageVector.vectorResource(id = R.drawable.ic_right),
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(24.dp).mirrorInRtl()
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.search_discover_more),
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
