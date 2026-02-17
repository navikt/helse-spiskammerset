ALTER TABLE forsikring ADD COLUMN arbeidssituasjonForsikringstype TEXT;
UPDATE forsikring SET arbeidssituasjonForsikringstype = 'SelvstendigForsikring';
ALTER TABLE forsikring ALTER COLUMN arbeidssituasjonForsikringstype SET NOT NULL;
