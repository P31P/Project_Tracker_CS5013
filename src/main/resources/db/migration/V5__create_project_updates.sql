UPDATE project SET status = 'ACTIVE' WHERE status IS NULL;

ALTER TABLE project
    ALTER COLUMN status SET DEFAULT 'ACTIVE',
    ALTER COLUMN status SET NOT NULL,
    ADD CONSTRAINT project_status_check CHECK (status IN ('PLANNING', 'ACTIVE', 'ON_HOLD', 'DONE')),
    ADD COLUMN updated_at TIMESTAMPTZ NOT NULL DEFAULT now();

CREATE TABLE project_update (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    project_id BIGINT NOT NULL REFERENCES project (id),
    author_id  BIGINT REFERENCES app_user (id),
    kind       TEXT NOT NULL DEFAULT 'NOTE' CHECK (kind IN ('NOTE', 'STATUS')),
    body       TEXT NOT NULL CHECK (btrim(body) <> ''),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX project_update_project_idx ON project_update (project_id);
