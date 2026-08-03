# Architecture

## Overview

```
┌────────────────────────── Mobile (Android + iOS) ─────────────────────────┐
│  composeApp (presentation)                                                │
│    Screens (Compose Multiplatform, Material 3)                            │
│    ViewModels (contract interfaces + StateFlow UI state)                  │
│              │ depends on abstractions only                               │
│  shared (domain + data)                                                   │
│    domain/  models · repository interfaces · AppResult/AppError           │
│    data/    Ktor client · DTO mappers · SQLDelight cache · TokenStorage   │
│    di/      Koin modules · expect/actual platform bindings                │
└──────────────────────────────┬────────────────────────────────────────────┘
                               │ HTTPS (JSON envelope)
┌──────────────────────────────▼────────────────────────────────────────────┐
│  backend (Ktor)                                                           │
│    routes/   REST endpoints, auth, param parsing                          │
│    service/  business rules, validation, ownership checks                 │
│    repository/  interfaces + Exposed implementations                      │
│    db/       Exposed tables (schema owned by Flyway migrations)           │
│    PostgreSQL                                                             │
└───────────────────────────────────────────────────────────────────────────┘
```

## Clean Architecture layers (mobile)

- **Presentation** (`composeApp`): every feature ships a `Contract` interface (e.g. `LoginContract`) implemented by its ViewModel — a pattern inherited from ComposeBase that keeps screens testable with fakes. UI state is a single sealed interface or data class per screen.
- **Domain** (`shared/domain`): pure Kotlin models and repository interfaces. No Ktor, no SQLDelight, no platform types.
- **Data** (`shared/data`): repository implementations mapping DTOs ↔ domain models, a single `safeApiCall` choke point that converts HTTP/transport failures into typed `AppError`s, SQLDelight-backed offline cache for explore feeds, and token persistence.

Adding a feature = new route + service on the backend, new repository interface + impl in shared, new contract/ViewModel/screen in composeApp. No existing file needs structural change.

## Error model

`AppResult<T> = Success | Failure(AppError)` replaces ComposeBase's
`FlowReturnResult` with a closed, typed set of cases:
`Network`, `Timeout`, `SessionExpired`, `Validation(fields)`, `Api(code,message)`, `Unknown`.
The backend mirrors this with an `ApiResponse` envelope and stable error codes, so validation errors surface field-by-field in the UI.

## Auth flow

1. Login/register returns an access token (15 min JWT) + refresh token (opaque, 30 days, stored **hashed** server-side).
2. The Ktor client's Bearer plugin transparently refreshes on 401; refresh **rotates** the token server-side.
3. If refresh fails, `TokenStorage` is cleared and `AuthRepository.onSessionExpired()` flips the global `AuthState` — the root composable switches back to the login graph (the KMP equivalent of ComposeBase's `AuthEventBus`).

## Recommendations

`ExploreService.recommended` ranks recipes by overlap with the user's in-stock pantry ingredients (SQL join + count), falling back to popular recipes. The API shape is feed-agnostic so the ranking can later move to a dedicated engine without client changes.

## Future-proofing notes

- Favorites/ratings/reviews: new tables + routes; `RecipeSummary` already carries `viewCount`, adding `favoriteCount` is additive.
- Offline sync: SQLDelight cache already isolates reads; a write-queue table can be added beside it.
- Cloud image storage: `coverImageUrl`/`imageUrl` are plain URLs; a future upload endpoint only adds a producer.
- Multi-language: all user-facing strings sit in composables ready to be lifted into Compose resources.
- Backend contracts: DTOs are intentionally duplicated between `backend` and `shared` (independent deployability); if drift becomes a problem, extract a `contracts` KMP module both depend on.
