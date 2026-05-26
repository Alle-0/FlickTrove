<div align="center">

# 🎬 FlickTrove

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android-3DDC84?style=for-the-badge&logo=android&logoColor=white" alt="Android">
  <img src="https://img.shields.io/badge/Kotlin-1.9+-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white" alt="Kotlin">
  <img src="https://img.shields.io/badge/Jetpack_Compose-Material_3-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white" alt="Jetpack Compose">
</p>

**Traccia film, serie TV e attori con uno stile mozzafiato.**

*FlickTrove è un'esperienza visiva premium. Un'app Android moderna costruita interamente con Jetpack Compose, caratterizzata da un'interfaccia glassmorphica unica, palette di colori dinamiche e animazioni fluide ad alta frequenza.*

</div>

---

<details open>
  <summary><b>📖 Indice</b></summary>
  <ol>
    <li><a href="#-features">✨ Features Principali</a></li>
    <li><a href="#-architettura-e-tecnologia">🛠 Architettura e Tecnologia</a></li>
    <li><a href="#-design--ui">🎨 Design & UI</a></li>
    <li><a href="#-installazione--setup">⚙️ Installazione & Setup</a></li>
  </ol>
</details>

---

## ✨ Features Principali

FlickTrove non è solo un tracker, ma una libreria personale costruita su misura per gli appassionati:

- 🔍 **Ricerca Completa**: Accedi all'intero catalogo TMDB. Trova film, serie TV, e scopri i dettagli biografici e la filmografia degli attori.
- 📁 **Organizzazione Custom**: Crea cartelle personalizzate. Assegna nomi, colori dinamici tramite Color Wheel e organizza le tue visioni come preferisci.
- 🔔 **Notifiche Intelligenti**: Non perdere mai un'uscita. Ricevi avvisi puntuali quando un titolo o l'episodio di una serie TV che stai aspettando viene rilasciato.
- 📊 **Statistiche Avanzate**: Monitora il tuo tempo di visione, analizza i generi che preferisci e guarda quanto tempo della tua vita hai dedicato al cinema e alle serie TV.
- 🎬 **Gestione Episodica**: Segna quali episodi hai già visto. Filtra per stagioni e resta sempre al passo con le tue serie preferite.
- ☁️ **Sincronizzazione**: Supporto nativo a Firebase per il salvataggio dei dati (account & backup).

---

## 🛠 Architettura e Tecnologia

Dietro un'interfaccia splendida c'è un motore solido e scalabile.

| Categoria | Stack / Libreria |
| :--- | :--- |
| **Linguaggio** | 100% Kotlin |
| **Architettura** | MVVM (Model-View-ViewModel) |
| **UI Framework** | Jetpack Compose + Material Design 3 |
| **Networking** | Retrofit 2 + OkHttp |
| **Database Locale** | Room (SQLite Object Mapping) |
| **Dependency Injection** | Dagger Hilt |
| **Immagini & Colori** | Coil (Caricamento e Color Extraction) |
| **Backend & Auth** | Firebase (Auth, Analytics, Firestore) |

<details>
<summary><b>Approfondimento Tecnico</b></summary>
<br>
FlickTrove adotta i più moderni pattern Android: Kotlin Coroutines e Flow per la gestione reattiva dei dati. Hilt semplifica le dipendenze rendendo il codice testabile e modulare. La navigazione avviene tramite il Compose Navigation framework integrato nativamente per evitare la frammentazione.
</details>

---

## 🎨 Design & UI

Il design è il vero fiore all'occhiello di FlickTrove.

- 🪟 **Glassmorphism Integrale**: Utilizziamo un motore di blur custom (tramite la libreria *Haze*) per sfocare i contenuti sotto pannelli, drawer e top bar in tempo reale a 60/120fps.
- 🌈 **Dynamic Theming**: I colori predominanti di locandine e sfondi vengono estratti dinamicamente da Coil per tematizzare l'intera schermata (gradienti, bottoni e accenti).
- ✨ **Animazioni Premium**: Micro-interazioni, feedback aptico aptico personalizzato e bounce-click effect rendono l'app incredibilmente reattiva e "viva" sotto le dita.

---

## ⚙️ Installazione & Setup

Per compilare FlickTrove localmente avrai bisogno di una chiave API fornita da **The Movie Database (TMDB)**.

1. **Ottieni la chiave API:**
   Registrati su [TheMovieDB.org](https://www.themoviedb.org/documentation/api) e richiedi una API Key per sviluppatori (è gratis!).

2. **Configura il progetto:**
   Clona la repository e apri il file `local.properties` (se non esiste, crealo nella root del progetto). Aggiungi questa riga:
   ```properties
   TMDB_API_KEY="INSERISCI_QUI_LA_TUA_CHIAVE"
   ```

3. **Compila e Avvia:**
   Apri il progetto su *Android Studio*, attendi il Gradle sync, seleziona il tuo dispositivo fisico o emulatore e clicca su **Run** (▶️).

---

<div align="center">
  <i>Sviluppato con passione per i maniaci del cinema e delle serie TV.</i>
</div>
