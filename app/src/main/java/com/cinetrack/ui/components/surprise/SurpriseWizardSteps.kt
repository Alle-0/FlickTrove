package com.cinetrack.ui.components.surprise

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cinetrack.R
import com.cinetrack.ui.components.mascot.AiSparkleOrb
import com.cinetrack.ui.components.mascot.IrisMascot
import com.cinetrack.ui.components.mascot.IrisMood
import com.cinetrack.ui.utils.bounceClick
import com.cinetrack.ui.viewmodel.SurpriseCompany
import com.cinetrack.ui.viewmodel.SurpriseMood
import com.cinetrack.ui.viewmodel.SurpriseTime

/**
 * Step components for Surprise Me wizard: Choice, Time, Mood, Company, and Loading.
 */
@Composable
fun SurpriseChoiceStep(
    onRandomClick: () -> Unit,
    onPromptClick: () -> Unit,
    onEmotionalClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            OptionGridCard(
                title = stringResource(R.string.surprise_mode_random),
                description = stringResource(R.string.surprise_mode_random_desc),
                icon = ImageVector.vectorResource(R.drawable.ic_dice),
                onClick = onRandomClick,
                modifier = Modifier.weight(1f)
            )
            OptionGridCard(
                title = stringResource(R.string.surprise_mode_emotional),
                description = stringResource(R.string.surprise_mode_emotional_desc),
                icon = ImageVector.vectorResource(R.drawable.ic_sparkle),
                onClick = onEmotionalClick,
                modifier = Modifier.weight(1f)
            )
        }

        PromptOptionCard(
            title = stringResource(R.string.surprise_mode_prompt),
            description = stringResource(R.string.surprise_mode_prompt_desc),
            icon = ImageVector.vectorResource(R.drawable.ic_pencil),
            onClick = onPromptClick,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun SurpriseTimeStep(onSelect: (SurpriseTime) -> Unit) {
    WizardStep(
        title = stringResource(R.string.surprise_q_time),
        options = listOf(
            WizardOption(stringResource(R.string.surprise_time_short), ImageVector.vectorResource(R.drawable.ic_crono), { onSelect(SurpriseTime.SHORT) }),
            WizardOption(stringResource(R.string.surprise_time_medium), ImageVector.vectorResource(R.drawable.ic_clock), { onSelect(SurpriseTime.MEDIUM) }),
            WizardOption(stringResource(R.string.surprise_time_long), ImageVector.vectorResource(R.drawable.ic_clessidra), { onSelect(SurpriseTime.LONG) }),
            WizardOption(stringResource(R.string.surprise_any), ImageVector.vectorResource(R.drawable.ic_sparkle), { onSelect(SurpriseTime.ANY) })
        )
    )
}

@Composable
fun SurpriseMoodStep(onSelect: (SurpriseMood) -> Unit) {
    WizardStep(
        title = stringResource(R.string.surprise_q_mood),
        options = listOf(
            WizardOption(stringResource(R.string.surprise_mood_laugh), ImageVector.vectorResource(R.drawable.ic_laugh), { onSelect(SurpriseMood.LAUGH) }),
            WizardOption(stringResource(R.string.surprise_mood_tension), ImageVector.vectorResource(R.drawable.ic_bolt), { onSelect(SurpriseMood.TENSION) }),
            WizardOption(stringResource(R.string.surprise_mood_emotion), ImageVector.vectorResource(R.drawable.ic_goccia), { onSelect(SurpriseMood.EMOTION) }),
            WizardOption(stringResource(R.string.surprise_mood_escape), ImageVector.vectorResource(R.drawable.ic_rocket), { onSelect(SurpriseMood.ESCAPE) }),
            WizardOption(stringResource(R.string.surprise_mood_any), ImageVector.vectorResource(R.drawable.ic_sparkle), { onSelect(SurpriseMood.ANY) })
        )
    )
}

@Composable
fun SurpriseCompanyStep(onSelect: (SurpriseCompany) -> Unit) {
    WizardStep(
        title = stringResource(R.string.surprise_q_company),
        options = listOf(
            WizardOption(stringResource(R.string.surprise_company_alone), ImageVector.vectorResource(R.drawable.ic_persona), { onSelect(SurpriseCompany.ALONE) }),
            WizardOption(stringResource(R.string.surprise_company_couple), ImageVector.vectorResource(R.drawable.ic_heart), { onSelect(SurpriseCompany.COUPLE) }),
            WizardOption(stringResource(R.string.surprise_company_friends), ImageVector.vectorResource(R.drawable.ic_people), { onSelect(SurpriseCompany.FRIENDS) }),
            WizardOption(stringResource(R.string.surprise_company_family), ImageVector.vectorResource(R.drawable.ic_home), { onSelect(SurpriseCompany.FAMILY) })
        )
    )
}

@Composable
fun SurpriseLoadingStep(isGhost: Boolean = false, isSearchingWithAi: Boolean = false) {
    val infiniteTransition = rememberInfiniteTransition(label = "loading_infinite")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (isSearchingWithAi) {
            AiSparkleOrb(
                size = 84.dp,
                isSearching = true,
                accentColor = MaterialTheme.colorScheme.primary
            )
        } else {
            IrisMascot(
                mood = IrisMood.SEARCHING,
                size = 84.dp
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        Text(
            text = stringResource(R.string.surprise_loading_1),
            color = Color.White.copy(alpha = 0.9f),
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            ),
            textAlign = TextAlign.Center
        )
    }
}

private data class WizardOption(val label: String, val icon: ImageVector, val onClick: () -> Unit)

@Composable
private fun WizardStep(title: String, options: List<WizardOption>) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(color = Color.White),
            modifier = Modifier.padding(bottom = 24.dp)
        )
        val chunkedOptions = options.chunked(2)
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            chunkedOptions.forEach { rowOptions ->
                Row(
                    modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Max),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    rowOptions.forEach { option ->
                        OptionGridCard(
                            title = option.label,
                            icon = option.icon,
                            isFullWidth = rowOptions.size == 1,
                            onClick = option.onClick,
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun OptionGridCard(
    title: String,
    description: String? = null,
    icon: ImageVector,
    isFullWidth: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .then(
                if (isFullWidth) {
                    Modifier.heightIn(min = if (description != null) 90.dp else 72.dp)
                } else {
                    Modifier.heightIn(min = if (description != null) 110.dp else 90.dp)
                }
            )
            .bounceClick(scaleDown = 0.92f, onClick = onClick)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.04f))
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        }
        Spacer(modifier = Modifier.height(if (description != null) 10.dp else 12.dp))
        Text(
            text = title,
            color = Color.White,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            textAlign = TextAlign.Center,
            lineHeight = 16.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        if (description != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                color = Color.White.copy(alpha = 0.6f),
                style = MaterialTheme.typography.bodySmall,
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                lineHeight = 14.sp
            )
        }
    }
}

@Composable
private fun PromptOptionCard(
    title: String,
    description: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .bounceClick(scaleDown = 0.94f, onClick = onClick)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.04f))
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = title,
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                        .padding(horizontal = 5.dp, vertical = 1.5.dp)
                ) {
                    Text(
                        text = "IRIS",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp
                        )
                    )
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                color = Color.White.copy(alpha = 0.6f),
                style = MaterialTheme.typography.bodySmall,
                fontSize = 11.sp,
                lineHeight = 14.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        Icon(
            imageVector = ImageVector.vectorResource(R.drawable.ic_right),
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.4f),
            modifier = Modifier.size(16.dp)
        )
    }
}
