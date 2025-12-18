# TASKS

## Current Sprint: Kotlin Native Migration
**Story: Kotlin native android migration**

- [x] Initial project scaffolding with Gradle (Kotlin DSL)
- [x] Define core data models (Beehive, DeviceCapabilities)
- [x] Implement Retrofit network layer for API communication
- [x] Create reusable UI components (PrimaryButton, SecondaryButton, BeehiveBadge)
- [x] Implement Home Screen with settings dialog
- [x] Implement Internal Camera screen using CameraX and AndroidView
- [x] Implement Gallery screen with MediaStore integration and Coil
- [x] Add educational comments for Jetpack Compose beginners
- [x] Implement Device Capabilities sync logic
- [ ] Add unit tests for network and data layers
- [ ] Test In-app camera flow (with confirmation dialog)
- [ ] Test Device-camera flow
- [ ] Test Gallery selection and upload flow
- [ ] Perform UI testing for the main flows

## Story: Strumenti di Sviluppo
**Ottimizzazione del workflow per Jetpack Compose**

1. **Usa "Live Edit"** (Il più importante per Compose)
   - *Cosa fa*: Quando modifichi una stringa, un colore o una dimensione (e.g., padding), la UI sul telefono si aggiorna in tempo reale senza dover ricompilare l'app.
   - *Come si attiva*: In Android Studio, vai nelle impostazioni > **Editor** > **Live Edit** > Spunta "**Push edits automatically**".

2. **Usa "Apply Changes" invece di "Run"**
   In alto a destra su Android Studio ci sono due pulsanti:
   - ▶️ **Run App (Shift+F10)**: Ricompila tutto e riavvia l'app (Lento).
   - ⚡ **Apply Changes (Ctrl+F10)**: Tenta di iniettare solo il codice modificato senza riavviare l'Activity (Molto più veloce). Usa sempre questo per piccole modifiche logiche.
