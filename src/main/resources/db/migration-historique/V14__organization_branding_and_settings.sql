-- ============================================================
-- V14 : Organization Branding & Settings
-- ============================================================

CREATE TABLE IF NOT EXISTS organization_branding (
    organization_id VARCHAR(255) PRIMARY KEY,
    logo_url VARCHAR(255),
    favicon_url VARCHAR(255),
    primary_color VARCHAR(20) DEFAULT '#0056b3',
    secondary_color VARCHAR(20) DEFAULT '#f8f9fa',
    accent_color VARCHAR(20),
    font_family VARCHAR(100),
    custom_css TEXT,
    
    CONSTRAINT fk_branding_org FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS organization_settings (
    organization_id VARCHAR(255) PRIMARY KEY,
    timezone VARCHAR(50) DEFAULT 'Africa/Dakar',
    date_format VARCHAR(20) DEFAULT 'DD/MM/YYYY',
    time_format VARCHAR(20) DEFAULT '24h',
    language VARCHAR(10) DEFAULT 'fr',
    academic_year_start DATE,
    academic_year_end DATE,
    
    CONSTRAINT fk_settings_org FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS organization_features (
    organization_id VARCHAR(255) PRIMARY KEY,
    enable_parent_portal BOOLEAN DEFAULT TRUE,
    enable_online_payment BOOLEAN DEFAULT FALSE,
    enable_live_classes BOOLEAN DEFAULT FALSE,
    enable_sms_notifications BOOLEAN DEFAULT FALSE,
    
    CONSTRAINT fk_features_org FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE
);

-- Init Default Tenant Branding and Settings
DO $$
DECLARE
    default_org_id VARCHAR(255);
BEGIN
    SELECT id INTO default_org_id FROM organizations ORDER BY created_at ASC LIMIT 1;
    
    IF default_org_id IS NOT NULL THEN
        INSERT INTO organization_branding (organization_id) VALUES (default_org_id) ON CONFLICT DO NOTHING;
        INSERT INTO organization_settings (organization_id) VALUES (default_org_id) ON CONFLICT DO NOTHING;
        INSERT INTO organization_features (organization_id) VALUES (default_org_id) ON CONFLICT DO NOTHING;
    END IF;
END $$;

