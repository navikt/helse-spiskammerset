-- Lagrer bare ned hendelsen for sprobarhet, og brukes for å sjekke om vi har håndtert hendelsen før
CREATE TABLE hendelse
(
    id                              BIGINT              GENERATED ALWAYS AS IDENTITY,
    hendelse_id                     UUID                NOT NULL,
    hendelsetype                    TEXT                NOT NULL,
    hendelse                        JSONB               NOT NULL,
    opprettet                       TIMESTAMPTZ         NOT NULL DEFAULT now(),
    PRIMARY KEY (id),
    CONSTRAINT unik_hendelse_id UNIQUE (hendelse_id)
);