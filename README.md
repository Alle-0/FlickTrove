<div align="center">

<img src="figma_exports_svg_v2/Icon.svg" alt="FlickTrove Logo" width="120" height="120" />

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
    <li><a href="#-screenshot-ui">📱 Screenshot UI</a></li>
    <li><a href="#-architettura-e-tecnologia">🛠 Architettura e Tecnologia</a></li>
    <li><a href="#-design--ui">🎨 Design & UI</a></li>
    <li><a href="#%EF%B8%8F-nota-per-sviluppatori">⚠️ Nota per Sviluppatori</a></li>
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
- 📴 **Offline-First**: Accedi alla tua libreria personale e salva le tue preferenze anche senza connessione internet, grazie al solido caching locale (Room DB).
- 🌍 **Localizzazione**: Architettura multi-lingua nativa per un'esperienza internazionale (attualmente Italiano e Inglese).

---

## 📱 Screenshot UI

<div align="center">
  <table>
    <tr>
      <td align="center"><b>Home & Blur Effect</b></td>
      <td align="center"><b>Ricerca & Colori Dinamici</b></td>
      <td align="center"><b>Dettaglio Film</b></td>
    </tr>
    <tr>
      <td><img src="https://via.placeholder.com/250x500/1e1e24/4285F4?text=Screen+1" width="250"/></td>
      <td><img src="https://via.placeholder.com/250x500/1e1e24/3DDC84?text=Screen+2" width="250"/></td>
      <td><img src="https://via.placeholder.com/250x500/1e1e24/7F52FF?text=Screen+3" width="250"/></td>
    </tr>
  </table>
  <br>
  <i>(Sostituisci questi link placeholder con screenshot reali della tua app!)</i>
</div>

---

## 🛠 Architettura e Tecnologia

Dietro un'interfaccia splendida c'è un motore solido e scalabile. Abbiamo utilizzato le best practice dello sviluppo Android moderno.

| Categoria | Stack / Libreria |
| :--- | :--- |
| **Linguaggio** | <img src="https://img.shields.io/badge/100%25_Kotlin-7F52FF?style=flat-square&logo=kotlin&logoColor=white" alt="Kotlin"> |
| **Architettura** | <img src="https://img.shields.io/badge/MVVM-3DDC84?style=flat-square&logo=android&logoColor=white" alt="MVVM"> |
| **UI Framework** | <img src="https://img.shields.io/badge/Jetpack_Compose-4285F4?style=flat-square&logo=jetpackcompose&logoColor=white" alt="Compose"> <img src="https://img.shields.io/badge/Material_3-EADDFF?style=flat-square&logo=materialdesign&logoColor=black" alt="Material 3"> |
| **Networking** | <img src="https://img.shields.io/badge/Retrofit_2-FF6347?style=flat-square&logo=square&logoColor=white" alt="Retrofit"> <img src="https://img.shields.io/badge/OkHttp-109D59?style=flat-square&logo=square&logoColor=white" alt="OkHttp"> |
| **Database Locale** | <img src="https://img.shields.io/badge/Room_DB-3DDC84?style=flat-square&logo=sqlite&logoColor=white" alt="Room"> |
| **Dependency Injection** | <img src="https://img.shields.io/badge/Dagger_Hilt-000000?style=flat-square&logo=google&logoColor=white" alt="Hilt"> |
| **Immagini & Colori** | <img src="https://img.shields.io/badge/Coil-2A2A2A?style=flat-square&logo=android&logoColor=white" alt="Coil"> (Caricamento & Estrattore Colori Dinamico) |
| **Backend & Auth** | <img src="https://img.shields.io/badge/Firebase-FFCA28?style=flat-square&logo=firebase&logoColor=black" alt="Firebase"> |

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

## ⚠️ Nota per Sviluppatori

Per ragioni di sicurezza, i file di configurazione del backend (come il `google-services.json` di Firebase) e le chiavi API private (TMDB, OMDB, Trakt) sono stati esclusi da questa repository. 

Pertanto, il progetto non può essere clonato e compilato "out-of-the-box". Il codice sorgente è esposto pubblicamente **unicamente a scopo di revisione e portfolio** per mostrare le competenze tecniche, l'architettura e le best practice adottate nello sviluppo dell'app.

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
* [Shields.io](https://shields.io) - Per i badge README

<br>

<div align="center">
  <i>Sviluppato con passione per i maniaci del cinema e delle serie TV.</i>
</div>
