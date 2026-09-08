package ecole_221.sn.ecole.modules.collaboration.domain.model;

import java.util.UUID;

/**
 * Factory pour créer des instances de Workgroup.
 * Permet d'accéder au constructeur privé de l'agrégat racine.
 */
public class WorkgroupFactory {
    
    /**
     * Crée un nouveau groupe de travail avec un ID généré.
     */
    public static Workgroup create(String name, String description, String creatorId) {
        String id = UUID.randomUUID().toString();
        return new Workgroup(id, name, description, creatorId, 0);
    }
    
    /**
     * Crée un nouveau groupe de travail avec un ID généré et un nombre de membres initial.
     */
    public static Workgroup create(String name, String description, String creatorId, int initialMemberCount) {
        String id = UUID.randomUUID().toString();
        return new Workgroup(id, name, description, creatorId, initialMemberCount);
    }
    
    /**
     * Reconstruit un groupe de travail existant (pour la persistance).
     */
    public static Workgroup reconstruct(String id, String name, String description, String creatorId, int memberCount) {
        return new Workgroup(id, name, description, creatorId, memberCount);
    }
}