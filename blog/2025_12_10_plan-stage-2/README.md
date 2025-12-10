# Roadmap: Piano di Sviluppo - Stage 2 (Migrazione Expo & Core Features)

**Data:** 10 Dicembre 2025  
**Obiettivo:** Rifondare l'applicazione utilizzando **Expo** per massimizzare la velocità di sviluppo e la manutenibilità, evitando l'ejection precoce.

---

## 📋 Riepilogo Esecutivo (Executive Summary)

| # | Attività | Stima (Giorni) | Priorità |
|:-:|:---|:---:|:---:|
| 1 | **Analisi e Progettazione** | 0.5 | 🔴 Alta |
| 2 | **Migrazione a Expo** | 1.0 | 🔴 Alta |
| 3 | **Auth, API & Storage** | 3.0 | 🔴 Alta |
| 4 | **Onboarding & Certificazione** | 2.0 | 🔴 Alta |
| 5 | **Setup Team** | 0.5 | 🔴 Alta |
| | **TOTALE STIMATO** | **~7.0 gg** | |

---

## 🚀 Priorità Immediata (Immediate Priority)

**Stima Totale Fase:** ~7 Giorni

### 1. Analisi e Progettazione
*   **Stima:** 0.5 Giorni
*   **Obiettivo:** Definizione chiara dei requisiti, della roadmap e dell'architettura del sistema.
*   **Task:**
    *   [x] Stesura e revisione del piano di sviluppo (Roadmap).
    *   [ ] Definizione dell'architettura dati e flussi utente principali.

### 2. Inizializzazione e Migrazione a Expo
*   **Stima:** 1 Giorno
*   **Obiettivo:** Avere una base solida in Expo che sostituisca l'attuale app nativa/ejected.
*   **Task:**
    *   [ ] Setup nuovo progetto Expo (Managed Workflow).
    *   [ ] Configurazione navigazione e struttura base.
    *   [ ] Eliminazione codice nativo custom (es. `ExternalCameraModule`) a favore di librerie Expo gestite (es. `expo-image-picker`).
    *   [ ] Verifica build e run su Expo Go.

### 3. Autenticazione, API e Secure Storage
*   **Stima:** 3 Giorni
*   **Obiettivo:** Gestire l'accesso utente e la persistenza sicura dei dati operativi.
*   **Task:**
    *   [ ] Implementazione UI di Login.
    *   [ ] Integrazione API Server (Login endpoint).
    *   [ ] Implementazione **Secure Storage** (es. `expo-secure-store`) per salvare credenziali (User/Pass) e Token.
    *   [ ] Logica di fetch e storage locale per: Profili, Apiari, Arnie.
    *   [ ] Gestione del ciclo di vita della sessione (persistenza login).

### 4. Onboarding Dispositivo e Certificazione
*   **Stima:** 2 Giorni
*   **Obiettivo:** Gestire la sicurezza e l'autorizzazione dei nuovi dispositivi che installano l'app.
*   **Task:**
    *   [ ] Rilevamento "Nuova Installazione/Nuovo Device".
    *   [ ] Flusso di invio foto di certificazione al server.
    *   [ ] Gestione UI "Stato In Attesa": blocco funzionalità finché il server (admin manuale) non valida le foto.
    *   [ ] Polling o check all'avvio per verificare avvenuta certificazione.

### 5. Setup Ambiente di Sviluppo per il Team
*   **Stima:** 0.5 Giorni
*   **Obiettivo:** Assicurare che tutti i membri del team possano contribuire al progetto Expo.
*   **Task:**
    *   [ ] Installazione Expo CLI e dipendenze necessarie sulle macchine dei dev.
    *   [ ] Condivisione configurazione e best practice di sviluppo.
    *   [ ] Documentazione rapida per l'avvio del progetto.


---

## 🗄️ Backlog (Future Steps)

Attività da analizzare e stimare dopo il completamento della fase "Priorità Immediata".

1.  **Sincronizzazione Offline:** Gestione avanzata dei dati quando manca la connettività nel campo (apiario).
2.  **Upload Foto Arnie:** Logica di invio foto operative (non solo certificazione) al server.
3.  **Miglioramento UX/UI:** Refinement grafico interfaccia utente.
4.  **Notifiche Push:** Avvisi per approvazione device o alert apiario.
