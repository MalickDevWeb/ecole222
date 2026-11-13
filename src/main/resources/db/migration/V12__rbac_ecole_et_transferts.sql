-- V12 — Droits par rôle propres à chaque école (RBAC) et transferts d'élèves entre écoles, 2026-11-13.

-- 1. Matrice des droits : avant, elle n'existait que dans le navigateur de l'administrateur (aucun effet sur le
--    serveur). Une ligne = un rôle du personnel autorisé à une permission dans une école. La ligne de permission
--    « * » indique que l'école a configuré ce rôle (sinon : droits par défaut).
CREATE TABLE IF NOT EXISTS public.permissions_roles_ecole (
    id character varying(400) NOT NULL PRIMARY KEY,
    organization_id character varying(255) NOT NULL CHECK (organization_id <> ''),
    role character varying(50) NOT NULL,
    permission character varying(100) NOT NULL,
    modifie_le timestamp(6) with time zone NOT NULL,
    modifie_par character varying(255),
    CONSTRAINT uk_permissions_roles_ecole UNIQUE (organization_id, role, permission)
);
CREATE INDEX IF NOT EXISTS idx_permissions_roles_ecole ON public.permissions_roles_ecole (organization_id, role);

-- 2. Transfert d'un élève : l'école d'origine fait la demande, l'école d'accueil l'accepte ou la refuse. À
--    l'acceptation, l'élève change d'école et une copie figée de son dossier (identité, notes, profil, observations,
--    documents) est remise à l'école d'accueil ; l'école d'origine garde ses archives.
CREATE TABLE IF NOT EXISTS public.demandes_transfert (
    id character varying(64) NOT NULL PRIMARY KEY,
    eleve_id character varying(255) NOT NULL,
    ecole_source_id character varying(255) NOT NULL,
    ecole_destination_id character varying(255) NOT NULL,
    motif character varying(1000),
    statut character varying(20) NOT NULL,
    cree_le timestamp(6) with time zone NOT NULL,
    cree_par character varying(255) NOT NULL,
    decide_le timestamp(6) with time zone,
    decide_par character varying(255),
    motif_decision character varying(1000),
    dossier text,
    CONSTRAINT ck_demandes_transfert_ecoles CHECK (ecole_source_id <> ecole_destination_id),
    CONSTRAINT ck_demandes_transfert_statut CHECK (statut IN ('EN_ATTENTE', 'ACCEPTEE', 'REFUSEE', 'ANNULEE'))
);
CREATE INDEX IF NOT EXISTS idx_demandes_transfert_source ON public.demandes_transfert (ecole_source_id, statut);
CREATE INDEX IF NOT EXISTS idx_demandes_transfert_destination ON public.demandes_transfert (ecole_destination_id, statut);
-- Une seule demande en attente par élève
CREATE UNIQUE INDEX IF NOT EXISTS uk_demandes_transfert_en_attente ON public.demandes_transfert (eleve_id) WHERE statut = 'EN_ATTENTE';
