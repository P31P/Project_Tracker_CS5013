CREATE TABLE app_user (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    email       TEXT NOT NULL UNIQUE,
    name        TEXT,
    allowlisted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE project (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    title       TEXT NOT NULL CHECK (btrim(title) <> ''),
    description TEXT,
    status      TEXT,
    owner_id    BIGINT REFERENCES app_user (id),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
