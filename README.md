[![CI](https://github.com/Deeraj1606/voltmate/actions/workflows/ci.yml/badge.svg)](https://github.com/Deeraj1606/voltmate/actions) [![Android CI](https://github.com/Deeraj1606/voltmate/actions/workflows/android.yml/badge.svg)](https://github.com/Deeraj1606/voltmate/actions) [![License](https://img.shields.io/github/license/Deeraj1606/=flat)](https://github.com/Deeraj1606/voltmate/blob/main/LICENSE) [![Stars](https://img.shields.io/github/stars/Deeraj1606/=flat)](https://github.com/Deeraj1606/voltmate/stargazers)

# VoltMate âš¡

[![GitHub license](https://img.shields.io/github/license/Deeraj1606/voltmate)](https://github.com/Deeraj1606/voltmate/blob/main/LICENSE)
[![GitHub stars](https://img.shields.io/github/stars/Deeraj1606/voltmate)](https://github.com/Deeraj1606/voltmate/stargazers)
[![GitHub issues](https://img.shields.io/github/issues/Deeraj1606/voltmate)](https://github.com/Deeraj1606/voltmate/issues)

## Overview
VoltMate is an **openâ€‘source Android application** that lets EV owners **share** idle home or shop chargers with other Tata EV drivers. It demonstrates a complete endâ€‘toâ€‘end flow:

- **Driver** discovers chargers on an interactive map, books a slot, taps NFC, charges, and pays **inâ€‘app**.
- **Owner** registers a charger, sets pricing & availability, earns from each session, and can withdraw simulated funds.
- All transactions are recorded in a **wallet** that works offline via `SharedPreferences` and syncs to Cloud Firestore when online.

The project showcases modern Android bestâ€‘practices: Jetpack Compose, Materialâ€¯3, Kotlin coroutines, manual DI, Firestore batch transactions, and graceful offline handling.

---

## Key Features
- **Realâ€‘time charger map** (OSMDroid) with distance filtering.
- **Dualâ€‘role dashboards** (Driver & Owner) that load instantly using demo data when Firestore is unavailable.
- **Inâ€‘app wallet** with topâ€‘up, payment, earnings, and simulated withdrawal flows.
- **Secure authentication** â€“ Google Signâ€‘In via Credential Manager + Email/Password fallback.
- **Robust UI** â€“ No whiteâ€‘onâ€‘light text, green accent palette, consistent Materialâ€¯3 theming.
- **Offlineâ€‘first** â€“ Demo data displayed instantly; Firestore calls wrapped in `try/catch + withTimeoutOrNull(4000)`.
- **Clear architecture** â€“ Singleâ€‘module app, manual DI container, MVVM with `StateFlow`.

---

## Tech Stack
| Layer | Technology |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose + Materialâ€¯3 |
| Build | Gradle Kotlin DSL, Version Catalog |
| Auth | Firebase Auth (Google Signâ€‘In via Credential Manager, Email/Password) |
| Database | Cloud Firestore (realâ€‘time listeners) |
| Location | Google Play Services Location + OSMDroid |
| Charger HW | Simulated `ChargerGateway` (NFC auth, CCâ€‘CV curve, telemetry) |
| Min SDK | 26 â€¢ Target/Compile SDK 35 |

---

## Architecture Snapshot
```
com.company.voltmate
â”œâ”€â”€ data
â”‚   â”œâ”€â”€ gateway/       # Simulated charger hardware
â”‚   â”œâ”€â”€ model/         # Kotlin data classes
â”‚   â””â”€â”€ repository/    # Auth, Charger, Booking, Session, Wallet
â”œâ”€â”€ di/                # Manual DI container (AppContainer)
â”œâ”€â”€ ui
â”‚   â”œâ”€â”€ auth/          # Splash, Login (Google + Email)
â”‚   â”œâ”€â”€ onboarding/    # Role pick, driver & owner onboarding
â”‚   â”œâ”€â”€ driver/        # Dashboard, ChargerDetail, Booking, ChargingSession
â”‚   â”œâ”€â”€ owner/         # Dashboard, Chargers, Earnings, Profile
â”‚   â””â”€â”€ theme/         # Colors, Typography, Logo, Theme
â”œâ”€â”€ MainActivity.kt    # NavHost with all routes
â””â”€â”€ VoltMateApp.kt     # Application class
```

---

## Getting Started
1. **Clone the repo**
   ```bash
   git clone https://github.com/Deeraj1606/voltmate.git
   cd voltmate
   ```
2. **Open in Android Studio** (Arctic Foxâ€¯or newer). The project uses Gradleâ€¯8.10.2 and JDKâ€¯17 (bundled JBR works).
3. **Add Firebase config** â€“ place your `google-services.json` in `app/` and enable Auth + Firestore.
4. **Build & run**
   ```bash
   ./gradlew :app:assembleDebug
   ```
   The app launches with a preâ€‘seeded demo wallet (`â‚¹â€¯500` welcome credit).

---

## Contributing
Contributions are welcome! Please follow these steps:
1. Fork the repository.
2. Create a feature branch (`git checkout -b feature/awesome-feature`).
3. Keep the code style consistent (Kotlin Formatting, Compose lint).
4. Write unit/instrumented tests for new logic.
5. Open a Pull Request with a clear description of the changes.

See the `CONTRIBUTING.md` file for detailed guidelines (code style, commit messages, testing).

---

## License
This project is licensed under the **MIT License** â€“ see the [LICENSE](LICENSE) file for details.

---

## Contact & Credits
- **Author**: Deeraj ([@Deeraj1606](https://github.com/Deeraj1606))
- **Feedback**: Open an issue or start a discussion on the repository.
- **Inspired by**: Tata Motors EV ecosystem and the openâ€‘source Android community.


