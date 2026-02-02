CREATE TABLE forsikring
(
    hendelse_id                     UUID        NOT NULL UNIQUE PRIMARY KEY,
    opprettet                       TIMESTAMPTZ NOT NULL DEFAULT now(),
    behandling_id                   UUID        NOT NULL,
    dekningsgrad                    INT         NOT NULL,
    nav_overtar_ansvar_for_ventetid BOOLEAN     NOT NULL
);

CREATE INDEX idx_behandling_id ON forsikring (behandling_id);

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

CREATE TABLE hylle
(
    hyllenummer                     BIGINT              GENERATED ALWAYS AS IDENTITY,
    personidentifikator             TEXT                NOT NULL,
    vedtaksperiode_id               UUID                NOT NULL,
    behandling_id                   UUID                NOT NULL,
    yrkesaktivitetstype             TEXT                NOT NULL,
    organisasjonsnummer             TEXT                DEFAULT NULL,
    fom                             DATE                NOT NULL,
    tom                             DATE                NOT NULL,
    periode                         DATERANGE           GENERATED ALWAYS AS (daterange(fom, tom + 1, '[)')) STORED,
    opprettet                       TIMESTAMPTZ         NOT NULL DEFAULT now(),
    endret                          TIMESTAMPTZ         NOT NULL DEFAULT now(),
    PRIMARY KEY (hyllenummer),
    CONSTRAINT gyldig_periode CHECK (tom >= fom), -- daterange tillater tom < fom, så sjekker det heller her
    CONSTRAINT unik_behandling_id UNIQUE (behandling_id)
);
-- Ryktene skal ha det til at dette gjør oppslag på person + periode rasende raskt
CREATE EXTENSION IF NOT EXISTS btree_gist;
CREATE INDEX idx_person_og_periode ON hylle USING GIST (personidentifikator, periode);
-- inntil videre ubekreftede rykter
CREATE INDEX idx_vedtaksperiode_id ON hylle (vedtaksperiode_id);

-- trigger som setter 'endret'-tidspunktet automatisk etter alle UPDATES mot hylle-tabellen
CREATE FUNCTION oppdater_endret_tidspunkt()
    RETURNS TRIGGER AS $$
BEGIN
    NEW.endret = now() ;
RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER trigger_hylle_endret BEFORE UPDATE ON hylle FOR EACH ROW EXECUTE FUNCTION oppdater_endret_tidspunkt();
-- trigger ferdig

CREATE TABLE hendelser_paa_hylla
(
    intern_hendelse_id          BIGINT          NOT NULL REFERENCES hendelse(id),
    hyllenummer                 BIGINT          NOT NULL REFERENCES hylle(hyllenummer),
    CONSTRAINT unik_kobling UNIQUE (intern_hendelse_id, hyllenummer)
);