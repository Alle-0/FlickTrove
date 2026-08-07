package com.cinetrack.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cinetrack.data.repository.MovieRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import kotlinx.coroutines.tasks.await
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.FirebaseNetworkException
import com.cinetrack.R
import com.cinetrack.ui.utils.UiText
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.firestore.SetOptions
import com.google.firebase.auth.userProfileChangeRequest

sealed interface AuthState {
    object Unauthenticated : AuthState
    object Anonymous : AuthState
    data class Loading(val message: UiText? = null, val progress: Float? = null) : AuthState
    object Authenticated : AuthState
    data class Success(val message: UiText) : AuthState
    data class Error(val message: UiText) : AuthState
    // Signals that the user must re-enter their password before deletion can proceed
    object NeedsReauth : AuthState
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val movieRepository: MovieRepository,
    private val emailValidatorUseCase: com.cinetrack.domain.EmailValidatorUseCase
) : ViewModel() {

    private val _processState = MutableStateFlow<AuthState?>(null)
    val processState: StateFlow<AuthState?> = _processState

    init {
        checkAutoSync()
    }

    private fun checkAutoSync() {
        val user = auth.currentUser
        if (user != null && !user.isAnonymous) {
            viewModelScope.launch {
                val movies = movieRepository.getLocalMovies()
                if (movies.isEmpty()) {
                    _processState.update { AuthState.Loading(UiText.StringResource(R.string.msg_auth_syncing)) }
                    movieRepository.syncWithFirebase(force = true) { syncProgress ->
                        _processState.update { AuthState.Loading(UiText.DynamicString(syncProgress.message), syncProgress.progress) }
                    }
                    _processState.update { null }
                }
                ensureUsernameExists(user.uid, user.email, user.displayName)
            }
        }
    }

    val authState: StateFlow<AuthState> = combine(
        callbackFlow {
            val listener = FirebaseAuth.AuthStateListener { auth ->
                val user = auth.currentUser
                if (user == null) {
                    trySend(AuthState.Unauthenticated)
                } else if (user.isAnonymous) {
                    trySend(AuthState.Anonymous)
                } else {
                    trySend(AuthState.Authenticated)
                }
            }
            auth.addAuthStateListener(listener)
            awaitClose { auth.removeAuthStateListener(listener) }
        },
        _processState
    ) { firebaseState, processState ->
        processState ?: firebaseState
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AuthState.Loading()
    )

    fun login(email: String, password: String) {
        if (emailValidatorUseCase.containsOffensiveWords(email)) {
            _processState.update { AuthState.Error(UiText.StringResource(R.string.msg_auth_offensive_email)) }
            return
        }

        if (email.isBlank() || password.isBlank()) {
            _processState.update { AuthState.Error(UiText.StringResource(R.string.msg_auth_fields_required)) }
            return
        }

        _processState.update { AuthState.Loading(UiText.StringResource(R.string.msg_auth_logging_in)) }
        
        viewModelScope.launch {
            val currentUser = auth.currentUser
            if (currentUser != null && currentUser.isAnonymous) {
                // If user was a guest and logs into an EXISTING account, we clear their local guest data
                movieRepository.clearAllData()
            }
            
            auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener { result ->
                    val uid = auth.currentUser?.uid
                    viewModelScope.launch {
                        if (uid != null) {
                            ensureUsernameExists(uid, email, auth.currentUser?.displayName)
                        }
                        _processState.update { AuthState.Loading(UiText.StringResource(R.string.msg_auth_syncing)) }
                        movieRepository.syncWithFirebase(force = true) { syncProgress ->
                            _processState.update { AuthState.Loading(UiText.DynamicString(syncProgress.message), syncProgress.progress) }
                        }
                        _processState.update { null }
                    }
                }
                .addOnFailureListener { exception ->
                    _processState.update { AuthState.Error(getErrorMessage(exception)) }
                }
        }
    }

    suspend fun isUsernameAvailable(username: String): Boolean {
        if (username.isBlank() || username.length < 3) return false
        return try {
            val document = Firebase.firestore.collection("usernames").document(username.lowercase()).get().await()
            !document.exists()
        } catch (e: Exception) {
            false
        }
    }

    suspend fun generateUniqueUsername(baseName: String): String {
        var currentName = baseName.replace(Regex("[^a-zA-Z0-9_]"), "")
        if (currentName.length < 3) currentName = "User" + (100..999).random()
        if (currentName.length > 15) currentName = currentName.substring(0, 15)
        
        var isAvailable = isUsernameAvailable(currentName)
        var count = 1
        var finalName = currentName
        while (!isAvailable) {
            val suffix = "_${(10..9999).random()}"
            finalName = currentName.take(20 - suffix.length) + suffix
            isAvailable = isUsernameAvailable(finalName)
            count++
            if (count > 10) break
        }
        return finalName
    }

    private suspend fun ensureUsernameExists(uid: String, fallbackEmail: String?, fallbackName: String?) {
        try {
            val userRef = Firebase.firestore.collection("users").document(uid)
            val snapshot = userRef.get().await()
            val hasDisplayName = snapshot.exists() && !snapshot.getString("displayName").isNullOrBlank()
            
            val currentUser = auth.currentUser
            val emailToSave = currentUser?.email ?: fallbackEmail ?: ""
            val avatarToSave = currentUser?.photoUrl?.toString() ?: ""
            
            val batch = Firebase.firestore.batch()
            // Always ensure email and avatar are up to date
            batch.set(userRef, mapOf("email" to emailToSave, "photoUrl" to avatarToSave), com.google.firebase.firestore.SetOptions.merge())
            
            if (!hasDisplayName) {
                val baseName = fallbackName?.takeIf { it.isNotBlank() } 
                    ?: fallbackEmail?.substringBefore("@") 
                    ?: "User"
                val uniqueName = generateUniqueUsername(baseName)
                
                currentUser?.updateProfile(com.google.firebase.auth.userProfileChangeRequest {
                    displayName = uniqueName
                })?.await()
                
                batch.set(userRef, mapOf("displayName" to uniqueName, "avatarBanned" to false, "nameChangesCount" to 0), com.google.firebase.firestore.SetOptions.merge())
                
                val usernameRef = Firebase.firestore.collection("usernames").document(uniqueName.lowercase())
                batch.set(usernameRef, mapOf("uid" to uid))
            }
            batch.commit().await()
        } catch (e: Exception) {
            // Ignore
        }
    }

    fun signUp(email: String, password: String) {
        if (emailValidatorUseCase.containsOffensiveWords(email)) {
            _processState.update { AuthState.Error(UiText.StringResource(R.string.msg_auth_offensive_email)) }
            return
        }

        if (email.isBlank() || password.isBlank()) {
            _processState.update { AuthState.Error(UiText.StringResource(R.string.msg_auth_fields_required)) }
            return
        }

        _processState.update { AuthState.Loading(UiText.StringResource(R.string.msg_auth_creating_account)) }
        
        val currentUser = auth.currentUser
        if (currentUser != null && currentUser.isAnonymous) {
            // Upgrade anonymous user to permanent account via Account Linking
            val credential = EmailAuthProvider.getCredential(email, password)
            currentUser.linkWithCredential(credential)
                .addOnSuccessListener {
                    val uid = auth.currentUser?.uid
                    if (uid != null) {
                        viewModelScope.launch {
                            try {
                                val baseName = email.substringBefore("@")
                                val uniqueName = generateUniqueUsername(baseName)

                                auth.currentUser?.updateProfile(userProfileChangeRequest {
                                    displayName = uniqueName
                                })?.await()
                                
                                val batch = Firebase.firestore.batch()
                                val userRef = Firebase.firestore.collection("users").document(uid)
                                batch.set(userRef, mapOf("displayName" to uniqueName, "avatarBanned" to false, "nameChangesCount" to 0), SetOptions.merge())
                                
                                val usernameRef = Firebase.firestore.collection("usernames").document(uniqueName.lowercase())
                                batch.set(usernameRef, mapOf("uid" to uid))
                                batch.commit().await()
                            } catch (e: Exception) {
                                // ignore
                            }
                            
                            _processState.update { AuthState.Loading(UiText.StringResource(R.string.msg_auth_syncing)) }
                            movieRepository.syncWithFirebase(force = true) { syncProgress ->
                                _processState.update { AuthState.Loading(UiText.DynamicString(syncProgress.message), syncProgress.progress) }
                            }
                            _processState.update { AuthState.Authenticated }
                        }
                    }
                }
                .addOnFailureListener { exception ->
                    _processState.update { AuthState.Error(getErrorMessage(exception)) }
                }
        } else {
            // Normal sign up
            auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener {
                    val uid = auth.currentUser?.uid
                    if (uid != null) {
                        viewModelScope.launch {
                            try {
                                val baseName = email.substringBefore("@")
                                val uniqueName = generateUniqueUsername(baseName)

                                auth.currentUser?.updateProfile(userProfileChangeRequest {
                                    displayName = uniqueName
                                })?.await()
                                
                                val batch = Firebase.firestore.batch()
                                val userRef = Firebase.firestore.collection("users").document(uid)
                                batch.set(userRef, mapOf("displayName" to uniqueName, "avatarBanned" to false, "nameChangesCount" to 0), SetOptions.merge())
                                
                                val usernameRef = Firebase.firestore.collection("usernames").document(uniqueName.lowercase())
                                batch.set(usernameRef, mapOf("uid" to uid))
                                batch.commit().await()
                            } catch (e: Exception) {
                                // ignore
                            }
                            
                            _processState.update { AuthState.Loading(UiText.StringResource(R.string.msg_auth_syncing)) }
                            movieRepository.syncWithFirebase(force = true) { syncProgress ->
                                _processState.update { AuthState.Loading(UiText.DynamicString(syncProgress.message), syncProgress.progress) }
                            }
                            _processState.update { null }
                        }
                    }
                }
                .addOnFailureListener { exception ->
                    _processState.update { AuthState.Error(getErrorMessage(exception)) }
                }
        }
    }

    fun signInWithGoogle(idToken: String) {
        _processState.update { AuthState.Loading(UiText.StringResource(R.string.msg_auth_logging_in)) }
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        
        val currentUser = auth.currentUser
        if (currentUser != null && currentUser.isAnonymous) {
            // Link anonymous account to Google
            currentUser.linkWithCredential(credential)
                .addOnSuccessListener { result ->
                    val isNewUser = result.additionalUserInfo?.isNewUser == true
                    val uid = auth.currentUser?.uid
                    if (uid != null) {
                        viewModelScope.launch {
                            if (isNewUser) {
                                val baseName = result.user?.displayName ?: "User"
                                val uniqueName = generateUniqueUsername(baseName)
                                
                                try {
                                    auth.currentUser?.updateProfile(userProfileChangeRequest {
                                        displayName = uniqueName
                                    })?.await()
                                    
                                    val batch = Firebase.firestore.batch()
                                    val userRef = Firebase.firestore.collection("users").document(uid)
                                    batch.set(userRef, mapOf("displayName" to uniqueName, "avatarBanned" to false, "nameChangesCount" to 0), SetOptions.merge())
                                    
                                    val usernameRef = Firebase.firestore.collection("usernames").document(uniqueName.lowercase())
                                    batch.set(usernameRef, mapOf("uid" to uid))
                                    batch.commit().await()
                                } catch (e: Exception) { }
                            } else {
                                ensureUsernameExists(uid, auth.currentUser?.email, result.user?.displayName)
                            }
                            
                            _processState.update { AuthState.Loading(UiText.StringResource(R.string.msg_auth_syncing)) }
                            movieRepository.syncWithFirebase(force = true) { syncProgress ->
                                _processState.update { AuthState.Loading(UiText.DynamicString(syncProgress.message), syncProgress.progress) }
                            }
                            _processState.update { AuthState.Authenticated }
                        }
                    }
                }
                .addOnFailureListener { exception ->
                    if (exception is FirebaseAuthUserCollisionException) {
                        // The Google account is already linked to another Firebase account.
                        // Sign in to that account instead and clear local guest data.
                        viewModelScope.launch {
                            movieRepository.clearAllData()
                            auth.signInWithCredential(credential).addOnSuccessListener { result ->
                                val uid = auth.currentUser?.uid
                                viewModelScope.launch {
                                    if (uid != null) {
                                        ensureUsernameExists(uid, auth.currentUser?.email, auth.currentUser?.displayName)
                                    }
                                    
                                    _processState.update { AuthState.Loading(UiText.StringResource(R.string.msg_auth_syncing)) }
                                    movieRepository.syncWithFirebase(force = true) { syncProgress ->
                                        _processState.update { AuthState.Loading(UiText.DynamicString(syncProgress.message), syncProgress.progress) }
                                    }
                                    _processState.update { null }
                                }
                            }.addOnFailureListener { signInException ->
                                _processState.update { AuthState.Error(getErrorMessage(signInException)) }
                            }
                        }
                    } else {
                        _processState.update { AuthState.Error(getErrorMessage(exception)) }
                    }
                }
        } else {
            // Normal Google Sign In
            auth.signInWithCredential(credential)
                .addOnSuccessListener { result ->
                    val isNewUser = result.additionalUserInfo?.isNewUser == true
                    val uid = auth.currentUser?.uid
                    if (uid != null) {
                        viewModelScope.launch {
                            if (isNewUser) {
                                val baseName = result.user?.displayName ?: "User"
                                val uniqueName = generateUniqueUsername(baseName)
                                
                                try {
                                    auth.currentUser?.updateProfile(userProfileChangeRequest {
                                        displayName = uniqueName
                                    })?.await()
                                    
                                    val batch = Firebase.firestore.batch()
                                    val userRef = Firebase.firestore.collection("users").document(uid)
                                    batch.set(userRef, mapOf("displayName" to uniqueName, "avatarBanned" to false, "nameChangesCount" to 0), SetOptions.merge())
                                    
                                    val usernameRef = Firebase.firestore.collection("usernames").document(uniqueName.lowercase())
                                    batch.set(usernameRef, mapOf("uid" to uid))
                                    batch.commit().await()
                                } catch (e: Exception) { }
                            } else {
                                ensureUsernameExists(uid, auth.currentUser?.email, result.user?.displayName)
                            }
                            
                            _processState.update { AuthState.Loading(UiText.StringResource(R.string.msg_auth_syncing)) }
                            movieRepository.syncWithFirebase(force = true) { syncProgress ->
                                _processState.update { AuthState.Loading(UiText.DynamicString(syncProgress.message), syncProgress.progress) }
                            }
                            _processState.update { null }
                        }
                    }
                }
                .addOnFailureListener { exception ->
                    _processState.update { AuthState.Error(getErrorMessage(exception)) }
                }
        }
    }

    fun linkGoogleAccount(idToken: String) {
        _processState.update { AuthState.Loading(UiText.StringResource(R.string.msg_auth_linking)) }
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        
        val currentUser = auth.currentUser
        if (currentUser != null) {
            currentUser.linkWithCredential(credential)
                .addOnSuccessListener {
                    viewModelScope.launch {
                        _processState.update { AuthState.Success(UiText.DynamicString("Google account linked successfully!")) }
                        kotlinx.coroutines.delay(1500)
                        _processState.update { null }
                    }
                }
                .addOnFailureListener { exception ->
                    if (exception is FirebaseAuthUserCollisionException) {
                        _processState.update { AuthState.Error(UiText.DynamicString("This Google account is already linked to another user.")) }
                    } else {
                        _processState.update { AuthState.Error(getErrorMessage(exception)) }
                    }
                }
        } else {
            _processState.update { AuthState.Error(UiText.DynamicString("User not logged in.")) }
        }
    }

    fun loginGuest() {
        _processState.update { AuthState.Loading(UiText.StringResource(R.string.msg_auth_guest_access)) }
        auth.signInAnonymously()
            .addOnSuccessListener {
                _processState.update { AuthState.Anonymous }
            }
            .addOnFailureListener { exception ->
                _processState.update { AuthState.Error(getErrorMessage(exception)) }
            }
    }

    fun logout() {
        viewModelScope.launch {
            auth.signOut()
            movieRepository.clearAllData()
            _processState.update { null }
        }
    }

    fun resetProcessState() {
        _processState.update { null }
    }

    fun deleteAccount(onComplete: (Boolean) -> Unit) {
        val user = auth.currentUser
        if (user == null) {
            viewModelScope.launch {
                movieRepository.clearAllData()
                auth.signOut()
                _processState.update { AuthState.Unauthenticated }
                onComplete(true)
            }
            return
        }

        _processState.update { AuthState.Loading(UiText.StringResource(R.string.msg_auth_deleting)) }
        
        user.delete()
            .addOnSuccessListener {
                viewModelScope.launch {
                    movieRepository.clearAllData()
                    auth.signOut()
                    _processState.update { AuthState.Unauthenticated }
                    onComplete(true)
                }
            }
            .addOnFailureListener { exception ->
                if (user.isAnonymous) {
                    viewModelScope.launch {
                        movieRepository.clearAllData()
                        auth.signOut()
                        _processState.update { AuthState.Unauthenticated }
                        onComplete(true)
                    }
                } else if (exception is FirebaseAuthRecentLoginRequiredException) {
                    // Firebase requires fresh credentials: show reauth dialog
                    _processState.update { AuthState.NeedsReauth }
                    onComplete(false)
                } else {
                    _processState.update { AuthState.Error(getErrorMessage(exception)) }
                    onComplete(false)
                }
            }
    }

    fun deleteAccountWithReauth(password: String, onComplete: (Boolean) -> Unit) {
        val user = auth.currentUser ?: return
        val email = user.email ?: return
        _processState.update { AuthState.Loading(UiText.StringResource(R.string.msg_auth_deleting)) }
        viewModelScope.launch {
            try {
                val credential = EmailAuthProvider.getCredential(email, password)
                user.reauthenticate(credential).await()
                user.delete().await()
                movieRepository.clearAllData()
                auth.signOut()
                _processState.update { AuthState.Unauthenticated }
                onComplete(true)
            } catch (e: Exception) {
                _processState.update { AuthState.Error(getErrorMessage(e)) }
                onComplete(false)
            }
        }
    }

    fun resetPassword(email: String) {
        if (email.isBlank()) {
            _processState.update { AuthState.Error(UiText.StringResource(R.string.msg_auth_enter_email_reset)) }
            return
        }
        _processState.update { AuthState.Loading(UiText.StringResource(R.string.msg_auth_sending_reset)) }
        auth.sendPasswordResetEmail(email)
            .addOnSuccessListener {
                _processState.update { AuthState.Success(UiText.StringResource(R.string.msg_auth_email_sent)) }
            }
            .addOnFailureListener { exception ->
                _processState.update { AuthState.Error(getErrorMessage(exception)) }
            }
    }

    fun clearError() {
        if (_processState.value is AuthState.Error) {
            _processState.update { null }
        }
    }

    private fun getErrorMessage(exception: Exception): UiText {
        val msg = exception.message ?: ""
        return when (exception) {
            is FirebaseAuthInvalidCredentialsException -> UiText.StringResource(R.string.msg_auth_invalid_credentials)
            is FirebaseAuthInvalidUserException -> UiText.StringResource(R.string.msg_auth_no_account)
            is FirebaseAuthUserCollisionException -> UiText.StringResource(R.string.msg_auth_collision)
            is FirebaseNetworkException -> UiText.StringResource(R.string.msg_auth_no_connection)
            is FirebaseAuthRecentLoginRequiredException -> UiText.StringResource(R.string.msg_auth_recent_login_required)
            else -> {
                if (msg.contains("INVALID_LOGIN_CREDENTIALS")) UiText.StringResource(R.string.msg_auth_invalid_credentials)
                else if (msg.contains("TOO_MANY_ATTEMPTS_TRY_LATER")) UiText.StringResource(R.string.msg_auth_too_many_attempts)
                else UiText.StringResource(R.string.msg_auth_unexpected_error, exception.localizedMessage ?: "")
            }
        }
    }
}
