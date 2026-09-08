# Student Module - Frontend Integration Guide
*Guide d'intégration rapide pour les développeurs frontend*

## 📋 Contenu

1. [Prérequis](#prérequis)
2. [Installation](#installation)
3. [Configuration](#configuration)
4. [Authentification](#authentification)
5. [Exemples d'utilisation](#exemples-dutilisation)
6. [Bonnes pratiques](#bonnes-pratiques)
7. [Dépannage](#dépannage)

---

## Prérequis

- Node.js 16+ (pour JavaScript/TypeScript)
- Un navigateur moderne avec support ES6+
- Accès à l'API backend: `http://localhost:8080/api`

---

## Installation

### Option 1: TypeScript (Recommandé)

1. Copiez le fichier `STUDENT_API_TYPES.ts` dans votre projet
2. Installez les dépendances si nécessaire:
```bash
npm install
```

### Option 2: JavaScript

1. Utilisez le fichier `STUDENT_API_TYPES.ts` comme référence
2. Convertissez les types TypeScript en interfaces JSDoc si nécessaire

---

## Configuration

### 1. Configuration de base

```typescript
// config/api.ts
export const API_CONFIG = {
  BASE_URL: 'http://localhost:8080/api',
  TIMEOUT: 30000, // 30 secondes
  RETRY_ATTEMPTS: 3,
};
```

### 2. Configuration de l'environnement

```typescript
// config/env.ts
const ENV = {
  development: {
    BASE_URL: 'http://localhost:8080/api',
  },
  production: {
    BASE_URL: 'https://api.ecole221.sn/api',
  },
};

export const API_BASE_URL = ENV[process.env.NODE_ENV || 'development'].BASE_URL;
```

---

## Authentification

### Flow d'authentification

```typescript
// services/auth.ts
import { StudentApiClient } from './STUDENT_API_TYPES';

class AuthService {
  private apiClient: StudentApiClient;
  private tokenKey = 'auth_token';

  constructor() {
    this.apiClient = new StudentApiClient();
    this.loadToken();
  }

  async login(email: string, password: string) {
    try {
      const response = await fetch(`${this.apiClient['baseUrl']}/auth/login-token`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email, password }),
      });

      if (!response.ok) {
        throw new Error('Login failed');
      }

      const data = await response.json();
      this.apiClient.setToken(data.token);
      localStorage.setItem(this.tokenKey, data.token);
      
      return data;
    } catch (error) {
      console.error('Login error:', error);
      throw error;
    }
  }

  logout() {
    this.apiClient.setToken('');
    localStorage.removeItem(this.tokenKey);
  }

  private loadToken() {
    const token = localStorage.getItem(this.tokenKey);
    if (token) {
      this.apiClient.setToken(token);
    }
  }

  isAuthenticated(): boolean {
    return !!localStorage.getItem(this.tokenKey);
  }
}

export const authService = new AuthService();
```

---

## Exemples d'utilisation

### React Example

```tsx
// components/StudentProfile.tsx
import React, { useEffect, useState } from 'react';
import { StudentApiClient, StudentProfile } from './STUDENT_API_TYPES';

const apiClient = new StudentApiClient();

export const StudentProfile: React.FC = () => {
  const [profile, setProfile] = useState<StudentProfile | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const loadProfile = async () => {
      try {
        setLoading(true);
        const data = await apiClient.getProfile();
        setProfile(data);
      } catch (err) {
        setError('Failed to load profile');
        console.error(err);
      } finally {
        setLoading(false);
      }
    };

    loadProfile();
  }, []);

  if (loading) return <div>Loading...</div>;
  if (error) return <div>Error: {error}</div>;

  return (
    <div className="profile">
      <h1>{profile?.prenom} {profile?.nom}</h1>
      <p>Email: {profile?.email}</p>
      <p>Matricule: {profile?.matricule || 'Non défini'}</p>
      <p>Filière: {profile?.filiere || 'Non défini'}</p>
    </div>
  );
};
```

### Vue Example

```vue
<!-- components/StudentDashboard.vue -->
<template>
  <div class="dashboard">
    <div v-if="loading">Loading...</div>
    <div v-else-if="error" class="error">{{ error }}</div>
    <div v-else class="dashboard-content">
      <h2>Tableau de bord</h2>
      <div class="stats">
        <div class="stat">
          <h3>Moyenne Générale</h3>
          <p>{{ dashboard?.moyenneGenerale || 0 }}/20</p>
        </div>
        <div class="stat">
          <h3>Crédits</h3>
          <p>{{ dashboard?.totalCredits || 0 }}</p>
        </div>
        <div class="stat">
          <h3>Cours en cours</h3>
          <p>{{ dashboard?.coursEnCours || 0 }}</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { StudentApiClient, StudentDashboard } from './STUDENT_API_TYPES';

const apiClient = new StudentApiClient();
const dashboard = ref<StudentDashboard | null>(null);
const loading = ref(true);
const error = ref<string | null>(null);

onMounted(async () => {
  try {
    dashboard.value = await apiClient.getDashboard();
  } catch (err) {
    error.value = 'Failed to load dashboard';
    console.error(err);
  } finally {
    loading.value = false;
  }
});
</script>
```

### Angular Example

```typescript
// services/student.service.ts
import { Injectable } from '@angular/core';
import { StudentApiClient, StudentProfile, StudentDashboard } from './STUDENT_API_TYPES';

@Injectable({
  providedIn: 'root'
})
export class StudentService {
  private apiClient: StudentApiClient;

  constructor() {
    this.apiClient = new StudentApiClient();
  }

  async getProfile(): Promise<StudentProfile> {
    return this.apiClient.getProfile();
  }

  async getDashboard(): Promise<StudentDashboard> {
    return this.apiClient.getDashboard();
  }

  setToken(token: string) {
    this.apiClient.setToken(token);
  }
}
```

```typescript
// components/profile/profile.component.ts
import { Component, OnInit } from '@angular/core';
import { StudentService, StudentProfile } from '../../services/student.service';

@Component({
  selector: 'app-profile',
  template: `
    <div *ngIf="loading">Loading...</div>
    <div *ngIf="error" class="error">{{ error }}</div>
    <div *ngIf="profile" class="profile">
      <h1>{{ profile.prenom }} {{ profile.nom }}</h1>
      <p>Email: {{ profile.email }}</p>
    </div>
  `
})
export class ProfileComponent implements OnInit {
  profile: StudentProfile | null = null;
  loading = true;
  error: string | null = null;

  constructor(private studentService: StudentService) {}

  async ngOnInit() {
    try {
      this.profile = await this.studentService.getProfile();
    } catch (err) {
      this.error = 'Failed to load profile';
      console.error(err);
    } finally {
      this.loading = false;
    }
  }
}
```

---

## Cas d'utilisation courants

### 1. Afficher la liste des cours

```typescript
const courses = await apiClient.getCourses();
console.log('Cours disponibles:', courses);
```

### 2. Effectuer un check-in QR code

```typescript
const checkInResult = await apiClient.selfCheckIn({
  type: 'PRESENT',
  method: 'QR_CODE'
});
console.log('Check-in réussi:', checkInResult.success);
```

### 3. Soumettre un devoir

```typescript
// HTML: <input type="file" id="homeworkFile" />
const fileInput = document.getElementById('homeworkFile') as HTMLInputElement;
if (fileInput.files && fileInput.files[0]) {
  const result = await apiClient.submitHomework(
    'course-1',
    'hw-001',
    fileInput.files[0],
    'Voici mon devoir'
  );
  console.log('Devoir soumis:', result.success);
}
```

### 4. Chatter avec le tuteur IA

```typescript
const response = await apiClient.tutorChat({
  message: 'J\'ai besoin d\'aide pour le TP Docker'
});
console.log('Réponse du tuteur:', response.text);
```

### 5. Récupérer les notes avec pagination

```typescript
const attendances = await apiClient.getAttendances(0, 20);
console.log(`Page ${attendances.page} sur ${attendances.totalPages}`);
console.log(`Total: ${attendances.totalElements} présences`);
```

---

## Bonnes pratiques

### 1. Gestion des erreurs

```typescript
async function safeApiCall<T>(
  apiCall: () => Promise<T>,
  errorMessage: string
): Promise<T | null> {
  try {
    return await apiCall();
  } catch (error) {
    console.error(errorMessage, error);
    // Afficher une notification à l'utilisateur
    showNotification(errorMessage, 'error');
    return null;
  }
}

// Utilisation
const profile = await safeApiCall(
  () => apiClient.getProfile(),
  'Impossible de charger le profil'
);
```

### 2. Loading states

```typescript
const [loading, setLoading] = useState(false);
const [data, setData] = useState(null);

async function loadData() {
  setLoading(true);
  try {
    const result = await apiClient.getDashboard();
    setData(result);
  } finally {
    setLoading(false);
  }
}
```

### 3. Cache simple

```typescript
const cache = new Map<string, { data: any; timestamp: number }>();
const CACHE_DURATION = 5 * 60 * 1000; // 5 minutes

async function cachedGet<T>(key: string, apiCall: () => Promise<T>): Promise<T> {
  const cached = cache.get(key);
  if (cached && Date.now() - cached.timestamp < CACHE_DURATION) {
    return cached.data;
  }

  const data = await apiCall();
  cache.set(key, { data, timestamp: Date.now() });
  return data;
}

// Utilisation
const dashboard = await cachedGet('dashboard', () => apiClient.getDashboard());
```

### 4. Retry logic

```typescript
async function retryApiCall<T>(
  apiCall: () => Promise<T>,
  maxRetries: number = 3
): Promise<T> {
  for (let i = 0; i < maxRetries; i++) {
    try {
      return await apiCall();
    } catch (error) {
      if (i === maxRetries - 1) throw error;
      await new Promise(resolve => setTimeout(resolve, 1000 * (i + 1)));
    }
  }
  throw new Error('Max retries exceeded');
}
```

---

## Dépannage

### Problème: Erreur 401 Unauthorized

**Cause:** Token manquant ou expiré

**Solution:**
```typescript
// Vérifier si le token est présent
const token = localStorage.getItem('auth_token');
if (!token) {
  // Rediriger vers la page de login
  window.location.href = '/login';
}

// Si le token est expiré, le user doit se reconnecter
```

### Problème: Erreur CORS

**Cause:** Configuration CORS du backend

**Solution:** Vérifier que le backend autorise votre domaine frontend

### Problème: Upload de fichier échoue

**Cause:** Mauvaise configuration du Content-Type

**Solution:**
```typescript
// NE PAS définir Content-Type manuellement pour multipart
// Le navigateur le fait automatiquement avec le boundary
const formData = new FormData();
formData.append('file', file);

// CORRECT
await fetch(url, {
  method: 'POST',
  headers: { 'Authorization': `Bearer ${token}` }, // PAS de Content-Type
  body: formData,
});

// INCORRECT
await fetch(url, {
  method: 'POST',
  headers: { 
    'Authorization': `Bearer ${token}`,
    'Content-Type': 'multipart/form-data' // À éviter
  },
  body: formData,
});
```

### Problème: Pagination ne fonctionne pas

**Cause:** Paramètres de pagination incorrects

**Solution:**
```typescript
// CORRECT
const data = await apiClient.getAttendances(0, 10); // page 0, size 10

// INCORRECT
const data = await apiClient.getAttendances(0); // size manquant
```

---

## Test de l'intégration

### Script de test rapide

```typescript
async function testIntegration() {
  const apiClient = new StudentApiClient();
  
  // Test login
  console.log('Testing login...');
  const loginResponse = await fetch('http://localhost:8080/api/auth/login-token', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email: 'student@ecole221.sn', password: 'password' }),
  });
  const loginData = await loginResponse.json();
  apiClient.setToken(loginData.token);
  console.log('✅ Login successful');

  // Test profile
  console.log('Testing profile...');
  const profile = await apiClient.getProfile();
  console.log('✅ Profile loaded:', profile.nom, profile.prenom);

  // Test dashboard
  console.log('Testing dashboard...');
  const dashboard = await apiClient.getDashboard();
  console.log('✅ Dashboard loaded:', dashboard.moyenneGenerale);

  // Test courses
  console.log('Testing courses...');
  const courses = await apiClient.getCourses();
  console.log('✅ Courses loaded:', courses.length, 'courses');

  console.log('🎉 All tests passed!');
}

testIntegration().catch(console.error);
```

---

## Ressources supplémentaires

- **Contrat API complet:** `STUDENT_API_CONTRACT.md`
- **Types TypeScript:** `STUDENT_API_TYPES.ts`
- **Documentation Swagger:** `http://localhost:8080/swagger-ui.html`
- **Résultats des tests:** `student_29endpoints_test_results.log`

---

## Support

Pour toute question ou problème:
1. Consultez le contrat API (`STUDENT_API_CONTRACT.md`)
2. Vérifiez les logs de tests (`student_29endpoints_test_results.log`)
3. Contactez l'équipe backend

---

## Checklist d'intégration

- [ ] Copier les fichiers API dans le projet
- [ ] Configurer l'URL de base de l'API
- [ ] Implémenter le système d'authentification
- [ ] Tester la connexion avec un endpoint simple (profile)
- [ ] Implémenter la gestion des erreurs
- [ ] Ajouter les loading states
- [ ] Tester les endpoints principaux
- [ ] Implémenter le cache si nécessaire
- [ ] Tester les uploads de fichiers
- [ ] Déployer en environnement de test

---

**Dernière mise à jour:** 2026-08-02
**Version:** 1.0.0
**Statut:** ✅ Tous les endpoints testés et fonctionnels
