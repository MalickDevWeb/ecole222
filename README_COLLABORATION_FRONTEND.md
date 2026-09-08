# README Frontend — Module Collaboration

Ce guide explique **exactement** comment consommer les endpoints du module collaboration depuis un frontend (React, Next.js, Vue, Angular, mobile web), avec authentification, dépendances, entrées/sorties et exemples.

## 1) Base URL et prérequis

- Base API: `http://localhost:8080/api`
- Module collaboration:
  - `http://localhost:8080/api/collaboration`
  - `http://localhost:8080/api/collaboration/groupchat`
- Auth **requise** pour les endpoints collaboration/groupchat.
- WebSocket STOMP endpoint: `ws://localhost:8080/api/ws` (ou SockJS: `http://localhost:8080/api/ws`)

## 2) Dépendances frontend recommandées

### HTTP + cache

```bash
npm i axios @tanstack/react-query
```

### WebSocket/STOMP

```bash
npm i @stomp/stompjs sockjs-client
```

### Types utiles (optionnel)

```bash
npm i zod
```

## 3) Authentification (obligatoire)

### 3.1 Login pour récupérer JWT

`POST /api/auth/login-token`

Body:

```json
{
  "email": "admin221@gmail.com",
  "password": "ecole221"
}
```

Réponse attendue:

```json
{
  "token": "<JWT>"
}
```

### 3.2 Utilisation du token

Ajouter l’en-tête sur toutes les requêtes collaboration:

```http
Authorization: Bearer <JWT>
```

## 4) Client HTTP prêt à utiliser (Axios)

```ts
import axios from 'axios';

export const api = axios.create({
  baseURL: 'http://localhost:8080/api',
  timeout: 15000,
});

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('auth_token');
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});
```

## 5) Format des erreurs à gérer

- `401`: token manquant/invalide/expiré
- `403`: rôle non autorisé
- `400`: payload invalide (ex: multipart sans fichier)
- `500`: erreur serveur

Exemple robuste côté frontend:

```ts
try {
  const { data } = await api.get('/collaboration/workgroups');
  return data;
} catch (e: any) {
  const code = e?.response?.status;
  const message = e?.response?.data?.message || 'Erreur inconnue';
  throw new Error(`[${code}] ${message}`);
}
```

## 6) Endpoints Collaboration (19)

Base: `/collaboration`

### Workgroups

1. `GET /workgroups` → liste des groupes
2. `POST /workgroups` → créer groupe
   - Entrée:
   ```json
   {
     "name": "Groupe React",
     "description": "Projet frontend",
     "creatorId": "usr-001"
   }
   ```
   - Sortie:
   ```json
   {
     "success": true,
     "id": "<workgroupId>"
   }
   ```

### Documents

3. `GET /workgroups/{groupId}/documents`
4. `POST /workgroups/{groupId}/documents`
   - Entrée:
   ```json
   {
     "name": "cahier-des-charges.pdf",
     "type": "pdf",
     "size": "2MB",
     "url": "https://...",
     "addedBy": "usr-001"
   }
   ```
   - Sortie: `{ "success": true }`

### Homeworks

5. `GET /workgroups/{groupId}/homeworks`
6. `POST /workgroups/{groupId}/homeworks` (retourne `success=true`)

### Tasks

7. `GET /workgroups/{groupId}/tasks`
8. `POST /workgroups/{groupId}/tasks`
   - Entrée:
   ```json
   {
     "title": "Intégrer API",
     "status": "TODO",
     "assignedTo": "usr-002"
   }
   ```
   - Sortie: `{ "success": true }`

### Messages

9. `GET /workgroups/{groupId}/messages`
10. `POST /workgroups/{groupId}/messages`
    - Entrée:
    ```json
    {
      "sender": "usr-001",
      "content": "Bonjour équipe"
    }
    ```
    - Sortie: `{ "success": true }`

### Meets

11. `GET /meets`
12. `POST /meets`
    - Entrée:
    ```json
    {
      "title": "Daily",
      "link": "https://meet.example.com/abc",
      "time": "2026-08-01T10:00"
    }
    ```
    - Sortie: `{ "success": true }`
13. `GET /admin-meets`

### Voice sessions

