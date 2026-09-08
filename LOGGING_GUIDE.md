# Guide de Logging - École 221

## Vue d'ensemble

Un système de logging complet a été mis en place pour faciliter le débogage et le suivi de toutes les actions dans l'application. Ce système capture automatiquement :

1. **Toutes les requêtes HTTP** (GET, POST, PUT, DELETE, PATCH)
2. **Toutes les méthodes de service** (use cases, services métier)
3. **Actions métier importantes** (authentification, administration, sécurité, etc.)

## Composants du Système de Logging

### 1. RequestLoggingFilter

**Fichier :** `src/main/java/ecole_221/sn/ecole/infrastructure/web/RequestLoggingFilter.java`

Ce filtre intercepte TOUTES les requêtes HTTP et log :
- Méthode HTTP et chemin complet
- Headers (avec masquage des données sensibles)
- Corps de la requête (pour POST, PUT, PATCH, DELETE)
- Adresse IP de l'utilisateur
- Statut de la réponse
- Temps de traitement
- Alertes de performance pour les requêtes lentes

### 2. MethodLoggingAspect

**Fichier :** `src/main/java/ecole_221/sn/ecole/infrastructure/logging/MethodLoggingAspect.java`

Cet aspect AOP intercepte TOUTES les méthodes des couches :
- Application (use cases, services)
- Domain (domain models, value objects)

Il log :
- Entrée de méthode avec arguments
- Sortie de méthode avec résultat
- Durée d'exécution
- Exceptions avec stack trace complet

### 3. ActionLogger

**Fichier :** `src/main/java/ecole_221/sn/ecole/infrastructure/logging/ActionLogger.java`

Service centralisé pour logger les actions métier importantes avec contexte.

**Méthodes disponibles :**
- `logAuthAction()` - Actions d'authentification (login, logout)
- `logStudentAction()` - Actions des étudiants
- `logProfessorAction()` - Actions des professeurs
- `logAdminAction()` - Actions d'administration
- `logSecurityAction()` - Actions de sécurité
- `logCollaborationAction()` - Actions de collaboration
- `logMediaAction()` - Actions d'upload média
- `logSyncAction()` - Actions de synchronisation
- `logSystemError()` - Erreurs système

## Contrôleurs avec ActionLogger Intégré

### ✅ Contrôleurs équipés de l'ActionLogger

1. **AuthController** - Actions d'authentification
   - POST /auth/login - Connexion utilisateur
   - POST /auth/logout - Déconnexion

2. **AdminController** - Actions d'administration
   - POST /admin/gates/assign - Assignation de portail
   - PATCH /admin/students/{id}/status - Modification statut étudiant

3. **SecurityController** - Actions de sécurité
   - POST /security/scan-logs - Scan de badge
   - POST /vigil/scan - Scan vigil

4. **ProfessorController** - Actions de professeur
   - POST /professor/courses/{courseId}/grades/{studentId} - Saisie de notes

5. **StudentController** - Actions d'étudiant
   - POST /student/attendances - Auto check-in

6. **CollaborationController** - Actions de collaboration
   - POST /collaboration/workgroups - Création de groupe

7. **UploadController** - Actions de média
   - POST /media/upload - Upload de fichier

8. **SyncController** - Actions de synchronisation
   - POST /sync - Synchronisation batch

### ⏳ Contrôleurs sans ActionLogger (pour l'instant)

1. **RootController** - Redirection simple
2. **CandidaturesController** - Lecture simple
3. **PlanningController** - Lecture simple
4. **CommonController** - Health check
5. **ProfExtraController** - Fonctionnalités supplémentaires
6. **LiveSessionMessageController** - WebSocket (différent)

## Configuration du Logging

**Fichier :** `src/main/resources/application.yaml`

```yaml
logging:
  level:
    root: INFO
    ecole_221.sn.ecole: DEBUG
    org.springframework.web: DEBUG
    org.springframework.security: DEBUG
    org.hibernate.SQL: DEBUG
    org.hibernate.type.descriptor.sql.BasicBinder: TRACE
    org.springframework.jdbc: DEBUG
    org.springframework.transaction: DEBUG
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n"
    file: "%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n"
  file:
    name: logs/application.log
    max-size: 10MB
    max-history: 30
```

## Sécurité des Logs

Le système de logging protège automatiquement les données sensibles :

1. **Headers sensibles masqués :**
   - Authorization
   - Cookie
   - Set-Cookie
   - X-Auth-Token
   - Password

2. **Données sensibles dans les arguments :**
   - Champs contenant "password", "secret", "token", "credential"
   - Sont remplacés par `[SENSITIVE_DATA_REDACTED]`

3. **Troncature des données longues :**
   - Corps de requête > 2000 caractères
   - Arguments > 200 caractères
   - Résultats > 500 caractères
   - Stack traces > 500 caractères

## Performance Monitoring

Le système génère des alertes automatiques pour les requêtes lentes :

