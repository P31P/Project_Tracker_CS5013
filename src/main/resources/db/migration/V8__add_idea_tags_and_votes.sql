ALTER TABLE idea ADD COLUMN tags TEXT[] NOT NULL DEFAULT '{}';

CREATE TABLE idea_vote (
    idea_id    BIGINT NOT NULL REFERENCES idea (id),
    user_id    BIGINT NOT NULL REFERENCES app_user (id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (idea_id, user_id)
);