14. `POST /voice-sessions`
    - Entrée:
    ```json
    {
      "groupId": "<workgroupId>",
      "title": "Session audio"
    }
    ```
    - Sortie (exemple):
    ```json
    {
      "sessionId": "voice-xxxxxx",
      "title": "Session audio",
      "groupId": "...",
      "createdAt": "...",
      "success": true
    }
    ```
15. `GET /voice-sessions`
16. `GET /voice-sessions/{sessionId}`
17. `DELETE /voice-sessions/{sessionId}`
18. `GET /workgroups/{groupId}/voice`
19. `POST /workgroups/{groupId}/voice`

## 7) Endpoints GroupChat (26)

Base: `/collaboration/groupchat`

1. `POST /groups`
2. `POST /groups/{id}/avatar` (**multipart**, champ `file`)
3. `GET /groups/{id}`
4. `GET /users/{userId}/chats`
5. `PUT /groups/{id}/name`
6. `POST /groups/{id}/participants`
7. `DELETE /groups/{id}/participants/{participantId}`
8. `POST /groups/{id}/description`
9. `POST /groups/{id}/admins`
10. `DELETE /groups/{id}/admins/{adminId}`
11. `POST /groups/{id}/mute`
12. `POST /groups/{id}/unmute`
13. `POST /groups/{id}/archive`
14. `POST /groups/{id}/unarchive`
15. `POST /groups/{id}/pin`
16. `POST /groups/{id}/unpin`
17. `GET /groups/types/{type}`
18. `POST /individual`
19. `POST /groups/{id}/messages/text`
20. `POST /groups/{id}/messages/audio` (**multipart**, champs `audio` + `senderId`)
21. `POST /groups/{id}/messages/image` (**multipart**, champs `image` + `senderId`)
22. `GET /groups/{id}/messages`
23. `POST /messages/{id}/read`
24. `DELETE /messages/{id}`
25. `GET /groups/{id}/frontend`
26. `POST /test/run-all`

## 8) Exemples multipart (important)

### Upload avatar

```ts
const form = new FormData();
form.append('file', selectedFile);
await api.post(`/collaboration/groupchat/groups/${groupId}/avatar`, form, {
  headers: { 'Content-Type': 'multipart/form-data' },
});
```

### Envoyer audio

```ts
const form = new FormData();
form.append('audio', audioFile);
form.append('senderId', 'user-001');
await api.post(`/collaboration/groupchat/groups/${groupId}/messages/audio`, form, {
  headers: { 'Content-Type': 'multipart/form-data' },
});
```

### Envoyer image

```ts
const form = new FormData();
form.append('image', imageFile);
form.append('senderId', 'user-001');
await api.post(`/collaboration/groupchat/groups/${groupId}/messages/image`, form, {
  headers: { 'Content-Type': 'multipart/form-data' },
});
```

## 9) WebSocket (style WhatsApp: temps réel)

Endpoint handshake OK: `/api/ws/info`

Exemple client STOMP:

```ts
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

const client = new Client({
  webSocketFactory: () => new SockJS('http://localhost:8080/api/ws'),
  reconnectDelay: 5000,
});

client.onConnect = () => {
  // adapter destinations selon vos topics backend
  client.subscribe('/topic/collaboration', (frame) => {
    const payload = JSON.parse(frame.body);
    console.log('Realtime event', payload);
  });
};

client.activate();
```

## 10) Contrat d’intégration frontend (à respecter)

1. Toujours authentifier l’utilisateur via `/auth/login-token`.
2. Injecter `Authorization: Bearer <token>` dans tous les appels collaboration.
3. Envoyer du JSON valide avec `Content-Type: application/json`.
4. Pour les uploads, utiliser `multipart/form-data` avec les **bons noms de champs**:
   - avatar: `file`
   - audio: `audio`
   - image: `image`
   - sender: `senderId`
5. Gérer `401/403` en redirigeant vers login + refresh session.
6. Gérer le temps réel via STOMP/SockJS pour une UX type messagerie.

## 11) Vérification rapide côté frontend

Checklist:

- [ ] Login OK, token stocké
- [ ] `GET /collaboration/workgroups` retourne `200`
- [ ] Création groupe chat OK
- [ ] Upload avatar/audio/image OK
- [ ] WebSocket `/api/ws/info` accessible
- [ ] Gestion erreurs `401/403/500` validée dans l’UI

---

Statut backend validé: tests module collaboration `45/45` passés.