-- V27 — Groupes WhatsApp de l'école et partages des annonces (FEAT-007, 16/11).
-- Sans API WhatsApp payante : l'école crée ses groupes dans WhatsApp et les déclare ici (lien d'invitation) ; une
-- annonce y est partagée par un lien de partage WhatsApp (l'administrateur choisit le groupe et envoie). Le partage est
-- noté « ouvert dans WhatsApp à … » : l'application ne peut pas savoir si le message a été envoyé.
CREATE TABLE IF NOT EXISTS public.groupes_whatsapp (
    id character varying(64) NOT NULL PRIMARY KEY,
    organization_id character varying(255) NOT NULL CHECK (organization_id <> ''),
    nom character varying(120) NOT NULL,
    public_vise character varying(20) NOT NULL CHECK (public_vise IN ('TOUTE_ECOLE', 'ELEVES', 'PARENTS', 'PROFESSEURS', 'PERSONNEL')),
    classe_id character varying(255),
    classe_nom character varying(200),
    lien character varying(200) NOT NULL,
    actif boolean NOT NULL DEFAULT true,
    cree_le timestamp(6) without time zone NOT NULL,
    cree_par character varying(255),
    CONSTRAINT uk_groupes_whatsapp_lien UNIQUE (organization_id, lien)
);
CREATE TABLE IF NOT EXISTS public.partages_whatsapp (
    id character varying(64) NOT NULL PRIMARY KEY,
    organization_id character varying(255) NOT NULL CHECK (organization_id <> ''),
    groupe_id character varying(64) NOT NULL,
    groupe_nom character varying(120) NOT NULL,
    titre character varying(200) NOT NULL,
    auteur_id character varying(255),
    auteur_nom character varying(200),
    partage_le timestamp(6) without time zone NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_partages_whatsapp_date ON public.partages_whatsapp (organization_id, partage_le DESC);
