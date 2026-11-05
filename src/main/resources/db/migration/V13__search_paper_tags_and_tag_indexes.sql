-- a generated column can't be changed in place, so the paper's is rebuilt with its tags (V12) added;
-- dropping the column drops its index too
ALTER TABLE paper DROP COLUMN search;

ALTER TABLE paper ADD COLUMN search TSVECTOR GENERATED ALWAYS AS (
    setweight(to_tsvector('english', coalesce(title, '')), 'A')
    || setweight(to_tsvector('english', coalesce(immutable_join(authors), '')), 'B')
    || setweight(to_tsvector('english', coalesce(doi, '')), 'C')
    || setweight(to_tsvector('english', immutable_join(tags)), 'C')
    || setweight(to_tsvector('english', translate(normalized_url, '/:', '  ')), 'D')
) STORED;

CREATE INDEX paper_search_idx ON paper USING GIN (search);

-- for the search tag filter (paper.tags got its index in V12)
CREATE INDEX project_tags_idx ON project USING GIN (tags);
CREATE INDEX idea_tags_idx ON idea USING GIN (tags);
