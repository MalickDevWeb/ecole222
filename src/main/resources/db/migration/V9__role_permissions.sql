-- ============================================================
-- V9 : Role Permissions
-- ============================================================

CREATE TABLE IF NOT EXISTS role_permissions (
    id VARCHAR(255) PRIMARY KEY,
    role_id VARCHAR(255) NOT NULL, -- Peut être un SystemRole ou CustomRole
    permission_id VARCHAR(255) NOT NULL,
    conditions JSONB,
    
    CONSTRAINT fk_role_perm_permission FOREIGN KEY (permission_id) REFERENCES permissions(id) ON DELETE CASCADE
);

CREATE INDEX idx_role_permissions_role_id ON role_permissions(role_id);
CREATE INDEX idx_role_permissions_perm_id ON role_permissions(permission_id);
