CREATE TABLE idea (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    title       TEXT NOT NULL CHECK (btrim(title) <> ''),
    description TEXT,
    author_id   BIGINT REFERENCES app_user (id),
    project_id  BIGINT REFERENCES project (id),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
