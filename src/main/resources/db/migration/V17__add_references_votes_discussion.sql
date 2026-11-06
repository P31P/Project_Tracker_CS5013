-- References: ideas get web links like projects have, and both can list papers from the archive
CREATE TABLE idea_link (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    idea_id    BIGINT NOT NULL REFERENCES idea (id),
    label      TEXT NOT NULL CHECK (btrim(label) <> ''),
    url        TEXT NOT NULL CHECK (url ~* '^https?://'),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idea_link_idea_idx ON idea_link (idea_id);

CREATE TABLE project_paper (
    project_id BIGINT NOT NULL REFERENCES project (id),
    paper_id   BIGINT NOT NULL REFERENCES paper (id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (project_id, paper_id)
);

CREATE TABLE idea_paper (
    idea_id    BIGINT NOT NULL REFERENCES idea (id),
    paper_id   BIGINT NOT NULL REFERENCES paper (id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (idea_id, paper_id)
);

-- for the "Used in" list on a paper's page
CREATE INDEX project_paper_paper_idx ON project_paper (paper_id);
CREATE INDEX idea_paper_paper_idx ON idea_paper (paper_id);

-- upvotes and a discussion on projects, like ideas have
CREATE TABLE project_vote (
    project_id BIGINT NOT NULL REFERENCES project (id),
    user_id    BIGINT NOT NULL REFERENCES app_user (id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (project_id, user_id)
);

CREATE TABLE project_comment (
    id         BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    project_id BIGINT NOT NULL REFERENCES project (id),
    author_id  BIGINT REFERENCES app_user (id),
    body       TEXT NOT NULL CHECK (btrim(body) <> ''),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX project_comment_project_idx ON project_comment (project_id);
