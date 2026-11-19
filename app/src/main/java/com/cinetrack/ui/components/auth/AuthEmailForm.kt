package com.cinetrack.ui.components.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cinetrack.R
import com.cinetrack.ui.theme.PrimaryTeal
import com.cinetrack.ui.utils.bounceClick

/**
 * Animated email and password credentials form for login and registration.
 */
@Composable
fun AuthEmailForm(
    email: String,
    onEmailChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    isLogin: Boolean,
    onToggleMode: () -> Unit,
    isLoading: Boolean,
    isGuestUser: Boolean,
    onForgotPassword: () -> Unit,
    onSubmit: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val isEmailValid = remember(email) {
            email.isEmpty() || (email.contains("@") && email.contains(".") && email.length > 5)
        }
        val isPasswordValid = remember(password) {
            password.isEmpty() || password.length >= 6
        }

        PremiumAuthTextField(
            value = email,
            onValueChange = onEmailChange,
            label = stringResource(R.string.auth_email),
            icon = Icons.Default.Email,
            isError = !isEmailValid && email.isNotEmpty(),
            errorText = if (!isEmailValid && email.isNotEmpty()) stringResource(R.string.auth_error_email_invalid) else null,
            enabled = !isLoading
        )

        Spacer(modifier = Modifier.height(4.dp))

        PremiumAuthTextField(
            value = password,
            onValueChange = onPasswordChange,
            label = stringResource(R.string.auth_password),
            icon = Icons.Default.Lock,
            isError = !isPasswordValid && password.isNotEmpty(),
            errorText = if (!isPasswordValid && password.isNotEmpty()) stringResource(R.string.auth_error_password_length) else null,
            isPassword = true,
            enabled = !isLoading
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp),
            contentAlignment = Alignment.CenterEnd
        ) {
            Box(
                modifier = Modifier
                    .bounceClick(scaleDown = 0.96f) {
                        onForgotPassword()
                    }
                    .padding(vertical = 4.dp, horizontal = 4.dp)
            ) {
                Text(
                    text = stringResource(R.string.auth_forgot_password),
                    color = Color.White.copy(alpha = 0.45f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        AnimatedVisibility(
            visible = isGuestUser,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(14.dp))
                    .border(1.dp, PrimaryTeal.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                    .padding(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Info,
                        contentDescription = null,
                        tint = PrimaryTeal,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = if (isLogin)
                            stringResource(R.string.auth_guest_login_warning)
                        else
                            stringResource(R.string.auth_guest_signup_info),
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }
            }
        }

        val canProceed = !isLoading && email.isNotEmpty() && password.isNotEmpty()

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .bounceClick(enabled = canProceed) {
                    onSubmit()
                }
                .clip(CircleShape)
                .background(
                    color = if (canProceed) PrimaryTeal else PrimaryTeal.copy(alpha = 0.5f)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    color = Color.Black,
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 3.dp
                )
            } else {
                Text(
                    text = if (isLogin) stringResource(R.string.auth_login) else stringResource(R.string.auth_create_account),
                    color = if (canProceed) Color.Black else Color.Black.copy(alpha = 0.4f),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .bounceClick(scaleDown = 0.96f) {
                    onToggleMode()
                }
                .padding(vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (isLogin) stringResource(R.string.auth_switch_to_register)
                else stringResource(R.string.auth_switch_to_login),
                color = Color.White,
                fontSize = 14.5.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Back to main options (pill button with localized text)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .clip(CircleShape)
                .bounceClick(scaleDown = 0.97f) {
                    onBack()
                }
                .border(1.dp, Color.White.copy(alpha = 0.08f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.detail_content_desc_back),
                color = Color.White.copy(alpha = 0.65f),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
