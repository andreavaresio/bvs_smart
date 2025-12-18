# Changelog

## [1.0.0] - 2025-12-18
### Added
- **Native Android Migration**: Complete rewrite of the app using Kotlin and Jetpack Compose.
- **UI/UX**:
  - Modern "Dark Mode" interface with Yellow/Black branding.
  - Reusable components (Buttons, Badges) for consistent design.
  - Loading Overlay with "dimming" effect and status text.
- **Camera Features**:
  - **In-app Camera**: Integrated CameraX with lifecycle handling and custom overlay.
  - **Device Camera**: Integration with system camera app via Intents.
  - **Gallery**: Photo selection from device storage using MediaStore and Coil.
- **Network**:
  - Robust Retrofit implementation for API communication.
  - Device Capabilities synchronization on app startup.
  - Photo upload with confirmation dialog ("Vuoi inviare la foto al server?").
  - Fixed handling of non-JSON server responses.
- **Developer Experience**:
  - Educational comments added to key files for learning Jetpack Compose.
  - Gradle optimizations (Kotlin DSL, 4GB heap, Parallel build).
