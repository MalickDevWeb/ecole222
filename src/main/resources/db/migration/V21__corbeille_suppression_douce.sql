-- V21 — Corbeille : suppression douce des contenus pédagogiques et de vie scolaire (décision du Tech Lead du 15/11).
-- « Si un professeur supprime une chose, elle est supprimée pour l'élève et pour le professeur, mais il pourra la
-- restaurer » ; « pour les choses critiques, on doit savoir qui l'a fait, quand ».
--
-- Trois colonnes par table : supprime_le (NULL = visible), supprime_par (identifiant du compte) et supprime_libelle
-- (libellé lisible conservé, ex. « Devoir “Exercices chapitre 3” — Algorithmique (L1 Info) »). Les entités portent
-- @SQLRestriction("supprime_le is null") : une ligne supprimée n'est plus lue par aucune requête JPA (élève, parent,
-- professeur, administration, exports, comptages). La corbeille lit, restaure et purge ces lignes en SQL, toujours
-- filtrée par organization_id. Purge définitive automatique 30 jours après la suppression.
--
-- Ajout de colonnes seulement : aucune ligne n'est modifiée, le schéma reste valide pour les entités antérieures.

ALTER TABLE homeworks          ADD COLUMN IF NOT EXISTS supprime_le timestamp(6), ADD COLUMN IF NOT EXISTS supprime_par varchar(255), ADD COLUMN IF NOT EXISTS supprime_libelle varchar(255);
ALTER TABLE lessons            ADD COLUMN IF NOT EXISTS supprime_le timestamp(6), ADD COLUMN IF NOT EXISTS supprime_par varchar(255), ADD COLUMN IF NOT EXISTS supprime_libelle varchar(255);
ALTER TABLE modules            ADD COLUMN IF NOT EXISTS supprime_le timestamp(6), ADD COLUMN IF NOT EXISTS supprime_par varchar(255), ADD COLUMN IF NOT EXISTS supprime_libelle varchar(255);
ALTER TABLE quizzes            ADD COLUMN IF NOT EXISTS supprime_le timestamp(6), ADD COLUMN IF NOT EXISTS supprime_par varchar(255), ADD COLUMN IF NOT EXISTS supprime_libelle varchar(255);
ALTER TABLE reminders          ADD COLUMN IF NOT EXISTS supprime_le timestamp(6), ADD COLUMN IF NOT EXISTS supprime_par varchar(255), ADD COLUMN IF NOT EXISTS supprime_libelle varchar(255);
ALTER TABLE evaluations_cours  ADD COLUMN IF NOT EXISTS supprime_le timestamp(6), ADD COLUMN IF NOT EXISTS supprime_par varchar(255), ADD COLUMN IF NOT EXISTS supprime_libelle varchar(255);
ALTER TABLE slots              ADD COLUMN IF NOT EXISTS supprime_le timestamp(6), ADD COLUMN IF NOT EXISTS supprime_par varchar(255), ADD COLUMN IF NOT EXISTS supprime_libelle varchar(255);
ALTER TABLE creneaux_planning  ADD COLUMN IF NOT EXISTS supprime_le timestamp(6), ADD COLUMN IF NOT EXISTS supprime_par varchar(255), ADD COLUMN IF NOT EXISTS supprime_libelle varchar(255);
ALTER TABLE rooms              ADD COLUMN IF NOT EXISTS supprime_le timestamp(6), ADD COLUMN IF NOT EXISTS supprime_par varchar(255), ADD COLUMN IF NOT EXISTS supprime_libelle varchar(255);
ALTER TABLE filieres           ADD COLUMN IF NOT EXISTS supprime_le timestamp(6), ADD COLUMN IF NOT EXISTS supprime_par varchar(255), ADD COLUMN IF NOT EXISTS supprime_libelle varchar(255);
ALTER TABLE classes            ADD COLUMN IF NOT EXISTS supprime_le timestamp(6), ADD COLUMN IF NOT EXISTS supprime_par varchar(255), ADD COLUMN IF NOT EXISTS supprime_libelle varchar(255);
ALTER TABLE messages           ADD COLUMN IF NOT EXISTS supprime_le timestamp(6), ADD COLUMN IF NOT EXISTS supprime_par varchar(255), ADD COLUMN IF NOT EXISTS supprime_libelle varchar(255);
ALTER TABLE meets              ADD COLUMN IF NOT EXISTS supprime_le timestamp(6), ADD COLUMN IF NOT EXISTS supprime_par varchar(255), ADD COLUMN IF NOT EXISTS supprime_libelle varchar(255);
ALTER TABLE reference_values   ADD COLUMN IF NOT EXISTS supprime_le timestamp(6), ADD COLUMN IF NOT EXISTS supprime_par varchar(255), ADD COLUMN IF NOT EXISTS supprime_libelle varchar(255);

-- Lecture de la corbeille (par école) et purge à 30 jours : index partiels, limités aux lignes supprimées.
CREATE INDEX IF NOT EXISTS idx_homeworks_corbeille         ON homeworks         (organization_id, supprime_le) WHERE supprime_le IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_lessons_corbeille           ON lessons           (organization_id, supprime_le) WHERE supprime_le IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_modules_corbeille           ON modules           (organization_id, supprime_le) WHERE supprime_le IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_quizzes_corbeille           ON quizzes           (organization_id, supprime_le) WHERE supprime_le IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_reminders_corbeille         ON reminders         (organization_id, supprime_le) WHERE supprime_le IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_evaluations_cours_corbeille ON evaluations_cours (organization_id, supprime_le) WHERE supprime_le IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_slots_corbeille             ON slots             (organization_id, supprime_le) WHERE supprime_le IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_creneaux_planning_corbeille ON creneaux_planning (organization_id, supprime_le) WHERE supprime_le IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_rooms_corbeille             ON rooms             (organization_id, supprime_le) WHERE supprime_le IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_filieres_corbeille          ON filieres          (organization_id, supprime_le) WHERE supprime_le IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_classes_corbeille           ON classes           (organization_id, supprime_le) WHERE supprime_le IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_messages_corbeille          ON messages          (organization_id, supprime_le) WHERE supprime_le IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_meets_corbeille             ON meets             (organization_id, supprime_le) WHERE supprime_le IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_reference_values_corbeille  ON reference_values  (organization_id, supprime_le) WHERE supprime_le IS NOT NULL;
