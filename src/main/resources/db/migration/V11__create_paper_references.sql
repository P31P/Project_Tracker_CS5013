CREATE TABLE paper_reference (
    id       BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    paper_id BIGINT NOT NULL REFERENCES paper (id),
    position INT NOT NULL,
    citation TEXT NOT NULL CHECK (btrim(citation) <> ''),
    doi      TEXT,
    UNIQUE (paper_id, position)
);
