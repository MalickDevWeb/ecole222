-- ============================================================
-- V10 : Delegations
-- ============================================================

CREATE TABLE IF NOT EXISTS delegations (
    id VARCHAR(255) PRIMARY KEY,
    organization_id VARCHAR(255) NOT NULL,
    delegator_id VARCHAR(255) NOT NULL,
    delegate_id VARCHAR(255) NOT NULL,
    permissions TEXT[] NOT NULL,
    start_date TIMESTAMP NOT NULL,
    end_date TIMESTAMP,
    scope_type VARCHAR(50),
    scope_id VARCHAR(255),
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT NOW(),
    revoked_at TIMESTAMP,
    
    CONSTRAINT fk_delegation_org FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE,
    CONSTRAINT fk_delegation_delegator FOREIGN KEY (delegator_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_delegation_delegate FOREIGN KEY (delegate_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT chk_delegation_status CHECK (status IN ('ACTIVE', 'EXPIRED', 'REVOKED')),
    CONSTRAINT chk_delegation_scope CHECK (scope_type IN ('ORGANIZATION', 'FACULTY', 'DEPARTMENT', 'FORMATION', 'LEVEL', 'CLASS', 'COURSE'))
);

CREATE INDEX idx_delegations_org_id ON delegations(organization_id);
CREATE INDEX idx_delegations_delegator ON delegations(delegator_id);
CREATE INDEX idx_delegations_delegate ON delegations(delegate_id);
CREATE INDEX idx_delegations_status ON delegations(status);
CREATE INDEX idx_delegations_dates ON delegations(start_date, end_date);
