# README Frontend — Module Étudiant (Student)

Ce guide explique comment consommer correctement les endpoints du module **Student** depuis un frontend.

## 1) Base URL

- Base API: `http://localhost:8080/api`
- Student: `http://localhost:8080/api/student`
- Auth: `http://localhost:8080/api/auth`
- WebSocket (SockJS): `http://localhost:8080/api/ws`

## 2) Dépendances frontend recommandées

```bash
npm i axios @tanstack/react-query
```

WebSocket (sessions live) :

```bash
npm i @stomp/stompjs sockjs-client
```

Option validation :

```bash
npm i zod
```

## 3) Authentification

### 3.1 Rôles autorisés

| Route | Accès |
|-------|--------|
| `POST /student/qr-validate` | **Public** (vigile / scanner QR) |
| Toutes les autres `/student/**` | `ETUDIANT`, `PROFESSEUR` ou `ADMIN` |

### 3.2 Login étudiant (cookie web)

`POST /api/auth/login`

Body:

```json
{
  "email": "etudiant221@gmail.com",
  "password": "ecole221"
}
```

Réponse: infos utilisateur + cookie `auth_token` HTTP-only.

### 3.3 Login token (API / Swagger / mobile)

`POST /api/auth/login-token`

Body:

```json
{
  "email": "etudiant221@gmail.com",
  "password": "ecole221"
}
```

Réponse:

```json
{
  "token": "<JWT>",
  "role": "ETUDIANT"
}
```

### 3.4 Profil auth

`GET /api/auth/me` — pour vérifier la session avant d’appeler `/student/*`.

> **Note backend actuelle :** le contrôleur Student lit les données de l’étudiant mock `usr-001` (pas encore le user JWT). L’auth sert surtout à **autoriser** l’accès aux routes.

## 4) Client HTTP prêt à l’emploi (Axios)

```ts
import axios from 'axios';

export const api = axios.create({
  baseURL: 'http://localhost:8080/api',
  timeout: 15000,
  withCredentials: true, // cookie web
});

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('auth_token');
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});
```

## 5) Endpoints Student (24 REST)

### Profil & dashboard

1. `GET /student/profile`
2. `GET /student/dashboard`

### Badge numérique & QR

3. `GET /student/digital-badge`
4. `POST /student/qr-validate` *(public)*

### Présences

5. `GET /student/attendances?page=0&size=10`
6. `POST /student/attendances`

### Sessions live

7. `GET /student/live-sessions?page=0&size=10`
8. `POST /student/live-sessions/{sessionId}/reaction`
9. `GET /student/live-sessions/{sessionId}/chat`
10. `POST /student/live-sessions/{sessionId}/chat`
11. `GET /student/live-sessions/{sessionId}/chat-history`

### Notes

12. `GET /student/grades`
13. `GET /student/grades/download` *(PDF)*
14. `POST /student/grades/{gradeId}/review`

### Cours

15. `GET /student/courses`
16. `GET /student/courses/{courseId}/details`

### Devoirs

17. `GET /student/homeworks`
18. `POST /student/courses/{courseId}/homeworks/{homeworkId}/submit` *(multipart)*
19. `POST /student/homeworks/{homeworkId}/start`
20. `POST /student/homeworks/{homeworkId}/progress`

### Quiz & tuteur IA

21. `POST /student/courses/{courseId}/quizzes/submit`
22. `POST /student/tutor/chat`

### Emails

23. `POST /student/send-notification`
24. `POST /student/request-confirmation`

> Note: 23 routes sur `StudentController` + 1 route historique chat sur `LiveSessionMessageController`.

## 6) Détail des réponses & payloads

### 6.1 Profil

`GET /student/profile`

```json
{
  "id": "usr-001",
  "nom": "Sall",
  "prenom": "Mamadou",
  "email": "etudiant221@gmail.com",
  "matricule": "SN-2026-1234",
  "dateNaissance": "2002-05-12",
  "telephone": "+221771234567",
  "adresse": "...",
  "filiere": "Genie Logiciel",
  "niveau": "Licence 3",
  "photoUrl": "https://..."
}
```

