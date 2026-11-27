[![CI](https://github.com/Deeraj1606/voltmate/actions/workflows/ci.yml/badge.svg)](https://github.com/Deeraj1606/voltmate/actions) [![Android CI](https://github.com/Deeraj1606/voltmate/actions/workflows/android.yml/badge.svg)](https://github.com/Deeraj1606/voltmate/actions) [![License](https://img.shields.io/github/license/Deeraj1606/=flat)](https://github.com/Deeraj1606/voltmate/blob/main/LICENSE) [![Stars](https://img.shields.io/github/stars/Deeraj1606/=flat)](https://github.com/Deeraj1606/voltmate/stargazers)\n\n# VoltMate âš¡

![Banner](./screenshot1.png)

## Overview
VoltMate is an **openâ€‘source Android application** that lets EV owners **share** idle home or shop chargers with other Tata EV drivers. It demonstrates a complete endâ€‘toâ€‘end flow:
- **Driver** discovers chargers on an interactive map, books a slot, taps NFC, charges, and pays **inâ€‘app**.
- **Owner** registers a charger, sets pricing & availability, earns from each session, and can withdraw simulated funds.
- All transactions are recorded in a **wallet** that works offline via `SharedPreferences` and syncs to Cloud Firestore when online.

### Key Features
- Realâ€‘time charger map (OSMDroid) with distance filtering.
- Dualâ€‘role dashboards (Driver & Owner) with demo data fallback.
- Inâ€‘app wallet with topâ€‘up, payment, earnings, and simulated withdrawal flows.
- Secure authentication â€“ Google Signâ€‘In via Credential Manager + Email/Password fallback.
- Offlineâ€‘first architecture with graceful error handling.
- Clean MVVM architecture using manual DI and `StateFlow`.

---

## Architecture
```mermaid
graph TD
    UI[UI (Compose + Materialâ€¯3)] --> DI[DI Container]
    DI --> ViewModel[ViewModel (StateFlow)]
    ViewModel --> Repo[Repository Layer]
    Repo --> Firestore[Firestore (remote)]
    Repo --> Local[SharedPreferences (local)]
```

---

## Tech Stack
| Layer | Technology |
|------|------------|
| Language | Kotlin |
| UI | Jetpack Compose + Materialâ€¯3 |
| Build | Gradle Kotlin DSL, Version Catalog |
| Auth | Firebase Auth (Google Signâ€‘In, Email/Password) |
| Database | Cloud Firestore (realâ€‘time) |
| Location | Google Play Services Location + OSMDroid |
| Min SDK | 26 â€¢ Target/Compile SDK 35 |

---

## Getting Started
```bash
git clone https://github.com/Deeraj1606/voltmate.git
cd voltmate
# Open in Android Studio (Arctic Fox+)
# Add your firebase config (google-services.json) to app/
./gradlew :app:assembleDebug
```

---

## Contributing
Please see `CONTRIBUTING.md` for guidelines on forkâ€‘PR workflow, code style, and testing.

---

## License
MIT License â€“ see the `LICENSE` file.

