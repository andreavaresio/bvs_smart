# TASKS

## Backlog / Future Improvements

- [ ] Implement Server API Login (Username/Password authentication)
- [ ] Fetch dynamic Beehive Registry (Angrafiche Arnie) from server API
- [ ] Update Photo Upload to use dynamic session/auth tokens
- [ ] Add unit tests for network and data layers
- [ ] Perform UI testing for the main flows
- [ ] Implement offline mode / retry logic for uploads

## Story: Strumenti di Sviluppo
**Ottimizzazione del workflow per Jetpack Compose**

1. **Usa "Live Edit"** (Il più importante per Compose)
   - *Cosa fa*: Quando modifichi una stringa, un colore o una dimensione (e.g., padding), la UI sul telefono si aggiorna in tempo reale senza dover ricompilare l'app.
   - *Come si attiva*: In Android Studio, vai nelle impostazioni > **Editor** > **Live Edit** > Spunta "**Push edits automatically**".

2. **Usa "Apply Changes" invece di "Run"**
   In alto a destra su Android Studio ci sono due pulsanti:
   - ▶️ **Run App (Shift+F10)**: Ricompila tutto e riavvia l'app (Lento).
   - ⚡ **Apply Changes (Ctrl+F10)**: Tenta di iniettare solo il codice modificato senza riavviare l'Activity (Molto più veloce). Usa sempre questo per piccole modifiche logiche.