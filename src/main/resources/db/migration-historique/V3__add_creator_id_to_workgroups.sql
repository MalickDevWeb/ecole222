-- Ajouter la colonne creator_id à la table workgroups
ALTER TABLE workgroups ADD COLUMN IF NOT EXISTS creator_id VARCHAR(255);