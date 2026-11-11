-- ============================================================
-- V12 : RBAC Refactoring (Role + Permission + Scope)
-- ============================================================

-- 1. Rename custom_roles to roles for a unified model
ALTER TABLE custom_roles RENAME TO roles;

-- 2. Modify organization_users to act as role_assignments
-- We need to link it to the roles table instead of hardcoded strings
ALTER TABLE organization_users ADD COLUMN IF NOT EXISTS role_id VARCHAR(255);

-- 3. Add foreign key to roles
ALTER TABLE organization_users 
ADD CONSTRAINT fk_org_users_role 
FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE SET NULL;

-- 4. Create System Roles for the Default Tenant (and others)
-- We will migrate existing string roles to System Roles.
DO $$
DECLARE
    sys_admin_id VARCHAR(255) := 'sys-role-admin';
    sys_prof_id VARCHAR(255) := 'sys-role-prof';
    sys_student_id VARCHAR(255) := 'sys-role-student';
    sys_parent_id VARCHAR(255) := 'sys-role-parent';
    
    default_org_id VARCHAR(255);
BEGIN
    SELECT id INTO default_org_id FROM organizations ORDER BY created_at ASC LIMIT 1;
    
    IF default_org_id IS NOT NULL THEN
        -- Insert System Roles
        INSERT INTO roles (id, organization_id, name, description, is_system_role, permissions) VALUES
        (sys_admin_id, default_org_id, 'Administrator', 'System Administrator', TRUE, '{}'),
        (sys_prof_id, default_org_id, 'Teacher', 'System Teacher', TRUE, '{}'),
        (sys_student_id, default_org_id, 'Student', 'System Student', TRUE, '{}'),
        (sys_parent_id, default_org_id, 'Parent', 'System Parent', TRUE, '{}')
        ON CONFLICT DO NOTHING;
        
        -- Migrate organization_users
        UPDATE organization_users SET role_id = sys_admin_id WHERE role IN ('ADMIN', 'SUPER_ADMIN');
        UPDATE organization_users SET role_id = sys_prof_id WHERE role IN ('PROFESSEUR', 'PROFESSOR', 'TEACHER');
        UPDATE organization_users SET role_id = sys_student_id WHERE role IN ('ETUDIANT', 'STUDENT');
        UPDATE organization_users SET role_id = sys_parent_id WHERE role = 'PARENT';
    END IF;
END $$;

