# Anciennes migrations (V1 à V19) — archivées le 2026-11-11

Ces scripts ne sont plus chargés par Flyway. Ils ne correspondaient plus au code : Flyway était
désactivé et le schéma était créé par Hibernate (`ddl-auto: update`).

Ils sont remplacés par `db/migration/V1__schema_complet.sql`, généré à partir du schéma réellement
produit par les 73 entités du code (APP-ARC-03). Une base existante sans historique Flyway est
« baselinée » à la version 1 : rien n'y est réexécuté.
