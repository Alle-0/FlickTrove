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

sealed interface AuthState {
    object Unauthenticated : AuthState
    object Anonymous : AuthState
    data class Loading(val message: String? = null, val progress: Float? = null) : AuthState
    object Authenticated : AuthState
    data class Success(val message: String) : AuthState
    data class Error(val message: String) : AuthState
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val movieRepository: MovieRepository,
    private val emailValidatorUseCase: com.cinetrack.domain.EmailValidatorUseCase
) : ViewModel() {

    private val _processState = MutableStateFlow<AuthState?>(null)

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
            _processState.update { AuthState.Error("L'email contiene parole non consentite") }
            return
        }

        if (email.isBlank() || password.isBlank()) {
            _processState.update { AuthState.Error("Email e password sono obbligatorie") }
            return
        }

        _processState.update { AuthState.Loading("Accesso in corso...") }
        
        viewModelScope.launch {
            val currentUser = auth.currentUser
            if (currentUser != null && currentUser.isAnonymous) {
                // If user was a guest and logs into an EXISTING account, we clear their local guest data
                movieRepository.clearAllData()
            }
            
            auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener { result ->
                    viewModelScope.launch {
                        _processState.update { AuthState.Loading("Sincronizzazione account in corso...", null) }
                        movieRepository.syncWithFirebase(force = true) { syncProgress ->
                            _processState.update { AuthState.Loading(syncProgress.message, syncProgress.progress) }
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
            _processState.update { AuthState.Error("L'email contiene parole non consentite") }
            return
        }

        if (email.isBlank() || password.isBlank()) {
            _processState.update { AuthState.Error("Email e password sono obbligatorie") }
            return
        }

        _processState.update { AuthState.Loading("Creazione account...") }
        
        val currentUser = auth.currentUser
        if (currentUser != null && currentUser.isAnonymous) {
            // Upgrade anonymous user to permanent account via Account Linking
            val credential = EmailAuthProvider.getCredential(email, password)
            currentUser.linkWithCredential(credential)
                .addOnSuccessListener {
                    _processState.update { null }
                }
                .addOnFailureListener { exception ->
                    _processState.update { AuthState.Error(getErrorMessage(exception)) }
                }
        } else {
            // Normal sign up
            auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener {
                    _processState.update { null }
                }
                .addOnFailureListener { exception ->
                    _processState.update { AuthState.Error(getErrorMessage(exception)) }
                }
        }
    }

    fun loginGuest() {
        _processState.update { AuthState.Loading("Accesso Ospite...") }
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

        _processState.update { AuthState.Loading("Eliminazione account...") }
        
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
            _processState.update { AuthState.Error("Inserisci l'email per il ripristino") }
            return
        }
        _processState.update { AuthState.Loading("Invio email di ripristino...") }
        auth.sendPasswordResetEmail(email)
            .addOnSuccessListener {
                _processState.update { AuthState.Success("Email di ripristino inviata con successo") }
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

    private fun getErrorMessage(exception: Exception): String {
        val msg = exception.message ?: ""
        return when (exception) {
            is FirebaseAuthInvalidCredentialsException -> "Email o password errati."
            is FirebaseAuthInvalidUserException -> "Nessun account trovato con questa email."
            is FirebaseAuthUserCollisionException -> "L'email è già registrata. Effettua l'accesso."
            is FirebaseNetworkException -> "Nessuna connessione a internet."
            is FirebaseAuthRecentLoginRequiredException -> "Operazione sensibile. Effettua nuovamente l'accesso."
            else -> {
                if (msg.contains("INVALID_LOGIN_CREDENTIALS")) "Email o password errati."
                else if (msg.contains("TOO_MANY_ATTEMPTS_TRY_LATER")) "Troppi tentativi falliti. Riprova più tardi."
                else "Errore imprevisto: ${exception.localizedMessage}"
            }
        }
    }
}
