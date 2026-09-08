package ecole_221.sn.ecole.modules.sync.domain.model;

import ecole_221.sn.ecole.modules.sync.domain.valueobject.SyncActionType;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Factory pour créer des instances de SyncAction.
 * Permet d'accéder au constructeur privé de l'agrégat racine.
 */
public class SyncActionFactory {
    
    /**
     * Crée une nouvelle action de synchronisation avec un ID généré et l'horodatage actuel.
     */
    public static SyncAction create(SyncActionType type, Map<String, Object> payload) {
        String id = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();
        return SyncAction.createInternal(id, type, now, payload);
    }

    /**
     * Reconstruit une action de synchronisation existante (pour la persistance).
     */
    public static SyncAction reconstruct(String id, SyncActionType type, LocalDateTime timestamp, Map<String, Object> payload) {
        return SyncAction.createInternal(id, type, timestamp, payload);
    }
}