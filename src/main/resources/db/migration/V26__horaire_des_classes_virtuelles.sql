-- V26 — Horaire des classes virtuelles (16/11, demande du Tech Lead).
-- Le professeur fixe une heure de début (le lien apparaît aux élèves et aux parents) et une heure de fin (le lien et la
-- classe virtuelle disparaissent de leur écran). Avant : date et heure en texte libre, facultatives, et lien visible
-- en permanence. Colonnes facultatives pour les séances antérieures : sans horaire, elles ne sont plus montrées aux
-- élèves ni aux parents.
ALTER TABLE public.meets ADD COLUMN IF NOT EXISTS debut timestamp(6) without time zone;
ALTER TABLE public.meets ADD COLUMN IF NOT EXISTS fin timestamp(6) without time zone;
