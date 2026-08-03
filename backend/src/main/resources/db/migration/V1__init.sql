-- V1: Core schema for RecipeApp.
-- Conventions: UUID primary keys, TIMESTAMPTZ timestamps in UTC,
-- case-insensitive uniqueness implemented with functional unique indexes.

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ---------------------------------------------------------------------------
-- Users & auth
-- ---------------------------------------------------------------------------
CREATE TABLE users (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name          VARCHAR(100)  NOT NULL,
    email         VARCHAR(255)  NOT NULL,
    password_hash VARCHAR(255)  NOT NULL,
    role          VARCHAR(16)   NOT NULL DEFAULT 'USER',
    created_at    TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT chk_users_role CHECK (role IN ('ADMIN', 'USER'))
);
-- Case-insensitive unique email.
CREATE UNIQUE INDEX ux_users_email_ci ON users (LOWER(email));

-- Refresh tokens are stored hashed (SHA-256); the raw token never touches disk.
CREATE TABLE refresh_tokens (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    token_hash CHAR(64)    NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked    BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX ix_refresh_tokens_user ON refresh_tokens (user_id);

-- ---------------------------------------------------------------------------
-- Ingredient catalog (global, shared by all users)
-- ---------------------------------------------------------------------------
CREATE TABLE ingredients (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name         VARCHAR(100) NOT NULL,
    category     VARCHAR(50)  NOT NULL DEFAULT 'OTHER',
    default_unit VARCHAR(20)  NOT NULL DEFAULT 'g',
    image_url    TEXT,
    -- APPROVED entries are visible to everyone. User submissions start as
    -- PENDING until an admin approves them, which keeps the catalog curated.
    status       VARCHAR(16)  NOT NULL DEFAULT 'APPROVED',
    created_by   UUID REFERENCES users (id) ON DELETE SET NULL,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT chk_ingredients_status CHECK (status IN ('APPROVED', 'PENDING', 'REJECTED'))
);
-- Case-insensitive unique ingredient names: 'Salt' == 'salt' == 'SALT'.
CREATE UNIQUE INDEX ux_ingredients_name_ci ON ingredients (LOWER(name));
CREATE INDEX ix_ingredients_category ON ingredients (category);

-- ---------------------------------------------------------------------------
-- Recipes
-- ---------------------------------------------------------------------------
CREATE TABLE recipes (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title                VARCHAR(200) NOT NULL,
    description          TEXT         NOT NULL DEFAULT '',
    cooking_time_minutes INT          NOT NULL,
    servings             INT          NOT NULL,
    difficulty           VARCHAR(16)  NOT NULL,
    cover_image_url      TEXT,
    -- Ordered list of instruction steps, stored as a JSONB array of strings.
    instructions         JSONB        NOT NULL DEFAULT '[]',
    created_by           UUID         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    view_count           BIGINT       NOT NULL DEFAULT 0,
    -- Soft delete: admins remove inappropriate recipes without breaking
    -- referential history; queries always filter is_deleted = FALSE.
    is_deleted           BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at           TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at           TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT chk_recipes_difficulty CHECK (difficulty IN ('EASY', 'MEDIUM', 'HARD')),
    CONSTRAINT chk_recipes_time CHECK (cooking_time_minutes > 0),
    CONSTRAINT chk_recipes_servings CHECK (servings > 0)
);
CREATE INDEX ix_recipes_created_at ON recipes (created_at DESC);
CREATE INDEX ix_recipes_view_count ON recipes (view_count DESC);
CREATE INDEX ix_recipes_created_by ON recipes (created_by);
CREATE INDEX ix_recipes_title ON recipes (LOWER(title));

-- Many-to-many between recipes and ingredients, with per-recipe quantity.
CREATE TABLE recipe_ingredients (
    recipe_id     UUID          NOT NULL REFERENCES recipes (id) ON DELETE CASCADE,
    ingredient_id UUID          NOT NULL REFERENCES ingredients (id) ON DELETE RESTRICT,
    quantity      NUMERIC(10,2) NOT NULL,
    unit          VARCHAR(20)   NOT NULL,
    note          VARCHAR(255),
    PRIMARY KEY (recipe_id, ingredient_id),
    CONSTRAINT chk_recipe_ingredients_quantity CHECK (quantity > 0)
);
CREATE INDEX ix_recipe_ingredients_ingredient ON recipe_ingredients (ingredient_id);

-- ---------------------------------------------------------------------------
-- Pantry (per user)
-- ---------------------------------------------------------------------------
CREATE TABLE pantry_items (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID          NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    ingredient_id   UUID          NOT NULL REFERENCES ingredients (id) ON DELETE CASCADE,
    quantity        NUMERIC(10,2) NOT NULL DEFAULT 0,
    unit            VARCHAR(20)   NOT NULL,
    is_out_of_stock BOOLEAN       NOT NULL DEFAULT FALSE,
    updated_at      TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT ux_pantry_user_ingredient UNIQUE (user_id, ingredient_id)
);
CREATE INDEX ix_pantry_user ON pantry_items (user_id);

-- ---------------------------------------------------------------------------
-- Shopping list (per user)
-- ---------------------------------------------------------------------------
CREATE TABLE shopping_items (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id       UUID         NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    -- Nullable: manual free-text items are allowed ("birthday candles").
    ingredient_id UUID REFERENCES ingredients (id) ON DELETE SET NULL,
    name          VARCHAR(100) NOT NULL,
    quantity      NUMERIC(10,2),
    unit          VARCHAR(20),
    source        VARCHAR(16)  NOT NULL DEFAULT 'MANUAL',
    is_purchased  BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    purchased_at  TIMESTAMPTZ,
    CONSTRAINT chk_shopping_source CHECK (source IN ('MANUAL', 'PANTRY'))
);
CREATE INDEX ix_shopping_user ON shopping_items (user_id, is_purchased);
