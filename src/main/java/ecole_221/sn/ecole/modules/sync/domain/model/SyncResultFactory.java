package ecole_221.sn.ecole.modules.sync.domain.model;

import ecole_221.sn.ecole.modules.sync.domain.valueobject.SyncStatus;

/**
 * Factory pour créer des instances de SyncResult.
 * Permet d'accéder au constructeur privé de l'agrégat racine.
 */
public class SyncResultFactory {
    
    /**
     * Crée un nouveau résultat de synchronisation avec l'horodatage actuel.
     */
    public static SyncResult create(String actionId, SyncStatus status, String message) {
        long now = System.currentTimeMillis();
        return SyncResult.createInternal(actionId, status, message, now);
    }

    /**
     * Crée un résultat de succès.
     */
    public static SyncResult success(String actionId, String message) {
        return create(actionId, SyncStatus.SUCCESS, message);
    }

    /**
     * Crée un résultat d'échec.
     */
    public static SyncResult failure(String actionId, String message) {
        return create(actionId, SyncStatus.FAILED, message);
    }

    /**
     * Reconstruit un résultat de synchronisation existant (pour la persistance).
     */
    public static SyncResult reconstruct(String actionId, SyncStatus status, String message, long timestamp) {
        return SyncResult.createInternal(actionId, status, message, timestamp);
    }
}