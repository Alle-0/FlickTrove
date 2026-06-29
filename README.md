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
    <li><a href="#-il-progetto">🎯 Il Progetto</a></li>
    <li><a href="#-features-principali">✨ Features Principali</a></li>
    <li><a href="#-architettura-e-tecnologia">🛠 Architettura e Tecnologia</a></li>
    <li><a href="#-design--ui">🎨 Design & UI</a></li>
    <li><a href="#%EF%B8%8F-installazione--setup">⚙️ Installazione & Setup</a></li>
    <li><a href="#-utilizzo">🚀 Utilizzo</a></li>
    <li><a href="#-licenza">📄 Licenza</a></li>
    <li><a href="#-contatti--ringraziamenti">📫 Contatti & Ringraziamenti</a></li>
  </ol>
</details>

---

## 🎯 Il Progetto

**Qual è la motivazione dietro FlickTrove?**
Ci sono moltissime app per tracciare film e serie TV, ma spesso mancano di cura per i dettagli visivi o risultano poco fluide. L'obiettivo di FlickTrove è quello di fornire agli appassionati non solo uno strumento utile e funzionale, ma un'esperienza premium, reattiva e piacevole da usare, sfruttando al massimo le moderne tecnologie Android. 

Il progetto risolve il bisogno di avere una libreria personale elegante, sincronizzata in cloud, e con notifiche tempestive per le nuove uscite.

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

Dietro un'interfaccia splendida c'è un motore solido e scalabile. Abbiamo utilizzato le best practice dello sviluppo Android moderno.

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
- ✨ **Animazioni Premium**: Micro-interazioni, feedback aptico personalizzato e bounce-click effect rendono l'app incredibilmente reattiva e "viva" sotto le dita.

---

## ⚙️ Installazione & Setup

Per compilare FlickTrove localmente avrai bisogno di una chiave API fornita da **The Movie Database (TMDB)**.

### Prerequisiti
- [Android Studio](https://developer.android.com/studio) (versione recente)
- JDK 17+

### Passaggi

1. **Ottieni la chiave API:**
   Registrati su [TheMovieDB.org](https://www.themoviedb.org/documentation/api) e richiedi una API Key per sviluppatori (è gratis!).

2. **Clona la repository:**
   ```bash
   git clone https://github.com/Alle-0/FlickTrove_Kotlin.git
   ```

3. **Configura il progetto:**
   Apri il progetto, e nel file `local.properties` (se non esiste, crealo nella root del progetto), aggiungi questa riga:
   ```properties
   TMDB_API_KEY="INSERISCI_QUI_LA_TUA_CHIAVE"
   ```

4. **Compila e Avvia:**
   Attendi la sincronizzazione di Gradle in Android Studio, seleziona il tuo dispositivo fisico o emulatore e clicca su **Run** (▶️).

---

## 🚀 Utilizzo

Una volta avviata l'app:
1. Accedi o crea un account (i dati verranno salvati su Firebase).
2. Usa la barra di ricerca per trovare il tuo primo film o serie TV.
3. Clicca su "Aggiungi a libreria" e seleziona in quale cartella inserirlo.
4. *(Opzionale)* Vai alla tab "Statistiche" per monitorare il tuo tempo di visione.

---

## 📄 Licenza

Copyright © 2026 Alessandro Basile. Tutti i diritti riservati.

Questo repository è pubblico esclusivamente a scopo di portfolio e consultazione. Non è consentita la riproduzione, copia, modifica o ridistribuzione del codice, né l'utilizzo per scopi commerciali o non commerciali, senza l'esplicita autorizzazione scritta dell'autore. Consulta il file `TERMS_OF_SERVICE.md` per ulteriori dettagli.

---

## 📫 Contatti & Ringraziamenti

**Contatti:**
* Alessandro Basile - alessandrobasile909@gmail.com
* Link al progetto: [https://github.com/Alle-0/FlickTrove_Kotlin](https://github.com/Alle-0/FlickTrove_Kotlin)

**Ringraziamenti & Risorse Utili:**
* [The Movie Database (TMDB) API](https://www.themoviedb.org/documentation/api) - Per i dati su film e serie TV
* [Haze](https://github.com/chrisbanes/haze) - Per gli incredibili effetti glassmorphici
* [Shields.io](https://shields.io) - Per i badge README

<br>

<div align="center">
  <i>Sviluppato con passione per i maniaci del cinema e delle serie TV.</i>
</div>
