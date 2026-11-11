-- ============================================================
-- V7 : Custom Roles
-- ============================================================

CREATE TABLE IF NOT EXISTS custom_roles (
    id VARCHAR(255) PRIMARY KEY,
    organization_id VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    permissions TEXT[] NOT NULL,
    is_system_role BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT NOW(),
    
    CONSTRAINT fk_custom_role_org FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE
);

CREATE INDEX idx_custom_roles_org_id ON custom_roles(organization_id);
CREATE INDEX idx_custom_roles_system ON custom_roles(is_system_role);
