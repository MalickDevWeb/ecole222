package ecole_221.sn.ecole.modules.professor.domain.model;

import java.util.UUID;

/**
 * Factory pour créer des instances de Course.
 * Permet d'accéder au constructeur privé de l'agrégat racine.
 */
public class CourseFactory {
    
    /**
     * Crée un nouveau cours avec un ID généré.
     */
    public static Course create(String name, String classe, int totalHours, String profId) {
        String id = UUID.randomUUID().toString();
        return Course.createInternal(id, name, classe, totalHours, profId, null, null);
    }

    /**
     * Crée un nouveau cours avec un ID généré et code/description personnalisés.
     */
    public static Course create(String name, String classe, int totalHours, String profId,
                             String code, String description) {
        String id = UUID.randomUUID().toString();
        return Course.createInternal(id, name, classe, totalHours, profId, code, description);
    }

    /**
     * Reconstruit un cours existant (pour la persistance).
     */
    public static Course reconstruct(String id, String name, String classe, int totalHours,
                                  String profId, String code, String description) {
        return Course.createInternal(id, name, classe, totalHours, profId, code, description);
    }
}