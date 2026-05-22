package com.cinetrack.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cinetrack.data.repository.MovieRepository
import com.google.firebase.auth.FirebaseAuth
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

    val authState: StateFlow<AuthState> = combine(
        callbackFlow {
            val listener = FirebaseAuth.AuthStateListener { auth ->
                trySend(if (auth.currentUser != null) AuthState.Authenticated else AuthState.Unauthenticated)
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
        if (email.isBlank() || password.isBlank()) {
            _processState.update { AuthState.Error("Email e password sono obbligatorie") }
            return
        }

        _processState.update { AuthState.Loading("Accesso in corso...") }
        
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

    fun signUp(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _processState.update { AuthState.Error("Email e password sono obbligatorie") }
            return
        }

        _processState.update { AuthState.Loading("Creazione account...") }
        
        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                _processState.update { null }
            }
            .addOnFailureListener { exception ->
                _processState.update { AuthState.Error(exception.message ?: "Errore nella creazione dell'account") }
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
