-- The account graders use in the Docker image ("Continue as demo user"): a plain member, never an admin.
-- Loaded once, after seed-mock-data.sql.
INSERT INTO app_user (email, name, allowlisted, background, bio, interests) VALUES
    ('demo@example.com', 'Demo User', true, 'Visiting the lab',
     'A demo account with **sample data**. Edit this profile, vote, comment, or start a project.',
     ARRAY['metabolic modeling', 'deep learning']::text[]);

-- a member of two projects, so Home has "Your projects" to show
INSERT INTO project_member (project_id, user_id)
SELECT p.id, u.id
FROM project p, app_user u
WHERE u.email = 'demo@example.com'
  AND p.title IN ('Flux sampling benchmark on genome-scale models', 'Protein language models for enzyme function');
