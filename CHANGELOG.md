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

### Added
- Updated `Attendance.md` with routine operational log update (2026-02-19).
- Updated `Attendance.md` with routine operational log update (2026-02-18).
- Updated `Attendance.md` with routine operational log update (2026-02-14).
- Updated `Attendance.md` with routine operational log update (2026-02-12).
- Updated `Attendance.md` with routine operational log update (2026-02-03).
- Updated `Attendance.md` with routine operational log update (2026-02-02).
- Updated `Attendance.md` with routine operational log update (2026-02-01).
- Updated `Attendance.md` with routine operational log update (2026-01-31).
- Updated `Attendance.md` with routine operational log update (2026-01-29).
- Updated `Attendance.md` with routine operational log update (2026-01-28).
- Updated `Attendance.md` with routine operational log update (2026-01-25).
- Updated `Attendance.md` with routine operational log update (2026-01-24).
- Updated `Attendance.md` with routine operational log update (2026-01-23).
- Updated `Attendance.md` with routine operational log update (2026-01-22).
- Updated `Attendance.md` with routine operational log update (2026-01-21).
- Updated `Attendance.md` with routine operational log update (2026-01-20).
- Updated `Attendance.md` with routine operational log update (2026-01-19).
- Updated `Attendance.md` with new operational engagement log entry for 2026-01-16.
- Updated `Attendance.md` with new operational engagement log entry for 2026-01-15.
- Updated `Attendance.md` with new operational engagement log entry for 2026-01-14.
- Updated `Attendance.md` with new operational engagement log entry for 2026-01-13.
- Updated `Attendance.md` with new operational engagement log entry for 2026-01-12.
- Updated `Attendance.md` with new operational engagement log entry for 2026-01-11.
- Updated `Attendance.md` with new operational engagement log entry for 2026-01-10.
- Updated `Attendance.md` with new operational engagement log entry for 2026-01-09.
- Updated `Attendance.md` with new operational engagement log entry for 2026-01-08.
- Updated `Attendance.md` with new operational engagement log entry for 2026-01-07.
- Updated `Attendance.md` with new operational engagement log entry for 2026-01-06.
- Updated `Attendance.md` with new operational engagement log entry for 2026-01-05.
- Updated `Attendance.md` with new operational engagement log entry for 2026-01-04.
- Updated `Attendance.md` with new operational engagement log entry for 2026-01-03.
- Updated `Attendance.md` with new operational engagement log entry.
- Updated `Attendance.md` with routine operational log update (2026-01-02).
- Initialized `Attendance.md` for Red Team operational engagement logging.
- Verified and standardized backup rules in `data_extraction_rules.xml` and `backup_rules.xml` to explicitly include shared preferences, databases, and files, while excluding `device.xml`.
- Confirmed `AndroidManifest.xml` correctly links to the backup configuration files.

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

### Performance
- Optimized `AdBlocker` by introducing a `ConcurrentHashMap` cache for domain checks.
  - Reduces repetitive ad-checking complexity from O(N*M) to O(1) for visited domains.
  - Removed redundant `AD_BLOCK_DOMAINS` from `Constants.kt` to reduce static memory usage.
- Optimized `AdBlocker` to reduce object allocation and redundant string processing on every network request.
  - Replaced `Set` with `List` for ad keywords to improve iteration speed.
  - Implemented `isAdHostRaw` to skip redundant lowercasing when source is already normalized (e.g., `HttpUrl`).
  - Pre-computed lowercase ad hosts set to avoid (N)$ lowercasing in the loop.
  - Added `isAd(HttpUrl)` overload to avoid parsing `URI` objects from strings in `AdBlockInterceptor`, utilizing OkHttp's already-parsed `HttpUrl`.
- Optimized `MovieAdapter` scrolling performance by reducing object allocation in `onBindViewHolder`.
  - Moved `OnClickListener` setup to `onCreateViewHolder`.
- Optimized `EpisodeAdapter` scrolling performance by reducing object allocation in `onBindViewHolder`.
  - Moved `OnClickListener` setup to `onCreateViewHolder`.
- Optimized `MovieRepository` scraping performance by pre-compiling Regex patterns.
  - Replaced on-the-fly Regex creation in `fetchTop10Malaysia` loops with `companion object` constants to reduce memory allocation and CPU usage during parsing.
- Optimized year parsing in `MovieRepository` scraping loop.
  - Replaced Regex matching with manual character checks (`length` and `isDigit`) in `fetchTop10Malaysia` to avoid Regex overhead and reduce object allocation for every item.
- Optimized `MovieAdapter` list updates by migrating to `ListAdapter` and `DiffUtil`.
  - Enables efficient background diffing of list changes.
  - Prevents full list rebinds on updates, critical for search performance.
- Optimized `MainActivity` to reuse RecyclerView adapter instances instead of recreating them on every data update.
- Optimized `PlayerActivity` ad-blocking checks to avoid expensive `java.net.URI` parsing.
  - Refactored `AdBlocker` to expose `isAdHost` for direct host checking.
  - Updated `WebViewClient` to use `android.net.Uri` properties directly, providing a ~21x speedup in ad detection logic.
- Optimized `NetworkClient` request interceptor.
  - Pre-computed cookie header string in `Constants` to eliminate allocation on every request.
  - Switched from `url.toString().contains()` to `url.host.contains()` to avoid full URL string allocation and prevent potential cookie leakage to non-target hosts.
- Optimized `MovieRepository` scraping to reduce memory pressure.
  - Refactored all HTML parsing logic to use `Jsoup.parse(InputStream)` instead of `Jsoup.parse(String)`.
  - This avoids allocating large String objects for the entire HTML response body, significantly reducing Garbage Collection pressure during scraping.

## 2025-12-25
- ⚡ Bolt: Reuse shared OkHttpClient in UpdateChecker to reduce memory overhead and leverage connection pooling.
