# NC-FLIX

NC-FLIX is an Android application that allows users to browse and stream movies and TV series. It scrapes content from external sources and provides a clean, ad-free viewing experience with a custom video player.

## Features

- **Home Screen**: Displays a featured movie and a list of trending movies.
- **Search**: Allows users to search for movies and TV shows.
- **Series Support**: Handles TV series with season and episode selection.
- **Custom Player**:
    - Native ExoPlayer for efficient video playback.
    - Integrated ad-blocking for network requests during scraping.
    - Standard ExoPlayer controls (play/pause, seek, buffering indicator).
    - Keeps screen on during playback.

## Architecture

The application follows a standard Android architecture:

-   **Model**: Data classes representing movies (`Movie.kt`).
-   **View**: Activities and Layouts (`MainActivity`, `EpisodeActivity`, `PlayerActivity`).
-   **ViewModel**: Manages UI-related data and business logic (`MainViewModel`, `EpisodeViewModel`, `PlayerViewModel`).
-   **Adapter**: RecyclerView adapters for displaying lists (`MovieAdapter`, `EpisodeAdapter`).
-   **Data/Repository**: Handles network requests and data parsing (`MovieRepository`).
-   **Utilities**: Ad-blocking interceptors and Resource wrapper for robust error handling.

## Setup and Installation

### Prerequisites

-   Android Studio Arctic Fox or later.
-   JDK 11 or later.
-   Android SDK.

### Building the Project

1.  Clone the repository:
    ```bash
    git clone https://github.com/your-username/nc-flix.git
    ```
2.  Open the project in Android Studio.
3.  Let Gradle sync the dependencies.
4.  Run the application on an emulator or a physical device.

## Usage

1.  **Launch the App**: You will see the home screen with a hero movie and a list of trending content.
2.  **Play a Movie**: Tap on any movie poster to open the player.
3.  **Search**: Use the search bar at the top to find specific titles.
4.  **Watch Series**: If you select a TV series, you will be taken to a screen to select the season and episode.
5.  **Player Controls**:
    -   Standard ExoPlayer controls are available during playback.

## Roadmap / To-Do

The following improvements are planned for future releases. Contributions are welcome!

- [x] **Architecture**: Migrate to MVVM (Model-View-ViewModel) architecture to better separate UI logic from data operations.
- [ ] **Dependency Injection**: Implement Hilt or Koin for better dependency management and testability.
- [ ] **Testing**: Add Unit tests for the Repository and ViewModels, and UI tests using Espresso/kakao.
- [x] **Error Handling**: Replace generic Exception handling with a robust Result/Resource wrapper (e.g., using `sealed class Result<T>`).
- [x] **Resources**: Extract hardcoded strings into `res/values/strings.xml` for localization support.
- [ ] **Data Source**: Add support for multiple streaming sources or a plugin system.
- [ ] **Caching**: Implement a local database (Room) to cache movie data and playback progress.
- [ ] **UI/UX**: Improve the UI design with Material Design 3 components and animations.

## Dependencies

-   **Jsoup**: For HTML parsing and web scraping.
-   **OkHttp**: For network requests.
-   **Coil**: For image loading.
-   **Coroutines**: For asynchronous programming.
-   **AndroidX Libraries**: Core, AppCompat, ConstraintLayout, RecyclerView, etc.
-   **Media3 ExoPlayer**: For native video playback.

## Disclaimer

This application is for educational purposes only. It scrapes content from third-party websites and does not host any content itself. The developers are not responsible for the content accessed through this application.