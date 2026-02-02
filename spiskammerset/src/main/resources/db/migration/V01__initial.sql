CREATE TABLE forsikring
(
    hendelse_id                     UUID        NOT NULL UNIQUE PRIMARY KEY,
    opprettet                       TIMESTAMPTZ NOT NULL DEFAULT now(),
    behandling_id                   UUID        NOT NULL,
    dekningsgrad                    INT         NOT NULL,
    nav_overtar_ansvar_for_ventetid BOOLEAN     NOT NULL
);

CREATE INDEX idx_behandling_id ON forsikring (behandling_id);

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
    hendelse_ider                   UUID[]              NOT NULL,
    CONSTRAINT gyldig_periode CHECK (tom >= fom), -- daterange tillater tom < fom, så sjekker det heller her
    CONSTRAINT unik_behandling_id UNIQUE (behandling_id),
    CONSTRAINT minst_en_hendelse CHECK (cardinality(hendelse_ider) > 0)
);
-- Ryktene skal ha det til at dette gjør oppslag på person + periode rasende raskt
CREATE EXTENSION IF NOT EXISTS btree_gist;
CREATE INDEX idx_person_og_periode ON hylle USING GIST (personidentifikator, periode);
-- inntil videre ubekreftede rykter

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