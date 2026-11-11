-- ============================================================
-- V6 : Organization Users (Memberships)
-- ============================================================

CREATE TABLE IF NOT EXISTS organization_users (
    id VARCHAR(255) PRIMARY KEY,
    user_id VARCHAR(255) NOT NULL,
    organization_id VARCHAR(255) NOT NULL,
    role VARCHAR(100) NOT NULL,
    permissions TEXT[], -- Array de permissions
    scope_type VARCHAR(50),
    scope_id VARCHAR(255),
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    joined_at TIMESTAMP DEFAULT NOW(),
    expires_at TIMESTAMP,
    is_delegated BOOLEAN DEFAULT FALSE,
    delegation_id VARCHAR(255),
    
    CONSTRAINT fk_org_user_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_org_user_org FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE,
    CONSTRAINT chk_org_user_status CHECK (status IN ('ACTIVE', 'INVITED', 'SUSPENDED', 'EXPIRED')),
    CONSTRAINT chk_scope_type CHECK (scope_type IN ('ORGANIZATION', 'FACULTY', 'DEPARTMENT', 'FORMATION', 'LEVEL', 'CLASS', 'COURSE')),
    CONSTRAINT uq_user_org UNIQUE (user_id, organization_id)
);

CREATE INDEX idx_org_users_user_id ON organization_users(user_id);
CREATE INDEX idx_org_users_org_id ON organization_users(organization_id);
CREATE INDEX idx_org_users_status ON organization_users(status);
CREATE INDEX idx_org_users_role ON organization_users(role);
