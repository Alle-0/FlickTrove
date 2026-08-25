# Goal Description

The user wants to remove the dialog that asks whether to import the oldest or newest watch date when importing data from external services (Trakt/Letterboxd). Instead, the app should now automatically process *all* watch dates for a movie and import them directly into the new `WatchHistory` (rewatch) system.

## User Review Required

- When importing multiple dates, the main `Movie` object in the library still needs a single `watchedAt` property. I will default to using the **latest (most recent)** date for the main library view, while *all* dates (including the first watch) will be saved in the `WatchHistory` table. This aligns with how most external services track the "last watched" status.

## Proposed Changes

### UI & ViewModel

#### [MODIFY] `SettingsScreen.kt`

- Remove `SettingsRewatchMigrationDialog` entirely.
- Remove the `pendingMigrationFilePath` state variable.
- In `externalMigrationLauncher.launch`, directly call `settingsViewModel.migrateExternalFile(path)` instead of showing the dialog.

#### [MODIFY] `SettingsViewModel.kt`

- Remove the `keepLatestWatchDate` parameter from `migrateExternalFile` and `migrateExternalData` since we will now process *all* dates automatically.

### Data Layer

#### [MODIFY] `Movie.kt`

- Add a new `@Transient var extractedWatchDates: MutableSet<String> = mutableSetOf()` to the `Movie` data class. This will act as a temporary bucket to collect all watch dates during the import parsing phase.

#### [MODIFY] `BackupRepository.kt`

- Inject `WatchHistoryDao` into the `BackupRepository` constructor.
- In `processAndSaveImportedItems`, when merging multiple entries of the same movie (e.g., from Trakt or Letterboxd CSVs), collect all `watchedAt` dates into the `extractedWatchDates` set instead of simply choosing one based on `keepLatestWatchDate`.
- After saving the merged `Movie` to `favoriteDao`, fetch the existing watch history for that movie from `WatchHistoryDao`.
- Filter out any dates that are already in the database to prevent duplicates.
- Sort the new dates chronologically. Determine which ones are `isRewatch` (if it's not the first chronological date).
- Insert the new dates into `WatchHistoryDao` using `insertAll`.

#### [MODIFY] `UniversalCsvImporter.kt` / `TraktJsonImporter.kt` / `TvTimeGdprImporter.kt`

- Remove the `keepLatestWatchDate` parameter from the `import...` functions as it's no longer needed to discard duplicates.

## Verification Plan

### Automated Tests

- N/A

### Manual Verification

- Attempt to import a CSV or JSON file containing a movie watched on two different dates.
- Verify that the dialog asking "which date to keep" no longer appears.
- Navigate to the movie's details page and open the Rewatch Bottom Sheet.
- Verify that *both* dates appear in the watch history list correctly.
