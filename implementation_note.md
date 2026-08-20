# Implementation Note: Version 3.6.4 (Mini Update)

Questo aggiornamento introduce miglioramenti alla UI dei dettagli multimediali, estende il supporto alle lingue e pone le basi progettuali per future integrazioni.

## Modifiche Principali:

1. **Cast & Crew UI (`DetailCast.kt`, `DetailUiStateMapper.kt`)**
   - Estesa la sezione "Cast" per supportare e visualizzare anche la "Troupe" (Crew).
   - I membri della crew sono ora raggruppati per dipartimento (es. Regia, Scrittura, Suono, Fotografia).

2. **Traduzioni ed Etichette (`strings.xml`)**
   - Aggiunte le traduzioni complete per tutte le etichette dei dipartimenti (es. `dept_directing`, `dept_writing`, `dept_camera`, ecc.) in **8 lingue** (Inglese, Italiano, Tedesco, Spagnolo, Francese, Hindi, Portoghese, Russo).

3. **Roadmap e Pianificazione (`docs/roadmap/SIMKL_INTEGRATION_PLAN.md`)**
   - Creato e archiviato il documento di progettazione architetturale per la futura integrazione della sincronizzazione con SIMKL.

4. **Bump della Versione (`app/build.gradle.kts`)**
   - `versionCode` aggiornato a **18**.
   - `versionName` aggiornato a **"3.6.4"**.
