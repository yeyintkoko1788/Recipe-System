# Setup guide

## Prerequisites

- JDK 17
- Android Studio (latest stable) with the Android SDK (API 36)
- Xcode 15+ (for iOS)
- Docker + Docker Compose (for the backend)

## Backend

### With Docker (recommended)

```bash
cp .env.example .env   # edit secrets — especially JWT_SECRET
docker compose up --build
```

This starts PostgreSQL 16 and the API on port 8080. Flyway migrations run automatically at startup; the seed migration inserts a starter ingredient catalog and the app creates the bootstrap admin account.

### Without Docker

Run PostgreSQL yourself, then:

```bash
export DATABASE_URL=jdbc:postgresql://localhost:5432/recipeapp
export DATABASE_USER=recipeapp DATABASE_PASSWORD=recipeapp
export JWT_SECRET=$(openssl rand -base64 48)
./gradlew :backend:run
```

### Environment variables

| Variable | Default | Purpose |
|---|---|---|
| `PORT` | `8080` | HTTP port |
| `DATABASE_URL` | `jdbc:postgresql://localhost:5432/recipeapp` | JDBC URL |
| `DATABASE_USER` / `DATABASE_PASSWORD` | `recipeapp` | DB credentials |
| `DATABASE_POOL_SIZE` | `10` | Hikari pool size |
| `JWT_SECRET` | dev value — **must** override | HMAC signing key |
| `JWT_ACCESS_TTL_MINUTES` | `15` | Access-token lifetime |
| `JWT_REFRESH_TTL_DAYS` | `30` | Refresh-token lifetime |
| `ADMIN_EMAIL` / `ADMIN_PASSWORD` | `admin@recipeapp.dev` / `Admin123!` | Bootstrap admin |
| `CORS_ALLOWED_HOSTS` | `*` | Comma-separated origins |

## Android app

1. Open the repo in Android Studio.
2. Pick the **stagingDebug** build variant — it targets `http://10.0.2.2:8080` (host machine from the emulator).
3. Run `composeApp`.

Production API URLs live in `composeApp/build.gradle.kts` (product flavors).

## iOS app

1. `open iosApp/iosApp.xcodeproj`
2. Fill in `TEAM_ID` in `iosApp/Configuration/Config.xcconfig`.
3. Run on a simulator. Debug builds target `http://localhost:8080` (see `MainViewController.kt`).

The Xcode build phase compiles the Kotlin framework via
`./gradlew :composeApp:embedAndSignAppleFrameworkForXcode`.

## Version pins

All dependency versions are centralized in `gradle/libs.versions.toml`. If a
version fails to resolve in your environment (e.g. a newer navigation-compose
beta), adjust it there — no build script edits required.
