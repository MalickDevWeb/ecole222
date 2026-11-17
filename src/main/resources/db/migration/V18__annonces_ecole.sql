-- V18 — Annonces et alertes de l'école reçues par les destinataires (décision du Tech Lead du 15/11).
-- Avant : POST /v1/notifications enregistrait une seule ligne « ALL » que personne ne lisait.
-- Désormais une annonce crée UNE notification par compte visé (liste calculée par le serveur) et une entrée
-- d'historique ci-dessous (nombre de comptes touchés, auteur).
--
-- 1. Le message d'une notification peut dépasser 255 caractères (annonce : 5 000 au plus). Avant : character varying(255),
--    un message plus long faisait échouer l'enregistrement.
ALTER TABLE public.notifications ALTER COLUMN message TYPE text;

-- 2. Historique des annonces de chaque école (cloisonné par organization_id, comme les notifications).
CREATE TABLE IF NOT EXISTS public.annonces_ecole (
    id character varying(64) NOT NULL PRIMARY KEY,
    organization_id character varying(255) NOT NULL CHECK (organization_id <> '')
        REFERENCES public.organizations (id) ON DELETE CASCADE,
    titre character varying(200) NOT NULL,
    message text NOT NULL,
    nature character varying(20) NOT NULL CHECK (nature IN ('ANNONCE', 'URGENCE')),
    destinataires character varying(20) NOT NULL
        CHECK (destinataires IN ('TOUTE_ECOLE', 'ELEVES', 'PARENTS', 'PROFESSEURS', 'PERSONNEL')),
    destinataires_touches integer NOT NULL CHECK (destinataires_touches >= 0),
    auteur_id character varying(255),
    auteur character varying(255),
    envoyee_le timestamp(6) without time zone NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_annonces_ecole_organization ON public.annonces_ecole (organization_id, envoyee_le);
-- lecture du nombre de destinataires ayant lu une annonce (notifications.action_type = 'annonce', action_link = id)
CREATE INDEX IF NOT EXISTS idx_notifications_annonce ON public.notifications (action_type, action_link);
