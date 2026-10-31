CREATE TABLE idea_comment (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    idea_id    BIGINT NOT NULL REFERENCES idea (id),
    author_id  BIGINT REFERENCES app_user (id),
    body       TEXT NOT NULL CHECK (btrim(body) <> ''),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idea_comment_idea_idx ON idea_comment (idea_id);
