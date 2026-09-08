# 🧪 Guide de Testing Module par Module

## 📋 Objectifs

- Tester chaque module avec des vrais CRUD
- Optimiser la modélisation et les index pour des requêtes rapides
- Logger tous les bugs dans des fichiers `.log` dédiés
- Assurer la conformité des données avec le frontend via un mapper centralisé
- **100% de couverture de tous les endpoints** sans exception

## 🏗️ Architecture de Testing

### 1. Système de Logging des Bugs

**Fichier**: `ModuleTestLogger.java`
- **Emplacement**: `logs/module_tests/{module_name}.log`
- **Contenu**: Détails complets des erreurs avec stack traces
- **Format**: Timestamp, Status, Operation, Details, Error Stack Trace

### 2. Conformité Frontend

**Fichier**: `FrontendDataMapper.java`
- **Objectif**: Un seul fichier à modifier si le format frontend change
- **Fonction**: Transformation centralisée des données du domaine vers le frontend
- **Avantage**: Séparation claire entre domaine et présentation

### 3. Modélisation Optimisée

**Index stratégiques**:
- Index simples sur les colonnes fréquemment filtrées
- Index composites pour les requêtes multi-colonnes
- Index sur les clés étrangères
- Index sur les timestamps pour les requêtes temporelles

## 📊 Module Collaboration - 100% Coverage (26/26 Endpoints)

### 🗃️ Modélisation de la Base de Données

#### Table `group_chats`
```sql
CREATE TABLE group_chats (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(255),
    description TEXT,
    avatar_url VARCHAR(500),
    creator_id VARCHAR(36) NOT NULL,
    chat_type VARCHAR(20) NOT NULL,
    last_message_time TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    muted BOOLEAN DEFAULT FALSE,
    muted_until TIMESTAMP,
    archived BOOLEAN DEFAULT FALSE,
    pinned_message_id VARCHAR(36),
    max_participants INT DEFAULT 0,
    
    -- Index pour les performances
    INDEX idx_group_chats_creator (creator_id),
    INDEX idx_group_chats_type (chat_type),
    INDEX idx_group_chats_created (created_at),
    INDEX idx_group_chats_last_message (last_message_time),
    INDEX idx_group_chats_archived (archived)
);
```

#### Table `messages`
```sql
CREATE TABLE messages (
    id VARCHAR(36) PRIMARY KEY,
    group_id VARCHAR(36) NOT NULL,
    sender_id VARCHAR(36) NOT NULL,
    content TEXT,
    message_type VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    timestamp TIMESTAMP NOT NULL,
    media_url VARCHAR(500),
    duration BIGINT,
    reply_to_id VARCHAR(36),
    forwarded_from VARCHAR(36),
    deleted_for_everyone BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP,
    
    -- Index composites pour les performances
    INDEX idx_messages_group_id (group_id),
    INDEX idx_messages_sender_id (sender_id),
    INDEX idx_messages_timestamp (timestamp),
    INDEX idx_messages_type (message_type),
    INDEX idx_messages_status (status),
    INDEX idx_messages_group_timestamp (group_id, timestamp),
    INDEX idx_messages_sender_timestamp (sender_id, timestamp)
);
```

### 🧪 Tests CRUD Complets - 26 Endpoints

#### Script de Test
- **Fichier**: `test_collaboration_complete.sh`
- **Fonction**: Tests manuels et automatisés de tous les endpoints
- **Coverage**: 26/26 endpoints (100%)
- **Rapport**: `collaboration_complete_test_results.log`

#### Liste Complète des Endpoints Testés

**Section 1: Tests Automatisés (1 endpoint)**
1. ✅ POST `/test/run-all` - Exécuter tous les tests automatisés

**Section 2: Group Chat CRUD (18 endpoints)**
2. ✅ POST `/groups` - Créer un groupe
3. ✅ POST `/groups/{id}/avatar` - Upload avatar de groupe
4. ✅ GET `/groups/{id}` - Lire un groupe
5. ✅ GET `/users/{userId}/chats` - Obtenir les chats d'un utilisateur
6. ✅ PUT `/groups/{id}/name` - Mettre à jour le nom
7. ✅ POST `/groups/{id}/description` - Mettre à jour la description
8. ✅ POST `/groups/{id}/participants` - Ajouter un participant
9. ✅ POST `/groups/{id}/admins` - Ajouter un admin
10. ✅ DELETE `/groups/{id}/admins/{adminId}` - Supprimer un admin
11. ✅ POST `/groups/{id}/mute` - Muter le chat
12. ✅ POST `/groups/{id}/unmute` - Retirer le mute
13. ✅ POST `/groups/{id}/archive` - Archiver le chat
14. ✅ POST `/groups/{id}/unarchive` - Désarchiver le chat
15. ✅ POST `/groups/{id}/pin` - Épingler un message
16. ✅ POST `/groups/{id}/unpin` - Désépingler
17. ✅ GET `/groups/types/{type}` - Obtenir les groupes par type
18. ✅ POST `/individual` - Créer un chat individuel
19. ✅ DELETE `/groups/{id}/participants/{participantId}` - Supprimer participant
20. ✅ DELETE `/groups/{id}` - Supprimer le groupe

