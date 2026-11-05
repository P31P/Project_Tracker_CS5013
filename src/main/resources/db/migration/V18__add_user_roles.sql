-- Who may do what: everyone is a member; a Lab Manager can also give and remove access; an admin (the lab's
-- head) can do that too and sees the lab-wide numbers on Home.
ALTER TABLE app_user
    ADD COLUMN role TEXT NOT NULL DEFAULT 'MEMBER' CHECK (role IN ('MEMBER', 'LAB_MANAGER', 'ADMIN'));

-- until the lab names its own, the two people who set the tracker up manage access
UPDATE app_user SET role = 'LAB_MANAGER'
WHERE email IN ('da24b026@smail.iitm.ac.in', 'cs24b047@smail.iitm.ac.in');
