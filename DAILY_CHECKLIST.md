# 📋 Daily Production Call Sheet (Daily Checklist)

Welcome to the **NC-FLIX Daily Call Sheet**. To ensure our "production" (the app) runs smoothly and avoids box office flops (crashes), perform these checks daily or before every new release.

**Last Set Inspection:** 2025-12-09

---

## 🎥 Set Inspection (Scraper & Network)

Since our "locations" (source websites) change frequently, these are the most critical checks:

- [x] **Scout the Locations (Verify Selectors)**
    - Visit `Constants.BASE_URL` in a browser.
    - Inspect the DOM: Do movie items still use `div.ml-item`?
    - Check the search page: Do results still use `div.ml-item` or `div.result-item`?
    - *Action*: If changed, update `MovieRepository.kt`.

- [x] **Check Credentials (Cookies & User-Agent)**
    - Is the `Constants.USER_AGENT` still effective? (Try visiting the site with it).
    - Are the `Constants.COOKIES` expired? (Check if the site demands a new session).
    - *Action*: Update `Constants.kt` with fresh values from a real browser session if needed.

- [x] **Test the Uplink (VPN & Network)**
    - Run the app **with a VPN** connected.
    - Verify that data loads within the 30s timeout.
    - *Action*: If timeouts occur, check `NetworkClient.kt` or try a different VPN server.

---

## 🎬 Action! (App Functionality)

- [x] **The "Home" Scene**
    - Does the Home Screen load the Hero movie and Trending list?
    - Do images load via Coil?

- [x] **The "Search" Scene**
    - Try searching for a known movie (e.g., "Avatar").
    - Does the list populate?
    - Does clearing the search return to the Home view?

- [x] **The "Episode" Scene**
    - Click a movie/series. Does it open `EpisodeActivity` (or Player)?
    - Do episodes list correctly (if it's a series)?

- [x] **The "Player" Scene**
    - **Crucial**: Do the video embeds extract correctly?
    - Are ads being blocked by `AdBlockInterceptor`?
    - Does the video actually play?

---

## 📝 Script Continuity (Code & Docs)

- [x] **Code Compilation**
    - Run `./gradlew :app:assembleDebug` to ensure no "Conflicting overloads" or missing imports.

- [x] **Update the Log**
    - Have you added your changes to `CHANGELOG.md`?
    - *Action*: Never skip this. It's our history.

---

## 🧹 Wrap Up (Cleanup)

- [x] **Clean Build** (Optional but good)
    - Run `./gradlew clean` occasionally to clear ghosts from the machine.
