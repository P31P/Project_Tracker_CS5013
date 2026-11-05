-- Rebuilds the project and idea search columns (V9) so the write-up added in V16 can be found too. A generated
-- column can't be changed in place, so each is dropped (with its index) and added again.
ALTER TABLE project DROP COLUMN search;

ALTER TABLE project ADD COLUMN search TSVECTOR GENERATED ALWAYS AS (
    setweight(to_tsvector('english', coalesce(title, '')), 'A')
    || setweight(to_tsvector('english', coalesce(description, '') || ' ' || coalesce(core_question, '')), 'B')
    || setweight(to_tsvector('english', coalesce(immutable_join(tags), '')), 'C')
    || setweight(to_tsvector('english', coalesce(motivation, '') || ' ' || coalesce(approach, '') || ' '
        || coalesce(resources, '') || ' ' || coalesce(challenges, '')), 'D')
) STORED;

CREATE INDEX project_search_idx ON project USING GIN (search);

ALTER TABLE idea DROP COLUMN search;

ALTER TABLE idea ADD COLUMN search TSVECTOR GENERATED ALWAYS AS (
    setweight(to_tsvector('english', coalesce(title, '')), 'A')
    || setweight(to_tsvector('english', coalesce(description, '') || ' ' || coalesce(core_question, '')), 'B')
    || setweight(to_tsvector('english', coalesce(immutable_join(tags), '')), 'C')
    || setweight(to_tsvector('english', coalesce(motivation, '') || ' ' || coalesce(approach, '') || ' '
        || coalesce(resources, '') || ' ' || coalesce(challenges, '')), 'D')
) STORED;

CREATE INDEX idea_search_idx ON idea USING GIN (search);
