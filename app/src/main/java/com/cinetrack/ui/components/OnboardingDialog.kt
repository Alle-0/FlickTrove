package com.cinetrack.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import kotlin.math.abs
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cinetrack.R
import com.cinetrack.ui.components.glass.hazeGlass
import dev.chrisbanes.haze.HazeState
import kotlinx.coroutines.launch

data class OnboardingSlide(
    val titleRes: Int,
    val subtitleRes: Int,
    val descriptionRes: Int,
    val iconRes: Int,
    val imageRes: Int? = null
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingDialog(
    accentColor: Color = Color(0xFF2DD4BF),
    hazeState: HazeState? = null,
    onDismiss: () -> Unit
) {
    val slides = remember {
        listOf(
            OnboardingSlide(
                titleRes = R.string.onboarding_slide1_title,
                subtitleRes = R.string.onboarding_slide1_sub,
                descriptionRes = R.string.onboarding_slide1_desc,
                iconRes = R.drawable.ic_star_piena,
                imageRes = R.drawable.onboarding_img_1
            ),
            OnboardingSlide(
                titleRes = R.string.onboarding_slide2_title,
                subtitleRes = R.string.onboarding_slide2_sub,
                descriptionRes = R.string.onboarding_slide2_desc,
                iconRes = R.drawable.ic_cartella,
                imageRes = R.drawable.onboarding_img_2
            ),
            OnboardingSlide(
                titleRes = R.string.onboarding_slide3_title,
                subtitleRes = R.string.onboarding_slide3_sub,
                descriptionRes = R.string.onboarding_slide3_desc,
                iconRes = R.drawable.ic_tick,
                imageRes = R.drawable.onboarding_img_3
            ),
            OnboardingSlide(
                titleRes = R.string.onboarding_slide4_title,
                subtitleRes = R.string.onboarding_slide4_sub,
                descriptionRes = R.string.onboarding_slide4_desc,
                iconRes = R.drawable.ic_link,
                imageRes = R.drawable.onboarding_img_4
            )
        )
    }

    val pagerState = rememberPagerState(pageCount = { slides.size })
    val coroutineScope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.65f)) // Scrim
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            )
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {} // Consume clicks inside modal
                )
                .hazeGlass(
                    state = hazeState,
                    shape = RoundedCornerShape(28.dp)
                )
                .border(
                    1.dp,
                    Brush.verticalGradient(
                        listOf(
                            Color.White.copy(alpha = 0.25f),
                            Color.White.copy(alpha = 0.05f)
                        )
                    ),
                    RoundedCornerShape(28.dp)
                )
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
                    // Top Bar with Back and Skip
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Back Button in top left (hidden on first slide)
                        if (pagerState.currentPage > 0) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.08f))
                                    .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape)
                                    .clickable {
                                        coroutineScope.launch {
                                            pagerState.animateScrollToPage(pagerState.currentPage - 1)
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = ImageVector.vectorResource(R.drawable.ic_left),
                                    contentDescription = "Back",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        } else {
                            Spacer(modifier = Modifier.size(38.dp))
                        }

                        // Skip Button in top right
                        Text(
                            text = stringResource(R.string.onboarding_skip),
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White.copy(alpha = 0.6f)
                            ),
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onDismiss() }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(330.dp)
                    ) { page ->
                        val slide = slides[page]
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            if (slide.imageRes != null) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(185.dp)
                                        .clip(RoundedCornerShape(20.dp))
                                        .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(20.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Image(
                                        painter = painterResource(id = slide.imageRes),
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        alignment = Alignment.Center,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            } else {
                                SlideIconBadge(
                                    iconRes = slide.iconRes,
                                    accentColor = accentColor
                                )
                            }

                            Spacer(modifier = Modifier.height(24.dp))

                            Text(
                                text = stringResource(slide.subtitleRes),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 1.5.sp
                                ),
                                color = accentColor
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = stringResource(slide.titleRes),
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Black,
                                    fontSize = 22.sp
                                ),
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = stringResource(slide.descriptionRes),
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = Color.White.copy(alpha = 0.7f),
                                    lineHeight = 20.sp
                                ),
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Pager Animated Indicators (matching Surprise Me style)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(slides.size) { index ->
                            val isCurrent = pagerState.currentPage == index
                            val isPast = pagerState.currentPage > index

                            val animatedWidth by animateDpAsState(
                                targetValue = if (isCurrent) 24.dp else 8.dp,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessLow
                                ),
                                label = "indicatorWidth"
                            )
                            val animatedColor by animateColorAsState(
                                targetValue = if (isCurrent || isPast) accentColor else Color.White.copy(alpha = 0.2f),
                                animationSpec = tween(durationMillis = 300),
                                label = "indicatorColor"
                            )
                            Box(
                                modifier = Modifier
                                    .height(8.dp)
                                    .width(animatedWidth)
                                    .clip(CircleShape)
                                    .background(animatedColor)
                                    .clickable {
                                        coroutineScope.launch {
                                            pagerState.animateScrollToPage(index)
                                        }
                                    }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    // Action Button
                    val isLastPage = pagerState.currentPage == slides.size - 1
                    Button(
                        onClick = {
                            if (isLastPage) {
                                onDismiss()
                            } else {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accentColor
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        Text(
                            text = if (isLastPage) stringResource(R.string.onboarding_start) else stringResource(R.string.onboarding_next),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                        )
                    }
                }
            }
        }

@Composable
private fun SlideIconBadge(
    iconRes: Int,
    accentColor: Color
) {
    Box(
        modifier = Modifier
            .size(88.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xFF1E1E2A))
            .border(
                width = 1.5.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        accentColor.copy(alpha = 0.6f),
                        accentColor.copy(alpha = 0.15f)
                    )
                ),
                shape = RoundedCornerShape(24.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = ImageVector.vectorResource(id = iconRes),
            contentDescription = null,
            tint = accentColor,
            modifier = Modifier.size(40.dp)
        )
    }
}

