-- ============================================================
-- V4 : Organizations (Tenants)
-- ============================================================

CREATE TABLE IF NOT EXISTS organizations (
    id VARCHAR(255) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    type VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    
    -- Informations établissement
    country VARCHAR(100),
    city VARCHAR(100),
    address TEXT,
    phone VARCHAR(50),
    email VARCHAR(255),
    website VARCHAR(255),
    logo VARCHAR(500),
    
    -- Configuration
    subscription_id VARCHAR(255),
    primary_domain_id VARCHAR(255),
    settings JSONB,
    
    -- Timestamps
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    validated_at TIMESTAMP,
    suspended_at TIMESTAMP,
    
    CONSTRAINT chk_organization_type CHECK (type IN ('ECOLE', 'UNIVERSITE', 'INSTITUT', 'CENTRE_FORMATION', 'LYCEE', 'COLLEGE')),
    CONSTRAINT chk_organization_status CHECK (status IN ('PENDING', 'ACTIVE', 'SUSPENDED', 'REJECTED', 'EXPIRED', 'CANCELLED'))
);

CREATE INDEX idx_organizations_status ON organizations(status);
CREATE INDEX idx_organizations_type ON organizations(type);
CREATE INDEX idx_organizations_subscription ON organizations(subscription_id);
