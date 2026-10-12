ALTER TABLE project ADD COLUMN tags TEXT[] NOT NULL DEFAULT '{}';

CREATE TABLE project_member (
    project_id BIGINT NOT NULL REFERENCES project (id),
    user_id    BIGINT NOT NULL REFERENCES app_user (id),
    PRIMARY KEY (project_id, user_id)
);

CREATE TABLE project_link (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    project_id BIGINT NOT NULL REFERENCES project (id),
    label      TEXT NOT NULL CHECK (btrim(label) <> ''),
    url        TEXT NOT NULL CHECK (url ~* '^https?://'),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX project_link_project_idx ON project_link (project_id);
