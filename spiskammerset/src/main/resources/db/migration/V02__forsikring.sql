CREATE TABLE forsikring
(
    hyllenummer                     BIGINT      NOT NULL PRIMARY KEY,
    opprettet                       TIMESTAMPTZ NOT NULL DEFAULT now(),
    dekningsgrad                    INT         NOT NULL,
    nav_overtar_ansvar_for_ventetid BOOLEAN     NOT NULL,
    premiegrunnlag                  INT         NOT NULL,
    versjon                         INT         NOT NULL
);
