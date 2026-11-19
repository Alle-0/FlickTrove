package com.cinetrack.ui.screens

import androidx.compose.ui.res.stringResource
import androidx.activity.compose.BackHandler
import com.cinetrack.R

import androidx.compose.ui.res.vectorResource
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.animation.*
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
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
import cafe.adriel.voyager.hilt.getViewModel
import cafe.adriel.voyager.navigator.LocalNavigator
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cinetrack.ui.components.common.CinematicBackground
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
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.compose.ui.platform.LocalContext
import com.cinetrack.util.Keys
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import kotlinx.coroutines.launch
import android.util.Log
import kotlinx.coroutines.delay
import com.cinetrack.ui.components.auth.AuthEmailForm
import com.cinetrack.ui.components.auth.AuthLoadingOverlay

// ─── Inline feedback message (replaces Toast) ───────────────────────────────
private data class FeedbackMessage(
    val text: String,
    val isError: Boolean = true
)

@Composable
fun AuthScreen(
    viewModel: AuthViewModel,
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
    var showEmailAuth by remember { mutableStateOf(false) }
    BackHandler(enabled = showEmailAuth, onBack = { showEmailAuth = false })
    val navigator = LocalNavigator.current

    // In-app feedback instead of Toast
    var feedback by remember { mutableStateOf<FeedbackMessage?>(null) }

    fun showFeedback(text: String, isError: Boolean = true) {
        feedback = FeedbackMessage(text, isError)
        scope.launch {
            delay(3500)
            feedback = null
        }
    }

    val context = androidx.compose.ui.platform.LocalContext.current

    val processState by viewModel.processState.collectAsStateWithLifecycle()

    LaunchedEffect(authState, processState) {
        if (authState is AuthState.Authenticated) {
            viewModel.resetProcessState()
            onLoginSuccess()
        } else if (processState is AuthState.Anonymous) {
            viewModel.resetProcessState()
            onLoginSuccess()
        }
    }

    LaunchedEffect(authState) {
        when (val state = authState) {
            is AuthState.Error -> {
                showFeedback(state.message.asString(context), isError = true)
                viewModel.clearError()
            }
            is AuthState.Success -> {
                showFeedback(state.message.asString(context), isError = false)
                viewModel.resetProcessState()
            }
            else -> {}
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(onTap = { focusManager.clearFocus() })
            }
    ) {
        // ── Background Content ──
        Box(
            modifier = Modifier
                .fillMaxSize()
                .haze(hazeState, style = HazeStyles.PremiumDark)
        ) {
            CinematicBackground()

            val handleGoogleSignIn = {
                scope.launch {
                    try {
                        val activityContext = generateSequence(context) {
                            (it as? android.content.ContextWrapper)?.baseContext
                        }.firstOrNull { it is android.app.Activity } ?: context

                        val credentialManager = CredentialManager.create(activityContext)
                        val googleIdOption = GetGoogleIdOption.Builder()
                            .setFilterByAuthorizedAccounts(false)
                            .setServerClientId(Keys.getGoogleClientId())
                            .setAutoSelectEnabled(false)
                            .build()

                        val request = GetCredentialRequest.Builder()
                            .addCredentialOption(googleIdOption)
                            .build()

                        val result = credentialManager.getCredential(activityContext, request)
                        val credential = result.credential

                        if (credential is androidx.credentials.CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                            viewModel.signInWithGoogle(googleIdTokenCredential.idToken)
                        } else {
                            Log.e("AuthScreen", "Unexpected type of credential")
                            android.widget.Toast.makeText(context, "Unexpected credential type", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        Log.e("AuthScreen", "Google Sign In Failed", e)
                        if (e !is androidx.credentials.exceptions.GetCredentialCancellationException) {
                            android.widget.Toast.makeText(context, "Google Sign In Error: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }

            val backgroundAlpha by animateFloatAsState(
                targetValue = if (showGuestWarning) 0.35f else 1f,
                animationSpec = tween(durationMillis = 220, easing = FastOutSlowInEasing),
                label = "AuthBackgroundDim"
            )
            val googleButtonBgAlpha by animateFloatAsState(
                targetValue = if (showGuestWarning) 0.12f else 1f,
                animationSpec = tween(durationMillis = 200),
                label = "GoogleBtnDim"
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .systemBarsPadding()
                    .imePadding(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .graphicsLayer { alpha = backgroundAlpha }
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    // ── Logo & Title ──────────────────────────────────────────────
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_flicktrove_logo),
                            contentDescription = "FlickTrove Logo",
                            tint = Color.Unspecified,
                            modifier = Modifier
                                .padding(bottom = 10.dp)
                                .size(width = 72.dp, height = 55.dp)
                        )
                        Text(
                            text = "FlickTrove",
                            color = Color.White,
                            fontSize = 54.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = (-2.5).sp
                        )
                        Text(
                            text = stringResource(R.string.auth_subtitle),
                            color = Color.White.copy(alpha = 0.65f),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 4.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }

                    // ── Initial Buttons & Pill ────────────────────────────────────
                    AnimatedVisibility(
                        visible = !showEmailAuth,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // ── Sleek Compact Discovery Pill (Option 1) ───────────
                            val isItalian = java.util.Locale.getDefault().language == "it"
                            Surface(
                                shape = CircleShape,
                                color = Color.White.copy(alpha = 0.05f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                                modifier = Modifier.padding(bottom = 20.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.ic_punto_cerchiato),
                                        contentDescription = null,
                                        tint = PrimaryTeal,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = stringResource(R.string.auth_disclaimer_title),
                                        color = PrimaryTeal,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 0.8.sp
                                    )
                                    Text(
                                        text = " • " + (if (isItalian) "Nessuno streaming" else "No streaming"),
                                        color = Color.White.copy(alpha = 0.65f),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }

                            // ── Continue with Google (Primary Pill, White) ───────────
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(54.dp)
                                    .bounceClick(scaleDown = 0.96f) {
                                        handleGoogleSignIn()
                                    }
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = googleButtonBgAlpha)),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    androidx.compose.foundation.Image(
                                        painter = painterResource(id = R.drawable.ic_google),
                                        contentDescription = "Google Logo",
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = stringResource(R.string.auth_continue_google),
                                        color = Color.Black,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // ── Log In / Create Account with Email (Secondary Glass Pill) ─
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(54.dp)
                                    .bounceClick(scaleDown = 0.96f) {
                                        showEmailAuth = true
                                    }
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.08f))
                                    .border(1.dp, Color.White.copy(alpha = 0.16f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Email,
                                        contentDescription = null,
                                        tint = Color.White.copy(alpha = 0.85f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = stringResource(R.string.auth_login) + " / " + stringResource(R.string.auth_create_account),
                                        color = Color.White,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // ── Divider ────────────────────────────────────────────────
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(1.dp)
                                        .background(Brush.horizontalGradient(listOf(Color.Transparent, Color.White.copy(alpha = 0.08f))))
                                )
                                Text(
                                    text = stringResource(R.string.auth_or_continue_with),
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    color = Color.White.copy(alpha = 0.4f),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 2.sp
                                )
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(1.dp)
                                        .background(Brush.horizontalGradient(listOf(Color.White.copy(alpha = 0.08f), Color.Transparent)))
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // ── Guest Button (Spento come richiesto) ────────────────────
                            val isGuestUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.isAnonymous == true
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .clip(CircleShape)
                                    .bounceClick(scaleDown = 0.97f) {
                                        if (isGuestUser) {
                                            navigator?.pop()
                                        } else {
                                            showGuestWarning = true
                                        }
                                    }
                                    .border(1.dp, Color.White.copy(alpha = 0.06f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (isGuestUser) stringResource(R.string.auth_guest_dialog_cancel) else stringResource(R.string.auth_btn_guest),
                                    color = Color.White.copy(alpha = 0.45f),
                                    fontSize = 14.5.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    // ── Email Auth Form ───────────────────────────────────────────
                    AnimatedVisibility(
                        visible = showEmailAuth,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        val isGuestUser = remember { com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.isAnonymous == true }
                        AuthEmailForm(
                            email = email,
                            onEmailChange = { email = it },
                            password = password,
                            onPasswordChange = { password = it },
                            isLogin = isLogin,
                            onToggleMode = {
                                isLogin = !isLogin
                                password = ""
                            },
                            isLoading = authState is AuthState.Loading,
                            isGuestUser = isGuestUser,
                            onForgotPassword = { viewModel.resetPassword(email.trim()) },
                            onSubmit = {
                                if (isLogin) viewModel.login(email, password)
                                else viewModel.signUp(email, password)
                            },
                            onBack = { showEmailAuth = false }
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // ── Legal Disclaimer Footer ──────────────────────────────────
                    Text(
                        text = buildAnnotatedString {
                            append(stringResource(R.string.auth_terms_prefix) + " ")
                            pushLink(androidx.compose.ui.text.LinkAnnotation.Url("https://alle-0.github.io/FlickTrove/terms.html"))
                            withStyle(style = SpanStyle(color = PrimaryTeal, fontWeight = FontWeight.Bold)) {
                                append(stringResource(R.string.auth_terms_link))
                            }
                            pop()
                            append(" " + stringResource(R.string.auth_terms_and) + " ")
                            pushLink(androidx.compose.ui.text.LinkAnnotation.Url("https://alle-0.github.io/FlickTrove/privacy.html"))
                            withStyle(style = SpanStyle(color = PrimaryTeal, fontWeight = FontWeight.Bold)) {
                                append(stringResource(R.string.auth_privacy_link))
                            }
                            pop()
                            append(stringResource(R.string.auth_terms_suffix))
                        },
                        style = TextStyle(
                            color = Color.White.copy(alpha = 0.60f),
                            fontSize = 11.5.sp,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    )
                }
            }
        }

        // ── Overlays (NOT sources for haze, but can consume the hazeState) ──
        PremiumConfirmDialog(
            visible = showGuestWarning,
            onDismiss = { showGuestWarning = false },
            onConfirm = { viewModel.loginGuest() },
            title = stringResource(R.string.auth_guest_dialog_title),
            message = stringResource(R.string.auth_guest_dialog_desc),
            confirmLabel = stringResource(R.string.auth_guest_dialog_confirm),
            cancelLabel = stringResource(R.string.auth_guest_dialog_cancel),
            type = ConfirmType.INFO,
            hazeState = hazeState
        )

        AuthLoadingOverlay(
            loadingState = authState as? AuthState.Loading,
            onContinueAnyway = { viewModel.continueAnyway() }
        )

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



