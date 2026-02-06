ALTER TABLE hylle ADD COLUMN behandling_opprettet TIMESTAMPTZ;
UPDATE hylle SET behandling_opprettet = opprettet;
ALTER TABLE hylle ALTER COLUMN behandling_opprettet SET NOT NULL;