### 6.2 Dashboard

`GET /student/dashboard`

```json
{
  "totalCredits": 60,
  "moyenneGenerale": 14.5,
  "coursEnCours": 5,
  "devoirsARendre": 3,
  "prochainsExamens": [
    { "id": "ex-1", "titre": "Architecture des Microservices", "date": "2026-08-15", "salle": "Amphi A" }
  ],
  "statsPresence": {
    "tauxPresence": 95.0,
    "present": 40,
    "absent": 1,
    "retards": 2
  },
  "notifications": [
    { "id": "n-1", "titre": "Nouveau devoir", "message": "...", "date": "2026-07-30", "lu": false }
  ]
}
```

### 6.3 Badge numérique

`GET /student/digital-badge`

```json
{
  "id": "badge-usr-001",
  "studentId": "usr-001",
  "studentName": "Mamadou Sall",
  "matricule": "SN-2026-1234",
  "classe": "M1 GL",
  "qrPayload": "...",
  "token": "<HMAC>",
  "minuteEpoch": 1234567890,
  "expiresInSeconds": 60,
  "status": "ACTIF"
}
```

Le token **expire chaque minute** : rafraîchir le badge côté UI (polling ~50–55 s).

### 6.4 Validation QR (public — vigile)

`POST /student/qr-validate`

```json
{
  "matricule": "SN-2026-1234",
  "minuteEpoch": 1234567890,
  "token": "<HMAC scanné>"
}
```

Réponse:

```json
{
  "valid": true,
  "matricule": "SN-2026-1234",
  "timestamp": "2026-07-30T12:00:00",
  "message": "Badge etudiant verifie avec succes"
}
```

### 6.5 Présences (paginé)

`GET /student/attendances?page=0&size=10`

Réponse `PageResponseDto`:

```json
{
  "content": [
    {
      "id": "att-123",
      "date": "2026-07-30T08:30:00",
      "status": "PRESENT",
      "studentId": "usr-001",
      "studentName": "Mamadou Sall",
      "courseId": "course-1"
    }
  ],
  "page": 0,
  "size": 10,
  "totalElements": 12,
  "totalPages": 2,
  "last": false
}
```

### 6.6 Auto check-in

`POST /student/attendances`

```json
{
  "type": "PRESENT",
  "method": "QR"
}
```

| Champ | Valeurs |
|-------|---------|
| `type` | `PRESENT`, `ABSENT`, `LATE`, `EXCUSED`, `UNKNOWN` (alias `COURS` / `COURSE` → `PRESENT`) |
| `method` | ex. `QR`, `GPS`, `MANUAL` |

Réponse:

```json
{
  "success": true,
  "attendanceId": "att-1234",
  "timestamp": "2026-07-30T08:31:00",
  "status": "VALIDATED"
}
```

### 6.7 Sessions live (paginé)

`GET /student/live-sessions?page=0&size=10`

```json
{
  "content": [
    {
      "id": "sess-1",
      "courseTitle": "Architecture Hexagonale",
      "professorName": "Dr. Aly Diatta",
      "status": "ACTIVE",
      "activeUsers": 24,
      "link": "https://...",
      "time": "08:30",
      "reactions": { "LIKE": 5, "CLAP": 2 }
    }
  ],
  "page": 0,
  "size": 10,
  "totalElements": 1,
  "totalPages": 1,
  "last": true
}
```

### 6.8 Réaction live (REST)

`POST /student/live-sessions/{sessionId}/reaction`

```json
{
  "type": "LIKE"
}
```

Types: `LIKE`, `HEART`, `CLAP`, `QUESTION`, `CONFUSED`, `THUMBS_UP`, `THUMBS_DOWN`.

Réponse:

```json
{
  "success": true,
  "sessionId": "sess-1",
  "reaction": "LIKE"
}
```

### 6.9 Chat live (REST)

`GET /student/live-sessions/{sessionId}/chat`  
`GET /student/live-sessions/{sessionId}/chat-history` — historique persisté à la reconnexion.

`POST /student/live-sessions/{sessionId}/chat`

