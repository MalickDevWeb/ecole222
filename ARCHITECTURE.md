# Architecture du Projet École 221

## Structure Organisationnelle

Le projet suit une architecture hexagonale modulaire avec une séparation claire entre le code partagé et le code spécifique aux modules.

## Structure des Dossiers

```
ecole_221/sn/ecole/
├── infrastructure/          # Infrastructure partagée
│   ├── config/            # Configuration Spring
│   ├── security/          # Sécurité JWT
│   ├── logging/           # Logging
│   └── aspect/            # Aspects AOP
├── modules/               # Modules métier
│   ├── admin/            # Administration
│   ├── student/          # Espace étudiant
│   ├── professor/        # Espace professeur
│   ├── auth/             # Authentification
│   ├── dashboard/        # Dashboard
│   ├── admissions/       # Admissions
│   └── ...              # Autres modules
└── partage/              # Code partagé entre modules
    ├── dto/              # DTOs communs
    ├── exception/        # Exceptions communes
    └── valueobject/      # Value objects partagés
```

## Convention de Structuration des Modules

Chaque module respecte la structure suivante pour garantir la cohérence :

```
module/
├── application/                    # Couche application
│   ├── port/                       # Ports (interfaces)
│   │   ├── dto/                   # DTOs spécifiques au module
│   │   ├── exception/             # Exceptions spécifiques au module
│   │   ├── usecase/               # Interfaces des cas d'utilisation
│   │   └── command/               # Commandes pour les actions
│   └── service/                   # Services métiers
├── domain/                        # Couche domaine
│   ├── model/                     # Modèles du domaine
│   ├── valueobject/               # Value objects spécifiques
│   └── port/                      # Ports de sortie
│       └── repository/           # Interfaces de repository
└── infrastructure/                # Couche infrastructure
    ├── persistence/               # JPA Entities et Repositories
    └── web/                       # Contrôleurs REST
```

## Règles de Séparation

### Code dans `partage/`

Le code doit être placé dans le dossier `partage/` si :

1. **DTOs Génériques** : Les DTOs utilisés par plusieurs modules (ex: `PageResponseDto`)
2. **Exceptions Communes** : Les exceptions levées par plusieurs modules (ex: `CourseNotFoundException`)
3. **Value Objects Partagés** : Les énumérations et value objects communs
4. **Utilitaires Génériques** : Les fonctions utilitaires utilisées par plusieurs modules

### Code dans les modules

Le code doit rester dans les modules spécifiques si :

1. **DTOs Spécifiques** : Les DTOs propres à un module (ex: `StudentProfileDto`, `CourseDto` spécifique)
2. **Exceptions Spécifiques** : Les exceptions propres à un module (ex: `StudentNotFoundException`)
3. **Logique Métier** : Services, mappers, cas d'utilisation spécifiques
4. **Entités JPA** : Entités de persistance spécifiques

## Réorganisation Effectuée

### Éléments déplacés dans `partage/`

1. **`partage/dto/PageResponseDto.java`**
   - DTO générique pour la pagination
   - Remplace les versions dupliquées dans `student` et `professor`
   - Utilisé par tous les modules pour les réponses paginées

2. **`partage/exception/CourseNotFoundException.java`**
   - Exception levée quand un cours n'est pas trouvé
   - Remplace les versions dupliquées dans `student` et `professor`
   - Partagée car les deux modules manipulent des cours

### Éléments gardés dans les modules

1. **DTOs spécifiques** :
   - `CourseDto` dans `student` (vue étudiant) vs `professor` (vue professeur)
   - `GradeDto` dans `student` (consultation) vs `professor` (saisie)
   - `HomeworkDto` spécifique à chaque contexte

2. **Exceptions spécifiques** :
   - `StudentNotFoundException` reste dans `student`
   - `LessonNotFoundException` reste dans `professor`
   - etc.

## Avantages de cette Architecture

1. **Évite la duplication** : Les éléments communs ne sont pas redéfinis
2. **Cohérence** : Tous les modules utilisent les mêmes structures de base
3. **Maintenabilité** : Les modifications aux éléments partagés sont propagées automatiquement
4. **Clarté** : Séparation nette entre ce qui est partagé et ce qui est spécifique
5. **Flexibilité** : Chaque module peut avoir ses propres DTOs adaptés à son contexte

## Bonnes Pratiques

1. **Toujours vérifier si un DTO existe déjà dans `partage/` avant d'en créer un nouveau**
2. **Ne créer une exception dans `partage/` que si elle est vraiment partagée**
3. **Documenter la raison du placement dans les commentaires JavaDoc**
4. **Privilégier la composition sur l'héritage pour les DTOs spécifiques**
5. **Maintenir la cohérence des noms entre modules**

## Prochaines Améliorations Possibles

1. Déplacer d'autres DTOs communs vers `partage/` (ex: `UserDto`, `NotificationDto`)
2. Créer des mappers partagés pour les conversions communes
3. Standardiser les réponses d'erreur (API Response wrappers)
4. Créer des value objects partagés pour les enums communs (statuts, priorités, etc.)
5. Documenter les contrats d'API pour chaque module

## Compilation et Tests

- ✅ Le projet compile avec succès après la réorganisation
- ✅ Les imports ont été mis à jour dans tous les modules concernés
- ✅ Les tests existants passent correctement
- ✅ Aucune régression introduite