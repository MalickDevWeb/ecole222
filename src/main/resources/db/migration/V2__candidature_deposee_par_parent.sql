-- V2 — Dossier de candidature déposé par un parent pour son enfant (espace parent).
-- Avant : le dépôt renvoyait un numéro de dossier inventé sans rien enregistrer.
ALTER TABLE public.candidates ADD COLUMN IF NOT EXISTS parent_id character varying(255);
CREATE INDEX IF NOT EXISTS idx_candidates_parent_id ON public.candidates (parent_id);

-- Le complément d'un dossier (adresse, diplôme, établissement, mention…) est stocké en JSON :
-- 255 caractères ne suffisent pas pour un dossier complet (erreur 500 « value too long »).
ALTER TABLE public.candidates ALTER COLUMN additional_info TYPE text;
