-- ============================================================
-- V11 : Add organization_id to all core tables for Multi-Tenancy
-- ============================================================

-- 1. Create a Default Tenant if organizations table is empty
INSERT INTO organizations (id, name, type, status, created_at, updated_at)
SELECT 'default-tenant-id', 'Default School', 'ECOLE', 'ACTIVE', NOW(), NOW()
WHERE NOT EXISTS (SELECT 1 FROM organizations);

-- Let's get a valid organization_id to use as default for existing data
-- We will use a DO block to assign existing data to the first available organization
DO $$
DECLARE
    default_org_id VARCHAR(255);
BEGIN
    -- Get the default organization id (either the one we just created or an existing one)
    SELECT id INTO default_org_id FROM organizations ORDER BY created_at ASC LIMIT 1;

    -- ==========================================
    -- Step 2: Add organization_id column to tables
    -- ==========================================
    
    ALTER TABLE filieres ADD COLUMN IF NOT EXISTS organization_id VARCHAR(255);
    ALTER TABLE classes ADD COLUMN IF NOT EXISTS organization_id VARCHAR(255);
    ALTER TABLE rooms ADD COLUMN IF NOT EXISTS organization_id VARCHAR(255);
    ALTER TABLE slots ADD COLUMN IF NOT EXISTS organization_id VARCHAR(255);
    ALTER TABLE modules ADD COLUMN IF NOT EXISTS organization_id VARCHAR(255);
    ALTER TABLE courses ADD COLUMN IF NOT EXISTS organization_id VARCHAR(255);
    ALTER TABLE lessons ADD COLUMN IF NOT EXISTS organization_id VARCHAR(255);
    ALTER TABLE homeworks ADD COLUMN IF NOT EXISTS organization_id VARCHAR(255);
    ALTER TABLE quizzes ADD COLUMN IF NOT EXISTS organization_id VARCHAR(255);
    ALTER TABLE grades ADD COLUMN IF NOT EXISTS organization_id VARCHAR(255);
    ALTER TABLE attendances ADD COLUMN IF NOT EXISTS organization_id VARCHAR(255);
    ALTER TABLE workgroups ADD COLUMN IF NOT EXISTS organization_id VARCHAR(255);
    ALTER TABLE messages ADD COLUMN IF NOT EXISTS organization_id VARCHAR(255);
    ALTER TABLE documents ADD COLUMN IF NOT EXISTS organization_id VARCHAR(255);
    ALTER TABLE meets ADD COLUMN IF NOT EXISTS organization_id VARCHAR(255);
    ALTER TABLE live_chat_messages ADD COLUMN IF NOT EXISTS organization_id VARCHAR(255);

    -- ==========================================
    -- Step 3: Update existing data
    -- ==========================================
    
    UPDATE filieres SET organization_id = default_org_id WHERE organization_id IS NULL;
    UPDATE classes SET organization_id = default_org_id WHERE organization_id IS NULL;
    UPDATE rooms SET organization_id = default_org_id WHERE organization_id IS NULL;
    UPDATE slots SET organization_id = default_org_id WHERE organization_id IS NULL;
    UPDATE modules SET organization_id = default_org_id WHERE organization_id IS NULL;
    UPDATE courses SET organization_id = default_org_id WHERE organization_id IS NULL;
    UPDATE lessons SET organization_id = default_org_id WHERE organization_id IS NULL;
    UPDATE homeworks SET organization_id = default_org_id WHERE organization_id IS NULL;
    UPDATE quizzes SET organization_id = default_org_id WHERE organization_id IS NULL;
    UPDATE grades SET organization_id = default_org_id WHERE organization_id IS NULL;
    UPDATE attendances SET organization_id = default_org_id WHERE organization_id IS NULL;
    UPDATE workgroups SET organization_id = default_org_id WHERE organization_id IS NULL;
    UPDATE messages SET organization_id = default_org_id WHERE organization_id IS NULL;
    UPDATE documents SET organization_id = default_org_id WHERE organization_id IS NULL;
    UPDATE meets SET organization_id = default_org_id WHERE organization_id IS NULL;
    UPDATE live_chat_messages SET organization_id = default_org_id WHERE organization_id IS NULL;

    -- ==========================================
    -- Step 4: Alter columns to SET NOT NULL
    -- ==========================================
    
    ALTER TABLE filieres ALTER COLUMN organization_id SET NOT NULL;
    ALTER TABLE classes ALTER COLUMN organization_id SET NOT NULL;
    ALTER TABLE rooms ALTER COLUMN organization_id SET NOT NULL;
    ALTER TABLE slots ALTER COLUMN organization_id SET NOT NULL;
    ALTER TABLE modules ALTER COLUMN organization_id SET NOT NULL;
    ALTER TABLE courses ALTER COLUMN organization_id SET NOT NULL;
    ALTER TABLE lessons ALTER COLUMN organization_id SET NOT NULL;
    ALTER TABLE homeworks ALTER COLUMN organization_id SET NOT NULL;
    ALTER TABLE quizzes ALTER COLUMN organization_id SET NOT NULL;
    ALTER TABLE grades ALTER COLUMN organization_id SET NOT NULL;
    ALTER TABLE attendances ALTER COLUMN organization_id SET NOT NULL;
    ALTER TABLE workgroups ALTER COLUMN organization_id SET NOT NULL;
    ALTER TABLE messages ALTER COLUMN organization_id SET NOT NULL;
    ALTER TABLE documents ALTER COLUMN organization_id SET NOT NULL;
    ALTER TABLE meets ALTER COLUMN organization_id SET NOT NULL;
    ALTER TABLE live_chat_messages ALTER COLUMN organization_id SET NOT NULL;

