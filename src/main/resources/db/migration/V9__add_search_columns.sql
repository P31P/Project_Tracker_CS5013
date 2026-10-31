-- array_to_string is not immutable, so generated columns need this wrapper
CREATE FUNCTION immutable_join(parts TEXT[]) RETURNS TEXT
    LANGUAGE sql IMMUTABLE PARALLEL SAFE
    AS $$ SELECT array_to_string(parts, ' ') $$;

ALTER TABLE project ADD COLUMN search TSVECTOR GENERATED ALWAYS AS (
    setweight(to_tsvector('english', coalesce(title, '')), 'A')
    || setweight(to_tsvector('english', coalesce(description, '')), 'B')
    || setweight(to_tsvector('english', coalesce(immutable_join(tags), '')), 'C')
) STORED;

ALTER TABLE idea ADD COLUMN search TSVECTOR GENERATED ALWAYS AS (
    setweight(to_tsvector('english', coalesce(title, '')), 'A')
    || setweight(to_tsvector('english', coalesce(description, '')), 'B')
    || setweight(to_tsvector('english', coalesce(immutable_join(tags), '')), 'C')
) STORED;

ALTER TABLE paper ADD COLUMN search TSVECTOR GENERATED ALWAYS AS (
    setweight(to_tsvector('english', coalesce(title, '')), 'A')
    || setweight(to_tsvector('english', coalesce(immutable_join(authors), '')), 'B')
    || setweight(to_tsvector('english', coalesce(doi, '')), 'C')
    -- the link with / and : as spaces, so an arXiv ID or a DOI's last part is a word of its own
    || setweight(to_tsvector('english', translate(normalized_url, '/:', '  ')), 'D')
) STORED;

CREATE INDEX project_search_idx ON project USING GIN (search);
CREATE INDEX idea_search_idx ON idea USING GIN (search);
CREATE INDEX paper_search_idx ON paper USING GIN (search);
