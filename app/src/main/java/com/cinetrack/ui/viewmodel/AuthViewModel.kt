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

sealed interface AuthState {
    object Unauthenticated : AuthState
    data class Loading(val message: String? = null, val progress: Float? = null) : AuthState
    object Authenticated : AuthState
    data class Error(val message: String) : AuthState
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val movieRepository: MovieRepository
) : ViewModel() {

    private val _processState = MutableStateFlow<AuthState?>(null)

    private val offensiveWords = listOf(
        // Generiche / Insulti
        "stronz", "cazzo", "cazzi", "cazzon", "cazzat", "merda", "merde", "merdos",
        "puttan", "zoccol", "mignott", "bagascia", "baldracca", "bastard", 
        "coglion", "minchia", "minchion", "cornut", "sfigat", "stracciacazz",
        // Discriminazione / Omofobia
        "frocio", "froci", "ricchion", "culatton", "mongoloid", "spastic", 
        // Atti / Espliciti / Composte
        "sborra", "sborro", "sborrat", "pompin", "bocchin", "succhiacazz", "succhiaminchi",
        "vaffancul", "fancul", "rottincul", "leccacul", "rompicazz", "cacacazz", "scassacazz",
        "testadicazz", "faccidimerda", "pezzodimerda", "figliodiputtan", "figlidiputtan",
        // Bestemmie e imprecazioni
        "porcodio", "diocane", "diocan", "porcamadonna", "dioporco", "diomerda",
        "canedio", "diocristo", "cristodio", "madonnaputtana", "madonnacagna",
        "mortacci"
    )

    private fun normalizeText(text: String): String {
        return text.lowercase()
            .replace("4", "a")
            .replace("@", "a")
            .replace("3", "e")
            .replace("1", "i")
            .replace("!", "i")
            .replace("0", "o")
            .replace("5", "s")
            .replace("$", "s")
            .replace("7", "t")
            .replace("+", "t")
            .replace("8", "b")
            .replace("2", "z")
    }

    private fun containsOffensiveWords(text: String): Boolean {
        val normalizedText = normalizeText(text)
        return offensiveWords.any { normalizedText.contains(it) }
    }

    val authState: StateFlow<AuthState> = combine(
        callbackFlow {
            val listener = FirebaseAuth.AuthStateListener { auth ->
                val user = auth.currentUser
                if (user != null && !user.isAnonymous) {
                    trySend(AuthState.Authenticated)
                } else {
                    trySend(AuthState.Unauthenticated)
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
        if (containsOffensiveWords(email)) {
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
                    val uid = result.user?.uid
                    viewModelScope.launch {
                        _processState.update { AuthState.Loading("Sincronizzazione account in corso...", null) }
                        movieRepository.syncWithFirebase(uid) { syncProgress ->
                            _processState.update { AuthState.Loading(syncProgress.message, syncProgress.progress) }
                        }
                        _processState.update { null }
                    }
                }
                .addOnFailureListener { exception ->
                    _processState.update { AuthState.Error(exception.message ?: "Errore di autenticazione") }
                }
        }
    }

    fun signUp(email: String, password: String) {
        if (containsOffensiveWords(email)) {
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
                    if (exception is FirebaseAuthUserCollisionException) {
                        _processState.update { AuthState.Error("L'email è già in uso. Effettua il login.") }
                    } else {
                        _processState.update { AuthState.Error(exception.message ?: "Errore nell'aggiornamento account") }
                    }
                }
        } else {
            // Normal sign up
            auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener {
                    _processState.update { null }
                }
                .addOnFailureListener { exception ->
                    _processState.update { AuthState.Error(exception.message ?: "Errore nella creazione dell'account") }
                }
        }
    }

    fun loginGuest() {
        _processState.update { AuthState.Loading("Accesso Ospite...") }
        auth.signInAnonymously()
            .addOnSuccessListener {
                _processState.update { null }
            }
            .addOnFailureListener { exception ->
                _processState.update { AuthState.Error(exception.message ?: "Errore accesso Guest") }
            }
    }

    fun logout() {
        viewModelScope.launch {
            auth.signOut()
            movieRepository.clearAllData()
            // State will update via callbackFlow
            _processState.update { null }
        }
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
                _processState.update { AuthState.Error(exception.message ?: "Errore durante l'eliminazione dell'account") }
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
                _processState.update { AuthState.Error("Email di ripristino inviata con successo") } // Using Error as a simple way to show message for now
            }
            .addOnFailureListener { exception ->
                _processState.update { AuthState.Error(exception.message ?: "Errore invio email") }
            }
    }

    fun clearError() {
        if (_processState.value is AuthState.Error) {
            _processState.update { null }
        }
    }
}
