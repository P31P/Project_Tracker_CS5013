ALTER TABLE paper ADD COLUMN tags TEXT[] NOT NULL DEFAULT '{}';

CREATE INDEX paper_tags_idx ON paper USING GIN (tags);

CREATE TABLE paper_vote (
    paper_id   BIGINT NOT NULL REFERENCES paper (id),
    user_id    BIGINT NOT NULL REFERENCES app_user (id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (paper_id, user_id)
);

-- private to each person: nobody else sees what someone has read
CREATE TABLE paper_read (
    paper_id BIGINT NOT NULL REFERENCES paper (id),
    user_id  BIGINT NOT NULL REFERENCES app_user (id),
    read_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (paper_id, user_id)
);
