-- ============================================================
-- V8 : Permissions System
-- ============================================================

CREATE TABLE IF NOT EXISTS permissions (
    id VARCHAR(255) PRIMARY KEY,
    name VARCHAR(255) UNIQUE NOT NULL,
    description TEXT,
    category VARCHAR(50) NOT NULL,
    resource VARCHAR(100) NOT NULL,
    action VARCHAR(50) NOT NULL,
    supports_scope BOOLEAN DEFAULT FALSE,
    
    CONSTRAINT chk_permission_category CHECK (category IN ('USERS', 'STUDENTS', 'TEACHERS', 'ACADEMIC', 'FINANCE', 'ADMIN', 'REPORTS', 'SETTINGS'))
);

CREATE INDEX idx_permissions_category ON permissions(category);
CREATE INDEX idx_permissions_resource ON permissions(resource);

-- Insérer les permissions par défaut
INSERT INTO permissions (id, name, description, category, resource, action, supports_scope) VALUES
-- Users
('perm-001', 'users.view', 'Voir les utilisateurs', 'USERS', 'users', 'view', FALSE),
('perm-002', 'users.create', 'Créer des utilisateurs', 'USERS', 'users', 'create', FALSE),
('perm-003', 'users.update', 'Modifier les utilisateurs', 'USERS', 'users', 'update', FALSE),
('perm-004', 'users.delete', 'Supprimer les utilisateurs', 'USERS', 'users', 'delete', FALSE),
('perm-005', 'users.manage', 'Gérer les utilisateurs', 'USERS', 'users', 'manage', FALSE),

-- Students
('perm-006', 'students.view', 'Voir les étudiants', 'STUDENTS', 'students', 'view', TRUE),
('perm-007', 'students.create', 'Créer des étudiants', 'STUDENTS', 'students', 'create', TRUE),
('perm-008', 'students.update', 'Modifier les étudiants', 'STUDENTS', 'students', 'update', TRUE),
('perm-009', 'students.delete', 'Supprimer des étudiants', 'STUDENTS', 'students', 'delete', TRUE),
('perm-010', 'students.grades.view', 'Voir les notes des étudiants', 'STUDENTS', 'students.grades', 'view', TRUE),
('perm-011', 'students.attendance.view', 'Voir les présences des étudiants', 'STUDENTS', 'students.attendance', 'view', TRUE),

-- Teachers
('perm-012', 'teachers.view', 'Voir les professeurs', 'TEACHERS', 'teachers', 'view', TRUE),
('perm-013', 'teachers.create', 'Créer des professeurs', 'TEACHERS', 'teachers', 'create', TRUE),
('perm-014', 'teachers.update', 'Modifier les professeurs', 'TEACHERS', 'teachers', 'update', TRUE),
('perm-015', 'teachers.delete', 'Supprimer des professeurs', 'TEACHERS', 'teachers', 'delete', TRUE),
('perm-016', 'teachers.assign', 'Assigner des cours aux professeurs', 'TEACHERS', 'teachers', 'assign', TRUE),

-- Academic
('perm-017', 'courses.view', 'Voir les cours', 'ACADEMIC', 'courses', 'view', TRUE),
('perm-018', 'courses.create', 'Créer des cours', 'ACADEMIC', 'courses', 'create', TRUE),
('perm-019', 'courses.update', 'Modifier les cours', 'ACADEMIC', 'courses', 'update', TRUE),
('perm-020', 'courses.delete', 'Supprimer des cours', 'ACADEMIC', 'courses', 'delete', TRUE),
('perm-021', 'timetables.view', 'Voir les emplois du temps', 'ACADEMIC', 'timetables', 'view', TRUE),
('perm-022', 'timetables.create', 'Créer des emplois du temps', 'ACADEMIC', 'timetables', 'create', TRUE),
('perm-023', 'timetables.update', 'Modifier les emplois du temps', 'ACADEMIC', 'timetables', 'update', TRUE),
('perm-024', 'formations.view', 'Voir les formations', 'ACADEMIC', 'formations', 'view', TRUE),
('perm-025', 'formations.create', 'Créer des formations', 'ACADEMIC', 'formations', 'create', TRUE),
('perm-026', 'formations.update', 'Modifier les formations', 'ACADEMIC', 'formations', 'update', TRUE),

-- Finance
('perm-027', 'finance.view', 'Voir les finances', 'FINANCE', 'finance', 'view', FALSE),
('perm-028', 'finance.payments.view', 'Voir les paiements', 'FINANCE', 'finance.payments', 'view', TRUE),
('perm-029', 'finance.payments.create', 'Créer des paiements', 'FINANCE', 'finance.payments', 'create', TRUE),
('perm-030', 'finance.invoices.view', 'Voir les factures', 'FINANCE', 'finance.invoices', 'view', TRUE),
('perm-031', 'finance.expenses.view', 'Voir les dépenses', 'FINANCE', 'finance.expenses', 'view', TRUE),
('perm-032', 'finance.expenses.create', 'Créer des dépenses', 'FINANCE', 'finance.expenses', 'create', TRUE),

-- Admin
('perm-033', 'roles.manage', 'Gérer les rôles', 'ADMIN', 'roles', 'manage', FALSE),
('perm-034', 'permissions.manage', 'Gérer les permissions', 'ADMIN', 'permissions', 'manage', FALSE),
('perm-035', 'settings.manage', 'Gérer les paramètres', 'ADMIN', 'settings', 'manage', FALSE),
('perm-036', 'members.manage', 'Gérer les membres', 'ADMIN', 'members', 'manage', FALSE),

-- Reports
('perm-037', 'reports.view', 'Voir les rapports', 'REPORTS', 'reports', 'view', FALSE),
('perm-038', 'reports.students', 'Rapports étudiants', 'REPORTS', 'reports.students', 'view', TRUE),
('perm-039', 'reports.finance', 'Rapports financiers', 'REPORTS', 'reports.finance', 'view', FALSE),
('perm-040', 'reports.attendance', 'Rapports de présence', 'REPORTS', 'reports.attendance', 'view', TRUE);
