# Contrat de données backend – École 221

Version : 1.0
Date de publication : 2026-07-23
Rôle : Source de vérité unique entre le frontend et le backend

---

## 1. Règles globales de cohérence

### 1.1 Formats obligatoires
- Dates et heures : format ISO 8601 lorsque possible, par exemple `2026-07-23T09:30:00Z`.
- Dates simples : format `YYYY-MM-DD`.
- Montants : nombres entiers en FCFA, sans devise dans le champ.
- Booléens : `true` / `false`.
- Identifiants : chaînes de caractères stables, préfixées si possible (`usr-`, `stu-`, `course-`, etc.).

### 1.2 Nommage des champs
- Utiliser des noms explicites et cohérents en anglais ou en français selon le contrat déjà défini.
- Privilégier la cohérence sur l’orthographe.
- Éviter les champs dupliqués.

### 1.3 États et statuts
Les statuts doivent être strictement normalisés et ne jamais être librement inventés.

---

## 2. Rôles utilisateur

```ts
export const ROLE = {
  ADMIN: 'ADMIN',
  SUPER_ADMIN: 'SUPER_ADMIN',
  ETUDIANT: 'ETUDIANT',
  PROFESSEUR: 'PROFESSEUR',
  SECRETAIRE: 'SECRETAIRE',
  COMPTABLE: 'COMPTABLE',
  VIGIL: 'VIGIL',
  VISITEUR: 'VISITEUR',
} as const;
```

### Structure utilisateur
```json
{
  "id": "usr-001",
  "nom": "Ndiaye",
  "prenom": "Moussa",
  "email": "moussa.ndiaye@ecole221.sn",
  "role": "ADMIN"
}
```

---

## 3. Authentification

### 3.1 POST /auth/login
#### Requête
```json
{
  "email": "moussa.ndiaye@ecole221.sn",
  "password": "motdepasse"
}
```

#### Réponse
```json
{
  "id": "usr-001",
  "nom": "Ndiaye",
  "prenom": "Moussa",
  "email": "moussa.ndiaye@ecole221.sn",
  "role": "ADMIN",
  "token": null
}
```

#### Règles
- Le token ne doit pas être renvoyé dans le body.
- Il doit être placé dans un cookie HTTP-only côté backend.

---

## 4. Cours

### 4.1 GET /professor/courses
#### Réponse
```json
[
  {
    "id": "course-1",
    "nom": "Architecture des Microservices",
    "code": "MIC-101",
    "classeId": "class-l2-gc",
    "description": "Cours avancé sur les microservices"
  }
]
```

### 4.2 GET /student/courses
#### Réponse
```json
[
  {
    "id": "course-1",
    "titre": "Architecture des Microservices",
    "coefficient": 3,
    "progress": 42,
    "statut": "en_cours",
    "unites": ["Backend", "Architecture"]
  }
]
```

### 4.3 Statuts de cours
- `en_cours`
- `termine`
- `non_demarre`

---

## 5. Étudiants

### 5.1 GET /professor/courses/{id}/students
#### Réponse
```json
[
  {
    "id": "stu-001",
    "nom": "Diop",
    "prenom": "Assane",
    "email": "assane.diop@ecole221.sn",
    "matricule": "2026-GC-001"
  }
]
```

### 5.2 Modèle étudiant complet
```json
{
  "id": "s-1",
  "matricule": "2026-GC-001",
  "name": "Assane Diop",
  "email": "assane.diop@ecole221.sn",
  "classe": "L1 Génie Civil",
  "financialStatus": "En Règle",
  "qrStatus": "AUTORISÉ",
  "gpa": "16.5/20",
  "phoneParent": "+221 77 450 12 34"
}
```

### 5.3 Valeurs autorisées
- `financialStatus`: `En Règle` | `En Retard`
- `qrStatus`: `AUTORISÉ` | `SUSPENDU`

---

## 6. Notes

### 6.1 GET /professor/courses/{id}/grades
#### Réponse
```json
[
  {
    "id": "grade-001",
    "studentId": "stu-001",
    "nom": "Diop",
    "prenom": "Assane",
    "cc": 12,
    "examen": 16
  }
]
```

### 6.2 POST /professor/courses/{id}/grades/{studentId}
#### Requête
```json
{
  "cc": 12,
  "examen": 16
}
```

### 6.3 GET /student/grades
#### Réponse
```json
[
  {
    "id": "grade-010",
    "module": "Bases de Données",
    "prof": "Prof. C. Diop",
    "cc": 12,
    "examen": 16,
    "moyPromo": 11.9
  }
]
```