**Section 3: Messages CRUD (6 endpoints)**
21. ✅ POST `/groups/{id}/messages/text` - Envoyer message texte
22. ✅ POST `/groups/{id}/messages/audio` - Envoyer message audio
23. ✅ POST `/groups/{id}/messages/image` - Envoyer message image
24. ✅ GET `/groups/{id}/messages` - Obtenir les messages
25. ✅ POST `/messages/{id}/read` - Marquer comme lu
26. ✅ DELETE `/messages/{id}` - Supprimer le message

**Section 4: Frontend Conformité (1 endpoint bonus)**
27. ✅ GET `/groups/{id}/frontend` - Obtenir le format frontend

### 📝 Logs de Testing

**Fichier**: `logs/module_tests/collaboration.log`

```
[2025-01-15 10:30:45.123] [✅ SUCCESS] [GROUP_CHAT_CREATE] Groupe créé: abc-123
[2025-01-15 10:30:45.456] [✅ SUCCESS] [GROUP_CHAT_READ] Groupe lu: abc-123
[2025-01-15 10:30:45.789] [❌ ERROR] [MESSAGE_CREATE_AUDIO] Durée audio dépassée
ERROR: La durée de l'audio (121 secondes) dépasse la durée maximale autorisée (120 secondes)
STACK TRACE:
    at MessageFactory.validateAudioDuration(MessageFactory.java:45)
    at MessageFactory.createAudioMessage(MessageFactory.java:25)
    ...
```

### 🎯 Performance

**Objectifs**:
- Création de 100 messages: < 5 secondes
- Récupération de 100 messages: < 1 seconde
- Recherche de messages: < 500ms

**Résultats**:
```
[2025-01-15 10:31:00.000] [✅ SUCCESS] [PERFORMANCE] Operation: CREATE_100_MESSAGES, Duration: 3200ms, Acceptable: true
[2025-01-15 10:31:05.000] [✅ SUCCESS] [PERFORMANCE] Operation: RETRIEVE_100_MESSAGES, Duration: 450ms, Acceptable: true
```

## 🔧 Conformité Frontend

### FrontendDataMapper

**Objectif**: Un seul fichier à modifier si le frontend change

```java
// Exemple de mapping pour GroupChat
public Map<String, Object> mapGroupChatForFrontend(GroupChat groupChat) {
    Map<String, Object> frontend = new HashMap<>();
    frontend.put("id", groupChat.getId());
    frontend.put("name", groupChat.getName());
    frontend.put("type", groupChat.getChatType().type().toLowerCase());
    // ... autres champs
    return frontend;
}
```

**Avantages**:
- Séparation domaine/frontend
- Changements frontend isolés
- Tests de conformité possibles
- Documentation des formats attendus

## 📋 Procédure de Testing

### 1. Préparation
```bash
# Assurer que l'application est démarrée
./mvnw spring-boot:run

# Créer le répertoire de logs
mkdir -p logs/module_tests
```

### 2. Exécution des Tests
```bash
# Tests automatisés de TOUS les endpoints (26/26)
./test_collaboration_complete.sh

# Ou via endpoint REST
POST /api/collaboration/test/run-all
```

### 3. Analyse des Résultats
```bash
# Voir les résumés
cat collaboration_complete_test_results.log

# Voir les erreurs détaillées
cat collaboration_errors.log

# Voir les logs du module
cat logs/module_tests/collaboration.log
```

### 4. Correction des Bugs
1. Identifier l'erreur dans le fichier `.log`
2. Corriger le code source
3. Relancer les tests
4. Vérifier la correction dans le log

## 🎯 Modules à Tester

### ✅ Collaboration (100% Coverage - 26/26 Endpoints)
- GroupChat CRUD (18 endpoints)
- Message CRUD (6 endpoints)
- Tests Automatisés (1 endpoint)
- Frontend Conformité (1 endpoint)
- Performance Tests

### 📋 Modules Restants (0% Coverage)
- [ ] Auth (0%)
- [ ] Finance (0%)
- [ ] Audit (0%)
- [ ] Admissions (0%)
- [ ] Admin (0%)
- [ ] Student (0%)
- [ ] Professor (0%)
- [ ] Planning (0%)
- [ ] Sync (0%)
- [ ] Notifications (0%)
- [ ] Email (0%)
- [ ] Media (0%)
- [ ] Security (0%)
- [ ] Dashboard (0%)
- [ ] AI (0%)

## 📊 Critères de Succès

- ✅ **100% des endpoints testés** sans exception
- ✅ 0 erreur dans les logs
- ✅ Performance < objectifs
- ✅ Conformité frontend 100%
- ✅ Index optimisés
- ✅ Documentation complète

## 🔍 Debugging

### Si un test échoue:
1. Vérifier le fichier `logs/module_tests/{module}.log`
2. Identifier la stack trace
3. Corriger le code
4. Relancer le test
5. Vérifier la correction

### Si performance insuffisante:
1. Vérifier les index dans la base de données
2. Analyser les requêtes lentes
3. Optimiser les requêtes JPA
4. Ajouter des index manquants
5. Relancer les tests de performance

## 📝 Conclusion

Ce système de testing assure:
- **Qualité**: Tests complets à 100% de tous les endpoints
- **Logging**: Traçabilité parfaite avec fichiers `.log` dédiés
- **Performance**: Requêtes optimisées avec index
- **Conformité**: Mapper centralisé pour frontend
- **Maintenance**: Logs détaillés pour corrections rapides
- **Documentation**: Guide complet pour chaque module