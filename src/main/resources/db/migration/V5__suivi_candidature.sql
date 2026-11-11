-- V5 — Suivi en ligne de sa candidature par le candidat (APP-BE-12).
-- Le mot de passe choisi au dépôt n'était jamais envoyé : il est désormais enregistré (haché, BCrypt).
ALTER TABLE public.candidates ADD COLUMN IF NOT EXISTS mot_de_passe_suivi character varying(100);
