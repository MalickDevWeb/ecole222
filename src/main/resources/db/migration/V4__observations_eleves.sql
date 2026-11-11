-- V4 — Journal d'observations sur un élève (suivi individuel par l'administration).
-- Avant : l'écran affichait le formulaire mais rien n'était enregistré (APP-BE-10).
CREATE TABLE IF NOT EXISTS public.observations_eleves (
    id character varying(64) NOT NULL PRIMARY KEY,
    organization_id character varying(255) NOT NULL,
    eleve_id character varying(255) NOT NULL,
    categorie character varying(40) NOT NULL,
    texte character varying(1000) NOT NULL,
    auteur_id character varying(255) NOT NULL,
    cree_le timestamp(6) without time zone NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_observations_eleve ON public.observations_eleves (organization_id, eleve_id);
