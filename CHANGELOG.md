# Changelog

## [1.5.0]

### Added
- Implemented app update checker for NCEngineering/NCFlix-Android GitHub repository.
- Display current app version on the main screen.

### Changed
- Moved ExoPlayer autoplay setting to initialization block for clarity in `NativePlayerActivity.kt`.
- Replaced text "N" logo with actual app icon in `MainActivity`.
- Updated versioning in `build.gradle.kts` to 1.5 (code 6).

### Fixed
- Corrected XML syntax error in `activity_main.xml`.
- Enabled search functionality on `MainActivity` by adding click listener to the search icon.
- Fixed nullability issue for `pInfo.versionName` in `MainActivity.kt`.
- Improved IMDb poster resolution logic in `MovieRepository.kt` to be more robust using regex.

## [Unreleased]

### Fixed
- Fixed memory leaks in `PlayerActivity` by replacing `Handler` with `lifecycleScope` and using `WeakReference` for `JavascriptInterface`.

### Added
- Created `CHANGELOG.md` to track project history.

### Fixed
- Fixed data fetch failure when connected to VPN by increasing network timeouts to 30 seconds.
- Unified User-Agent configuration to use `Constants.USER_AGENT` consistently.
- Fixed application crash/failure when using the search feature by implementing proper search logic and UI handling.
- Refactored `fetchEpisodes` to `getEpisodes` to resolve conflicting overloads compilation error.

### Added
- Added `DAILY_CHECKLIST.md` for daily maintenance and verification tasks.

### Changed
- Updated `DAILY_CHECKLIST.md` to reflect successful verification on 2025-12-09.
## [Unreleased]
- Performance: Optimized `AdBlocker` to reduce object allocation and redundant string processing on every network request.
  - Pre-computed lowercase ad hosts set to avoid (N)$ lowercasing in the loop.
  - Added `isAd(HttpUrl)` overload to avoid parsing `URI` objects from strings in `AdBlockInterceptor`, utilizing OkHttp's already-parsed `HttpUrl`.
- Performance: Optimized `MovieAdapter` scrolling performance by reducing object allocation in `onBindViewHolder`.
  - Moved `OnClickListener` setup to `onCreateViewHolder`.
- Performance: Optimized `EpisodeAdapter` scrolling performance by reducing object allocation in `onBindViewHolder`.
  - Moved `OnClickListener` setup to `onCreateViewHolder`.
