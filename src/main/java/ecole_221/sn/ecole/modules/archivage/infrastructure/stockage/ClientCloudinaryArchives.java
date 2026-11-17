package ecole_221.sn.ecole.modules.archivage.infrastructure.stockage;

import java.io.IOException;

/**
 * Opérations Cloudinary utiles aux archives, sur des fichiers BRUTS ({@code resource_type raw}) en accès privé
 * ({@code type authenticated}). Séparées du SDK pour tester le découpage sans réseau.
 */
public interface ClientCloudinaryArchives {

    void televerser(String publicId, byte[] contenu) throws IOException;

    /** Contenu du fichier ; IOException s'il n'existe pas ou n'est pas lisible. */
    byte[] telecharger(String publicId) throws IOException;

    void detruire(String publicId) throws IOException;

    boolean existe(String publicId) throws IOException;
}
