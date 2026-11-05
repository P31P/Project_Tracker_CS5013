-- The fields the lab asked for when adding a project or an idea. All optional apart from the start date, which
-- existing projects take from the day they were added.
ALTER TABLE project
    ADD COLUMN started_on       DATE,
    ADD COLUMN core_question    TEXT,
    ADD COLUMN motivation       TEXT,
    ADD COLUMN approach         TEXT,
    ADD COLUMN resources        TEXT,
    ADD COLUMN challenges       TEXT,
    ADD COLUMN open_for_collab  BOOLEAN NOT NULL DEFAULT FALSE,
    -- when collaboration was last opened, so Home can tell people about newly open projects
    ADD COLUMN collab_opened_at TIMESTAMPTZ;

UPDATE project SET started_on = created_at::date;

ALTER TABLE project
    ALTER COLUMN started_on SET DEFAULT CURRENT_DATE,
    ALTER COLUMN started_on SET NOT NULL;

ALTER TABLE idea
    ADD COLUMN core_question   TEXT,
    ADD COLUMN motivation      TEXT,
    ADD COLUMN approach        TEXT,
    ADD COLUMN resources       TEXT,
    ADD COLUMN challenges      TEXT,
    ADD COLUMN open_for_collab BOOLEAN NOT NULL DEFAULT FALSE;
