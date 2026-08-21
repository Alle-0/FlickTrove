# 🚀 FlickTrove v3.6.4

## ✨ What's New & UI Improvements

- **Offline Mode for TV Series**: You can now view and toggle watched episodes for TV Series even when completely offline! The app will gracefully fall back to local data and synchronize changes once a connection is restored.
- **"In Theaters" Badge Redesign**: Moved the localized "In theaters" badge to the "Where to watch" section, themed beautifully to match the premium provider rows.
- **Expanded Cast & Crew UI**: The "Cast" section has been heavily upgraded to include Crew members. Clicking on the Director section now opens a bottom sheet showing the full crew, neatly grouped by department (e.g., Directing, Writing, Sound).
- **Markdown Support**: Added Markdown support for comments in details, allowing richer text formatting!
- **Sleeker Comment Editor & Haptics**: 
  - Replaced the generic text "GIF" button with a polished `ic_gif` icon.
  - Added smooth `bounceClick` animations and haptic feedback to the new editor icons (GIF, Image, Markdown) and the Cast/Crew section headers.
- **Unreleased Media Enhancements**:
  - Unreleased movies now prominently display their full release date highlighted in the accent color.
  - Rating and interactive actions are now intelligently disabled for unreleased TV series to prevent invalid states.

## 🐛 Bug Fixes & Logic Tweaks

- **Ratings Visibility**: Fixed a UI bug where the global "FlickTrove Rating" would fade out. It now remains fully visible even when the user's rate action is disabled.
- **Trakt Sync**: Fixed underlying logic in the Trakt sync worker and rating synchronization.
- **Code Cleanup & Stability**:
  - Fixed a compilation issue related to `MediaType` parsing in the UI state mapper.
  - Removed duplicate imports in `DetailComments.kt`.
  - Removed the MVP percentage from `DetailCast` for a cleaner look.

## 🗺️ Roadmap Planning

- **SIMKL Integration (Backlog)**: Drafted the architectural implementation plan (`SIMKL_INTEGRATION_PLAN.md`) to support SIMKL sync alongside Trakt in a future update.
