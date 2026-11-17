-- ============================================================
-- V16 : Add slug to organizations for URL routing
-- ============================================================

ALTER TABLE organizations ADD COLUMN IF NOT EXISTS slug VARCHAR(100) UNIQUE;

-- Create an index for faster tenant resolution by slug
CREATE INDEX IF NOT EXISTS idx_organizations_slug ON organizations(slug);

-- Update the default organization created in V11 to be 'Ecole 221'
DO $$
DECLARE
    default_org_id VARCHAR(255);
BEGIN
    SELECT id INTO default_org_id FROM organizations ORDER BY created_at ASC LIMIT 1;
    
    IF default_org_id IS NOT NULL THEN
        -- Force the name to Ecole 221 and the slug to ecole221
        UPDATE organizations 
        SET name = 'Ecole 221', slug = 'ecole221' 
        WHERE id = default_org_id;

        -- Update branding colors for the default tenant
        UPDATE organization_branding
        SET primary_color = '#5B6BF9', secondary_color = '#1E2235'
        WHERE organization_id = default_org_id;
    END IF;
END $$;
