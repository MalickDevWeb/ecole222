-- V20 — Accès aux comptes sans mot de passe dans un message (décisions du Tech Lead du 15/11, conception de Fallou Ndiaye).
-- 1. Liens à usage unique : la table de V7 (jeton de 256 bits haché SHA-256) sert aussi à l'ACTIVATION d'un compte et à
--    la RÉINITIALISATION décidée par l'école ; un nouveau lien annule les précédents (annule_le). Pour un compte à
--    privilèges (administrateur, directeur) ou un numéro modifié depuis moins de 24 h : accord d'un second
--    administrateur (accord_requis / accorde_par) ; pour un compte à privilèges, code oral de 6 chiffres (code_hache).
ALTER TABLE public.reinitialisations_mot_de_passe
    ADD COLUMN IF NOT EXISTS type character varying(20) NOT NULL DEFAULT 'REINITIALISATION',
    ADD COLUMN IF NOT EXISTS organization_id character varying(255),
    ADD COLUMN IF NOT EXISTS cree_par character varying(255),
    ADD COLUMN IF NOT EXISTS canal character varying(20),
    ADD COLUMN IF NOT EXISTS numero_masque character varying(40),
    ADD COLUMN IF NOT EXISTS annule_le timestamp(6) without time zone,
    ADD COLUMN IF NOT EXISTS accord_requis boolean NOT NULL DEFAULT false,
    ADD COLUMN IF NOT EXISTS accorde_par character varying(255),
    ADD COLUMN IF NOT EXISTS code_hache character varying(64),
    ADD COLUMN IF NOT EXISTS essais_code integer NOT NULL DEFAULT 0;
ALTER TABLE public.reinitialisations_mot_de_passe
    ADD CONSTRAINT ck_reinit_mdp_type CHECK (type IN ('ACTIVATION', 'REINITIALISATION'));
ALTER TABLE public.reinitialisations_mot_de_passe
    ADD CONSTRAINT ck_reinit_mdp_canal CHECK (canal IS NULL OR canal IN ('WHATSAPP', 'SMS', 'EMAIL'));
CREATE INDEX IF NOT EXISTS idx_reinit_mdp_organization ON public.reinitialisations_mot_de_passe (organization_id);

-- 2. Demandes d'aide à la connexion (« je n'arrive pas à me connecter »), traitées par l'administration de l'école.
--    Conservées 30 jours (purge planifiée). L'adresse IP n'est gardée que hachée.
CREATE TABLE IF NOT EXISTS public.demandes_aide_connexion (
    id character varying(64) NOT NULL PRIMARY KEY,
    organization_id character varying(255) NOT NULL REFERENCES public.organizations (id) ON DELETE CASCADE,
    user_id character varying(255) NOT NULL,
    identifiant_saisi character varying(100) NOT NULL,
    cree_le timestamp(6) without time zone NOT NULL,
    statut character varying(20) NOT NULL CHECK (statut IN ('EN_ATTENTE', 'TRAITEE', 'REJETEE')),
    traitee_par character varying(255),
    traitee_le timestamp(6) without time zone,
    ip_hachee character varying(64)
);
CREATE INDEX IF NOT EXISTS idx_demandes_aide_organization ON public.demandes_aide_connexion (organization_id, statut, cree_le);

-- 3. Date de la dernière modification du numéro de téléphone d'un compte : un lien n'est pas envoyé à un numéro
--    modifié depuis moins de 24 h sans l'accord d'un second administrateur.
ALTER TABLE public.users ADD COLUMN IF NOT EXISTS telephone_modifie_le timestamp(6) without time zone;
