# RecipeApp — Kotlin Multiplatform Recipe Management

A production-oriented recipe management system built on the ComposeBase project patterns, ported to Kotlin Multiplatform:

- **composeApp/** — Compose Multiplatform UI (Android + iOS), MVVM with contract-based ViewModels
- **shared/** — KMP domain + data layers (Ktor Client, SQLDelight cache, Koin DI, token storage)
- **backend/** — Ktor server (PostgreSQL, Exposed, Flyway, JWT auth, Docker)
- **iosApp/** — SwiftUI host project for the iOS app
- **docs/** — architecture, API, database and setup documentation

## Features

Recipes (CRUD, search, filtering, sorting, pagination), global ingredient catalog with case-insensitive uniqueness and admin moderation, per-user pantry, shopping list with auto-generation from out-of-stock pantry items and pantry restock on purchase, explore feeds (popular / new / recommended by pantry overlap), JWT auth with refresh-token rotation, role-based admin APIs, light/dark theme, offline cache for explore feeds.

## Quick start

### Backend

```bash
cp .env.example .env          # then edit JWT_SECRET etc.
docker compose up --build     # Postgres + API on http://localhost:8080
```

A bootstrap admin account is created on first start (`ADMIN_EMAIL` / `ADMIN_PASSWORD`, defaults `admin@recipeapp.dev` / `Admin123!`). Verify with:

```bash
curl http://localhost:8080/healthz
```

### Android

Open the repository in Android Studio and run the `composeApp` configuration (choose the **staging** flavor for local development — it points at `http://10.0.2.2:8080`, the host machine as seen from the emulator).

### iOS

```bash
open iosApp/iosApp.xcodeproj
```

Set your team in `iosApp/Configuration/Config.xcconfig`, then run on a simulator (the app targets `http://localhost:8080` in debug).

### Tests

```bash
./gradlew :backend:test              # backend service tests
./gradlew :shared:testDebugUnitTest  # shared layer tests (Android target)
./gradlew :composeApp:testDebugUnitTest
```

## Key design decisions

| Area | Choice | Why |
|---|---|---|
| Networking | Ktor Client | Retrofit is JVM/Android-only; Ktor is the standard KMP client |
| Serialization | kotlinx.serialization | Gson is reflection-based and JVM-only |
| Local storage | SQLDelight + multiplatform-settings | Room/SharedPreferences are Android-only; SQLDelight generates typed APIs for both platforms |
| DI (mobile) | Koin | Hilt is Android-only; Koin is the most mature KMP DI |
| DI (backend) | Manual composition root | Explicit, compile-time safe, no reflection; trivial to fake in tests |
| Navigation | JetBrains navigation-compose | Navigation3 (used in ComposeBase) is Android-only today |
| ORM | Exposed + Flyway | Flyway owns the schema (versioned SQL migrations); Exposed only reads/writes it |
| Auth | Short-lived JWT + rotating opaque refresh tokens (stored hashed) | A leaked DB or stolen refresh token has minimal blast radius |
| Errors | `ApiResponse` envelope + typed `AppError` | One error contract from Postgres to Compose |

More detail in [docs/architecture.md](docs/architecture.md).

## Documentation

- [Architecture](docs/architecture.md)
- [Setup guide](docs/setup.md)
- [API reference](docs/api.md)
- [Database schema](docs/database.md)
