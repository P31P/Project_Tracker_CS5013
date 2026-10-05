ALTER TABLE paper
    ADD COLUMN title        TEXT,
    ADD COLUMN authors      TEXT[],
    ADD COLUMN published_on DATE,
    ADD COLUMN doi          TEXT,
    ADD COLUMN source       TEXT,
    ADD COLUMN attempts     INT NOT NULL DEFAULT 0;
