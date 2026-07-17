package com.cinetrack.ui.components.detail

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.cinetrack.R
import com.cinetrack.data.api.Person
import com.cinetrack.ui.utils.bounceClick
import com.cinetrack.util.ImageType
import com.cinetrack.util.LocalImageQuality
import com.cinetrack.util.buildTmdbImageUrl

@Composable
fun PersonHeroHeader(
    person: Person,
    paddingValues: PaddingValues,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxWidth().height(480.dp)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF2E2E48),
                            Color(0xFF1E1E32),
                            Color(0xFF0A0A0A)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            val initials = remember(person.name) {
                person.name.split(" ")
                    .filter { it.isNotBlank() }
                    .mapNotNull { it.firstOrNull()?.toString() }
                    .take(2)
                    .joinToString("")
                    .uppercase()
            }
            Box(
                modifier = Modifier
                    .offset(y = (-30).dp)
                    .size(130.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.04f))
                    .border(1.5.dp, Color.White.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (initials.isNotEmpty()) {
                    Text(
                        text = initials,
                        color = Color.White.copy(alpha = 0.35f),
                        fontSize = 44.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp
                    )
                } else {
                    Icon(
                        imageVector = ImageVector.vectorResource(id = R.drawable.ic_persona),
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = Color.White.copy(alpha = 0.3f)
                    )
                }
            }
        }

        if (!person.profilePath.isNullOrBlank()) {
            AsyncImage(
                model = buildTmdbImageUrl(person.profilePath, ImageType.PROFILE, LocalImageQuality.current),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp + paddingValues.calculateTopPadding())
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Black.copy(alpha = 0.85f), Color.Transparent)
                    )
                )
        )

        Box(
            modifier = Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    listOf(
                        Color.Transparent,
                        Color.Transparent,
                        Color(0xFF0A0A0A).copy(alpha = 0.55f),
                        Color(0xFF0A0A0A)
                    )
                )
            )
        )
    }
}

@Composable
fun PersonBioAndInfoSection(
    person: Person,
    showFullBio: Boolean,
    onToggleBio: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(horizontal = 24.dp)) {
        Text(
            text = person.name,
            color = Color.White,
            fontSize = 42.sp,
            fontWeight = FontWeight.Black,
            lineHeight = 46.sp,
            textAlign = TextAlign.Start,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = person.knownForDepartment ?: "",
            color = Color.White.copy(alpha = 0.4f),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        person.biography?.let { bio ->
            Column(
                modifier = Modifier
                    .bounceClick(scaleDown = 0.99f) { onToggleBio() }
            ) {
                AnimatedContent(
                    targetState = showFullBio,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(350)) togetherWith
                                fadeOut(animationSpec = tween(350)) using
                                SizeTransform(clip = true) { _, _ ->
                                    spring(
                                        dampingRatio = Spring.DampingRatioNoBouncy,
                                        stiffness = Spring.StiffnessMediumLow
                                    )
                                }
                    },
                    label = "BioExpansion"
                ) { targetExpanded ->
                    Text(
                        text = bio,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            lineHeight = 20.sp,
                            letterSpacing = 0.2.sp,
                            fontSize = 14.sp
                        ),
                        color = Color.White.copy(alpha = 0.7f),
                        maxLines = if (targetExpanded) Int.MAX_VALUE else 4,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (bio.length > 200) {
                    Text(
                        text = if (showFullBio) stringResource(R.string.person_read_less) else stringResource(R.string.person_read_more),
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color.White,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }
            }
        } ?: Text(
            text = stringResource(R.string.person_no_bio),
            style = MaterialTheme.typography.bodyMedium.copy(
                lineHeight = 20.sp,
                letterSpacing = 0.2.sp,
                fontSize = 14.sp
            ),
            color = Color.White.copy(alpha = 0.7f)
        )

        Spacer(modifier = Modifier.height(30.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column(modifier = Modifier.weight(1f)) {
                PersonInfoItem(
                    ImageVector.vectorResource(id = R.drawable.ic_star),
                    stringResource(R.string.person_birthday),
                    person.birthday ?: stringResource(R.string.person_nd)
                )
                PersonInfoItem(
                    ImageVector.vectorResource(id = R.drawable.ic_partenone),
                    stringResource(R.string.person_birth_place),
                    person.placeOfBirth ?: stringResource(R.string.person_nd)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                PersonInfoItem(
                    ImageVector.vectorResource(id = R.drawable.ic_documento),
                    stringResource(R.string.person_known_for),
                    person.knownForDepartment ?: stringResource(R.string.person_nd)
                )
                PersonInfoItem(
                    ImageVector.vectorResource(id = R.drawable.ic_star),
                    stringResource(R.string.person_popularity),
                    person.popularity?.let { "%.1f".format(it) } ?: stringResource(R.string.person_nd)
                )
            }
        }
    }
}

@Composable
fun PersonInfoItem(icon: ImageVector, label: String, value: String) {
    Row(modifier = Modifier.padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Surface(color = Color.White.copy(alpha = 0.03f), shape = RoundedCornerShape(8.dp), modifier = Modifier.size(32.dp)) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.4f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(label, color = Color.White.copy(alpha = 0.3f), fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.2.sp)
            Text(value, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
    }
}
