CREATE TABLE grunnlagsdata
(
    id               UUID        NOT NULL PRIMARY KEY DEFAULT uuidv7(),
    lagret_tidspunkt TIMESTAMPTZ NOT NULL             DEFAULT now(),
    data             JSONB       NOT NULL,
    type             TEXT        NOT NULL,
    melding_ref      UUID        NOT NULL REFERENCES melding (id)
);
