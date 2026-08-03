# Database schema

PostgreSQL 16 · migrations in `backend/src/main/resources/db/migration` (Flyway, run automatically at startup). Conventions: UUID PKs (`gen_random_uuid()`), `TIMESTAMPTZ` in UTC, case-insensitive uniqueness via functional unique indexes on `LOWER(col)`.

```
users ──< refresh_tokens
users ──< recipes ──< recipe_ingredients >── ingredients
users ──< pantry_items >── ingredients
users ──< shopping_items >── ingredients (nullable)
```

## Tables

### users
| column | type | notes |
|---|---|---|
| id | uuid PK | |
| name | varchar(100) | |
| email | varchar(255) | unique on `LOWER(email)` |
| password_hash | varchar(255) | BCrypt (cost 12) |
| role | varchar(16) | `ADMIN` \| `USER` (CHECK) |
| created_at / updated_at | timestamptz | |

### refresh_tokens
Raw tokens never stored — only SHA-256 hashes. Rotation revokes the old row.
| column | type | notes |
|---|---|---|
| id | uuid PK | |
| user_id | uuid FK → users (CASCADE) | |
| token_hash | char(64) unique | |
| expires_at | timestamptz | |
| revoked | boolean | |

### ingredients
| column | type | notes |
|---|---|---|
| id | uuid PK | |
| name | varchar(100) | **unique on `LOWER(name)`** — "Salt" = "salt" = "SALT" |
| category | varchar(50) | e.g. SPICE, VEGETABLE, MEAT |
| default_unit | varchar(20) | |
| image_url | text nullable | |
| status | varchar(16) | `APPROVED` \| `PENDING` \| `REJECTED` — user submissions await moderation |
| created_by | uuid FK nullable (SET NULL) | |

### recipes
| column | type | notes |
|---|---|---|
| id | uuid PK | |
| title | varchar(200), indexed on LOWER | |
| description | text | |
| cooking_time_minutes / servings | int, CHECK > 0 | |
| difficulty | varchar(16), CHECK EASY/MEDIUM/HARD | |
| cover_image_url | text nullable | |
| instructions | jsonb | ordered array of step strings |
| created_by | uuid FK → users (CASCADE) | |
| view_count | bigint | popularity signal, indexed |
| is_deleted | boolean | soft delete for moderation |

### recipe_ingredients (many-to-many, never plain text)
| column | type | notes |
|---|---|---|
| recipe_id | uuid FK → recipes (CASCADE) | composite PK |
| ingredient_id | uuid FK → ingredients (**RESTRICT** — an ingredient in use cannot be deleted) | composite PK |
| quantity | numeric(10,2), CHECK > 0 | |
| unit | varchar(20) | |
| note | varchar(255) nullable | |

### pantry_items
| column | type | notes |
|---|---|---|
| id | uuid PK | |
| user_id + ingredient_id | FKs, **unique pair** | one row per ingredient per user |
| quantity | numeric(10,2) | |
| unit | varchar(20) | |
| is_out_of_stock | boolean | drives shopping-list generation |
| updated_at | timestamptz | |

### shopping_items
| column | type | notes |
|---|---|---|
| id | uuid PK | |
| user_id | uuid FK (CASCADE) | |
| ingredient_id | uuid FK nullable (SET NULL) | null ⇒ free-text item |
| name | varchar(100) | denormalized display name |
| quantity / unit | nullable | |
| source | `MANUAL` \| `PANTRY` | how the item was created |
| is_purchased / purchased_at | | purchase restocks the linked pantry row |
