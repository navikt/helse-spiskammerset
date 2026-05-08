CREATE TABLE melding
(
    id               UUID        NOT NULL PRIMARY KEY,
    lagret_tidspunkt TIMESTAMPTZ NOT NULL DEFAULT now(),
    data             JSONB       NOT NULL
);
