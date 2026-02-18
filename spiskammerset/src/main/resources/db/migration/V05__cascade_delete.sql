-- Add ON DELETE CASCADE to foreign key constraints that reference hylle(hyllenummer)
-- This ensures that when a hylle is deleted, all related records are automatically deleted

-- 1. Update hendelser_paa_hylla to cascade delete
ALTER TABLE hendelser_paa_hylla
    DROP CONSTRAINT IF EXISTS hendelser_paa_hylla_hyllenummer_fkey;

ALTER TABLE hendelser_paa_hylla
    ADD CONSTRAINT hendelser_paa_hylla_hyllenummer_fkey
        FOREIGN KEY (hyllenummer)
        REFERENCES hylle(hyllenummer)
        ON DELETE CASCADE;

-- 2. Update hylleeier to cascade delete
ALTER TABLE hylleeier
    DROP CONSTRAINT IF EXISTS hylleeier_hyllenummer_fkey;

ALTER TABLE hylleeier
    ADD CONSTRAINT hylleeier_hyllenummer_fkey
        FOREIGN KEY (hyllenummer)
        REFERENCES hylle(hyllenummer)
        ON DELETE CASCADE;

-- 3. Add foreign key constraint to forsikring with cascade delete
-- forsikring uses hyllenummer as its PRIMARY KEY (1-to-1 relationship with hylle)
ALTER TABLE forsikring
    ADD CONSTRAINT forsikring_hyllenummer_fkey
        FOREIGN KEY (hyllenummer)
        REFERENCES hylle(hyllenummer)
        ON DELETE CASCADE;


