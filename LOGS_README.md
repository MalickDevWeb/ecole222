# 📝 Configuration des Logs de Debug

Ce document explique comment **activer** et **désactiver** les logs de debug dans l'application.

---

## 🖥️ Backend (Spring Boot - Java)

### Structure des logs

Les logs ont été ajoutés dans les fichiers suivants avec **SLF4J/Logback** :

#### Domain Model
- `modules/professor/domain/model/Course.java` - Logs pour les getters/setters et constructeurs

#### Mappers
- `modules/professor/application/service/ProfessorMapper.java` - Logs pour toutes les méthodes de conversion

#### Controllers
- `modules/professor/infrastructure/web/ProfessorController.java` - Logs pour toutes les endpoints REST
- `modules/professor/infrastructure/web/ProfExtraController.java` - Logs pour les endpoints supplémentaires

### Niveaux de log

| Niveau | Couleur (Console) | Quand utiliser | Exemple |
|--------|-------------------|----------------|---------|
| **DEBUG** | Bleu | Débogage détaillé, suivi des flux | `logger.debug("Processing course: {}", courseId)` |
| **INFO** | Vert | Informations importantes, événements | `logger.info("Login successful, user: {}", userId)` |
| **WARN** | Jaune | Avertissements, situations anormales non critiques | `logger.warn("Null value detected")` |
| **ERROR** | Rouge | Erreurs, exceptions | `logger.error("Failed to load course: {}", error)` |

### Configuration

#### 1. **Fichier de configuration**

Le fichier `src/main/resources/logback-spring.xml` contrôle le niveau de log selon le profil :

```xml
<!-- Développement : tous les logs (DEBUG, INFO, WARN, ERROR) -->
<springProfile name="dev">
    <root level="DEBUG"/>
</springProfile>

<!-- Production : seulement INFO, WARN, ERROR -->
<springProfile name="prod">
    <root level="INFO"/>
</springProfile>
```

#### 2. **Activer le mode Développement**

Lancer l'application avec le profil **dev** :

```bash
# Avec Maven
mvn spring-boot:run -Dspring.profiles.active=dev

# Avec l'IDE (IntelliJ/VSCode)
# Ajouter dans VM Options: -Dspring.profiles.active=dev
```

**Résultat** : Tous les logs DEBUG et plus seront affichés dans la console.

#### 3. **Activer le mode Production**

Lancer l'application avec le profil **prod** :

```bash
# Avec Maven
mvn spring-boot:run -Dspring.profiles.active=prod

# En production (Docker, etc.)
java -jar votre-application.jar --spring.profiles.active=prod
```

**Résultat** : Seuls les logs INFO, WARN et ERROR seront affichés. Les logs DEBUG seront masqués.

#### 4. **Logs dans des fichiers (optionnel)**

Le fichier `logback-spring.xml` configure aussi l'écriture des logs dans `logs/application.log`.

Pour désactiver les logs fichiers, commentez la ligne :
```xml
<appender-ref ref="FILE"/>
```

---

## 🌐 Frontend (TypeScript/Angular/Vite)

### Structure des logs

#### Configuration centrale
- `src/core/config.ts` - Configuration générale et fonctions de log
- `src/shared/utils/logger.ts` - Logger centralisé avec formatage

#### Stores
- `src/core/store/authStore.ts` - Logs pour la gestion d'authentification (login/logout)

#### API Repository
- `src/features/auth/infrastructure/api/ApiAuthRepository.ts` - Logs pour les requêtes HTTP (déjà existant)

### Niveaux de log

| Niveau | Méthode | Couleur | Quand utiliser |
|--------|---------|---------|----------------|
| Debug | `logger.debug()` | Bleu | Débogage détaillé |
| Info | `logger.info()` | Vert | Informations |
| Warn | `logger.warn()` | Jaune | Avertissements |
| Error | `logger.error()` | Rouge | Erreurs |
| Trace | `logger.trace()` | Gris | Trace complète de la stack |
| Group | `logger.group()` | - | Grouper des logs |

### Configuration

#### 1. **Activer les logs dans le Frontend**

Créer ou modifier le fichier `.env` à la racine du projet frontend :

```bash
# .env ou .env.development
VITE_ENABLE_DEBUG_LOGS=true
NODE_ENV=development
```

**Résultat** : Tous les logs seront affichés dans la console du navigateur.

#### 2. **Désactiver les logs en Production**

Dans le fichier `.env.production` :

```bash
# .env.production
VITE_ENABLE_DEBUG_LOGS=false
NODE_ENV=production
```

Ou simplement ne pas définir `VITE_ENABLE_DEBUG_LOGS`.

**Résultat** : Aucun log de debug ne sera affiché.

#### 3. **Utilisation dans le code**

Importer le logger dans vos fichiers :

