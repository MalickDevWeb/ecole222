# Student Module API Contract
*Contrat API simple pour le développement frontend - Module Étudiant*

## Base URL
```
http://localhost:8080/api
```

## Authentification
Toutes les requêtes doivent inclure le header:
```
Authorization: Bearer {token}
```

---

## 1. PROFILE & DASHBOARD

### 1.1 Get Student Profile
**Récupérer le profil de l'étudiant**

```http
GET /student/profile
```

**Response (200 OK):**
```json
{
  "id": "usr-001",
  "nom": "Sall",
  "prenom": "Mamadou",
  "email": "student@ecole221.sn",
  "matricule": "",
  "dateNaissance": null,
  "telephone": "",
  "adresse": "",
  "filiere": "",
  "niveau": "",
  "photoUrl": ""
}
```

---

### 1.2 Get Student Dashboard
**Récupérer le tableau de bord**

```http
GET /student/dashboard
```

**Response (200 OK):**
```json
{
  "totalCredits": 0,
  "moyenneGenerale": 0.0,
  "coursEnCours": 0,
  "devoirsARendre": 0,
  "prochainsExamens": [],
  "statsPresence": {
    "tauxPresence": 0.0,
    "present": 0,
    "absent": 0,
    "retards": 0
  },
  "notifications": []
}
```

---

### 1.3 Get Digital Badge
**Récupérer le badge QR numérique**

```http
GET /student/digital-badge
```

**Response (200 OK):**
```json
{
  "id": "badge-usr-001",
  "studentId": "usr-001",
  "studentName": "Mamadou Sall",
  "matricule": "SN-2026--9704",
  "classe": "M1 GL",
  "qrPayload": "STU_SECURE:SN-2026--9704:29761314:FC756E24",
  "token": "FC756E24",
  "minuteEpoch": 29761314,
  "expiresInSeconds": 60,
  "status": "ACTIF"
}
```

---

## 2. ATTENDANCES

### 2.1 Get Attendances History
**Récupérer l'historique des présences**

```http
GET /student/attendances?page=0&size=10
```

**Query Parameters:**
- `page` (number, default: 0) - Numéro de page
- `size` (number, default: 10) - Taille de la page

**Response (200 OK):**
```json
{
  "content": [
    {
      "id": "att-87c8b446",
      "date": "2026-07-25T15:00:12.050980856",
      "status": {
        "value": "PRESENT"
      },
      "studentId": "usr-001",
      "studentName": "Mamadou Sall",
      "courseId": "self-check"
    }
  ],
  "page": 0,
  "size": 10,
  "totalElements": 8,
  "totalPages": 1,
  "last": true,
  "empty": false
}
```

---

### 2.2 Self Check-in
**Auto-déclaration de présence**

```http
POST /student/attendances
Content-Type: application/json
```

**Request Body:**
```json
{
  "type": "PRESENT",
  "method": "QR_CODE"
}
```

**Values possibles:**
- `type`: "PRESENT", "ABSENT", "RETARD"
- `method`: "QR_CODE", "MANUAL", "AUTO", "GPS"

**Response (200 OK):**
```json
{
  "success": true,
  "attendanceId": "att-7804",
  "timestamp": "2026-08-02T13:54:07.806317414",
  "status": "VALIDATED"
}
```

---

### 2.3 Validate QR (Public)
**Valider un QR code (utilisé par les vigiles)**

```http
POST /student/qr-validate
Content-Type: application/json
```

**Request Body:**
```json
{
  "matricule": "MAT-2023-001",
  "minuteEpoch": 29761314,
  "token": "FC756E24"
}
```

**Response (200 OK):**
```json
{
  "valid": false,
  "matricule": "MAT-2023-001",
  "message": "Token QR expire ou invalide",
  "timestamp": "2026-08-02T13:54:05.844025460"
}
```

---

## 3. LIVE SESSIONS

