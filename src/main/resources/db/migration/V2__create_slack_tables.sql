CREATE TABLE slack_message (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    channel_id      TEXT NOT NULL,
    message_ts      TEXT NOT NULL,
    slack_user_id   TEXT NOT NULL,
    slack_user_name TEXT,
    text            TEXT NOT NULL,
    permalink       TEXT,
    received_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (channel_id, message_ts)
);

CREATE TABLE paper (
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    normalized_url TEXT NOT NULL UNIQUE,
    status         TEXT NOT NULL DEFAULT 'PENDING'
                   CHECK (status IN ('PENDING', 'RESOLVED', 'UNRESOLVED')),
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE paper_share (
    id               BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    paper_id         BIGINT NOT NULL REFERENCES paper (id),
    slack_message_id BIGINT NOT NULL REFERENCES slack_message (id),
    posted_url       TEXT NOT NULL,
    UNIQUE (paper_id, slack_message_id)
);