- **⚠️ SLOW REQUEST DETECTED (>1s)** - Requêtes très lentes
- **⚠️ MODERATE REQUEST TIME (>500ms)** - Requêtes modérément lentes
- **✓ Response time acceptable** - Requêtes normales

## Utilisation dans le Code

### 1. Ajouter le logging d'actions dans les contrôleurs

Les contrôleurs principaux sont déjà équipés de l'ActionLogger. Pour les nouveaux contrôleurs :

```java
@RestController
@RequestMapping("/auth")
public class AuthController {
    
    private final ActionLogger actionLogger;
    
    public AuthController(LoginUseCase loginUseCase, ActionLogger actionLogger) {
        this.loginUseCase = loginUseCase;
        this.actionLogger = actionLogger;
    }
    
    @PostMapping("/login")
    public ResponseEntity<LoginResponseDto> login(@RequestBody LoginCommand command) {
        try {
            LoginResponseDto dto = loginUseCase.login(command);
            
            // Logger l'action réussie
            actionLogger.logAuthAction("LOGIN", command.getEmail(), 
                dto.getRole().name(), true, "Connexion réussie");
            
            return ResponseEntity.ok(dto);
        } catch (Exception e) {
            // Logger l'action échouée
            actionLogger.logAuthAction("LOGIN", command.getEmail(), 
                "UNKNOWN", false, "Échec: " + e.getMessage());
            throw e;
        }
    }
}
```

### 2. Les méthodes sont automatiquement loggées par l'aspect

Aucune configuration nécessaire - toutes les méthodes des couches application et domain sont automatiquement loggées.

### 3. Les requêtes HTTP sont automatiquement loggées par le filtre

Aucune configuration nécessaire - toutes les requêtes HTTP sont automatiquement loggées.

## Débogage

### Voir les logs en temps réel

```bash
# Suivre les logs de la console
./mvnw spring-boot:run

# Ou suivre les fichiers de logs
tail -f logs/application.log
```

### Filtrer les logs

```bash
# Voir uniquement les requêtes HTTP
grep "INCOMING HTTP REQUEST" logs/application.log

# Voir uniquement les actions métier
grep "ACTION LOGGED" logs/application.log

# Voir uniquement les erreurs
grep "ERROR" logs/application.log

# Voir uniquement les requêtes lentes
grep "SLOW REQUEST" logs/application.log
```

### Niveaux de log

- **TRACE** - Détails très fins (SQL avec paramètres)
- **DEBUG** - Informations de débogage (méthodes, requêtes)
- **INFO** - Informations générales (actions, événements)
- **WARN** - Avertissements (performance, problèmes mineurs)
- **ERROR** - Erreurs (exceptions, échecs)

## Meilleures Pratiques

1. **Utiliser ActionLogger pour les actions métier importantes**
   - Authentification
   - Modifications de données
   - Actions administratives
   - Opérations de sécurité

2. **Laisser l'aspect AOP logger les méthodes automatiquement**
   - Pas besoin d'ajouter des logs manuels dans les services
   - L'aspect capture déjà les entrées/sorties

3. **Le filtre HTTP capture déjà toutes les requêtes**
   - Pas besoin de logger les requêtes manuellement
   - Les headers et corps sont déjà capturés

4. **Protéger les données sensibles**
   - Le système masque automatiquement les données sensibles
   - Éviter de logger des mots de passe ou tokens en clair

5. **Surveiller les performances**
   - Les alertes de performance sont automatiques
   - Surveiller les requêtes > 500ms

## Logs Stockés

Les logs sont stockés dans :
- **Console** - Sortie standard Maven
- **Fichier** - `logs/application.log` (rotation automatique)
  - Taille max : 10MB par fichier
  - Historique : 30 jours
  - Format : texte avec timestamps

## Résolution de Problèmes

### Problème : Trop de logs

**Solution :** Ajuster les niveaux de log dans `application.yaml`
```yaml
logging:
  level:
    ecole_221.sn.ecole: INFO  # Au lieu de DEBUG
    org.springframework.web: INFO  # Au lieu de DEBUG
```

### Problème : Logs manquants

**Solution :** Vérifier que les composants sont activés
- RequestLoggingFilter doit être un @Component
- MethodLoggingAspect doit être un @Component
- ActionLogger doit être un @Component

### Problème : Données sensibles dans les logs

**Solution :** Le système les masque automatiquement, mais vérifiez :
- Les headers sensibles sont dans SENSITIVE_HEADERS
- Les mots-clés sensibles sont détectés
- Évitez de logger manuellement des données sensibles

## Conclusion

Ce système de logging complet permet de :
- **Déboguer facilement** toutes les requêtes et méthodes
- **Surveiller les performances** de l'application
- **Tracer les actions métier** importantes
- **Protéger les données sensibles** automatiquement
- **Identifier les problèmes** rapidement

Le système est entièrement automatique pour les requêtes HTTP et les méthodes de service. Les actions métier importantes nécessitent un logging explicite via l'ActionLogger, qui est déjà intégré dans les principaux contrôleurs de l'application.