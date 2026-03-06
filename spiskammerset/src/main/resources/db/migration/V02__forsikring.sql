CREATE TABLE forsikring
(
    id              UUID        NOT NULL PRIMARY KEY DEFAULT uuidv7(),
    opprettet       TIMESTAMPTZ NOT NULL             DEFAULT now(),
    forsikringstype TEXT        NOT NULL,
    premiegrunnlag  INT         NOT NULL,
    startdato       DATE        NOT NULL,
    sluttdato       DATE,
    versjon         INT         NOT NULL
);
