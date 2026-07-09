package com.cinetrack.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cinetrack.data.repository.MovieRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuthUserCollisionException
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

sealed interface AuthState {
    object Unauthenticated : AuthState
    object Anonymous : AuthState
    data class Loading(val message: UiText? = null, val progress: Float? = null) : AuthState
    object Authenticated : AuthState
    data class Success(val message: UiText) : AuthState
    data class Error(val message: UiText) : AuthState
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
                    viewModelScope.launch {
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
                    viewModelScope.launch {
                        _processState.update { AuthState.Loading(UiText.StringResource(R.string.msg_auth_syncing)) }
                        movieRepository.syncWithFirebase(force = true) { syncProgress ->
                            _processState.update { AuthState.Loading(UiText.DynamicString(syncProgress.message), syncProgress.progress) }
                        }
                        _processState.update { AuthState.Authenticated }
                    }
                }
                .addOnFailureListener { exception ->
                    _processState.update { AuthState.Error(getErrorMessage(exception)) }
                }
        } else {
            // Normal sign up
            auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener {
                    viewModelScope.launch {
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
            onComplete(false)
            return
        }

        _processState.update { AuthState.Loading(UiText.StringResource(R.string.msg_auth_deleting)) }
        
        user.delete()
            .addOnSuccessListener {
                viewModelScope.launch {
                    movieRepository.clearAllData()
                    _processState.update { null }
                    onComplete(true)
                }
            }
            .addOnFailureListener { exception ->
                _processState.update { AuthState.Error(getErrorMessage(exception)) }
                onComplete(false)
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
