-- V9 — Terminaux d'accès (bornes, lecteurs de portique) de chaque école (APP-BE-21).
-- Avant : l'écran « Gestion des bornes » n'avait aucune source (liste vide, boutons Activer / Révoquer sans effet),
-- l'affectation d'un vigile à une porte proposait trois portes écrites dans le code et une seule affectation était
-- gardée par école.
-- Choix (à valider par le Tech Lead) : le terminal n'a pas de secret propre ; les scans viennent de la session du
-- vigile qui lui est affecté. Désactiver ou révoquer un terminal bloque les scans de ce vigile.
CREATE TABLE IF NOT EXISTS public.terminaux_acces (
    id character varying(64) NOT NULL PRIMARY KEY,
    organization_id character varying(255) NOT NULL CHECK (organization_id <> ''),
    nom character varying(100) NOT NULL,
    emplacement character varying(200),
    statut character varying(20) NOT NULL CHECK (statut IN ('Actif', 'Inactif', 'Révoqué')),
    vigile_id character varying(255),
    cree_le timestamp(6) without time zone NOT NULL,
    modifie_le timestamp(6) without time zone NOT NULL,
    revoque_le timestamp(6) without time zone,
    CONSTRAINT uk_terminaux_acces_nom UNIQUE (organization_id, nom)
);
CREATE INDEX IF NOT EXISTS idx_terminaux_acces_vigile ON public.terminaux_acces (organization_id, vigile_id);
