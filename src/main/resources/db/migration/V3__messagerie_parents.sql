-- V3 — Messagerie parent ↔ école (professeur ou administration).
-- Avant : l'envoi renvoyait un message inventé et « marquer comme lu » ne faisait rien.
-- interlocuteur_id : identifiant du professeur, ou « ADMINISTRATION » (boîte partagée par les administrateurs).
CREATE TABLE IF NOT EXISTS public.messages_parents (
    id character varying(64) NOT NULL PRIMARY KEY,
    organization_id character varying(255) NOT NULL,
    parent_id character varying(255) NOT NULL,
    interlocuteur_id character varying(255) NOT NULL,
    auteur_id character varying(255) NOT NULL,
    de_parent boolean NOT NULL,
    sujet character varying(200) NOT NULL,
    contenu text NOT NULL,
    envoye_le timestamp(6) without time zone NOT NULL,
    lu_le timestamp(6) without time zone,
    repond_a character varying(64)
);
CREATE INDEX IF NOT EXISTS idx_msg_parents_parent ON public.messages_parents (organization_id, parent_id);
CREATE INDEX IF NOT EXISTS idx_msg_parents_interlocuteur ON public.messages_parents (organization_id, interlocuteur_id);
