-- ============================================================
-- V1 : Schéma initial Ecole 221
-- ============================================================

-- Utilisateurs (auth)
CREATE TABLE IF NOT EXISTS utilisateurs (
    id          VARCHAR(255) PRIMARY KEY,
    nom         VARCHAR(255),
    prenom      VARCHAR(255),
    email       VARCHAR(255) UNIQUE NOT NULL,
    password    VARCHAR(255),
    role        VARCHAR(50),
    photo_url   VARCHAR(500)
);

-- Filières
CREATE TABLE IF NOT EXISTS filieres (
    id          VARCHAR(255) PRIMARY KEY,
    nom         VARCHAR(255) NOT NULL,
    code        VARCHAR(50),
    description TEXT
);

-- Classes
CREATE TABLE IF NOT EXISTS classes (
    id          VARCHAR(255) PRIMARY KEY,
    nom         VARCHAR(255) NOT NULL,
    niveau      VARCHAR(50),
    filiere_id  VARCHAR(255) REFERENCES filieres(id)
);

-- Salles
CREATE TABLE IF NOT EXISTS rooms (
    id          VARCHAR(255) PRIMARY KEY,
    nom         VARCHAR(255) NOT NULL,
    capacite    INTEGER,
    type        VARCHAR(50)
);

-- Créneaux planning
CREATE TABLE IF NOT EXISTS slots (
    id              VARCHAR(255) PRIMARY KEY,
    jour            VARCHAR(20),
    heure_debut     VARCHAR(10),
    heure_fin       VARCHAR(10),
    nom             VARCHAR(255),
    type            VARCHAR(50),
    professeur      VARCHAR(255),
    salle           VARCHAR(255),
    description     TEXT,
    classe_id       VARCHAR(255) REFERENCES classes(id)
);

-- Modules de cours
CREATE TABLE IF NOT EXISTS modules (
    id              VARCHAR(255) PRIMARY KEY,
    titre           VARCHAR(255) NOT NULL,
    description     TEXT,
    professeur_id   VARCHAR(255) REFERENCES utilisateurs(id),
    classe_id       VARCHAR(255) REFERENCES classes(id)
);

-- Cours (leçons vidéo/documents)
CREATE TABLE IF NOT EXISTS courses (
    id              VARCHAR(255) PRIMARY KEY,
    titre           VARCHAR(255) NOT NULL,
    description     TEXT,
    type            VARCHAR(50),
    url             VARCHAR(500),
    module_id       VARCHAR(255) REFERENCES modules(id),
    date_creation   TIMESTAMP DEFAULT NOW()
);

-- Leçons
CREATE TABLE IF NOT EXISTS lessons (
    id              VARCHAR(255) PRIMARY KEY,
    titre           VARCHAR(255),
    contenu         TEXT,
    module_id       VARCHAR(255) REFERENCES modules(id),
    ordre           INTEGER DEFAULT 0,
    date_creation   TIMESTAMP DEFAULT NOW()
);

-- Devoirs
CREATE TABLE IF NOT EXISTS homeworks (
    id              VARCHAR(255) PRIMARY KEY,
    titre           VARCHAR(255),
    description     TEXT,
    date_limite     TIMESTAMP,
    module_id       VARCHAR(255) REFERENCES modules(id),
    date_creation   TIMESTAMP DEFAULT NOW()
);

-- Quiz
CREATE TABLE IF NOT EXISTS quizzes (
    id              VARCHAR(255) PRIMARY KEY,
    titre           VARCHAR(255),
    questions       TEXT,
    module_id       VARCHAR(255) REFERENCES modules(id),
    date_creation   TIMESTAMP DEFAULT NOW()
);

-- Notes
CREATE TABLE IF NOT EXISTS grades (
    id              VARCHAR(255) PRIMARY KEY,
    valeur          DOUBLE PRECISION,
    commentaire     TEXT,
    etudiant_id     VARCHAR(255) REFERENCES utilisateurs(id),
    module_id       VARCHAR(255) REFERENCES modules(id),
    date_creation   TIMESTAMP DEFAULT NOW()
);

-- Présences
CREATE TABLE IF NOT EXISTS attendances (
    id              VARCHAR(255) PRIMARY KEY,
    present         BOOLEAN DEFAULT FALSE,
    date_seance     TIMESTAMP,
    etudiant_id     VARCHAR(255) REFERENCES utilisateurs(id),
    slot_id         VARCHAR(255) REFERENCES slots(id)
);

-- Groupes de travail (collaboration)
CREATE TABLE IF NOT EXISTS workgroups (
    id          VARCHAR(255) PRIMARY KEY,
    nom         VARCHAR(255),
    description TEXT,
    createur_id VARCHAR(255) REFERENCES utilisateurs(id)
);

-- Membres des groupes (table de jointure)
CREATE TABLE IF NOT EXISTS workgroup_membres (
    workgroup_id    VARCHAR(255) REFERENCES workgroups(id),
    user_id         VARCHAR(255) REFERENCES utilisateurs(id),
    PRIMARY KEY (workgroup_id, user_id)
);

-- Messages de chat collaboration
CREATE TABLE IF NOT EXISTS messages (
    id              VARCHAR(255) PRIMARY KEY,
    contenu         TEXT,
    auteur_id       VARCHAR(255) REFERENCES utilisateurs(id),
    workgroup_id    VARCHAR(255) REFERENCES workgroups(id),
    date_envoi      TIMESTAMP DEFAULT NOW()
);

-- Documents partagés
CREATE TABLE IF NOT EXISTS documents (
    id              VARCHAR(255) PRIMARY KEY,
    nom             VARCHAR(255),
    url             VARCHAR(500),
    type            VARCHAR(50),
    workgroup_id    VARCHAR(255) REFERENCES workgroups(id),
    date_upload     TIMESTAMP DEFAULT NOW()
);

-- Sessions Meet (vidéo conférence)
CREATE TABLE IF NOT EXISTS meets (
    id              VARCHAR(255) PRIMARY KEY,
    titre           VARCHAR(255),
    url             VARCHAR(500),
    professeur_id   VARCHAR(255) REFERENCES utilisateurs(id),
    date_debut      TIMESTAMP,
    actif           BOOLEAN DEFAULT FALSE
);

-- Messages chat cours en direct
CREATE TABLE IF NOT EXISTS live_chat_messages (
    id              VARCHAR(255) PRIMARY KEY,
    contenu         TEXT,
    auteur_id       VARCHAR(255) REFERENCES utilisateurs(id),
    meet_id         VARCHAR(255) REFERENCES meets(id),
    date_envoi      TIMESTAMP DEFAULT NOW()
);
