-- V16 — Inscription d'un candidat admis en élève (APP-TL-23), 2026-11-14.
-- Avant : POST /admissions/automation/matricules calculait des matricules sans rien enregistrer ; les admis étaient
-- recréés à la main dans Gestion Élèves, sans lien avec leur dossier ni avec le parent qui l'avait déposé.

-- 1. Matricule unique sur toute la plateforme (BR-02) : il sert d'identifiant de connexion aux élèves sans e-mail.
--    Avant : seule l'application vérifiait l'unicité (deux créations simultanées pouvaient passer).
--    Contrôle préalable : la migration s'arrête si des doublons existent déjà, sans rien modifier.
DO $$
DECLARE
    doublons text;
BEGIN
    SELECT string_agg(m, ', ') INTO doublons FROM (
        SELECT upper(trim(matricule)) AS m FROM public.users
         WHERE matricule IS NOT NULL AND trim(matricule) <> ''
         GROUP BY upper(trim(matricule)) HAVING count(*) > 1
         LIMIT 20) d;
    IF doublons IS NOT NULL THEN
        RAISE EXCEPTION 'V16 : matricules attribués à plusieurs comptes (%). Corrigez-les avant la migration : '
            'SELECT upper(trim(matricule)), count(*) FROM users WHERE matricule IS NOT NULL GROUP BY 1 HAVING count(*) > 1;', doublons;
    END IF;
END $$;
CREATE UNIQUE INDEX IF NOT EXISTS uk_users_matricule ON public.users (upper(trim(matricule)))
    WHERE matricule IS NOT NULL AND trim(matricule) <> '';

-- 2. Identifiant National de l'Élève (INE) : facultatif, saisi par l'école (jamais généré), unique s'il est renseigné.
ALTER TABLE public.users ADD COLUMN IF NOT EXISTS ine character varying(30);
CREATE UNIQUE INDEX IF NOT EXISTS uk_users_ine ON public.users (upper(ine)) WHERE ine IS NOT NULL;

-- 3. Dossier de candidature inscrit : élève créé (ou rattaché), date et auteur. Un élève ne reçoit qu'un dossier.
ALTER TABLE public.candidates ADD COLUMN IF NOT EXISTS eleve_id character varying(255);
ALTER TABLE public.candidates ADD COLUMN IF NOT EXISTS inscrit_le timestamp(6) with time zone;
ALTER TABLE public.candidates ADD COLUMN IF NOT EXISTS inscrit_par character varying(255);
CREATE UNIQUE INDEX IF NOT EXISTS uk_candidates_eleve ON public.candidates (eleve_id) WHERE eleve_id IS NOT NULL;
