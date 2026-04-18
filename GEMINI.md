# EdgePanel - Project Context

EdgePanel is a secure Android clipboard manager that provides a floating "Edge Panel" overlay for quick access to clipboard history, including both text and images. It features encrypted storage and specialized techniques to ensure reliable clipboard access across various Android distributions (including MIUI).

## Project Overview

- **Core Functionality:**
    - **Clipboard Monitoring:** Captures text and image clips in the background using an Accessibility Service.
    - **Edge Panel Overlay:** A draggable handle at the edge of the screen that expands into a scrollable history panel.
    - **Secure Storage:** All clipboard items are stored in an encrypted Room database.
    - **Image Support:** Captures and displays image clips, storing them as compressed JPEGs in internal storage.
    - **MIUI Compatibility:** Uses a "Focus Window" hack to gain temporary window focus for clipboard access on restricted systems.

## Architecture

- **UI Layer:**
    - `MainActivity.kt`: Settings and configuration UI built with **Jetpack Compose**.
    - `EdgePanelService.kt`: Manages the system overlay window using traditional **Android Views** and a custom `GestureScrollView`.
    - `ClipboardProxyActivity.kt`: A transparent activity used for certain clipboard operations if needed.
- **Service Layer:**
    - `EdgePanelService`: The primary foreground service managing the overlay and the "Focus Window".
    - `ClipboardAccessibilityService`: Monitors clipboard events and captures content.
- **Data Layer:**
    - `ClipDatabase`: Room database encrypted with **SQLCipher**.
    - `ClipRepository`: Orchestrates data operations between the services and the DAO.
    - `ClipEntity`: Represents a single clipboard item (Text or Image).
- **Communication:**
    - Uses Kotlin **Flow** to observe database changes and update the overlay UI in real-time.

## Key Technologies

- **Language:** Kotlin
- **UI Framework:** Jetpack Compose (Settings) & Custom Views (Overlay)
- **Database:** Room with SQLCipher (Encryption)
- **Security:** AndroidX Security Crypto (for storing DB passphrases)
- **Background Tasks:** Kotlin Coroutines & Flow
- **Android APIs:** Accessibility Service, WindowManager (System Overlay)

## Building and Running

- **Build APK:** `./gradlew assembleDebug`
- **Install on Device:** `./gradlew installDebug`
- **Run Tests:** `./gradlew test` (Unit tests) or `./gradlew connectedAndroidTest` (Instrumentation tests)
- **Release Build:** Requires `keystore.properties` to be configured (see `app/build.gradle.kts`).

## Development Conventions

- **Overlay UI:** The overlay is built using programmatic view construction in `EdgePanelService` for performance and precise control over the `WindowManager`.
- **Data Encryption:** Never store sensitive information in plain text. Always use the `ClipRepository` which handles encryption via SQLCipher.
- **Clipboard Access:** On modern Android versions, clipboard access from the background is restricted. Always use `ClipboardAccessibilityService` in conjunction with the "Focus Window" strategy in `EdgePanelService`.
- **Memory Management:** Image clips are capped at 50 items by default. The `ClipRepository` handles the eviction of old files and database entries.

## Project Structure

```text
app/src/main/java/dev/bmg/edgepanel/
├── MainActivity.kt           # Main settings UI (Compose)
├── clipboard/
│   ├── ClipboardAccessibilityService.kt  # Background monitor
│   └── ClipboardProxyActivity.kt         # Helper activity
├── data/
│   ├── ClipDao.kt            # Room DAO
│   ├── ClipDatabase.kt       # Encrypted Room DB setup
│   ├── ClipEntity.kt         # Data model
│   └── ClipRepository.kt     # Data orchestration
├── service/
│   └── EdgePanelService.kt   # Overlay window manager
└── view/
    └── GestureScrollView.kt  # Custom view for panel gestures
```