END $$;

-- ==========================================
-- Step 5: Add Foreign Key Constraints & Indexes
-- ==========================================

ALTER TABLE filieres ADD CONSTRAINT fk_filieres_org FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE;
ALTER TABLE classes ADD CONSTRAINT fk_classes_org FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE;
ALTER TABLE rooms ADD CONSTRAINT fk_rooms_org FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE;
ALTER TABLE slots ADD CONSTRAINT fk_slots_org FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE;
ALTER TABLE modules ADD CONSTRAINT fk_modules_org FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE;
ALTER TABLE courses ADD CONSTRAINT fk_courses_org FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE;
ALTER TABLE lessons ADD CONSTRAINT fk_lessons_org FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE;
ALTER TABLE homeworks ADD CONSTRAINT fk_homeworks_org FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE;
ALTER TABLE quizzes ADD CONSTRAINT fk_quizzes_org FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE;
ALTER TABLE grades ADD CONSTRAINT fk_grades_org FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE;
ALTER TABLE attendances ADD CONSTRAINT fk_attendances_org FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE;
ALTER TABLE workgroups ADD CONSTRAINT fk_workgroups_org FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE;
ALTER TABLE messages ADD CONSTRAINT fk_messages_org FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE;
ALTER TABLE documents ADD CONSTRAINT fk_documents_org FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE;
ALTER TABLE meets ADD CONSTRAINT fk_meets_org FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE;
ALTER TABLE live_chat_messages ADD CONSTRAINT fk_live_chat_msgs_org FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE;

CREATE INDEX idx_filieres_org ON filieres(organization_id);
CREATE INDEX idx_classes_org ON classes(organization_id);
CREATE INDEX idx_rooms_org ON rooms(organization_id);
CREATE INDEX idx_slots_org ON slots(organization_id);
CREATE INDEX idx_modules_org ON modules(organization_id);
CREATE INDEX idx_courses_org ON courses(organization_id);
CREATE INDEX idx_grades_org ON grades(organization_id);
CREATE INDEX idx_workgroups_org ON workgroups(organization_id);
CREATE INDEX idx_meets_org ON meets(organization_id);

