-- V10 — Membres des groupes de travail (Collaboration), 2026-11-13.
-- Avant : seul un compteur members_count existait ; les membres, le chef et la classe choisis à la création étaient
-- perdus (« Chef : » vide, « 0 membres »), et le chef ne pouvant jamais être reconnu, aucun élève ne pouvait rendre
-- le devoir de son groupe.
CREATE TABLE IF NOT EXISTS public.membres_groupes_travail (
    id character varying(64) NOT NULL PRIMARY KEY,
    organization_id character varying(255) NOT NULL CHECK (organization_id <> ''),
    groupe_id character varying(255) NOT NULL,
    utilisateur_id character varying(255) NOT NULL,
    chef boolean NOT NULL DEFAULT false,
    ajoute_le timestamp(6) without time zone NOT NULL,
    CONSTRAINT uk_membres_groupes_travail UNIQUE (organization_id, groupe_id, utilisateur_id)
);
CREATE INDEX IF NOT EXISTS idx_membres_groupes_travail_groupe ON public.membres_groupes_travail (organization_id, groupe_id);
CREATE INDEX IF NOT EXISTS idx_membres_groupes_travail_utilisateur ON public.membres_groupes_travail (organization_id, utilisateur_id);

-- Classe concernée par le groupe (facultative : un groupe créé par un élève n'en a pas).
ALTER TABLE public.workgroups ADD COLUMN IF NOT EXISTS classe_id character varying(255);
