# SIMKL Integration Implementation Plan (Backlog)

The suggestion to integrate SIMKL as an alternative synchronization option alongside Trakt fits perfectly into FlickTrove's ecosystem-driven approach. Since FlickTrove already handles Trakt sync very well (as seen in `TraktSyncWorker.kt` and `TraktService.kt`), integrating SIMKL will largely follow a similar architectural pattern.

## Key Decisions (From Initial Review)

- **API Keys**: To be registered when development begins.
- **Mutual Exclusivity**: SIMKL and Trakt sync will be mutually exclusive. The user will choose only one active sync provider in the settings to avoid conflict resolution headaches.
- **Scope (MVP)**: The initial implementation will focus exclusively on **Watch History** and **Watchlist**. Ratings and other features will be deferred to later iterations.

## Proposed Architecture

We will replicate the structure used for Trakt, introducing specific SIMKL components.

### 1. API & Network Layer

We need to add models and Retrofit services to communicate with the SIMKL API (`https://api.simkl.com/`).

#### [NEW] `app/src/main/java/com/cinetrack/data/api/SimklService.kt`
- Retrofit interface defining the SIMKL endpoints (e.g., `/sync/history`, `/sync/watchlist`, `/users/settings`).

#### [NEW] `app/src/main/java/com/cinetrack/data/api/SimklModels.kt`
- Data classes for Simkl requests and responses, covering movies, shows, anime, and authentication tokens.

#### [NEW] `app/src/main/java/com/cinetrack/data/api/SimklAuthInterceptor.kt` & `SimklAuthenticator.kt`
- OkHttp interceptor and authenticator to handle OAuth2 token injection and refreshing.

---

### 2. Repository Layer

#### [NEW] `app/src/main/java/com/cinetrack/data/repository/SimklAuthRepository.kt`
- Handles storing and retrieving SIMKL access and refresh tokens (using DataStore or EncryptedSharedPreferences).
- Exposes `isLoggedIn` Flow for the UI.

#### [NEW] `app/src/main/java/com/cinetrack/data/repository/SimklSyncRepository.kt`
- Wraps the API calls and abstracts the logic for fetching and pushing sync data.

---

### 3. Worker & Sync Logic

#### [NEW] `app/src/main/java/com/cinetrack/worker/SimklSyncWorker.kt`
- A CoroutineWorker similar to `TraktSyncWorker.kt`.
- Handles bidirectional synchronization (local database to SIMKL and vice versa) using the Last Activities endpoint to minimize bandwidth.
- Emits progress via Notifications.

---

### 4. UI & Settings

#### [MODIFY] `app/src/main/java/com/cinetrack/ui/components/settings/SettingsScreen.kt`
- Add a new section under "Integrations" or "Synchronization" for SIMKL.
- Implement logic to ensure only Trakt OR SIMKL can be enabled at a time.
- Add an OAuth login flow (launching a Custom Tab or Intent to SIMKL's authorization URL).

---

### 5. Database Modifications (Room)

#### [MODIFY] `app/src/main/java/com/cinetrack/data/model/...`
- Ensure our local media entities can store `simkl_id`. This is crucial for matching items accurately if TMDB/IMDB IDs are missing or mismatched on SIMKL's side, especially for Anime.