### 3.1 Get Live Sessions
**Récupérer les sessions live actives**

```http
GET /student/live-sessions?page=0&size=10
```

**Query Parameters:**
- `page` (number, default: 0) - Numéro de page
- `size` (number, default: 10) - Taille de la page

**Response (200 OK):**
```json
{
  "content": [
    {
      "id": "meet-f1bb6d87",
      "courseTitle": "Point hebdo",
      "professorName": "Professeur",
      "status": "ACTIVE",
      "activeUsers": 0,
      "link": "null",
      "time": "null",
      "reactions": {}
    }
  ],
  "page": 0,
  "size": 10,
  "totalElements": 26,
  "totalPages": 1,
  "last": true,
  "empty": false
}
```

---

### 3.2 Send Reaction
**Envoyer une réaction à une session live**

```http
POST /student/live-sessions/{sessionId}/reaction
Content-Type: application/json
```

**Path Parameters:**
- `sessionId` (string) - ID de la session live

**Request Body:**
```json
{
  "type": "LIKE",
  "emoji": "👍"
}
```

**Values possibles:**
- `type`: "LIKE", "LOVE", "CLAP", "QUESTION"
- `emoji`: n'importe quel emoji

**Response (200 OK):**
```json
{
  "success": true,
  "sessionId": "meet-f1bb6d87",
  "reaction": "LIKE"
}
```

---

### 3.3 Get Live Session Chat
**Récupérer le chat d'une session live**

```http
GET /student/live-sessions/{sessionId}/chat
```

**Path Parameters:**
- `sessionId` (string) - ID de la session live

**Response (200 OK):**
```json
[
  {
    "id": "msg-468e68f8",
    "text": "Test chat auto",
    "userRole": "Student",
    "user": "Mamadou Sall",
    "userId": "usr-001",
    "timestamp": "2026-07-30T14:48:55.974859"
  }
]
```

---

### 3.4 Post to Live Session Chat
**Envoyer un message dans le chat live**

```http
POST /student/live-sessions/{sessionId}/chat
Content-Type: application/json
```

**Path Parameters:**
- `sessionId` (string) - ID de la session live

**Request Body:**
```json
{
  "content": "Test message",
  "senderId": "usr-001",
  "senderName": "Test Student"
}
```

**Response (200 OK):**
```json
{
  "id": "msg-1785678882442",
  "success": true
}
```

---

### 3.5 Get Chat History
**Récupérer l'historique complet du chat**

```http
GET /student/live-sessions/{sessionId}/chat-history
```

**Path Parameters:**
- `sessionId` (string) - ID de la session live

**Response (200 OK):**
```json
[
  {
    "id": "msg-468e68f8",
    "sessionId": "meet-f1bb6d87",
    "userId": "usr-001",
    "userName": "Mamadou Sall",
    "userRole": "Student",
    "text": "Test chat auto",
    "createdAt": "2026-07-30T14:48:55.974851",
    "timestamp": "2026-07-30T14:48:55.974859"
  }
]
```

---

## 4. GRADES

### 4.1 Get Grades
**Récupérer les notes de l'étudiant**

```http
GET /student/grades
```

**Response (200 OK):**
```json
[
  {
    "id": "g-1",
    "module": "Architecture Hexagonale",
    "prof": "Dr. Aly Diatta",
    "ects": 6,
    "cc": 16.5,
    "examen": 18.0,
    "moyPromo": 13.5,
    "finalGrade": 17.4,
    "mention": "A",
    "validated": true
  },
  {
    "id": "g-2",
    "module": "DevOps & CI/CD",
    "prof": "M. Badiane",
    "ects": 4,
    "cc": 14.0,
    "examen": 15.5,
    "moyPromo": 14.2,
    "finalGrade": 14.9,
    "mention": "B",
    "validated": true
  }
]
```

---

### 4.2 Download Grades Report
**Télécharger le bulletin en PDF**

