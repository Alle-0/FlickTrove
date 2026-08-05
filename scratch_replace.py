import re

with open('app/src/main/java/com/cinetrack/ui/screens/AuthScreen.kt', 'r', encoding='utf-8') as f:
    content = f.read()

# Find the start of // -- Input Fields
start_idx = content.find('                // -- Input Fields ----------------------------------------------')
# Find the end (before Spacer and Legal Disclaimer)
end_idx = content.find('                Spacer(modifier = Modifier.height(24.dp))', start_idx)

if start_idx != -1 and end_idx != -1:
    new_content = '''                var showEmailAuth by remember { mutableStateOf(false) }
                val context = LocalContext.current
                val coroutineScope = rememberCoroutineScope()
                
                val handleGoogleSignIn = {
                    coroutineScope.launch {
                        try {
                            val credentialManager = CredentialManager.create(context)
                            val googleIdOption = GetGoogleIdOption.Builder()
                                .setFilterByAuthorizedAccounts(false)
                                .setServerClientId(Keys.getGoogleClientId())
                                .setAutoSelectEnabled(false)
                                .build()

                            val request = GetCredentialRequest.Builder()
                                .addCredentialOption(googleIdOption)
                                .build()

                            val result = credentialManager.getCredential(context, request)
                            val credential = result.credential

                            if (credential is androidx.credentials.CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                                viewModel.signInWithGoogle(googleIdTokenCredential.idToken)
                            } else {
                                Log.e("AuthScreen", "Unexpected type of credential")
                            }
                        } catch (e: Exception) {
                            Log.e("AuthScreen", "Google Sign In Failed", e)
                        }
                    }
                }

                // -- Initial Buttons -------------------------------------------
                AnimatedVisibility(
                    visible = !showEmailAuth,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Continue with Email
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .bounceClick(scaleDown = 0.96f) {
                                    showEmailAuth = true
                                }
                                .background(PrimaryTeal, RoundedCornerShape(20.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Continue with Email",
                                color = Color.Black,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Continue with Google
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .bounceClick(scaleDown = 0.96f) {
                                    handleGoogleSignIn()
                                }
                                .background(Color.White, RoundedCornerShape(20.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = stringResource(R.string.auth_continue_google),
                                    color = Color.Black,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Divider
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(1.dp)
                                    .background(Brush.horizontalGradient(listOf(Color.Transparent, Color.White.copy(alpha = 0.08f))))
                            )
                            Text(
                                stringResource(R.string.auth_or_continue_with),
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
                                    .background(Brush.horizontalGradient(listOf(Color.White.copy(alpha = 0.08f), Color.Transparent)))
                            )
                        }

                        // Guest Button
                        val isGuestUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.isAnonymous == true
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .bounceClick(scaleDown = 0.96f) {
                                    if (isGuestUser) {
                                        navigator?.pop()
                                    } else {
                                        showGuestWarning = true
                                    }
                                }
                                .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(20.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                if (isGuestUser) stringResource(R.string.auth_guest_dialog_cancel) else stringResource(R.string.auth_btn_guest),
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // -- Email Auth Form -------------------------------------------
                AnimatedVisibility(
                    visible = showEmailAuth,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column {
                        val isEmailValid = email.isEmpty() || (email.contains("@") && email.contains(".") && email.length > 5)
                        val isPasswordValid = password.isEmpty() || password.length >= 6

                        PremiumTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = stringResource(R.string.auth_email),
                            icon = androidx.compose.material.icons.Icons.Default.Email,
                            isError = !isEmailValid && email.isNotEmpty(),
                            errorText = if (!isEmailValid && email.isNotEmpty()) stringResource(R.string.auth_error_email_invalid) else null,
                            enabled = authState !is AuthState.Loading
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))

                        PremiumTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = stringResource(R.string.auth_password),
                            icon = androidx.compose.material.icons.Icons.Default.Lock,
                            isError = !isPasswordValid && password.isNotEmpty(),
                            errorText = if (!isPasswordValid && password.isNotEmpty()) stringResource(R.string.auth_error_password_length) else null,
                            isPassword = true,
                            enabled = authState !is AuthState.Loading
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            Box(
                                modifier = Modifier
                                    .bounceClick(scaleDown = 0.96f) {
                                        viewModel.resetPassword(email.trim())
                                    }
                                    .padding(vertical = 4.dp, horizontal = 4.dp)
                            ) {
                                Text(
                                    stringResource(R.string.auth_forgot_password),
                                    color = Color.White.copy(alpha = 0.4f),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        val canProceed = authState !is AuthState.Loading && email.isNotEmpty() && password.isNotEmpty()

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
                                    text = if (isLogin) stringResource(R.string.auth_login) else stringResource(R.string.auth_create_account),
                                    color = if (canProceed) Color.Black else Color.Black.copy(alpha = 0.4f),
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .bounceClick(scaleDown = 0.96f) {
                                    isLogin = !isLogin
                                    password = ""
                                }
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isLogin) stringResource(R.string.auth_switch_to_register)
                                       else stringResource(R.string.auth_switch_to_login),
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        
                        // Back to main options
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .bounceClick(scaleDown = 0.96f) {
                                    showEmailAuth = false
                                }
                                .padding(bottom = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Back",
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

'''
    
    final_content = content[:start_idx] + new_content + content[end_idx:]
    with open('app/src/main/java/com/cinetrack/ui/screens/AuthScreen.kt', 'w', encoding='utf-8') as f:
        f.write(final_content)
    print("Replaced successfully")
else:
    print("Could not find start/end indexes")