```typescript
import { logger } from '@/shared/utils/logger';

// Exemples
type User = { id: string; name: string };

function processUser(user: User) {
  logger.debug('Processing user', { userId: user.id });
  
  try {
    // ... traitement
    logger.info('User processed successfully', { userId: user.id });
  } catch (error) {
    logger.error('Failed to process user', { userId: user.id, error });
  }
}

// Avec groupe de logs
logger.group('User Registration', () => {
  logger.debug('Step 1: Validating data');
  logger.debug('Step 2: Saving to database');
  logger.debug('Step 3: Sending email');
});
```

---

## 🎯 Bonnes pratiques

### Backend (Java)

1. **Utilisez les niveaux de log appropriés** :
   - `DEBUG` pour le débogage interne (appels de méthodes, paramètres)
   - `INFO` pour les événements importants (connexion réussie, création de ressource)
   - `WARN` pour les situations anormales mais gérées
   - `ERROR` pour les exceptions et erreurs critiques

2. **Évitez les logs sensibles** :
   ```java
   // ❌ Mauvaise pratique - expose le mot de passe
   logger.debug("User login: email={}, password={}", email, password);
   
   // ✅ Bonne pratique - masque les données sensibles
   logger.debug("User login attempt: email={}, password=*****", email);
   ```

3. **Ne loggez pas dans les boucles** :
   ```java
   // ❌ Mauvaise pratique - flood de logs
   for (User user : users) {
     logger.info("Processing user: {}", user.getId());
   }
   
   // ✅ Bonne pratique - log unique
   logger.info("Processing {} users", users.size());
   ```

### Frontend (TypeScript)

1. **Masquez les données sensibles** :
   ```typescript
   // ❌ Mauvaise pratique
   logger.debug('Login payload', { email, password });
   
   // ✅ Bonne pratique
   logger.debug('Login attempt', { email, password: '******' });
   ```

2. **Utilisez le mode offline avec précaution** :
   - Les données sauvegardées dans localStorage peuvent être accédées par JavaScript
   - Ne stockez jamais de tokens ou mots de passe en localStorage
   - Utilisez HttpOnly cookies pour la sécurité

3. **Testez toujours avec les logs activés** :
   - Avant de déployer, testez en mode développement avec les logs activés
   - Vérifiez que les logs sont utiles pour le débogage

---

## 🚀 Configuration par environnement

### Développement local

```bash
# Backend
mvn spring-boot:run -Dspring.profiles.active=dev

# Frontend
# Dans .env.development:
VITE_ENABLE_DEBUG_LOGS=true
```

### Staging/Test

```bash
# Backend
java -jar app.jar --spring.profiles.active=dev

# Frontend
# Dans .env.staging:
VITE_ENABLE_DEBUG_LOGS=true
```

### Production

```bash
# Backend
java -jar app.jar --spring.profiles.active=prod

# Frontend
# Dans .env.production:
VITE_ENABLE_DEBUG_LOGS=false
# ou ne pas définir la variable
```

---

## 📊 Exemples de logs

### Backend

```
2026-07-25 15:30:45.123 [http-nio-8080-exec-1] DEBUG e.s.e.m.p.d.m.Course - Getting id for Course: course-123
2026-07-25 15:30:45.124 [http-nio-8080-exec-1] INFO  e.s.e.m.p.i.w.ProfessorController - GET /professor/courses - profId=usr-002, page=0, size=10
2026-07-25 15:30:45.150 [http-nio-8080-exec-1] DEBUG e.s.e.m.p.a.s.ProfessorMapper - Converting Course to CourseDto, courseId=course-123
2026-07-25 15:30:45.155 [http-nio-8080-exec-1] ERROR e.s.e.m.p.i.w.ProfessorController - Error fetching courses: Connection timeout
```

### Frontend

```
[2026-07-25T15:30:45.123Z] [DEBUG] [development] Requête HTTP POST vers auth/login avec l'email: user@ecole.sn
[2026-07-25T15:30:45.456Z] [INFO] [development] Mode local backend activé ? true
[2026-07-25T15:30:45.789Z] [INFO] [development] [AUTH STORE] Login successful, user: { userId: 'usr-002', email: 'user@ecole.sn' }
```

---

## 🛠 Outils de monitoring

### Backend
- **Log files** : `logs/application.log` (créé automatiquement)
- **Console** : Tous les logs sont affichés en couleur
- **Outils** : Utilisez `tail -f logs/application.log` pour suivre les logs en temps réel

### Frontend
- **Console du navigateur** : F12 → Console
- **Filtres** : Utilisez les filtres par niveau (DEBUG, INFO, WARN, ERROR)
- **Format** : Les logs sont formatés avec timestamp et niveau

---

## ✅ Résumé

| Environnement | Backend (Profile) | Frontend (VITE_ENABLE_DEBUG_LOGS) | Niveau de log |
|---------------|------------------|------------------------------------|---------------|
| Développement | `dev` | `true` | DEBUG + INFO + WARN + ERROR |
| Staging | `dev` ou `staging` | `true` | DEBUG + INFO + WARN + ERROR |
| Production | `prod` | `false` ou non défini | INFO + WARN + ERROR |

---

*Dernière mise à jour : 25 juillet 2026*
