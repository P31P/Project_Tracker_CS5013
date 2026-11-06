ALTER TABLE app_user
    ADD COLUMN bio TEXT,
    ADD COLUMN background TEXT,
    ADD COLUMN interests TEXT[] NOT NULL DEFAULT '{}';

-- for matching new work against people's interests, and the People tab's interest filter
CREATE INDEX app_user_interests_idx ON app_user USING GIN (interests);
