# 🛠️ NC-FLIX Developer Reference

Welcome backstage! 🎫 This guide contains everything you need to know to work on the NC-FLIX codebase.

---

## 🏗️ Architecture

NC-FLIX follows the **MVVM (Model-View-ViewModel)** architectural pattern to ensure a separation of concerns and testability.

### 🎭 The Cast (Components)

*   **Model**: Data classes representing the core entities (e.g., `Movie.kt`).
*   **View**: The UI layer. Displays data and captures user interactions.
    *   `MainActivity`: The home stage.
    *   `PlayerActivity`: Where the action happens.
    *   `EpisodeActivity`: For series bingers.
*   **ViewModel**: The bridge between UI and Data. Holds UI state and survives configuration changes.
    *   `MainViewModel`, `PlayerViewModel`, `EpisodeViewModel`.
*   **Repository**: The single source of truth for data. Orchestrates fetching from network or cache.
    *   `MovieRepository`: Manages movie data fetching.

---

## 📂 Project Structure

```
com.ncflix.app
├── model/       # Data classes
├── ui/          # Activities, Fragments, ViewModels
├── data/        # Repositories, API services
├── adapter/     # RecyclerView adapters
└── utils/       # Helper classes, Constants, Extensions
```

---

## 🎬 Dependencies (The Crew)

We rely on these libraries to keep the show running:

*   **Jsoup**: For scraping content from the web.
*   **OkHttp**: Handles all network requests.
*   **Coil**: Loads images asynchronously.
*   **Coroutines**: Manages background tasks and async code.
*   **Media3 ExoPlayer**: The engine behind the video playback.
*   **AndroidX**: The standard suite of Android libraries.

---

## 📝 Implementation Notes & TODOs

### Current Implementation
*   **Scraping**: The app currently scrapes data directly. Be mindful of website structure changes breaking the parser.
*   **Ad-Blocking**: We use a custom interceptor to block ad domains during scraping.

### Technical To-Do List
*   [ ] **Dependency Injection**: Introduce Hilt for cleaner dependency management.
*   [ ] **Testing Strategy**:
    *   Unit tests for `MovieRepository`.
    *   UI tests using Espresso.
*   [ ] **Error Handling**: Further refine `Resource` wrapper to handle specific network errors gracefully.
*   [ ] **Clean Architecture**: Consider adding UseCases/Interactors if business logic grows complex.

---

## 🚀 Getting Started

1.  **Clone & Sync**: Clone the repo and let Gradle download the "crew" (dependencies).
2.  **Run**: Launch on an emulator or device.
3.  **Debug**: Use Logcat with tag `NCFLIX` to trace the action.
