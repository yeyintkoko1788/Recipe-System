# API reference

Base URL: `/api/v1` · All responses use the envelope:

```json
{ "success": true,  "data": { … } }
{ "success": false, "error": { "code": "VALIDATION_ERROR", "message": "…", "details": { "field": "msg" } } }
```

Paged responses: `{ "items": [...], "page": 0, "size": 20, "totalItems": 42, "totalPages": 3 }`

Common query parameters for listings: `page` (0-based), `size` (≤100), `sort` (whitelisted per endpoint), `order` (`asc`|`desc`).

Error codes: `VALIDATION_ERROR` 400 · `UNAUTHORIZED` 401 · `FORBIDDEN` 403 · `NOT_FOUND` 404 · `CONFLICT` 409 · `INTERNAL_ERROR` 500.

Authentication: `Authorization: Bearer <accessToken>` on everything except register/login/refresh. Auth endpoints are rate-limited (20 req/min/IP).

## Auth

| Method | Path | Body | Notes |
|---|---|---|---|
| POST | `/auth/register` | `{name,email,password}` | 201, returns user + tokens |
| POST | `/auth/login` | `{email,password}` | Same error for wrong email/password |
| POST | `/auth/refresh` | `{refreshToken}` | Rotates the refresh token |
| POST | `/auth/logout` | `{refreshToken}` | Revokes the token |
| GET | `/auth/me` | — | Current user |

`AuthResponse`: `{ user, accessToken, refreshToken, expiresInSeconds }`

## Recipes

| Method | Path | Notes |
|---|---|---|
| GET | `/recipes` | `query, difficulty, maxTime, mine, sort(createdAt·title·cookingTime·popularity)` |
| GET | `/recipes/{id}` | Full recipe; increments view count |
| POST | `/recipes` | Create (see body below) |
| PUT | `/recipes/{id}` | Author or admin |
| DELETE | `/recipes/{id}` | Soft delete; author or admin |

Recipe body:

```json
{
  "title": "Fried rice", "description": "…",
  "cookingTimeMinutes": 20, "servings": 2, "difficulty": "EASY",
  "coverImageUrl": null,
  "instructions": ["Cook rice", "Fry everything"],
  "ingredients": [ { "ingredientId": "<uuid>", "quantity": 200, "unit": "g", "note": "day-old" } ]
}
```

## Ingredients

| Method | Path | Notes |
|---|---|---|
| GET | `/ingredients` | `search, category, page…`; admins may pass `status` |
| GET | `/ingredients/{id}` | |
| POST | `/ingredients` | Users → `PENDING`, admins → `APPROVED`. Names unique **case-insensitively** (Salt = salt = SALT → 409) |
| PUT | `/ingredients/{id}` | Admin |
| DELETE | `/ingredients/{id}` | Admin; 409 if referenced by recipes |

## Pantry

| Method | Path | Notes |
|---|---|---|
| GET | `/pantry` | Current user's items (ingredient embedded) |
| POST | `/pantry` | `{ingredientId, quantity, unit?}` upsert |
| PATCH | `/pantry/{id}` | `{quantity?, unit?, isOutOfStock?}` (quantity 0 ⇒ out of stock) |
| POST | `/pantry/{id}/out-of-stock` | Mark empty |
| POST | `/pantry/{id}/restock` | `{quantity}` |
| DELETE | `/pantry/{id}` | |

## Shopping list

| Method | Path | Notes |
|---|---|---|
| GET | `/shopping-list` | `includePurchased=true|false` |
| POST | `/shopping-list` | `{ingredientId?}` or `{name}` (free text) |
| POST | `/shopping-list/generate` | Adds all out-of-stock pantry items not already listed |
| POST | `/shopping-list/{id}/purchase` | `{restockPantry?:true, quantity?}` — restores pantry stock |
| DELETE | `/shopping-list/{id}` | |
| DELETE | `/shopping-list/purchased` | Clear purchased items |

## Explore

| Method | Path | Notes |
|---|---|---|
| GET | `/explore/popular` | By view count |
| GET | `/explore/new` | By creation date |
| GET | `/explore/recommended` | Pantry-overlap ranking, falls back to popular |

## Admin (role ADMIN)

| Method | Path | Notes |
|---|---|---|
| GET | `/admin/users` | `search, page…` |
| PATCH | `/admin/users/{id}/role` | `{role: "ADMIN"|"USER"}`; cannot change own role |
| DELETE | `/admin/users/{id}` | Cannot delete self |
| PATCH | `/admin/ingredients/{id}/status` | `{status: APPROVED|PENDING|REJECTED}` |
| GET | `/admin/stats` | Users/recipes/ingredients/pending counts |

Recipe moderation reuses `DELETE /recipes/{id}` (admins bypass ownership).

## Health

`GET /healthz` → `{"success":true,"data":{"message":"OK"}}`
