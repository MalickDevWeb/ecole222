# Skill : Historisation des Bugs (Bug Logging)

## Objectif
Lorsque tu rencontres, provoques ou corriges un bug (erreur de compilation, crash d'application, erreur 500, boucle infinie, etc.), tu dois OBLIGATOIREMENT créer un fichier de log spécifique pour tracer le problème.

## Règle d'exécution
1. **Création du fichier** : Crée un fichier nommé `nom_du_bug.log` à la racine de l'application concernée (par exemple `boot_error.log`, `react_router_loop.log`, etc.).
2. **Contenu du fichier** : 
   Le fichier doit contenir :
   - La date et l'heure du bug.
   - La trace complète de l'erreur (stack trace) ou le message d'erreur exact.
   - Le contexte d'apparition (ce qui a déclenché l'erreur).
   - Les étapes de résolution appliquées.
3. **Conservation** : Ne supprime pas ce fichier une fois corrigé, sauf si explicitement demandé par l'utilisateur. Cela sert d'historique (post-mortem) pour de futures références.

## Exemple
Si une erreur survient au lancement de Spring Boot, génère un fichier `boot_error_flyway.log` contenant les logs d'erreur, puis résous le problème en te basant sur ce fichier.
