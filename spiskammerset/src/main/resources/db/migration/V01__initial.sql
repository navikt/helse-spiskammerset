CREATE TABLE forsikring
(
    hendelse_id                     UUID        NOT NULL UNIQUE PRIMARY KEY,
    opprettet                       TIMESTAMPTZ NOT NULL DEFAULT now(),
    behandling_id                   UUID        NOT NULL,
    dekningsgrad                    INT         NOT NULL,
    nav_overtar_ansvar_for_ventetid BOOLEAN     NOT NULL
);

CREATE INDEX idx_behandling_id ON forsikring (behandling_id);