package com.cinetrack.ui.components.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.cinetrack.ui.utils.bounceClick
import com.cinetrack.ui.viewmodel.PersonStat
import com.cinetrack.util.ImageType
import com.cinetrack.util.LocalImageQuality
import com.cinetrack.util.buildTmdbImageUrl

// ════════════════════════════════════════════════════════════════════
// Person horizontal list (cast / directors)
// ════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PersonSection(
    people: List<PersonStat>,
    accentColor: Color,
    isExpanded: Boolean,
    onPersonClick: (Long, String?) -> Unit = { _, _ -> }
) {
    if (isExpanded) {
        // Full grid when expanded
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.Center,
            verticalArrangement = Arrangement.spacedBy(18.dp),
            maxItemsInEachRow = 4
        ) {
            people.forEach { person ->
                PersonAvatar(person = person, accentColor = accentColor, onPersonClick = onPersonClick)
            }
        }
    } else {
        // Horizontal list when collapsed
        LazyRow(
            contentPadding = PaddingValues(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            items(people.take(10), key = { it.id }, contentType = { "person" }) { person ->
                PersonAvatar(person = person, accentColor = accentColor, onPersonClick = onPersonClick)
            }
        }
    }
}

@Composable
fun PersonAvatar(
    person: PersonStat,
    accentColor: Color,
    onPersonClick: (Long, String?) -> Unit = { _, _ -> }
) {
    val avatarSize = 66.dp
    val badgeSize  = 22.dp
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(72.dp)
            .bounceClick { onPersonClick(person.id, person.profilePath) }
    ) {
        Box(modifier = Modifier.size(avatarSize)) {
            // Outer teal ring
            Box(
                modifier = Modifier
                    .size(avatarSize)
                    .clip(RoundedCornerShape(50))
                    .background(Color.White.copy(alpha = 0.06f))
                    .border(1.5.dp, accentColor.copy(alpha = 0.5f), RoundedCornerShape(50)),
                contentAlignment = Alignment.Center
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = person.name.split(" ")
                            .mapNotNull { it.firstOrNull()?.toString() }
                            .take(2)
                            .joinToString(""),
                        color = accentColor.copy(alpha = 0.7f),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black
                    )
                }
                if (!person.profilePath.isNullOrBlank()) {
                    AsyncImage(
                        model = buildTmdbImageUrl(person.profilePath, ImageType.PROFILE, LocalImageQuality.current),
                        contentDescription = person.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
            // Badge: teal pill with count, bottom-right
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .defaultMinSize(minWidth = badgeSize)
                    .height(badgeSize)
                    .background(accentColor, RoundedCornerShape(50))
                    .border(2.dp, Color(0xFF0D1117), RoundedCornerShape(50))
                    .padding(horizontal = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                CountingText(
                    target = person.count,
                    color = Color.Black,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = person.name,
            color = Color.White.copy(alpha = 0.85f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 13.sp
        )
    }
}