```http
GET /student/grades/download
```

**Response (200 OK):**
- Content-Type: application/pdf
- Le fichier PDF est directement retourné

---

### 4.3 Request Grade Review
**Demander une révision de note**

```http
POST /student/grades/{gradeId}/review
Content-Type: application/json
```

**Path Parameters:**
- `gradeId` (string) - ID de la note

**Request Body:**
```json
{
  "reason": "Request for review",
  "comment": "Please check calculation"
}
```

**Response (200 OK):**
```json
{
  "reason": "Request for review",
  "gradeId": "grade-001",
  "success": true,
  "status": "PENDING_REVIEW"
}
```

---

## 5. COURSES

### 5.1 Get Courses
**Récupérer la liste des cours**

```http
GET /student/courses
```

**Response (200 OK):**
```json
[
  {
    "id": "sc-1",
    "titre": "Architecture des Microservices",
    "coefficient": 3,
    "progress": 42,
    "statut": {
      "value": "EN_COURS"
    },
    "unites": []
  }
]
```

**Values possibles pour statut.value:**
- "EN_COURS"
- "TERMINE"
- "NON_COMMENCE"

---

### 5.2 Get Course Details
**Récupérer les détails d'un cours**

```http
GET /student/courses/{courseId}/details
```

**Path Parameters:**
- `courseId` (string) - ID du cours (ex: "course-1")

**Response (200 OK):**
```json
{
  "devoirs": [
    {
      "id": "dev-1",
      "titre": "TP 1 : Configuration API Gateway",
      "points": "20 pts",
      "statut": "A rendre",
      "dateLimite": "Dans 3 jours"
    }
  ],
  "quizzes": [
    {
      "options": ["SOAP", "gRPC", "FTP", "WebSockets"],
      "question": "Quel protocole est généralement recommandé ?",
      "reponseCorrecte": 1
    }
  ],
  "imageUrl": "https://images.unsplash.com/photo-1517694712202-14dd9538aa97?w=800&q=80&auto=format&fit=crop",
  "chapitres": [
    {
      "titre": "Chapitre 1 : Introduction aux Microservices",
      "lecons": [
        {
          "duree": "45 min",
          "titre": "1.1 Architecture Monolithique vs Microservices",
          "complete": true
        }
      ]
    }
  ],
  "ressources": [
    {
      "type": "pdf",
      "titre": "Supports de cours complet - Microservices.pdf",
      "taille": "8.4 Mo"
    }
  ]
}
```

---

## 6. HOMEWORKS

### 6.1 Get Homeworks
**Récupérer la liste des devoirs**

```http
GET /student/homeworks
```

**Response (200 OK):**
```json
[
  {
    "id": "hw-001",
    "statut": "soumis",
    "titre": "TP Docker",
    "deadline": null,
    "prio": "",
    "courseId": "course-1",
    "desc": "",
    "progress": 0,
    "overdue": false
  }
]
```

**Values possibles pour statut:**
- "soumis"
- "en_cours"
- "a_rendre"

---

### 6.2 Submit Homework
**Soumettre un devoir (multipart)**

```http
POST /student/courses/{courseId}/homeworks/{homeworkId}/submit
Content-Type: multipart/form-data
```

**Path Parameters:**
- `courseId` (string) - ID du cours
- `homeworkId` (string) - ID du devoir

**Form Data:**
- `file` (file) - Le fichier à soumettre
- `comment` (string) - Commentaire optionnel

**Response (200 OK):**
```json
{
  "homeworkId": "hw-001",
  "fileName": "dummy.txt",
  "size": 11,
  "success": true,
  "status": "Soumis"
}
```

---

### 6.3 Start Homework
**Démarrer un devoir**

```http
POST /student/homeworks/{homeworkId}/start
Content-Type: application/json
```

**Path Parameters:**
- `homeworkId` (string) - ID du devoir

