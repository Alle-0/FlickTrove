<div align="center">

# 🎬 FlickTrove

**Traccia film, serie TV e attori con uno stile unico.**
Un'app Android moderna costruita interamente con Jetpack Compose, con un'interfaccia glassmorphica e animazioni fluide.

</div>

---

## Cosa fa

FlickTrove è la tua app personale per gestire la tua lista di cose da vedere. Puoi:

- **Cercare** milioni di film, serie TV e attori tramite il catalogo TMDB
- **Tenere traccia** di cosa vuoi guardare e cosa hai già visto
- **Organizzare** i titoli in cartelle personalizzate con colori e nomi a tua scelta
- **Ricevere notifiche** quando un titolo che segui viene rilasciato
- **Scoprire** dettagli, cast, stagioni ed episodi direttamente in-app

## Come è fatto

Costruita interamente in **Kotlin** con architettura MVVM. Le librerie principali:

| Libreria | Utilizzo |
|---|---|
| Jetpack Compose + Material 3 | UI dichiarativa |
| [Haze](https://github.com/chrisbanes/haze) | Effetto glassmorphismo (blur) |
| Coil | Caricamento immagini e color extraction |
| Room | Database locale |
| Retrofit + OkHttp | Chiamate API TMDB |
| Hilt | Dependency injection |
| Firebase | Analytics e notifiche |

## Setup

Serve una chiave API TMDB gratuita da [themoviedb.org](https://www.themoviedb.org/documentation/api).

Aggiungila in `local.properties`:
```
TMDB_API_KEY="la_tua_chiave_qui"
```

Poi apri il progetto in Android Studio e premi Run.

---

<div align="center">
  Fatto con Kotlin · Jetpack Compose · Android
</div>
