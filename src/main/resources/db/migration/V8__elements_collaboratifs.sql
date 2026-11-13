-- V8 — Wiki, notes, journal d'activité, présences aux réunions et devoirs de groupe enregistrés en base (APP-QA-04).
-- Avant (balayage du 2026-11-13) : ces éléments vivaient dans des listes en mémoire du serveur, perdues à chaque
-- redémarrage et COMMUNES À TOUTES LES ÉCOLES (une page du wiki de l'école A était lue par l'école B) ; les notes
-- « privées » étaient lues par tous ; les devoirs de groupe n'étaient jamais enregistrés.
-- Une table par école (organization_id, @TenantId), un genre par type d'élément, contenu validé stocké en JSON.
CREATE TABLE IF NOT EXISTS public.elements_collaboratifs (
    id character varying(64) NOT NULL PRIMARY KEY,
    organization_id character varying(255) NOT NULL CHECK (organization_id <> ''),
    genre character varying(30) NOT NULL,
    cle character varying(100) NOT NULL,
    groupe_id character varying(100),
    auteur_id character varying(255) NOT NULL,
    prive boolean NOT NULL DEFAULT false,
    contenu text NOT NULL,
    cree_le timestamp(6) without time zone NOT NULL,
    modifie_le timestamp(6) without time zone NOT NULL,
    CONSTRAINT uk_elements_collaboratifs_cle UNIQUE (organization_id, genre, cle)
);
CREATE INDEX IF NOT EXISTS idx_elements_collaboratifs_genre ON public.elements_collaboratifs (organization_id, genre, groupe_id);