**Request Body:**
```json
{
  "startTime": "2026-08-02T13:54:35.790768805"
}
```

**Response (200 OK):**
```json
{
  "homeworkId": "hw-001",
  "success": true
}
```

---

### 6.4 Update Homework Progress
**Mettre à jour la progression d'un devoir**

```http
POST /student/homeworks/{homeworkId}/progress
Content-Type: application/json
```

**Path Parameters:**
- `homeworkId` (string) - ID du devoir

**Request Body:**
```json
{
  "progress": 50,
  "timeSpent": 30
}
```

**Response (200 OK):**
```json
{
  "homeworkId": "hw-001",
  "success": true
}
```

---

## 7. QUIZZES

### 7.1 Submit Quiz
**Soumettre un quiz**

```http
POST /student/courses/{courseId}/quizzes/submit
Content-Type: application/json
```

**Path Parameters:**
- `courseId` (string) - ID du cours

**Request Body:**
```json
{
  "quizId": "quiz-001",
  "answers": [
    {
      "questionId": "q1",
      "answer": "A"
    }
  ]
}
```

**Response (200 OK):**
```json
{
  "success": true,
  "courseId": "sc-1",
  "score": 0,
  "total": 1,
  "message": "Quiz evalue avec succes par le serveur !",
  "percentage": 0.0
}
```

---

## 8. AI TUTOR

### 8.1 Tutor Chat
**Chat avec le tuteur IA**

```http
POST /student/tutor/chat
Content-Type: application/json
```

**Request Body:**
```json
{
  "message": "I need help with the homework"
}
```

**Response (200 OK):**
```json
{
  "provider": "Mock AI",
  "available": true,
  "text": "Je comprends votre question concernant : \"I need help with the homework\". En tant que tuteur IA de l'École 221, je peux vous aider à approfondir ce sujet. Voulez-vous que je vous propose des explications, des exemples ou des exercices pratiques ?"
}
```

---

## 9. NOTIFICATIONS

### 9.1 Send Notification
**Envoyer une notification**

```http
POST /student/send-notification
Content-Type: application/json
```

**Request Body:**
```json
{
  "recipientId": "user-002",
  "content": "Test notification"
}
```

**Response (200 OK):**
```json
{
  "provider": "Mock Email",
  "success": true,
  "available": true,
  "message": "Notification envoyée avec succès"
}
```

---

### 9.2 Request Confirmation
**Demander une confirmation (email/SMS)**

```http
POST /student/request-confirmation
Content-Type: application/json
```

**Request Body:**
```json
{
  "type": "EMAIL",
  "target": "test@example.com"
}
```

**Values possibles:**
- `type`: "EMAIL", "SMS"
- `target`: adresse email ou numéro de téléphone

**Response (200 OK):**
```json
{
  "provider": "Mock Email",
  "success": true,
  "available": true,
  "message": "Confirmation envoyée avec succès"
}
```

---

## 10. ADMIN ENDPOINTS (Access Admin Requis)

### 10.1 Get Student Profile 360
**Récupérer le profil complet 360°**

```http
GET /admin/students/{studentId}/profile360
```

**Path Parameters:**
- `studentId` (string) - ID de l'étudiant