```json
{
  "user": "Mamadou Sall",
  "text": "Question sur le chapitre 1"
}
```

Réponse:

```json
{
  "success": true,
  "id": "msg-1720000000000"
}
```

### 6.10 Notes

`GET /student/grades`

```json
[
  {
    "id": "g1",
    "module": "Architecture Hexagonale",
    "prof": "Dr. Aly Diatta",
    "ects": 6,
    "cc": 16.5,
    "examen": 18.0,
    "moyPromo": 13.5
  }
]
```

Formule côté UI (aussi utilisée pour le PDF) :

- Note finale = `cc * 0.4 + examen * 0.6`
- Validé si note finale ≥ 10

### 6.11 Télécharger le relevé PDF

`GET /student/grades/download`

```ts
const res = await api.get('/student/grades/download', { responseType: 'blob' });
const url = URL.createObjectURL(new Blob([res.data], { type: 'application/pdf' }));
// déclencher un <a download="releve_notes_officiel_ecole221_S2.pdf">
```

Headers réponse: `Content-Type: application/pdf`, `Content-Disposition: attachment; filename="releve_notes_officiel_ecole221_S2.pdf"`.

### 6.12 Demande de révision de note

`POST /student/grades/{gradeId}/review`

```json
{
  "reason": "Erreur de calcul sur le contrôle continu du 12/06"
}
```

Contraintes: `reason` entre **10 et 500** caractères.

Réponse:

```json
{
  "success": true,
  "gradeId": "g1",
  "reason": "Erreur de calcul...",
  "status": "PENDING_REVIEW"
}
```

### 6.13 Cours

`GET /student/courses`

```json
[
  {
    "id": "course-1",
    "titre": "Architecture des Microservices",
    "coefficient": 3,
    "progress": 42,
    "statut": "EN_COURS",
    "unites": ["Intro", "Patterns", "Déploiement"]
  }
]
```

Statuts: `EN_COURS`, `TERMINE`, `A_COMMENCER`, `ABANDONNE`.

`GET /student/courses/{courseId}/details` — objet libre (JSONB). IDs acceptés: `course-1` ou legacy `c-1`.  
`404` si cours inconnu.

### 6.14 Devoirs

`GET /student/homeworks`

```json
[
  {
    "id": "hw-001",
    "statut": "a_rendre",
    "titre": "TP Docker",
    "deadline": "2026-08-01",
    "prio": "haute",
    "courseId": "course-1",
    "desc": "Containeriser une API Spring",
    "progress": 50
  }
]
```

**Soumettre un fichier** (multipart) :

`POST /student/courses/{courseId}/homeworks/{homeworkId}/submit`

```ts
const form = new FormData();
form.append('file', file); // champ obligatoire: "file"
await api.post(`/student/courses/${courseId}/homeworks/${homeworkId}/submit`, form, {
  headers: { 'Content-Type': 'multipart/form-data' },
});
```

Réponse:

```json
{
  "success": true,
  "homeworkId": "hw-001",
  "fileName": "tp-docker.zip",
  "size": 20480,
  "status": "Soumis"
}
```

**Démarrer / progression** :

```http
POST /student/homeworks/{homeworkId}/start
```

```http
POST /student/homeworks/{homeworkId}/progress
Content-Type: application/json

{ "addedProgress": 10 }
```

### 6.15 Quiz

`POST /student/courses/{courseId}/quizzes/submit`

```json
{
  "answers": [
    { "selectedOption": 1, "correctOption": 1 },
    { "selectedOption": 0, "correctOption": 2 }
  ]
}
```

Réponse:

```json
{
  "success": true,
  "courseId": "course-1",
  "score": 1,
  "total": 2,
  "message": "Quiz evalue avec succes par le serveur !"
}
```

### 6.16 Tuteur IA

`POST /student/tutor/chat`

```json
{
  "message": "Explique-moi l'architecture hexagonale"
}
```

Réponse:

```json
{
  "text": "...",
  "provider": "openai|...",
  "available": true
}
```

### 6.17 Notifications email

`POST /student/send-notification`

```json
{
  "to": "etudiant221@gmail.com",
  "subject": "Rappel devoir",
  "content": "Le TP Docker est à rendre demain."
}
```

