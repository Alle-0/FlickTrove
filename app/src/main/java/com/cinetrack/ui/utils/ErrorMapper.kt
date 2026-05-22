package com.cinetrack.ui.utils

object ErrorMapper {
    fun map(message: String?): String {
        if (message == null) return "Ops! Qualcosa è andato storto. Riprova più tardi."
        
        return when {
            message.contains("Unable to resolve host", ignoreCase = true) || 
            message.contains("UnknownHostException", ignoreCase = true) -> 
                "Problema di connessione. Verifica che il tuo dispositivo sia online."
                
            message.contains("timeout", ignoreCase = true) || 
            message.contains("SocketTimeoutException", ignoreCase = true) -> 
                "La connessione è lenta o il server non risponde. Riprova tra un istante."
                
            message.contains("401", ignoreCase = true) -> 
                "Errore di autenticazione. Le chiavi di accesso potrebbero essere scadute."
                
            message.contains("404", ignoreCase = true) -> 
                "Il contenuto richiesto non è stato trovato."
                
            message.contains("500", ignoreCase = true) || 
            message.contains("503", ignoreCase = true) -> 
                "Il server è temporaneamente fuori servizio. Riprova più tardi."
                
            else -> "Si è verificato un errore imprevisto. Riprova tra poco."
        }
    }
}
