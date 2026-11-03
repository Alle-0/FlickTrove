package com.cinetrack.ui.components.badge

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cinetrack.R
import com.cinetrack.data.repository.BadgeRepository
import com.cinetrack.data.repository.BadgeRepositoryEntryPoint
import com.cinetrack.ui.theme.Gold
import com.cinetrack.ui.utils.bounceClick
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.launch

@Composable
fun ExecutiveProducerDonationSection(
    isUnlocked: Boolean,
    onVerifySupporter: (suspend (String) -> BadgeRepository.SupporterVerificationResult)?,
    modifier: Modifier = Modifier,
    onExpandedChange: ((Boolean) -> Unit)? = null,
    onFocusField: (() -> Unit)? = null,
    onUpdateCreditSettings: (suspend (String, Boolean) -> Boolean)? = null,
    onGetCreditSettings: (() -> Pair<String, Boolean>)? = null,
    onFetchSupporters: (suspend () -> List<String>)? = null
) {
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val uriHandler = LocalUriHandler.current
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current

    val badgeRepo = remember {
        try {
            EntryPointAccessors.fromApplication(
                context.applicationContext,
                BadgeRepositoryEntryPoint::class.java
            ).badgeRepository()
        } catch (_: Exception) {
            null
        }
    }

    var emailInput by remember { mutableStateOf("") }
    var isVerifying by remember { mutableStateOf(false) }
    var verificationResult by remember { mutableStateOf<BadgeRepository.SupporterVerificationResult?>(null) }
    var showCreditsModal by remember { mutableStateOf(false) }

    // Preferenze Titoli di Coda
    var creditNameInput by remember { mutableStateOf("") }
    var showInCredits by remember { mutableStateOf(true) }
    var isSavingCredits by remember { mutableStateOf(false) }
    var creditsSavedFeedback by remember { mutableStateOf(false) }

    val currentUser = FirebaseAuth.getInstance().currentUser
    val currentEmail = currentUser?.email

    LaunchedEffect(Unit) {
        if (!currentEmail.isNullOrBlank() && emailInput.isBlank()) {
            emailInput = currentEmail
        }

        val settings = onGetCreditSettings?.invoke() ?: badgeRepo?.getSupporterCreditSettings()
        if (settings != null) {
            creditNameInput = settings.first
            showInCredits = settings.second
        }
    }

    if (showCreditsModal) {
        CinemaCreditsModal(
            isVisible = true,
            onDismissRequest = { showCreditsModal = false },
            onFetchSupporters = {
                onFetchSupporters?.invoke() ?: badgeRepo?.fetchPublicCreditsSupporters().orEmpty()
            }
        )
    }

    Column(modifier = modifier.fillMaxWidth()) {
        if (isUnlocked) {
            // Card celebrativa dorata per il mecenate
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color(0xFFEAB308).copy(alpha = 0.14f),
                                Color(0xFFCA8A04).copy(alpha = 0.05f)
                            )
                        )
                    )
                    .border(1.dp, Color(0xFFEAB308).copy(alpha = 0.38f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 14.dp, vertical = 14.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_badge_shaking_hands),
                        contentDescription = null,
                        tint = Color(0xFFFDE047),
                        modifier = Modifier.size(34.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.trophy_producer_already_unlocked_title),
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        ),
                        color = Color(0xFFFEF08A),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = stringResource(R.string.trophy_producer_already_unlocked_desc),
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 12.sp,
                            lineHeight = 17.sp
                        ),
                        color = Color.White.copy(alpha = 0.75f),
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Sezione GDPR-Compliant: Gestione Presenza nei Titoli di Coda
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = 0.04f))
                    .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(16.dp))
                    .padding(14.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = stringResource(R.string.trophy_producer_credits_title),
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp
                        ),
                        color = Gold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.trophy_producer_credits_desc),
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 11.5.sp,
                            lineHeight = 16.sp
                        ),
                        color = Color.White.copy(alpha = 0.65f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Switch Opt-in
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.trophy_producer_credits_switch),
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                            color = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.weight(1f)
                        )
                        Switch(
                            checked = showInCredits,
                            onCheckedChange = { showInCredits = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.Black,
                                checkedTrackColor = Gold,
                                uncheckedThumbColor = Color.White.copy(alpha = 0.6f),
                                uncheckedTrackColor = Color.White.copy(alpha = 0.1f)
                            )
                        )
                    }

                    if (showInCredits) {
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = creditNameInput,
                            onValueChange = {
                                if (it.length <= 30) {
                                    creditNameInput = it
                                    creditsSavedFeedback = false
                                }
                            },
                            placeholder = {
                                Text(
                                    text = stringResource(R.string.trophy_producer_credits_nickname_hint),
                                    color = Color.White.copy(alpha = 0.35f),
                                    fontSize = 12.5.sp
                                )
                            },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp)),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color.White.copy(alpha = 0.03f),
                                unfocusedContainerColor = Color.White.copy(alpha = 0.03f),
                                unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                                focusedBorderColor = Gold.copy(alpha = 0.80f),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                cursorColor = Gold
                            ),
                            textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Bottone Salva
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .bounceClick(enabled = !isSavingCredits) {
                                    focusManager.clearFocus()
                                    isSavingCredits = true
                                    scope.launch {
                                        val success = onUpdateCreditSettings?.invoke(creditNameInput, showInCredits)
                                            ?: badgeRepo?.updateSupporterCreditSettings(creditNameInput, showInCredits)
                                            ?: true
                                        isSavingCredits = false
                                        creditsSavedFeedback = success
                                        if (success) {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        }
                                    }
                                }
                                .clip(RoundedCornerShape(10.dp))
                                .background(Gold),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSavingCredits) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = Color.Black,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text(
                                    text = if (creditsSavedFeedback) stringResource(R.string.trophy_producer_credits_saved) else stringResource(R.string.trophy_producer_credits_save_btn),
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.5.sp
                                    ),
                                    color = Color.Black
                                )
                            }
                        }

                        // Bottone Guarda i Titoli di Coda
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .bounceClick {
                                    focusManager.clearFocus()
                                    showCreditsModal = true
                                }
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color.White.copy(alpha = 0.08f))
                                .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.trophy_producer_view_credits),
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 12.sp
                                ),
                                color = Color.White
                            )
                        }
                    }
                }
            }
        } else {
            // Pulsanti compatti e discreti per sostenere su Ko-fi o PayPal
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .bounceClick {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            uriHandler.openUri("https://ko-fi.com/alle0")
                        }
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFFF5E5B).copy(alpha = 0.16f))
                        .border(1.dp, Color(0xFFFF5E5B).copy(alpha = 0.35f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_kofi),
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Ko-fi",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.5.sp
                            ),
                            color = Color.White
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .bounceClick {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            uriHandler.openUri("https://paypal.me/AlessandroBasile0")
                        }
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF0079C1).copy(alpha = 0.16f))
                        .border(1.dp, Color(0xFF0079C1).copy(alpha = 0.35f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_paypal),
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "PayPal",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.5.sp
                            ),
                            color = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Card Verifica Donazione Espandibile (per chi ha già donato con email differente)
            var isExpanded by remember { mutableStateOf(false) }
            val arrowRotation by animateFloatAsState(
                targetValue = if (isExpanded) 90f else 0f,
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                label = "ArrowRotation"
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = 0.04f))
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                    .animateContentSize()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .bounceClick {
                                isExpanded = !isExpanded
                                onExpandedChange?.invoke(isExpanded)
                            }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.trophy_producer_verify_section_title),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            ),
                            color = Color.White
                        )

                        Icon(
                            imageVector = ImageVector.vectorResource(id = R.drawable.ic_right),
                            contentDescription = if (isExpanded) "Collassa" else "Espandi",
                            tint = if (isExpanded) Color.White else Color.White.copy(alpha = 0.40f),
                            modifier = Modifier
                                .size(14.dp)
                                .graphicsLayer { rotationZ = arrowRotation }
                        )
                    }

                    AnimatedVisibility(
                        visible = isExpanded,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 14.dp, end = 14.dp, bottom = 14.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.trophy_producer_verify_section_desc),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 11.5.sp,
                                    lineHeight = 16.sp
                                ),
                                color = Color.White.copy(alpha = 0.60f)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedTextField(
                                value = emailInput,
                                onValueChange = {
                                    emailInput = it
                                    if (verificationResult != null) verificationResult = null
                                },
                                placeholder = {
                                    Text(
                                        text = stringResource(R.string.trophy_producer_email_placeholder),
                                        color = Color.White.copy(alpha = 0.35f),
                                        fontSize = 13.sp
                                    )
                                },
                                singleLine = true,
                                enabled = !isVerifying,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .onFocusChanged { focusState ->
                                        if (focusState.isFocused) {
                                            onFocusField?.invoke()
                                        }
                                    }
                                    .clip(RoundedCornerShape(14.dp)),
                                shape = RoundedCornerShape(14.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = Color.White.copy(alpha = 0.03f),
                                    unfocusedContainerColor = Color.White.copy(alpha = 0.03f),
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.15f),
                                    focusedBorderColor = Color.White.copy(alpha = 0.80f),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    cursorColor = Color.White
                                ),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Email,
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(
                                    onDone = { focusManager.clearFocus() }
                                ),
                                textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            val isButtonEnabled = emailInput.isNotBlank() && !isVerifying
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                                    .bounceClick(enabled = isButtonEnabled) {
                                        if (isButtonEnabled && onVerifySupporter != null) {
                                            focusManager.clearFocus()
                                            isVerifying = true
                                            scope.launch {
                                                val res = onVerifySupporter(emailInput)
                                                verificationResult = res
                                                isVerifying = false
                                            }
                                        }
                                    }
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (isButtonEnabled) {
                                            Color.White
                                        } else {
                                            Color.White.copy(alpha = 0.10f)
                                        }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isVerifying) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            color = Color.Black,
                                            strokeWidth = 2.dp
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = stringResource(R.string.trophy_producer_verifying),
                                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                            color = Color.Black
                                        )
                                    }
                                } else {
                                    Text(
                                        text = stringResource(R.string.trophy_producer_verify_button),
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        ),
                                        color = if (isButtonEnabled) Color.Black else Color.White.copy(alpha = 0.35f)
                                    )
                                }
                            }

                            // Box di Feedback (Successo, Non Trovato, Errore)
                            AnimatedVisibility(
                                visible = verificationResult != null,
                                enter = fadeIn() + expandVertically(),
                                exit = fadeOut() + shrinkVertically()
                            ) {
                                verificationResult?.let { res ->
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        Spacer(modifier = Modifier.height(12.dp))
                                        val tuple = when (res) {
                                            BadgeRepository.SupporterVerificationResult.SUCCESS -> {
                                                SupportFeedbackTuple(
                                                    Color(0xFF10B981).copy(alpha = 0.14f),
                                                    Color(0xFF10B981).copy(alpha = 0.40f),
                                                    Color(0xFF6EE7B7),
                                                    R.string.trophy_producer_verify_success
                                                )
                                            }
                                            BadgeRepository.SupporterVerificationResult.ALREADY_CLAIMED -> {
                                                SupportFeedbackTuple(
                                                    Color(0xFFEF4444).copy(alpha = 0.14f),
                                                    Color(0xFFEF4444).copy(alpha = 0.40f),
                                                    Color(0xFFFCA5A5),
                                                    R.string.trophy_producer_verify_already_claimed
                                                )
                                            }
                                            BadgeRepository.SupporterVerificationResult.NOT_FOUND -> {
                                                SupportFeedbackTuple(
                                                    Color(0xFFF59E0B).copy(alpha = 0.14f),
                                                    Color(0xFFF59E0B).copy(alpha = 0.40f),
                                                    Color(0xFFFCD34D),
                                                    R.string.trophy_producer_verify_not_found
                                                )
                                            }
                                            BadgeRepository.SupporterVerificationResult.INVALID_EMAIL -> {
                                                SupportFeedbackTuple(
                                                    Color(0xFFEF4444).copy(alpha = 0.14f),
                                                    Color(0xFFEF4444).copy(alpha = 0.40f),
                                                    Color(0xFFFCA5A5),
                                                    R.string.trophy_producer_verify_invalid_email
                                                )
                                            }
                                            BadgeRepository.SupporterVerificationResult.ERROR -> {
                                                SupportFeedbackTuple(
                                                    Color(0xFFEF4444).copy(alpha = 0.14f),
                                                    Color(0xFFEF4444).copy(alpha = 0.40f),
                                                    Color(0xFFFCA5A5),
                                                    R.string.trophy_producer_verify_error
                                                )
                                            }
                                        }
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(tuple.bg)
                                                .border(1.dp, tuple.border, RoundedCornerShape(12.dp))
                                                .padding(horizontal = 16.dp, vertical = 12.dp)
                                        ) {
                                            Text(
                                                text = stringResource(tuple.stringRes),
                                                style = MaterialTheme.typography.bodySmall.copy(
                                                    fontSize = 11.5.sp,
                                                    lineHeight = 16.sp
                                                ),
                                                color = tuple.text
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private data class SupportFeedbackTuple(
    val bg: Color,
    val border: Color,
    val text: Color,
    val stringRes: Int
)
