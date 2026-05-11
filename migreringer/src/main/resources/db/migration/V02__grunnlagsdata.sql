CREATE TABLE grunnlagsdata
(
    id               UUID        NOT NULL PRIMARY KEY,
    lagret_tidspunkt TIMESTAMPTZ NOT NULL,
    data             JSONB       NOT NULL,
    type             TEXT        NOT NULL,
    melding_ref      UUID        NOT NULL REFERENCES melding (id) ON DELETE RESTRICT
);
