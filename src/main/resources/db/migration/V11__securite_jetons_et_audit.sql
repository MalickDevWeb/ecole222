-- V11 — Sécurité des sessions et du journal d'audit, 2026-11-13.
--
-- 1. Déconnexion réelle : avant, /auth/logout n'effaçait que le cookie ; le jeton restait valable 24 h pour qui
--    l'avait copié. Le jeton présenté à la déconnexion est désormais inscrit ici (empreinte SHA-256, jamais le jeton
--    lui-même) jusqu'à son expiration, et refusé par l'API et le WebSocket.
CREATE TABLE IF NOT EXISTS public.jetons_revoques (
    empreinte character varying(64) NOT NULL PRIMARY KEY,
    expire_le timestamp(6) with time zone NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_jetons_revoques_expiration ON public.jetons_revoques (expire_le);

-- 2. Changement ou réinitialisation du mot de passe : les jetons émis AVANT ce moment sont refusés (un jeton volé ne
--    survit pas au changement de mot de passe de la victime).
CREATE TABLE IF NOT EXISTS public.invalidations_jetons (
    utilisateur_id character varying(255) NOT NULL PRIMARY KEY,
    valides_apres timestamp(6) with time zone NOT NULL
);

-- 3. Journal d'audit inaltérable (BR-05, ENF-07) : une entrée enregistrée ne peut plus être modifiée ni supprimée,
--    même par l'application (seules les insertions sont admises).
CREATE OR REPLACE FUNCTION public.audit_logs_inalterable() RETURNS trigger
    LANGUAGE plpgsql AS $$
BEGIN
    RAISE EXCEPTION 'Journal d''audit inaltérable : % interdit sur audit_logs', TG_OP
        USING ERRCODE = 'insufficient_privilege';
END;
$$;

DROP TRIGGER IF EXISTS tr_audit_logs_inalterable ON public.audit_logs;
CREATE TRIGGER tr_audit_logs_inalterable
    BEFORE UPDATE OR DELETE ON public.audit_logs
    FOR EACH ROW EXECUTE FUNCTION public.audit_logs_inalterable();

DROP TRIGGER IF EXISTS tr_audit_logs_inalterable_vidage ON public.audit_logs;
CREATE TRIGGER tr_audit_logs_inalterable_vidage
    BEFORE TRUNCATE ON public.audit_logs
    FOR EACH STATEMENT EXECUTE FUNCTION public.audit_logs_inalterable();