**Response (200 OK):**
```json
{
  "admissionDetails": {
    "dateInscription": "15/09/2023",
    "lyceeOrigine": "Lycée Seydou Nourou Tall",
    "bacSerie": "S1 Scientifique",
    "bacMention": "Très Bien (16.8/20)",
    "statut": "ADMIS",
    "matricule": "MAT-2023-usr-001",
    "commissionAdmission": "Commission Régionale Dakar",
    "dateDecision": "05/09/2023"
  },
  "ueList": [
    {
      "status": "VALIDÉ",
      "coefficient": 4,
      "teacher": "Mme Sow",
      "module": "Mathématiques & Analyse",
      "grade": 16.5,
      "ue": "UE Scientifique Fondamentale"
    }
  ],
  "connectionLogs": [
    {
      "status": "RÉUSSI",
      "device": "Chrome Desktop",
      "date": "2026-07-17 08:15",
      "location": "Dakar, Sénégal"
    }
  ],
  "activityTrail": [
    {
      "id": "log-1",
      "result": "SUCCÈS",
      "module": "Planning",
      "action": "Consultation emploi du temps",
      "timestamp": "2026-07-17 08:35",
      "actor": "Étudiant"
    }
  ],
  "timeline": [
    {
      "status": "COMPLETED",
      "desc": "Dossier accepté",
      "title": "Candidature & Admission",
      "year": "2023-09"
    }
  ],
  "payments": [
    {
      "status": "PAYÉ",
      "date": "15/09/2023",
      "receiptNumber": "REC-2023-001",
      "method": "Orange Money",
      "amount": "250,000 FCFA",
      "label": "Inscription Administrative"
    }
  ],
  "extra": {
    "bourses": "Aucune bourse",
    "sante": "O+,Aucune allergie connue",
    "messages": [
      {
        "text": "Bienvenue sur la plateforme École 221.",
        "sender": "Administration",
        "date": "10/07/2026 09:00"
      }
    ]
  }
}
```

---

### 10.2 Update Student Profile 360
**Mettre à jour le profil 360°**

```http
POST /admin/students/{studentId}/profile360
Content-Type: application/json
```

**Path Parameters:**
- `studentId` (string) - ID de l'étudiant

**Request Body:**
```json
{
  "admissionDetails": {
    "dateInscription": "15/09/2023",
    "lyceeOrigine": "Lycée Test",
    "bacSerie": "S1",
    "bacMention": "Bien",
    "statut": "ADMIS"
  }
}
```

**Response (200 OK):**
```json
{
  "admissionDetails": {
    "dateInscription": "15/09/2023",
    "lyceeOrigine": "Lycée Seydou Nourou Tall",
    "bacSerie": "S1 Scientifique",
    "bacMention": "Très Bien (16.8/20)",
    "statut": "ADMIS",
    "matricule": "MAT-2023-usr-001",
    "commissionAdmission": "Commission Régionale Dakar",
    "dateDecision": "05/09/2023"
  },
  "ueList": [...],
  "connectionLogs": [...],
  "activityTrail": [...],
  "timeline": [...],
  "payments": [...],
  "extra": {...}
}
```

---

### 10.3 Get Student Documents
**Récupérer les documents d'un étudiant**

```http
GET /admin/students/{studentId}/documents
```

**Path Parameters:**
- `studentId` (string) - ID de l'étudiant

**Response (200 OK):**
```json
[
  {
    "id": "doc-1785678884522",
    "name": "Transcript",
    "size": "2 Mo",
    "url": "http://example.com/transcript.pdf"
  }
]
```

---

### 10.4 Add Student Document
**Ajouter un document à un étudiant**

```http
POST /admin/students/{studentId}/documents
Content-Type: application/json
```

**Path Parameters:**
- `studentId` (string) - ID de l'étudiant

**Request Body:**
```json
{
  "name": "Transcript",
  "size": "2 Mo",
  "url": "http://example.com/transcript.pdf"
}
```

**Response (200 OK):**
```json
{
  "documents": [
    {
      "id": "doc-1785678884522",
      "name": "Transcript",
      "size": "2 Mo",
      "url": "http://example.com/transcript.pdf"
    },
    {
      "id": "doc-1785678960988",
      "name": "Transcript",
      "size": "2 Mo",
      "url": "http://example.com/transcript.pdf"
    }
  ]
}
```

---

### 10.5 Generate Student Diagnostic
**Générer un diagnostic académique**

```http
POST /admin/student-diagnostic
Content-Type: application/json
```

**Request Body:**
```json
{
  "studentId": "usr-001"
}
```

