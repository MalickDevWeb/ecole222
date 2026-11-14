-- V13 — Périodes scolaires (trimestres) et clôture des notes (BR-04), 2026-11-14.
-- BR-04 : « une note … ne peut être modifiée après clôture du trimestre ». Avant : aucun trimestre n'existait ; seul
-- le verrouillage évaluation par évaluation était possible. Une évaluation appartient à la période qui contient sa
-- date ; quand la période est clôturée, ses évaluations et leurs notes ne peuvent plus être créées, modifiées ni
-- supprimées.
CREATE TABLE IF NOT EXISTS public.periodes_scolaires (
    id character varying(64) NOT NULL PRIMARY KEY,
    organization_id character varying(255) NOT NULL CHECK (organization_id <> ''),
    libelle character varying(60) NOT NULL,
    date_debut date NOT NULL,
    date_fin date NOT NULL,
    cloturee boolean NOT NULL DEFAULT false,
    cloturee_le timestamp(6) without time zone,
    cloturee_par character varying(255),
    cree_le timestamp(6) without time zone NOT NULL,
    CONSTRAINT ck_periodes_scolaires_dates CHECK (date_debut <= date_fin),
    CONSTRAINT uk_periodes_scolaires_libelle UNIQUE (organization_id, libelle)
);
CREATE INDEX IF NOT EXISTS idx_periodes_scolaires_dates ON public.periodes_scolaires (organization_id, date_debut, date_fin);
