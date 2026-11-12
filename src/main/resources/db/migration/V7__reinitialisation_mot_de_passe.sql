-- V7 — Réinitialisation du mot de passe en ligne (APP-FEAT-02).
-- Avant : « mot de passe oublié » affichait « lien envoyé » sans aucun appel au serveur.
-- Jeton aléatoire à usage unique, stocké HACHÉ (SHA-256) : la base ne contient jamais le lien lui-même.
-- Table de plateforme (les comptes sont communs aux écoles) : pas de colonne organization_id.
CREATE TABLE IF NOT EXISTS public.reinitialisations_mot_de_passe (
    id character varying(64) NOT NULL PRIMARY KEY,
    user_id character varying(255) NOT NULL,
    jeton_hache character varying(64) NOT NULL UNIQUE,
    cree_le timestamp(6) without time zone NOT NULL,
    expire_le timestamp(6) without time zone NOT NULL,
    utilise_le timestamp(6) without time zone
);
CREATE INDEX IF NOT EXISTS idx_reinit_mdp_user ON public.reinitialisations_mot_de_passe (user_id);
