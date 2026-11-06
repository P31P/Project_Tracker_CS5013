-- Who may do what: everyone is a member; a Lab Manager can also give and remove access; an admin (the lab's
-- head) can do that too and sees the lab-wide numbers on Home.
ALTER TABLE app_user
    ADD COLUMN role TEXT NOT NULL DEFAULT 'MEMBER' CHECK (role IN ('MEMBER', 'LAB_MANAGER', 'ADMIN'));

-- the two people who set the tracker up: one admin, so someone can name the lab head an admin from Manage access,
-- and one Lab Manager
UPDATE app_user SET role = 'ADMIN' WHERE email = 'da24b026@smail.iitm.ac.in';
UPDATE app_user SET role = 'LAB_MANAGER' WHERE email = 'cs24b047@smail.iitm.ac.in';