### 6.4 Règle métier
- Note finale = `cc * 0.4 + examen * 0.6`
- Statut de validation : `Validé` si note finale >= 10, sinon `Rattrapage`

---

## 7. Planning

### 7.1 GET /planning/slots
#### Réponse
```json
[
  {
    "id": "slot-001",
    "jour": "Lundi",
    "heureDebut": "08:00",
    "heureFin": "11:00",
    "nom": "Algorithmique",
    "type": "CM",
    "professeur": "Prof. Mbaye",
    "salle": "Amphi A",
    "description": "Cours de base",
    "classeId": "class-l2-gc"
  }
]
```

### 7.2 GET /professor/schedule
#### Réponse
```json
[
  {
    "id": "sched-001",
    "courseTitle": "Algorithmique",
    "day": "Lundi",
    "time": "08:00 - 11:00",
    "room": "Amphi A",
    "courseId": "course-1",
    "type": "CM",
    "dateStr": "2026-07-23",
    "status": "a_venir"
  }
]
```

### 7.3 Statuts d’horaire
- `a_venir`
- `annule`
- `reporte`
- `effectue`

### 7.4 GET /planning/filieres
```json
[
  {
    "id": "filiere-1",
    "nom": "Génie Civil",
    "code": "GC"
  }
]
```

### 7.5 GET /planning/classes
```json
[
  {
    "id": "class-l2-gc",
    "nom": "L2 Génie Civil",
    "niveau": "L2",
    "filiereId": "filiere-1"
  }
]
```

### 7.6 GET /planning/rooms
```json
[
  {
    "id": "room-1",
    "nom": "Amphi A",
    "capacite": 120,
    "type": "Amphi"
  }
]
```

---

## 8. Devoirs

### 8.1 GET /professor/courses/{id}/homeworks
#### Réponse
```json
[
  {
    "id": "hw-001",
    "titre": "TP Docker",
    "desc": "Créer une image Docker",
    "prio": "haute",
    "deadlineStr": "2026-07-28"
  }
]
```

### 8.2 GET /student/homeworks
#### Réponse
```json
[
  {
    "id": "hw-001",
    "statut": "a_rendre",
    "titre": "TP Docker",
    "deadlineStr": "2026-07-28",
    "prio": "haute"
  }
]
```

### 8.3 Statuts devoirs
- `a_rendre`
- `rendu`
- `en_retard`

---

## 9. Collaboration

### 9.1 GET /collaboration/workgroups
```json
[
  {
    "id": "wg-001",
    "nom": "Groupe IA",
    "description": "Travail collaboratif sur le projet IA",
    "createurId": "usr-002",
    "membres": ["usr-002", "usr-003"]
  }
]
```

### 9.2 GET /collaboration/workgroups/{id}/messages
```json
[
  {
    "id": "msg-001",
    "contenu": "Bonjour à tous",
    "auteurId": "usr-002",
    "dateEnvoi": "2026-07-23T08:00:00Z"
  }
]
```

### 9.3 GET /collaboration/meets
```json
[
  {
    "id": "meet-001",
    "titre": "Réunion projet",
    "url": "https://meet.example.com/123",
    "professeurId": "usr-002",
    "dateDebut": "2026-07-23T10:00:00Z",
    "actif": true
  }
]
```

---

## 10. Candidatures

### 10.1 GET /candidatures/settings
#### Réponse
```json
{
  "ouvert": true,
  "dateOuverture": "2026-06-01",
  "dateFermeture": "2026-08-31",
  "messageAvis": "Les candidatures sont ouvertes",
  "showRegisterButton": true,
  "docsRequis": {
    "cni": true,
    "bulletin": true,
    "diplome": true,
    "motivation": true
  }
}
```

### 10.2 Structure d’une candidature (UI admin)
```json
{
  "id": "cand-001",
  "nomComplet": "Seynabou Diop",
  "email": "seynabou.diop@example.sn",
  "telephone": "+221 77 123 45 67",
  "typeDepot": "En ligne",
  "promotionNom": "L1 Génie Civil",
  "motivation": "Je souhaite intégrer l’école pour développer mes compétences",
  "statut": "En attente",
  "dateSoumission": "2026-07-23",
  "numeroCni": "123456789",
  "dernierEtablissement": "Lycée de l’Amitié",
  "dernierDiplome": "Baccalauréat",
  "nomFichierCni": "cni.pdf",
  "nomFichierBulletin": "bulletin.pdf",
  "nomFichierDiplome": "diplome.pdf"
}
```

