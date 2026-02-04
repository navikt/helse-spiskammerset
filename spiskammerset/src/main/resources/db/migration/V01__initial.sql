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

-- Et nivå oppå behandling som gjør det enklere å finne rett hylle gitt forskjellig input-parametre.
-- Gjør det også mulig for oppbevarsingsboksene å ikke kjenne til eller håndtere problematikken
-- med bytte av personidentifikator, ny periode på behandling og forskjellige muligheter å slå opp data på
-- De trenger kun å forholde seg til et hyllenummer.
CREATE TABLE hylle
(
    hyllenummer                     BIGINT              GENERATED ALWAYS AS IDENTITY,
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
    CONSTRAINT gyldig_periode CHECK (tom >= fom), -- daterange tillater tom < fom, så sjekker det heller her en å tillate merkelige perioder
    CONSTRAINT unik_behandling_id UNIQUE (behandling_id)
);
-- Ryktene skal ha det til at dette gjør oppslag periode rasende raskt
CREATE EXTENSION IF NOT EXISTS btree_gist;
CREATE INDEX idx_periode ON hylle USING GIST (periode, vedtaksperiode_id);
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

-- Gjør det mulig å finne alle hendelser som er koblet til en hylle (for feilsøking og sporing)
CREATE TABLE hendelser_paa_hylla
(
    intern_hendelse_id          BIGINT          NOT NULL REFERENCES hendelse(id),
    hyllenummer                 BIGINT          NOT NULL REFERENCES hylle(hyllenummer),
    CONSTRAINT unik_kobling UNIQUE (intern_hendelse_id, hyllenummer)
);

-- Gjør det mulig å håndtere identbytte på en behandling
CREATE TABLE hylleeier
(
    personidentifikator         TEXT            NOT NULL,
    hyllenummer                 BIGINT          NOT NULL REFERENCES hylle(hyllenummer),
    CONSTRAINT unik_kombinasjon UNIQUE (personidentifikator, hyllenummer) -- skal angivelig også funke som index
);
