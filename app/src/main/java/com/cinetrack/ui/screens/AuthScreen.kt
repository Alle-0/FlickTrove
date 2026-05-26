package com.cinetrack.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cinetrack.ui.components.CinematicBackground
import com.cinetrack.ui.components.glass.glassmorphic
import com.cinetrack.ui.components.shared.PremiumConfirmDialog
import com.cinetrack.ui.components.shared.ConfirmType
import com.cinetrack.ui.utils.bounceClick
import com.cinetrack.ui.theme.PrimaryTeal
import com.cinetrack.ui.theme.HazeStyles
import com.cinetrack.ui.viewmodel.AuthState
import com.cinetrack.ui.viewmodel.AuthViewModel
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ─── Inline feedback message (replaces Toast) ───────────────────────────────
private data class FeedbackMessage(
    val text: String,
    val isError: Boolean = true
)

@Composable
fun AuthScreen(
    viewModel: AuthViewModel = hiltViewModel(),
    onLoginSuccess: () -> Unit
) {
    var isLogin by remember { mutableStateOf(true) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val authState by viewModel.authState.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current
    val hazeState = remember { HazeState() }
    val scope = rememberCoroutineScope()
    var showGuestWarning by remember { mutableStateOf(false) }

    // In-app feedback instead of Toast
    var feedback by remember { mutableStateOf<FeedbackMessage?>(null) }

    fun showFeedback(text: String, isError: Boolean = true) {
        feedback = FeedbackMessage(text, isError)
        scope.launch {
            delay(3500)
            feedback = null
        }
    }

    LaunchedEffect(authState) {
        when (val state = authState) {
            is AuthState.Authenticated -> onLoginSuccess()
            is AuthState.Error -> {
                showFeedback(state.message, isError = true)
                viewModel.clearError()
            }
            else -> {}
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .haze(hazeState, style = HazeStyles.PremiumDark)
            .pointerInput(Unit) {
                detectTapGestures(onTap = { focusManager.clearFocus() })
            }
    ) {
        // ── Background Content ──
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            CinematicBackground()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 28.dp)
                    .systemBarsPadding(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // ── Logo & Title ──────────────────────────────────────────────
                Column(
                    modifier = Modifier.padding(bottom = 44.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "FlickTrove",
                        color = Color.White,
                        fontSize = 58.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-3).sp
                    )
                    Text(
                        text = "YOUR CINEMATIC ARCHIVE",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 4.sp,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                // ── Input Fields ──────────────────────────────────────────────
                val isEmailValid = remember(email) {
                    email.isEmpty() || (email.contains("@") && email.contains(".") && email.length > 5)
                }
                val isPasswordValid = remember(password) {
                    password.isEmpty() || password.length >= 6
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PremiumTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = "Email",
                        icon = Icons.Rounded.Email,
                        isError = !isEmailValid && email.isNotEmpty(),
                        errorText = if (!isEmailValid && email.isNotEmpty()) "Email non valida" else null,
                        enabled = authState !is AuthState.Loading
                    )

                    PremiumTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = "Password",
                        icon = Icons.Rounded.Lock,
                        isError = !isPasswordValid && password.isNotEmpty(),
                        errorText = if (!isPasswordValid && password.isNotEmpty()) "Minimo 6 caratteri" else null,
                        isPassword = true,
                        enabled = authState !is AuthState.Loading
                    )

                    // Password dimenticata
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        Box(
                            modifier = Modifier
                                .bounceClick(scaleDown = 0.96f) {
                                    val targetEmail = email.trim()
                                    viewModel.resetPassword(targetEmail)
                                }
                                .padding(vertical = 4.dp, horizontal = 4.dp)
                        ) {
                            Text(
                                "Password dimenticata?",
                                color = Color.White.copy(alpha = 0.4f),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                }

                Spacer(modifier = Modifier.height(8.dp))

                // ── Main Action Button ────────────────────────────────────────
                val canProceed = authState !is AuthState.Loading &&
                    email.isNotEmpty() && password.isNotEmpty()

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .bounceClick(enabled = canProceed) {
                            if (isLogin) viewModel.login(email, password)
                            else viewModel.signUp(email, password)
                        }
                        .background(
                            color = if (canProceed) PrimaryTeal else PrimaryTeal.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(20.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (authState is AuthState.Loading) {
                        CircularProgressIndicator(
                            color = Color.Black,
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 3.dp
                        )
                    } else {
                        Text(
                            text = if (isLogin) "Accedi" else "Crea Account",
                            color = if (canProceed) Color.Black else Color.Black.copy(alpha = 0.4f),
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // ── Toggle Login/SignUp ───────────────────────────────────────
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .bounceClick(scaleDown = 0.96f) {
                            isLogin = !isLogin
                            password = "" // reset password when switching
                        }
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isLogin) "Non hai un account? Registrati"
                               else "Hai già un account? Accedi",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // ── Divider ───────────────────────────────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(1.dp)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Color.Transparent, Color.White.copy(alpha = 0.08f))
                                )
                            )
                    )
                    Text(
                        "OPPURE",
                        modifier = Modifier.padding(horizontal = 20.dp),
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(1.dp)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Color.White.copy(alpha = 0.08f), Color.Transparent)
                                )
                            )
                    )
                }

                // ── Guest Button ──────────────────────────────────────────────
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .bounceClick(scaleDown = 0.96f) {
                            showGuestWarning = true
                        }
                        .border(
                            width = 1.dp,
                            color = Color.White.copy(alpha = 0.05f),
                            shape = RoundedCornerShape(20.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Continua come Ospite",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // ── Legal Disclaimer ──────────────────────────────────────────
                val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
                androidx.compose.foundation.text.ClickableText(
                    text = buildAnnotatedString {
                        append("Continuando, accetti i nostri ")
                        pushStringAnnotation(tag = "URL", annotation = "https://raw.githubusercontent.com/Alle-0/FlickTrove_Kotlin/main/TERMS_OF_SERVICE.md")
                        withStyle(style = SpanStyle(color = PrimaryTeal, fontWeight = FontWeight.Bold)) {
                            append("Termini di Servizio")
                        }
                        pop()
                        append(" e la ")
                        pushStringAnnotation(tag = "URL", annotation = "https://raw.githubusercontent.com/Alle-0/FlickTrove_Kotlin/main/PRIVACY_POLICY.md")
                        withStyle(style = SpanStyle(color = PrimaryTeal, fontWeight = FontWeight.Bold)) {
                            append("Privacy Policy")
                        }
                        pop()
                        append(".")
                    },
                    style = TextStyle(
                        color = Color.White.copy(alpha = 0.75f),
                        fontSize = 12.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    ),
                    onClick = { offset ->
                        // Questo trick serve a trovare l'annotazione URL cliccata 
                        // all'interno della stringa e ad aprirla nel browser!
                        val annotatedString = buildAnnotatedString {
                            append("Continuando, accetti i nostri ")
                            pushStringAnnotation(tag = "URL", annotation = "https://raw.githubusercontent.com/Alle-0/FlickTrove_Kotlin/main/TERMS_OF_SERVICE.md")
                            withStyle(style = SpanStyle(color = PrimaryTeal, fontWeight = FontWeight.Bold)) { append("Termini di Servizio") }
                            pop()
                            append(" e la ")
                            pushStringAnnotation(tag = "URL", annotation = "https://raw.githubusercontent.com/Alle-0/FlickTrove_Kotlin/main/PRIVACY_POLICY.md")
                            withStyle(style = SpanStyle(color = PrimaryTeal, fontWeight = FontWeight.Bold)) { append("Privacy Policy") }
                            pop()
                            append(".")
                        }
                        annotatedString.getStringAnnotations("URL", offset, offset)
                            .firstOrNull()?.let { annotation ->
                                uriHandler.openUri(annotation.item)
                            }
                    }
                )
            }
        }

        // ── Overlays (NOT sources for haze, but can consume the hazeState) ──
        PremiumConfirmDialog(
            visible = showGuestWarning,
            onDismiss = { showGuestWarning = false },
            onConfirm = { viewModel.loginGuest() },
            title = "Modalità Ospite",
            message = "I tuoi dati verranno salvati solo su questo dispositivo. Se disinstalli l'app o pulisci la cache, i dati andranno persi definitivamente.",
            confirmLabel = "Accedi come Ospite",
            cancelLabel = "Torna indietro",
            type = ConfirmType.WARNING,
            hazeState = hazeState
        )

        val loadingState = authState as? AuthState.Loading
        val targetProgress = loadingState?.progress
        val animatedProgress by animateFloatAsState(
            targetValue = targetProgress ?: 0f,
            animationSpec = tween(durationMillis = 420, easing = FastOutSlowInEasing),
            label = "AuthSyncProgress"
        )

        AnimatedVisibility(
            visible = loadingState != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.zIndex(30f)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f))
                    .clickable(enabled = false) {},
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .widthIn(max = 420.dp)
                        .fillMaxWidth(0.9f)
                        .background(
                            color = HazeStyles.GlassColor,
                            shape = RoundedCornerShape(28.dp)
                        )
                        .border(
                            width = 1.dp,
                            color = HazeStyles.GlassBorderColor.copy(alpha = HazeStyles.GlassBorderAlphaTop),
                            shape = RoundedCornerShape(28.dp)
                        )
                        .padding(horizontal = 22.dp, vertical = 18.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = loadingState?.message ?: "Sync in corso...",
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        if (targetProgress == null) {
                            LinearProgressIndicator(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp),
                                color = PrimaryTeal,
                                trackColor = Color.White.copy(alpha = 0.12f)
                            )
                        } else {
                            val clamped = animatedProgress.coerceIn(0f, 1f)
                            LinearProgressIndicator(
                                progress = { clamped },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp),
                                color = PrimaryTeal,
                                trackColor = Color.White.copy(alpha = 0.12f)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "${(clamped * 100).toInt()}%",
                                color = Color.White.copy(alpha = 0.7f),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }

        // ── Feedback Snackbar ─────────────────────────────────────────────
        AnimatedVisibility(
            visible = feedback != null,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 32.dp, start = 24.dp, end = 24.dp)
                .zIndex(10f)
        ) {
            feedback?.let { msg ->
                Box(
                    modifier = Modifier
                        .background(
                            color = if (msg.isError)
                                Color(0xFF3A0D0D)
                            else
                                Color(0xFF0D3A20),
                            shape = RoundedCornerShape(50.dp)
                        )
                        .border(
                            1.dp,
                            if (msg.isError) Color(0xFFE57373).copy(alpha = 0.5f)
                            else PrimaryTeal.copy(alpha = 0.6f),
                            RoundedCornerShape(50.dp)
                        )
                        .padding(horizontal = 24.dp, vertical = 14.dp)
                ) {
                    Text(
                        text = msg.text,
                        color = if (msg.isError) Color(0xFFFF8A80) else PrimaryTeal,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}


// ─── Premium Text Field ───────────────────────────────────────────────────────
@Composable
private fun PremiumTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: ImageVector,
    isError: Boolean = false,
    errorText: String? = null,
    isPassword: Boolean = false,
    enabled: Boolean = true
) {
    var isFocused by remember { mutableStateOf(false) }
    val isFloating = isFocused || value.isNotEmpty()
    val density = LocalDensity.current

    // ── Label animation math (all values in dp, within the outer Box coordinate system)
    //
    // Layout:
    //   Outer Box: fillMaxWidth, wraps height.
    //   padding(top = 20.dp) → field starts at y = 20dp.
    //   Field height = 58dp → field center at y = 20 + 29 = 49dp.
    //   Field padding(horizontal = 16.dp), icon 20dp, spacer 12dp → text at x = 48dp.
    //
    // NOT floating: label visually inside field, vertically centered, after the icon.
    //   translationY = 49 - 10 = 39dp  (label top, assuming ~20dp label height)
    //   translationX = 48dp
    //
    // Floating: label above the top border (border at y=20dp), left-aligned.
    //   translationY = 2dp  (label sits just above the border at y=20dp)
    //   translationX = 16dp  (same horizontal padding as the field content)
    //
    // Scale goes from 1f → 0.75f, origin = top-left.

    val labelOffsetY by animateDpAsState(
        targetValue = if (isFloating) 3.dp else 39.dp,
        animationSpec = spring(
            stiffness = Spring.StiffnessMediumLow,
            dampingRatio = Spring.DampingRatioLowBouncy
        ),
        label = "LabelY"
    )
    val labelOffsetX by animateDpAsState(
        targetValue = if (isFloating) 16.dp else 48.dp,
        animationSpec = spring(
            stiffness = Spring.StiffnessMediumLow,
            dampingRatio = Spring.DampingRatioLowBouncy
        ),
        label = "LabelX"
    )
    val labelScale by animateFloatAsState(
        targetValue = if (isFloating) 0.75f else 1f,
        animationSpec = spring(
            stiffness = Spring.StiffnessMediumLow,
            dampingRatio = Spring.DampingRatioLowBouncy
        ),
        label = "LabelScale"
    )
        val labelColor by animateColorAsState(
            targetValue = when {
                isError -> Color(0xFFE57373)
                isFocused -> PrimaryTeal
                isFloating -> Color.White.copy(alpha = 0.9f)
                else -> Color.White.copy(alpha = 0.7f)
            },
            label = "LabelColor"
        )
        val borderColor by animateColorAsState(
            targetValue = when {
                isError -> Color(0xFFE57373).copy(alpha = 0.7f)
                isFocused || value.isNotEmpty() -> PrimaryTeal.copy(alpha = 0.8f)
                else -> Color.White.copy(alpha = 0.25f)
            },
            label = "BorderColor"
        )
    val containerColor by animateColorAsState(
        targetValue = if (isFocused) Color.White.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.04f),
        label = "ContainerColor"
    )

    // Outer Box: 20dp top padding gives space for the floating label above the field border
    Box(modifier = Modifier.fillMaxWidth()) {

        // ── Field container — border top at y = 20dp in outer Box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 20.dp)
                .height(58.dp)
                .background(color = containerColor, shape = RoundedCornerShape(18.dp))
                .border(1.5.dp, borderColor, RoundedCornerShape(18.dp))
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxSize()
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = when {
                        isError -> Color(0xFFE57373)
                        isFocused || value.isNotEmpty() -> PrimaryTeal
                        else -> Color.White.copy(alpha = 0.6f)
                    },
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier
                        .weight(1f)
                        .onFocusChanged { isFocused = it.isFocused },
                    textStyle = TextStyle(
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    cursorBrush = SolidColor(PrimaryTeal),
                    visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None,
                    enabled = enabled,
                    singleLine = true
                )
            }
        }

        // ── Error text below the field
        AnimatedVisibility(
            visible = isError && !errorText.isNullOrEmpty(),
            enter = scaleIn(initialScale = 0.95f) + fadeIn(),
            exit = scaleOut(targetScale = 0.95f) + fadeOut(),
            modifier = Modifier.padding(top = 82.dp, start = 16.dp)
        ) {
            Text(
                text = errorText ?: "",
                color = Color(0xFFE57373),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }

        // ── Label: single Text that smoothly moves via graphicsLayer.
        // When NOT floating → sits inside the field (vertically centered, after the icon).
        // When floating    → rises above the top border, aligned left.
        // No dark background. Scale 1f → 0.75f with top-left transform origin.
        Text(
            text = label,
            color = labelColor,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            modifier = Modifier.graphicsLayer {
                scaleX = labelScale
                scaleY = labelScale
                translationX = with(density) { labelOffsetX.toPx() }
                translationY = with(density) { labelOffsetY.toPx() }
                transformOrigin = TransformOrigin(0f, 0f)
            }
        )
    }
}
