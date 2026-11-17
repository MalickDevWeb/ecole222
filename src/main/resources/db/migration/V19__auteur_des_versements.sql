-- V19 — Auteur de la saisie d'un versement (15/11). Avant : le serveur enregistrait le moyen de paiement et la
-- référence, mais pas QUI avait saisi le versement ; les reçus et l'écran « Encaissements » ne pouvaient pas l'afficher.
-- Renseigné pour les NOUVEAUX versements seulement : les anciens restent sans auteur (jamais d'auteur inventé).
ALTER TABLE public.transactions ADD COLUMN IF NOT EXISTS saisi_par_id character varying(255);
ALTER TABLE public.transactions ADD COLUMN IF NOT EXISTS saisi_par character varying(255);
