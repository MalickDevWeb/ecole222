-- ============================================================
-- V18 : Processus de Transfert Inter-Ecoles pour les Élèves
-- ============================================================

CREATE TABLE IF NOT EXISTS student_transfers (
    id VARCHAR(255) PRIMARY KEY,
    student_id VARCHAR(255) NOT NULL,
    from_organization_id VARCHAR(255) NOT NULL,
    to_organization_id VARCHAR(255) NOT NULL,
    transfer_date TIMESTAMP NOT NULL,
    reason TEXT,
    academic_dossier_snapshot JSONB,
    
    CONSTRAINT fk_transfer_student FOREIGN KEY (student_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_transfer_from_org FOREIGN KEY (from_organization_id) REFERENCES organizations(id) ON DELETE CASCADE,
    CONSTRAINT fk_transfer_to_org FOREIGN KEY (to_organization_id) REFERENCES organizations(id) ON DELETE CASCADE
);

CREATE INDEX idx_student_transfers_student ON student_transfers(student_id);
CREATE INDEX idx_student_transfers_to_org ON student_transfers(to_organization_id);

-- Add TRANSFERRED status to MembershipStatus if needed, but since it's an enum in Java, 
-- and we store it as VARCHAR in DB, we don't need a schema change unless there's a CHECK constraint.
-- Let's check if organization_users has a CHECK constraint on status:
-- In V12 it's just a VARCHAR.
