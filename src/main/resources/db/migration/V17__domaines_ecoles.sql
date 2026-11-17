-- V17 — Domaines propres des écoles (décision du Tech Lead du 15/11 : « pointage + déclaration »).
-- Un domaine propre (ex. portail.lycee-thies.sn) n'est servi que s'il est DÉCLARÉ par un administrateur de l'école
-- ET que sa chaîne CNAME mène exactement à <slug>.<domaine-plateforme> ET que l'école est ACTIVE.
-- Table GLOBALE (sans cloisonnement par école) : la résolution d'un hôte a lieu avant que l'école soit connue.
-- Un hostname = une seule école (contrainte unique), en A-label minuscule (punycode pour un nom accentué).
-- Statuts : EN_ATTENTE_POINTAGE (déclaré, CNAME pas encore vu), ACTIF (CNAME vérifié), DESACTIVE (3 vérifications
-- consécutives en échec : plus servi jusqu'à ce que le CNAME soit de nouveau vu).
CREATE TABLE IF NOT EXISTS public.domaines_ecoles (
    id character varying(64) NOT NULL PRIMARY KEY,
    organization_id character varying(255) NOT NULL
        REFERENCES public.organizations (id) ON DELETE CASCADE,
    hostname character varying(253) NOT NULL CHECK (hostname <> '' AND hostname = lower(hostname)),
    statut character varying(30) NOT NULL CHECK (statut IN ('EN_ATTENTE_POINTAGE', 'ACTIF', 'DESACTIVE')),
    echecs_consecutifs integer NOT NULL DEFAULT 0,
    verifie_le timestamp(6) without time zone,
    derniere_verification timestamp(6) without time zone,
    cree_par character varying(255),
    created_at timestamp(6) without time zone NOT NULL,
    CONSTRAINT uk_domaines_ecoles_hostname UNIQUE (hostname)
);
CREATE INDEX IF NOT EXISTS idx_domaines_ecoles_organization ON public.domaines_ecoles (organization_id);
