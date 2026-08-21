# Implementation Note: v3.6.5 Update

## Updates Included in this Release:

### 1. Offline Mode for TV Series Episodes
- **Problem**: When the app was offline, the TV Series episodes list failed to load because it couldn't fetch the season details (such as episode names and images) from TMDB.
- **Solution**: Implemented a fallback mechanism in `EpisodesBottomSheet.kt`. If the TMDB network call fails due to no internet connection, the UI now generates a mock list of episodes based on the local `episodeCount` stored in the `MovieEntity`'s `seasons` property.
- **Result**: You can now successfully view the checklist of episodes and mark them as watched/unwatched while completely offline. The changes are saved locally and will synchronize once a connection is restored.

### 2. "In Theaters" Badge Relocation and Theming
- Moved the "In Theaters" badge from the main header (`DetailHeader.kt`) to the "Where to watch" section (`DetailMetaRows.kt`) to improve layout hierarchy.
- Themed the badge to look like a premium provider row, aligning perfectly with the app's aesthetic guidelines.

### 3. SIMKL Integration Plan (Backlog)
- Created the SIMKL implementation plan document at `docs/roadmap/SIMKL_INTEGRATION_PLAN.md` for future reference, covering the architecture and integration points to synchronize watch history alongside Trakt.

### 4. UI Polish & Bug Fixes
- **Click Modifiers**: Added the custom `bounceClick` modifier to the "CAST >" and "CREW >" section headers in `DetailCast.kt`, as well as to the GIF, Image, and Markdown attachment icons in `CommentsScreen.kt`, providing consistent haptic and visual feedback across the app.
- **Personal Zone Rating Alpha**: Fixed a visual bug in `DetailPersonalZone.kt` where the entire row was being dimmed (`alpha = 0.4f`) when the rating action was disabled (e.g. movie not watched yet). The dimming is now only applied to the left "RATE" side, keeping the "FLICKTROVE RATING" section on the right fully visible as intended.

### 5. Version Bump
- Bumped app version to **3.6.5** (versionCode: 19) in `app/build.gradle.kts`.
