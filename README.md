# NovelReader

A personal Android web novel reader companion app for tracking reading progress and syncing with the NovelApp web tracker.

## Features

- **Sync with web app**: Push and pull reading progress from NovelApp web tracker
- **Multiple novel sources**: Support for numerous web novel platforms
- **Material 3 UI**: Modern, clean interface following Android design guidelines
- **Local database**: Offline access to your library and reading history
- **Text-to-speech**: Built-in audio narration (background playback supported)
- **Live translation**: Translate novel content on-the-fly with Gemini AI
- **Light and dark themes**: Adaptive theming options
- **Advanced reader features**:
  - Custom font and font size
  - Infinite scroll for seamless reading
  - Text selection and highlighting
  - Background playback controls

## Tech Stack

- Kotlin + Jetpack Compose (Material 3)
- Room (SQLite) for local storage
- Retrofit + OkHttp for networking
- Hilt for dependency injection
- WorkManager for background tasks
- Coil for image loading

## Getting Started

1. Clone the repository
2. Open in Android Studio
3. Build and run on a device or emulator
4. Configure the NovelApp web server URL in Settings → Data

## Syncing with NovelApp

To enable sync:
1. Start the NovelApp web server (runs on `http://localhost:3000` by default)
2. Update the server URL in the app settings (ensure your device can reach the server)
3. Tap "Sync with web app" in Settings → Data

The app will push your reading progress and pull any updates from the web tracker.

## License

This project is released under the GNU General Public License v3 (GPL-3). This is a modified version that builds upon prior work in the web novel reader community.
