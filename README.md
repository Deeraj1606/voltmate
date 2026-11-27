# VoltMate ⚡

[![GitHub license](https://img.shields.io/github/license/Deeraj1606/voltmate)](https://github.com/Deeraj1606/voltmate/blob/main/LICENSE)
[![GitHub stars](https://img.shields.io/github/stars/Deeraj1606/voltmate)](https://github.com/Deeraj1606/voltmate/stargazers)
[![GitHub issues](https://img.shields.io/github/issues/Deeraj1606/voltmate)](https://github.com/Deeraj1606/voltmate/issues)

## Overview
VoltMate is an **open‑source Android application** that lets EV owners **share** idle home or shop chargers with other Tata EV drivers. It demonstrates a complete end‑to‑end flow:

- **Driver** discovers chargers on an interactive map, books a slot, taps NFC, charges, and pays **in‑app**.
- **Owner** registers a charger, sets pricing & availability, earns from each session, and can withdraw simulated funds.
- All transactions are recorded in a **wallet** that works offline via `SharedPreferences` and syncs to Cloud Firestore when online.

The project showcases modern Android best‑practices: Jetpack Compose, Material 3, Kotlin coroutines, manual DI, Firestore batch transactions, and graceful offline handling.

---

## Key Features
- **Real‑time charger map** (OSMDroid) with distance filtering.
- **Dual‑role dashboards** (Driver & Owner) that load instantly using demo data when Firestore is unavailable.
- **In‑app wallet** with top‑up, payment, earnings, and simulated withdrawal flows.
- **Secure authentication** – Google Sign‑In via Credential Manager + Email/Password fallback.
- **Robust UI** – No white‑on‑light text, green accent palette, consistent Material 3 theming.
- **Offline‑first** – Demo data displayed instantly; Firestore calls wrapped in `try/catch + withTimeoutOrNull(4000)`.
- **Clear architecture** – Single‑module app, manual DI container, MVVM with `StateFlow`.

---

## Tech Stack
| Layer | Technology |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Build | Gradle Kotlin DSL, Version Catalog |
| Auth | Firebase Auth (Google Sign‑In via Credential Manager, Email/Password) |
| Database | Cloud Firestore (real‑time listeners) |
| Location | Google Play Services Location + OSMDroid |
| Charger HW | Simulated `ChargerGateway` (NFC auth, CC‑CV curve, telemetry) |
| Min SDK | 26 • Target/Compile SDK 35 |

---

## Architecture Snapshot
```
com.company.voltmate
├── data
│   ├── gateway/       # Simulated charger hardware
│   ├── model/         # Kotlin data classes
│   └── repository/    # Auth, Charger, Booking, Session, Wallet
├── di/                # Manual DI container (AppContainer)
├── ui
│   ├── auth/          # Splash, Login (Google + Email)
│   ├── onboarding/    # Role pick, driver & owner onboarding
│   ├── driver/        # Dashboard, ChargerDetail, Booking, ChargingSession
│   ├── owner/         # Dashboard, Chargers, Earnings, Profile
│   └── theme/         # Colors, Typography, Logo, Theme
├── MainActivity.kt    # NavHost with all routes
└── VoltMateApp.kt     # Application class
```

---

## Getting Started
1. **Clone the repo**
   ```bash
   git clone https://github.com/Deeraj1606/voltmate.git
   cd voltmate
   ```
2. **Open in Android Studio** (Arctic Fox or newer). The project uses Gradle 8.10.2 and JDK 17 (bundled JBR works).
3. **Add Firebase config** – place your `google-services.json` in `app/` and enable Auth + Firestore.
4. **Build & run**
   ```bash
   ./gradlew :app:assembleDebug
   ```
   The app launches with a pre‑seeded demo wallet (`₹ 500` welcome credit).

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
This project is licensed under the **MIT License** – see the [LICENSE](LICENSE) file for details.

---

## Contact & Credits
- **Author**: Deeraj Patel ([@Deeraj1606](https://github.com/Deeraj1606))
- **Feedback**: Open an issue or start a discussion on the repository.
- **Inspired by**: Tata Motors EV ecosystem and the open‑source Android community.