### 10.3 Statuts candidature
- `En attente`
- `Accepté`
- `Refusé`

---

## 11. Administration – Finances

### 11.1 Étudiant finance
```json
{
  "id": "STU-2026-001",
  "name": "Assane Diop",
  "classe": "L1 Génie Civil",
  "status": "En Règle",
  "debt": 0,
  "paid": 450000
}
```

### 11.2 Dépense
```json
{
  "id": "exp-1",
  "label": "Salaires des professeurs vacataires",
  "category": "RH",
  "amount": 4800000,
  "date": "2026-07-01"
}
```

### 11.3 Valeurs autorisées
- `status`: `En Règle` | `En Avance` | `En Retard`
- `category`: libre mais normalisé côté backend

---

## 12. Administration – Personnel

### 12.1 Personnel
```json
{
  "id": "EMP-2026-001",
  "firstName": "Marie",
  "lastName": "Sow",
  "email": "marie.sow@ecole221.sn",
  "phone": "77 123 45 67",
  "role": "Secrétaire",
  "status": "Actif",
  "assignedClass": "L2 Génie Civil"
}
```

### 12.2 Présence
```json
{
  "id": "att-001",
  "name": "Marie Sow",
  "photo": "",
  "role": "Secrétaire",
  "checkIn": "08:00",
  "checkOut": "17:00",
  "hours": 8,
  "status": "À l'heure"
}
```

### 12.3 Valeurs autorisées
- `status`: `Actif` | `Suspendu`
- `attendance status`: `À l'heure` | `En retard` | `Absent`

---

## 13. Administration – Sécurité

### 13.1 Journal d’audit
```json
{
  "id": "log-001",
  "time": "2026-07-10 09:12",
  "user": "Marie Sow",
  "role": "Secrétaire",
  "action": "Validation Inscription",
  "details": "A validé l'inscription de l'élève Nafissatou Diallo",
  "ip": "196.207.240.12",
  "agent": "Chrome / Linux"
}
```

### 13.2 Terminal d’accès
```json
{
  "id": "term-a",
  "name": "Portail Principal - Entrée A",
  "status": "Actif",
  "location": "Hall Central"
}
```

### 13.3 Valeurs autorisées
- `status`: `Actif` | `Inactif` | `Révoqué`

---

## 14. Administration – Notifications

```json
{
  "id": "camp-001",
  "title": "Réunion générale de rentrée parents-profs",
  "target": "Tous les Parents",
  "channels": ["Email", "WhatsApp"],
  "date": "2026-07-01",
  "openRate": 85,
  "deliveryRate": 98
}
```

---

## 15. Recommandations backend

1. Mettre ce document en référence absolue pour tous les endpoints.
2. Ne jamais ajouter un champ sans l’ajuster ici.
3. Utiliser des enums stricts pour les statuts.
4. Garder la même logique de nommage partout.
5. Valider les données côté backend avant sauvegarde.

---

## 16. Résumé rapide des endpoints principaux

| Endpoint | Méthode | Entrée | Sortie |
|---|---|---|---|
| /auth/login | POST | email, password | utilisateur + role |
| /professor/courses | GET | aucune | cours professeur |
| /student/courses | GET | aucune | cours étudiant |
| /professor/courses/{id}/students | GET | courseId | étudiants inscrits |
| /professor/courses/{id}/grades | GET | courseId | notes par étudiant |
| /professor/courses/{id}/grades/{studentId} | POST | courseId, studentId, cc, examen | confirmation |
| /student/grades | GET | aucune | notes étudiant |
| /planning/slots | GET | aucune | créneaux |
| /professor/schedule | GET | aucune | emploi du temps |
| /professor/courses/{id}/homeworks | GET | courseId | devoirs |
| /student/homeworks | GET | aucune | devoirs étudiant |
| /planning/filieres | GET | aucune | filières |
| /planning/classes | GET | aucune | classes |
| /planning/rooms | GET | aucune | salles |
| /collaboration/workgroups | GET | aucune | groupes |
| /collaboration/workgroups/{id}/messages | GET | workgroupId | messages |
| /collaboration/meets | GET | aucune | sessions vidéo |
| /candidatures/settings | GET | aucune | paramètres candidatures |
| /admin/students/{id}/status | PATCH | studentId, statutFrais | confirmation |
