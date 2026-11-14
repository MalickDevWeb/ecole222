-- V14 — Durée de conservation du journal d'audit, paramétrable (décision du Tech Lead, 2026-11-14).
--
-- « Pour l'instant 30 minutes, mais on peut le changer : la durée ne doit pas être figée dans le code. »
--
-- 1. Paramètres de la plateforme, modifiables par le super-administrateur (écran Paramètres › Sécurité), sans
--    redéploiement. Première clé : audit.conservation (intervalle PostgreSQL, ex. « 30 minutes », « 90 days »).
-- 2. Journal d'audit (V11) : la MODIFICATION d'une entrée reste interdite, comme le vidage de la table ; la
--    SUPPRESSION n'est admise que pour une entrée plus ancienne que la durée de conservation (purge). Une entrée
--    récente ne peut être supprimée par personne, même directement dans la base.
--    Horodatage comparé à localtimestamp : serveur d'application et base sur le même fuseau (UTC+0 ici).

CREATE TABLE IF NOT EXISTS public.parametres_plateforme (
    cle character varying(100) PRIMARY KEY,
    valeur character varying(255) NOT NULL,
    modifie_le timestamp(6) without time zone NOT NULL DEFAULT localtimestamp,
    modifie_par character varying(255)
);

INSERT INTO public.parametres_plateforme (cle, valeur) VALUES ('audit.conservation', '30 minutes')
    ON CONFLICT (cle) DO NOTHING;

CREATE OR REPLACE FUNCTION public.audit_logs_inalterable() RETURNS trigger
    LANGUAGE plpgsql AS $$
DECLARE
    conservation interval;
BEGIN
    IF TG_OP = 'DELETE' THEN
        SELECT valeur::interval INTO conservation FROM public.parametres_plateforme WHERE cle = 'audit.conservation';
        IF conservation IS NOT NULL AND OLD.action_timestamp IS NOT NULL
                AND OLD.action_timestamp < localtimestamp - conservation THEN
            RETURN OLD;   -- entrée échue : purge de conservation admise
        END IF;
        RAISE EXCEPTION 'Journal d''audit inaltérable : suppression interdite avant la fin de la durée de conservation (%)',
            coalesce(conservation::text, 'non définie') USING ERRCODE = 'insufficient_privilege';
    END IF;
    RAISE EXCEPTION 'Journal d''audit inaltérable : % interdit sur audit_logs', TG_OP
        USING ERRCODE = 'insufficient_privilege';
END;
$$;
