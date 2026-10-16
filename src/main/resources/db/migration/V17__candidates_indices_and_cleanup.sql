-- ============================================================
-- V17 : Optimisation & Indexation des Candidatures
-- ============================================================

CREATE INDEX IF NOT EXISTS idx_candidates_org_id ON candidates(organization_id);
CREATE INDEX IF NOT EXISTS idx_candidates_email ON candidates(email);
CREATE INDEX IF NOT EXISTS idx_candidates_step ON candidates(current_step);
CREATE INDEX IF NOT EXISTS idx_candidates_fee_paid ON candidates(registration_fee_paid);

