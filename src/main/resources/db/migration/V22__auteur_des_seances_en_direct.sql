-- V22 — Séances en direct : auteur et cours (2026-11-16).
-- Avant : la table meets ne gardait pas qui avait lancé la séance ; n'importe quel professeur de l'école pouvait arrêter
-- (mettre à la corbeille) la séance d'un collègue. Désormais l'arrêt est réservé à l'auteur, à l'enseignant du cours et à
-- l'administration de l'école.
--
-- auteur_id : compte qui a lancé ou planifié la séance (NULL pour les séances antérieures à cette migration : pour elles,
--             l'enseignant du cours de même intitulé garde le droit de l'arrêter) ;
-- course_id : cours de l'enseignant auquel la séance est rattachée, quand il est identifiable sans ambiguïté.
--
-- Ajout de colonnes facultatives seulement : aucune ligne n'est modifiée. IF EXISTS / IF NOT EXISTS : la base de
-- développement créée par Hibernate puis marquée V1 peut déjà avoir (ou ne pas avoir) ces colonnes.

ALTER TABLE IF EXISTS meets ADD COLUMN IF NOT EXISTS auteur_id varchar(255);
ALTER TABLE IF EXISTS meets ADD COLUMN IF NOT EXISTS course_id varchar(255);
