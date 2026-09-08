# DDD Best Practices Skill

Ce skill contient les meilleures pratiques pour l'architecture DDD + Hexagonale afin d'éviter les erreurs de compilation récurrentes.

## 🚨 Erreurs Courantes à Éviter

### 1. Utilisation directe des constructeurs privés
**ERREUR** : `new Agrégat()` au lieu d'utiliser les Factories
```java
// ❌ ERREUR
Workgroup workgroup = new Workgroup();
workgroup.setId("id");
workgroup.setName("name");

// ✅ CORRECT
Workgroup workgroup = WorkgroupFactory.create("name", "description", "creatorId", 1);
```

### 2. Modification d'objets immuables
**ERREUR** : Utiliser des setters sur des objets sans setters
```java
// ❌ ERREUR
task.setStatus("COMPLETED");
securityEvent.setId("new-id");

// ✅ CORRECT
// Créer une nouvelle instance avec les valeurs mises à jour
Task updatedTask = TaskFactory.reconstruct(task.getId(), task.getTitle(), 
    task.getDescription(), task.getWorkgroupId(), task.getAssigneeId(), 
    TaskStatus.COMPLETED, task.getPriority());
```

### 3. Conversions Value Objects ↔ String
**ERREUR** : Assigner directement String ↔ Value Object
```java
// ❌ ERREUR
String status = task.getStatus(); // TaskStatus ne peut pas être converti en String
task.setStatus("COMPLETED"); // String ne peut pas être converti en TaskStatus

// ✅ CORRECT
String status = task.getStatus().name(); // Utiliser .name() pour String
TaskStatus taskStatus = TaskStatus.valueOf("COMPLETED"); // Utiliser .valueOf() pour Value Object
```

### 4. Types de retour Optional
**ERREUR** : Retourner l'objet au lieu d'Optional
```java
// ❌ ERREUR
public User getById(String id) {
    return repository.findById(id).orElse(null);
}

// ✅ CORRECT
public Optional<User> getById(String id) {
    return repository.findById(id);
}
```

### 5. Appels aux constructeurs privés dans les Factories
**ERREUR** : Les Factories n'ont pas accès aux constructeurs privés
```java
// ❌ ERREUR dans Factory
public static Task create(...) {
    return new Task(...); // Constructeur privé inaccessible
}

// ✅ CORRECT
// Dans l'agrégat, le constructeur doit être package-private ou public pour les Factories
// Ou utiliser une méthode statique interne
public class Task {
    private Task(...) { ... }
    
    // Méthode package-private pour Factory
    static Task createInternal(...) {
        return new Task(...);
    }
}
```

## 🎯 Pattern Correct DDD + Hexagonale

### Structure d'un Agrégat
```java
public class Workgroup {
    private final String id;
    private final String name;
    private final String description;
    private final String creatorId;
    private final int memberCount;
    
    // Constructeur privé
    private Workgroup(String id, String name, String description, String creatorId, int memberCount) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.creatorId = creatorId;
        this.memberCount = memberCount;
    }
    
    // Getters seulement (pas de setters)
    public String getId() { return id; }
    public String getName() { return name; }
    // ...
}
```

### Structure d'une Factory
```java
public class WorkgroupFactory {
    
    public static Workgroup create(String name, String description, String creatorId) {
        String id = UUID.randomUUID().toString();
        return new Workgroup(id, name, description, creatorId, 0);
    }
    
    public static Workgroup create(String name, String description, String creatorId, int initialMemberCount) {
        String id = UUID.randomUUID().toString();
        return new Workgroup(id, name, description, creatorId, initialMemberCount);
    }
    
    public static Workgroup reconstruct(String id, String name, String description, String creatorId, int memberCount) {
        return new Workgroup(id, name, description, creatorId, memberCount);
    }
}
```

### Structure d'un Service
```java
@Service
public class CollaborationService implements CollaborationUseCase {
    
    private final CollaborationRepositoryPort repository;
    
    @Override
    public WorkgroupDto createWorkgroup(String name, String description) {
        // ✅ Utiliser la Factory
        Workgroup workgroup = WorkgroupFactory.create(name, description, "system", 1);
        Workgroup saved = repository.saveWorkgroup(workgroup);
        return toDto(saved);
    }
    
    @Override
    public void updateTaskStatus(String taskId, String status) {
        // ❌ NE PAS faire : task.setStatus(status);
        // ✅ Créer une nouvelle instance
        Task existing = repository.findTaskById(taskId).orElseThrow();
        Task updated = TaskFactory.reconstruct(
            existing.getId(), 
            existing.getTitle(), 
            existing.getDescription(),
            existing.getWorkgroupId(), 
            existing.getAssigneeId(),
            TaskStatus.valueOf(status), // Conversion String → Value Object
            existing.getPriority()
        );
        repository.saveTask(updated);
    }
}
```

## 📋 Checklist Avant Commit

- [ ] **Jamais** de `new Agrégat()` → Toujours utiliser `Factory.create()`
- [ ] **Jamais** de setters sur objets immuables → Créer nouvelle instance
- [ ] **Toujours** convertir Value Objects ↔ String avec `.name()` / `.valueOf()`
- [ ] **Toujours** retourner `Optional<T>` dans les interfaces UseCase
- [ ] **Toujours** vérifier que les Factories ont accès aux constructeurs
- [ ] **Toujours** utiliser les méthodes de reconstruction pour la persistance

## 🔧 Correction Rapide

### Si erreur "constructor has private access"
```java
// Ajouter cette méthode dans l'agrégat
static ClassName createInternal(...) {
    return new ClassName(...);
}

// Et dans la Factory
public static ClassName create(...) {
    return ClassName.createInternal(...);
}
```

### Si erreur "incompatible types: ValueObject cannot be converted to String"
```java
// Remplacer
String value = object.getValueObject();

// Par
String value = object.getValueObject().name();
```

### Si erreur "incompatible types: String cannot be converted to ValueObject"
```java
// Remplacer
object.setValueObject(stringValue);

// Par
object.setValueObject(ValueObject.valueOf(stringValue));
```

## 🎯 Objectif

Respecter strictement ces règles garantit :
- ✅ Compilation sans erreur
- ✅ Architecture DDD cohérente
- ✅ Objets immuables respectés
- ✅ Séparation domaine/application correcte