-- V24 — Archivage des écoles (décisions du Tech Lead du 16/11 ; étude docs/architecture/archivage-des-ecoles.md, § 10).
--
-- Une école SUSPENDUE depuis le seuil (30 jours par défaut) est archivée : ses données métier partent dans un fichier
-- chiffré (stockage local en développement, Cloudinary en production), puis sont retirées de la base. La ligne
-- organizations reste (pierre tombale, statut ARCHIVED) ; comptes, audit, domaines et transferts restent aussi.
--
-- 1. archivages_ecoles : une ligne par archivage. Elle porte le cycle de vie fin (ARCHIVAGE_EN_COURS, ARCHIVEE,
--    SUPPRESSION_A_CONFIRMER, RESTAURATION_EN_COURS, RESTAUREE, SUPPRIMEE_DEFINITIVEMENT, ECHEC). Le statut de l'école
--    (organizations.status) ne change pas de liste de valeurs : ARCHIVED pendant tout le cycle, ce qui ferme déjà
--    l'école partout (403, connexion et WebSocket refusés).
--    Une seule opération vivante par école (index unique partiel) : un double clic ou deux super-admins ne lancent
--    jamais deux archivages ni un archivage et une restauration croisés.
--    La clé de l'archive est rangée ICI, chiffrée par la clé maîtresse (ARCHIVES_CLE, jamais en base ni avec les
--    archives) : l'effacer rend l'archive illisible, même s'il en reste une copie.
-- 2. archivages_ecoles_journal : historique de chaque étape, jamais purgé (le journal d'audit n'est conservé que
--    30 minutes, V14).
-- 3. Réglages modifiables par le super-administrateur (bornes contrôlées par l'application) : seuil de suspension
--    avant archivage et durée de conservation d'une archive. Format intervalle PostgreSQL, comme audit.conservation.
--
-- Pas de clé étrangère vers organizations : la trace doit survivre à tout (pierre tombale).

CREATE TABLE IF NOT EXISTS public.archivages_ecoles (
    id character varying(64) PRIMARY KEY,
    organization_id character varying(255) NOT NULL,
    ecole_nom character varying(255),
    ecole_slug character varying(255),
    etat character varying(40) NOT NULL,
    etape character varying(40),
    etat_avant_restauration character varying(40),
    declenchement character varying(20) NOT NULL,
    statut_initial character varying(40) NOT NULL,
    suspendue_le timestamp(6) without time zone,
    motif character varying(1000),
    demande_par character varying(255),
    demande_par_nom character varying(255),
    demande_le timestamp(6) without time zone NOT NULL,
    archivee_le timestamp(6) without time zone,
    conservation_jours integer,
    conservation_jusqu_au timestamp(6) without time zone,
    a_confirmer_depuis timestamp(6) without time zone,
    restauration_demandee_par character varying(255),
    restauration_demandee_le timestamp(6) without time zone,
    restauree_le timestamp(6) without time zone,
    supprimee_le timestamp(6) without time zone,
    supprimee_par character varying(255),
    stockage character varying(30),
    emplacement character varying(1000),
    fichier_a_supprimer boolean NOT NULL DEFAULT false,
    empreinte_sha256 character varying(64),
    taille_octets bigint,
    version_schema character varying(50),
    cle_enveloppee text,
    cle_id character varying(64),
    compteurs jsonb,
    nombre_lignes bigint,
    nombre_medias integer,
    purge jsonb,
    restes jsonb,
    conflits jsonb,
    erreur character varying(2000),
    tentatives integer NOT NULL DEFAULT 0,
    maj_le timestamp(6) without time zone NOT NULL DEFAULT localtimestamp,
    CONSTRAINT ck_archivages_ecoles_etat CHECK (etat IN ('ARCHIVAGE_EN_COURS', 'ARCHIVEE', 'SUPPRESSION_A_CONFIRMER',
        'RESTAURATION_EN_COURS', 'RESTAUREE', 'SUPPRIMEE_DEFINITIVEMENT', 'ECHEC')),
    CONSTRAINT ck_archivages_ecoles_declenchement CHECK (declenchement IN ('MANUEL', 'AUTOMATIQUE')),
    CONSTRAINT ck_archivages_ecoles_ecole CHECK (organization_id <> '')
);

-- une seule opération vivante par école
CREATE UNIQUE INDEX IF NOT EXISTS uk_archivages_ecoles_vivant ON public.archivages_ecoles (organization_id)
    WHERE etat IN ('ARCHIVAGE_EN_COURS', 'ARCHIVEE', 'SUPPRESSION_A_CONFIRMER', 'RESTAURATION_EN_COURS');
CREATE INDEX IF NOT EXISTS idx_archivages_ecoles_ecole ON public.archivages_ecoles (organization_id, demande_le);
CREATE INDEX IF NOT EXISTS idx_archivages_ecoles_etat ON public.archivages_ecoles (etat, conservation_jusqu_au);

CREATE TABLE IF NOT EXISTS public.archivages_ecoles_journal (
    id bigserial PRIMARY KEY,
    archivage_id character varying(64) NOT NULL,
    organization_id character varying(255) NOT NULL,
    survenu_le timestamp(6) without time zone NOT NULL DEFAULT localtimestamp,
    evenement character varying(60) NOT NULL,
    etat character varying(40),
    acteur character varying(255),
    detail character varying(2000)
);
CREATE INDEX IF NOT EXISTS idx_archivages_ecoles_journal ON public.archivages_ecoles_journal (archivage_id, survenu_le);
CREATE INDEX IF NOT EXISTS idx_archivages_ecoles_journal_ecole ON public.archivages_ecoles_journal (organization_id, survenu_le);

-- parametres_plateforme est créée par V14 ; recréée ici au besoin (base où V14 aurait été marquée sans s'exécuter)
CREATE TABLE IF NOT EXISTS public.parametres_plateforme (
    cle character varying(100) PRIMARY KEY,
    valeur character varying(255) NOT NULL,
    modifie_le timestamp(6) without time zone NOT NULL DEFAULT localtimestamp,
    modifie_par character varying(255)
);
INSERT INTO public.parametres_plateforme (cle, valeur) VALUES ('ecoles.archivage.delai_suspension', '30 days')
    ON CONFLICT (cle) DO NOTHING;
INSERT INTO public.parametres_plateforme (cle, valeur) VALUES ('ecoles.archive.conservation', '30 days')
    ON CONFLICT (cle) DO NOTHING;
