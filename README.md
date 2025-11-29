# NC-FLIX

NC-FLIX is an Android application that allows users to browse and stream movies and TV series. It scrapes content from external sources and provides a clean, ad-free viewing experience with a custom video player.

## Features

- **Home Screen**: Displays a featured movie and a list of trending movies.
- **Search**: Allows users to search for movies and TV shows.
- **Series Support**: Handles TV series with season and episode selection.
- **Custom Player**:
    - Embedded WebView player that removes ads and popups.
    - Gesture controls (double-tap to seek, tap to play/pause).
    - Auto-play and resume functionality.
    - Automatic server switching if a source is dead.

## Architecture

The application follows a standard Android architecture:

-   **Model**: Data classes representing movies (`Movie.kt`).
-   **View**: Activities and Layouts (`MainActivity`, `EpisodeActivity`, `PlayerActivity`).
-   **Adapter**: RecyclerView adapters for displaying lists (`MovieAdapter`, `EpisodeAdapter`).
-   **Data/Repository**: Handles network requests and data parsing (`MovieRepository`).

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
    -   **Play/Pause**: Tap the center of the screen.
    -   **Seek Forward 10s**: Double-tap the right side of the screen.
    -   **Seek Backward 10s**: Double-tap the left side of the screen.

## Dependencies

-   **Jsoup**: For HTML parsing and web scraping.
-   **OkHttp**: For network requests.
-   **Coil**: For image loading.
-   **Coroutines**: For asynchronous programming.
-   **AndroidX Libraries**: Core, AppCompat, ConstraintLayout, RecyclerView, etc.

## Disclaimer

This application is for educational purposes only. It scrapes content from third-party websites and does not host any content itself. The developers are not responsible for the content accessed through this application.
