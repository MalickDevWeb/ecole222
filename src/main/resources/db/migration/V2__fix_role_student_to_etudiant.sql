-- ============================================================
-- V2 : Correction des rôles STUDENT -> ETUDIANT
-- ============================================================

-- Mise à jour des rôles STUDENT vers ETUDIANT pour correspondre à l'enum Java
UPDATE utilisateurs 
SET role = 'ETUDIANT' 
WHERE role = 'STUDENT';

-- Mise à jour d'autres rôles potentiels si nécessaire
UPDATE utilisateurs 
SET role = 'PROFESSEUR' 
WHERE role IN ('PROFESSOR', 'TEACHER');

UPDATE utilisateurs 
SET role = 'SECRETAIRE' 
WHERE role = 'SECRETARY';

UPDATE utilisateurs 
SET role = 'COMPTABLE' 
WHERE role = 'ACCOUNTANT';

UPDATE utilisateurs 
SET role = 'VIGIL' 
WHERE role IN ('VIGILE', 'GUARD');

UPDATE utilisateurs 
SET role = 'VISITEUR' 
WHERE role = 'VISITOR';

UPDATE utilisateurs 
SET role = 'SUPER_ADMIN' 
WHERE role IN ('SUPERADMIN', 'SUPER-ADMIN');