**Response (200 OK):**
```json
{
  "text": "Je comprends votre question concernant : \"Génère un diagnostic académique concis (5-8 lignes) pour l'étudiant ID usr-001. Mentionne forces, axes d'amélioration et recommandations.\". En tant que tuteur IA de l'École 221, je peux vous aider à approfondir ce sujet. Voulez-vous que je vous propose des explications, des exemples ou des exercices pratiques ?"
}
```

---

### 10.6 Assign Schedule
**Assigner un emploi du temps**

```http
POST /admin/schedule/assign
Content-Type: application/json
```

**Request Body:**
```json
{
  "courseId": "sc-1",
  "day": "Monday",
  "time": "09:00",
  "room": "Room A"
}
```

**Response (200 OK):**
```json
{
  "room": "Room A",
  "time": "09:00",
  "courseId": "sc-1",
  "success": true,
  "day": "Monday"
}
```

---

## ERROR RESPONSES

### Standard Error Format
```json
{
  "error": "Error Type",
  "message": "Detailed error message",
  "timestamp": "2026-08-02T13:54:05.844025460"
}
```

### Common HTTP Status Codes
- `200 OK` - Requête réussie
- `400 Bad Request` - Paramètres invalides
- `401 Unauthorized` - Non authentifié
- `403 Forbidden` - Accès refusé
- `404 Not Found` - Ressource non trouvée
- `500 Internal Server Error` - Erreur serveur

---

## EXEMPLE D'UTILISATION (JavaScript)

### Authentification
```javascript
const API_BASE = 'http://localhost:8080/api';
let authToken = '';

// Login
async function login(email, password) {
  const response = await fetch(`${API_BASE}/auth/login-token`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email, password })
  });
  const data = await response.json();
  authToken = data.token;
  return data;
}

// Requête authentifiée
async function authenticatedFetch(url, options = {}) {
  const headers = {
    'Authorization': `Bearer ${authToken}`,
    'Content-Type': 'application/json',
    ...options.headers
  };
  return fetch(url, { ...options, headers });
}
```

### Exemples d'appels
```javascript
// Récupérer le profil
const profile = await authenticatedFetch(`${API_BASE}/student/profile`)
  .then(res => res.json());

// Récupérer les présences
const attendances = await authenticatedFetch(
  `${API_BASE}/student/attendances?page=0&size=10`
).then(res => res.json());

// Auto check-in
const checkIn = await authenticatedFetch(`${API_BASE}/student/attendances`, {
  method: 'POST',
  body: JSON.stringify({
    type: 'PRESENT',
    method: 'QR_CODE'
  })
}).then(res => res.json());

// Soumettre un devoir (multipart)
const formData = new FormData();
formData.append('file', fileInput.files[0]);
formData.append('comment', 'Mon devoir');

const submitHomework = await fetch(
  `${API_BASE}/student/courses/course-1/homeworks/hw-001/submit`,
  {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${authToken}`
    },
    body: formData
  }
).then(res => res.json());
```

---

## NOTES IMPORTANTES

1. **Pagination**: Tous les endpoints listés supportent la pagination avec `page` et `size`
2. **IDs**: Les IDs sont des strings, pas des nombres
3. **Dates**: Les dates sont au format ISO 8601
4. **File Upload**: Utilisez `multipart/form-data` pour les uploads de fichiers
5. **QR Validation**: L'endpoint `/student/qr-validate` est public (pas besoin d'authentification)
6. **Session IDs**: Utilisez les IDs retournés par `/student/live-sessions` pour les opérations sur les sessions
7. **Course IDs**: Les IDs de cours disponibles sont retournés par `/student/courses`

---

## CONTACT & SUPPORT

Pour toute question sur l'API, contactez l'équipe backend ou consultez la documentation Swagger disponible à:
```
http://localhost:8080/swagger-ui.html
```