`POST /student/request-confirmation`

```json
{
  "to": "etudiant221@gmail.com",
  "type": "inscription",
  "data": { "nom": "Sall", "filiere": "GL" }
}
```

## 7) WebSocket — chat & réactions live (temps réel)

Endpoint SockJS: `http://localhost:8080/api/ws`

| Action | Destination client → serveur | Broadcast serveur → clients |
|--------|------------------------------|-----------------------------|
| Chat | `/app/live-sessions/{sessionId}/chat` | `/topic/live-sessions/{sessionId}/chat` |
| Réaction | `/app/live-sessions/{sessionId}/reaction` | `/topic/live-sessions/{sessionId}/reaction` |

Payload chat recommandé:

```json
{
  "senderId": "usr-001",
  "senderName": "Mamadou Sall",
  "content": "Bonjour"
}
```

Exemple minimal STOMP:

```ts
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

const client = new Client({
  webSocketFactory: () => new SockJS('http://localhost:8080/api/ws'),
  onConnect: () => {
    client.subscribe('/topic/live-sessions/sess-1/chat', (msg) => {
      console.log(JSON.parse(msg.body));
    });
    client.publish({
      destination: '/app/live-sessions/sess-1/chat',
      body: JSON.stringify({
        senderId: 'usr-001',
        senderName: 'Mamadou Sall',
        content: 'Hello live',
      }),
    });
  },
});
client.activate();
```

Flux recommandé UI live:

1. `GET /student/live-sessions` → lister les sessions
2. `GET /student/live-sessions/{id}/chat-history` → historique
3. Connecter STOMP → s’abonner aux topics
4. Envoyer messages / réactions via STOMP (ou REST en fallback)

## 8) Gestion des erreurs côté frontend

- `200/201`: succès
- `400`: payload invalide (validation `@Valid`, multipart sans `file`, reason trop court, etc.)
- `401`: non authentifié (token/cookie absent ou expiré)
- `403`: rôle non autorisé (ex. route student sans ETUDIANT/PROF/ADMIN)
- `404`: ressource absente (ex. `courseId` inconnu)
- `500`: erreur serveur

Exemple:

```ts
try {
  const { data } = await api.get('/student/dashboard');
  return data;
} catch (e: any) {
  const status = e?.response?.status;
  const message = e?.response?.data?.message || 'Erreur inconnue';
  throw new Error(`[${status}] ${message}`);
}
```

## 9) Contrat d’intégration frontend

1. Se connecter via `/auth/login` ou `/auth/login-token` **avant** les routes protégées.
2. Mode token: `Authorization: Bearer <JWT>`.
3. Mode cookie web: `withCredentials: true`.
4. JSON: `Content-Type: application/json` (sauf submit devoir → `multipart/form-data`).
5. PDF notes: `responseType: 'blob'`.
6. Badge QR: rafraîchir avant expiration (60 s).
7. Live: historique REST + temps réel STOMP.
8. Gérer explicitement `401/403/404/500` dans l’UI.

## 10) Checklist rapide

- [ ] Login OK (`/auth/login-token` avec `etudiant221@gmail.com`)
- [ ] `GET /auth/me` retourne le profil auth
- [ ] `GET /student/profile` → `200`
- [ ] `GET /student/dashboard` → `200`
- [ ] `GET /student/digital-badge` → token + `minuteEpoch`
- [ ] `POST /student/qr-validate` (sans auth) avec badge frais → `valid: true`
- [ ] `GET /student/attendances` paginé OK
- [ ] `POST /student/attendances` check-in OK
- [ ] `GET /student/grades` + `GET /student/grades/download` (PDF)
- [ ] `GET /student/courses` + details
- [ ] `GET /student/homeworks` + submit multipart
- [ ] `GET /student/live-sessions` + chat-history + STOMP

---

Compte de test étudiant: `etudiant221@gmail.com` / `ecole221`  
Rôles acceptés sur `/student/**`: `ETUDIANT`, `PROFESSEUR`, `ADMIN`  
Route publique: `POST /student/qr-validate`
