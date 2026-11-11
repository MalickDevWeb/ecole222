-- V6 — Aucune donnée sans école (APP-ARC-07, anomalie A1).
-- Sans école dans le contexte, HibernateTenantIdentifierResolver renvoie '' : les lectures ne trouvent rien,
-- mais une INSERTION passait la contrainte NOT NULL avec organization_id = '' (donnée rattachée à aucune école).
-- Chaque table portant organization_id reçoit CHECK (organization_id <> '').
-- NOT VALID : appliquée à toute nouvelle insertion ou modification, sans faire échouer la migration si une base
-- existante contenait déjà de telles lignes (aucune en QA après la suite complète, vérifié le 2026-11-11).
DO $$
DECLARE
    r record;
BEGIN
    FOR r IN
        SELECT c.table_name
        FROM information_schema.columns c
        JOIN information_schema.tables t ON t.table_schema = c.table_schema AND t.table_name = c.table_name
        WHERE c.table_schema = 'public' AND c.column_name = 'organization_id' AND t.table_type = 'BASE TABLE'
    LOOP
        IF NOT EXISTS (
            SELECT 1 FROM pg_constraint
            WHERE conname = 'ck_' || r.table_name || '_ecole' AND conrelid = format('public.%I', r.table_name)::regclass
        ) THEN
            EXECUTE format('ALTER TABLE public.%I ADD CONSTRAINT %I CHECK (organization_id <> '''') NOT VALID',
                           r.table_name, 'ck_' || r.table_name || '_ecole');
        END IF;
    END LOOP;
END $$;
