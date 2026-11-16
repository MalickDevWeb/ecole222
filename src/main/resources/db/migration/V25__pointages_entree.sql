-- V25 — Pointages d'arrivée et de départ (16/11).
-- L'élève ou le professeur scanne le QR d'entrée affiché par le vigile (signé par le serveur, renouvelé chaque minute,
-- propre à l'école) ; le serveur vérifie ce QR et enregistre l'arrivée ou le départ.
-- Avant : le portail élève envoyait « arrivée »/« départ » à POST /student/attendances, qui le refusait toujours (400),
-- et le portail professeur n'envoyait rien ; tout QR scanné était accepté par l'écran.
CREATE TABLE IF NOT EXISTS public.pointages (
    id character varying(64) NOT NULL PRIMARY KEY,
    organization_id character varying(255) NOT NULL CHECK (organization_id <> ''),
    utilisateur_id character varying(255) NOT NULL,
    nom character varying(200),
    role character varying(40) NOT NULL,
    type character varying(10) NOT NULL CHECK (type IN ('ARRIVEE', 'DEPART')),
    horodatage timestamp(6) without time zone NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_pointages_utilisateur ON public.pointages (organization_id, utilisateur_id, horodatage DESC);
