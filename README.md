# VoltMate ⚡

Peer-to-peer EV charger sharing for Tata EV owners in India. Owners register idle home/shop chargers and earn. Drivers find chargers on a map, book a slot, tap NFC, charge, pay in-app, and only then the connector unlocks.

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Build | Gradle Kotlin DSL, Version Catalog |
| Auth | Firebase Auth (Google Sign-In via Credential Manager, Email/Password) |
| Database | Cloud Firestore (real-time listeners) |
| Location | Google Play Services Location + OSMDroid |
| Charger HW | Simulated ChargerGateway (NFC auth, CC-CV charging curve, telemetry) |
| Min SDK | 26 · Target/Compile SDK 35 |

## Architecture

Single-module `app`, manual DI via `AppContainer`, MVVM with `StateFlow`.

```
com.company.voltmate
├── data
│   ├── gateway/       # ChargerGateway (simulated HW)
│   ├── model/         # Kotlin data classes
│   └── repository/    # Auth, Charger, Booking, Session repos
├── di/                # AppContainer (manual DI)
├── ui
│   ├── auth/          # Splash, Login (Google + Email)
│   ├── onboarding/    # Role pick, Driver & Owner onboarding
│   ├── driver/        # Dashboard, ChargerDetail, Booking, ChargingSession
│   ├── owner/         # Dashboard, Chargers, Earnings, Profile
│   └── theme/         # Colors, Typography, Logo, Theme
├── MainActivity.kt    # NavHost with all routes
└── VoltMateApp.kt     # Application class
```

## Screens

### Driver Flow
1. **Splash** → auto-route based on auth + onboarding state
2. **Login** → Google Sign-In or Email/Password
3. **Role Pick** → Driver or Owner
4. **Driver Onboarding** → Select Tata car model, battery, connector, NFC tag
5. **Driver Dashboard** → 4 tabs: Map, Chargers, Bookings, Profile
6. **Charger Detail** → Hero card, info grid, amenities, availability days
7. **Booking** → Time slot picker with transactional Firestore booking
8. **Charging Session** → 4-step flow: NFC Tap → Live Charging → Payment → Done
   - Live telemetry: SoC ring, power, voltage, current, temp, energy, cost
   - CC-CV charging curve simulation with realistic taper above 80%
   - Demo speed controls (1x, 10x, 60x)
   - Connector stays locked until payment confirmed

### Owner Flow
1. **Owner Onboarding** → Register charger with type, power, pricing, schedule, amenities
2. **Owner Dashboard** → 4 tabs: Home (stats), Chargers (toggle on/off), Earnings, Profile
3. **Earnings** → Total revenue, session count, kWh sold, transaction history

## Build

```bash
# Requires JDK 17 (Android Studio JBR works)
./gradlew :app:assembleDebug
```

## Firebase Setup

Place your `google-services.json` in `app/`. The app uses:
- Firebase Authentication
- Cloud Firestore

See `firestore.rules` for security rules.

## Demo Data

The app ships with 8 demo chargers around Chennai and 7 demo owner sessions for a realistic first-run experience. All demo data merges seamlessly with live Firestore data.

## License

This project is for educational/demo purposes. Not affiliated with Tata Motors.
