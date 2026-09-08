# README Frontend — Modules Auth & Dashboard

Ce guide explique comment consommer correctement les endpoints `Auth` et `Dashboard` depuis un frontend.

## 1) Base URL

- Base API: `http://localhost:8080/api`
- Auth: `http://localhost:8080/api/auth`
- Dashboard: `http://localhost:8080/api/dashboard`

## 2) Dépendances frontend recommandées

```bash
npm i axios @tanstack/react-query
```

Option validation:

```bash
npm i zod
```

## 3) Authentification

### 3.1 Login cookie (web)

`POST /api/auth/login`

Body:

```json
{
  "email": "admin221@gmail.com",
  "password": "ecole221"
}
```

Réponse: infos utilisateur (token masqué dans le body), cookie `auth_token` HTTP-only créé côté navigateur.

### 3.2 Login token (API/Swagger/mobile)

`POST /api/auth/login-token`

Body:

```json
{
  "email": "admin221@gmail.com",
  "password": "ecole221"
}
```

Réponse:

```json
{
  "token": "<JWT>",
  "role": "ADMIN"
}
```

### 3.3 Déconnexion

`POST /api/auth/logout`

Réponse:

```json
{
  "message": "Déconnexion réussie"
}
```

### 3.4 Profil connecté

`GET /api/auth/me`

Réponse:

```json
{
  "id": "...",
  "nom": "...",
  "prenom": "...",
  "email": "...",
  "role": "ADMIN"
}
```

## 4) Client HTTP prêt à l’emploi (Axios)

```ts
import axios from 'axios';

export const api = axios.create({
  baseURL: 'http://localhost:8080/api',
  timeout: 15000,
  withCredentials: true,
});

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('auth_token');
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});
```

## 5) Endpoints Auth (4)

1. `POST /auth/login`
2. `POST /auth/login-token`
3. `POST /auth/logout`
4. `GET /auth/me`

## 6) Endpoints Dashboard (19)

### KPIs

1. `GET /dashboard/kpis`
2. `GET /dashboard/kpis/{kpiKey}`
3. `GET /dashboard/kpis/category/{category}`
4. `POST /dashboard/kpis`
5. `PUT /dashboard/kpis/{id}`
6. `PATCH /dashboard/kpis/{kpiKey}/value`

### Alertes

7. `GET /dashboard/alerts`
8. `GET /dashboard/alerts/active`
9. `GET /dashboard/alerts/urgent`
10. `POST /dashboard/alerts`
11. `PUT /dashboard/alerts/{id}`
12. `PATCH /dashboard/alerts/{id}/deactivate`

### Activités

13. `GET /dashboard/activities?limit=10`
14. `GET /dashboard/activities/type/{activityType}`
15. `POST /dashboard/activities`

### Stats régionales

16. `GET /dashboard/regional-stats/{academicYear}`
17. `PUT /dashboard/regional-stats/{id}`

### Agrégations

18. `GET /dashboard/complete`
19. `GET /dashboard/financial-kpis`
20. `GET /dashboard/academic-kpis`
21. `GET /dashboard/security-kpis`
22. `POST /dashboard/system-alert`

> Note: le dashboard contient 22 routes exposées dans le contrôleur actuel.

## 7) Exemples de payloads

### Créer un KPI

`POST /dashboard/kpis`

```json
{
  "name": "Taux de réussite",
  "value": "92",
  "unit": "%",
  "trend": "UP"
}
```

### Mettre à jour la valeur d’un KPI

`PATCH /dashboard/kpis/{kpiKey}/value`

```json
{
  "value": "95"
}
```

### Créer une alerte

`POST /dashboard/alerts`

```json
{
  "alertType": "SYSTEM",
  "title": "Incident réseau",
  "message": "Latence élevée détectée",
  "severity": "HIGH",
  "isActive": true
}
```

### Logger une activité

`POST /dashboard/activities`

```json
{
  "activityType": "AUTH",
  "description": "Connexion admin",
  "module": "auth"
}
```

### Créer une alerte système rapide

`POST /dashboard/system-alert`

```json
{
  "alertType": "SYSTEM",
  "title": "CPU élevé",
  "message": "Charge > 90%",
  "severity": "CRITICAL"
}
```

## 8) Gestion des erreurs côté frontend

- `200/201`: succès
- `401`: non authentifié (token/cookie absent ou expiré)
- `403`: interdit selon les règles de sécurité
- `404`: ressource absente (ex: `kpiKey` inexistant)
- `500`: erreur serveur

Exemple:

```ts
try {
  const { data } = await api.get('/dashboard/complete');
  return data;
} catch (e: any) {
  const status = e?.response?.status;
  const message = e?.response?.data?.message || 'Erreur inconnue';
  throw new Error(`[${status}] ${message}`);
}
```

## 9) Contrat d’intégration frontend

1. Toujours appeler `/auth/login` ou `/auth/login-token` avant les routes protégées.
2. Si mode token: envoyer `Authorization: Bearer <JWT>`.
3. Si mode cookie web: `withCredentials: true`.
4. Envoyer du JSON valide avec `Content-Type: application/json`.
5. Gérer explicitement `401/403/404/500` dans l’UI.

## 10) Checklist rapide

- [ ] Login OK (`/auth/login-token`)
- [ ] `GET /auth/me` retourne le profil
- [ ] `GET /dashboard/kpis` retourne `200`
- [ ] `POST /dashboard/kpis` puis `PUT/PATCH` OK
- [ ] `POST /dashboard/alerts` puis désactivation OK
- [ ] `GET /dashboard/complete` OK

---

Statut backend validé pendant cette session: tests `auth + dashboard` exécutés, endpoints couverts en succès, comportement `404` confirmé pour clé KPI inexistante.