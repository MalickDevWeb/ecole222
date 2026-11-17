package ecole_221.sn.ecole.modules.archivage.domain.port;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

/**
 * Port de sortie : où vit le fichier d'archive chiffré d'une école (décision du Tech Lead : stockage adaptatif selon
 * l'environnement, un seul réglage {@code ecole.archivage.stockage} choisit l'adaptateur).
 *
 * <p>Adaptateurs : {@code local} (dossier de la machine ou volume Docker, développement) et {@code cloudinary}
 * (production, fichiers bruts privés découpés en morceaux). Un adaptateur compatible S3 pourra s'ajouter sans toucher
 * au moteur d'archivage. Le fichier confié est déjà chiffré : l'adaptateur ne voit jamais de donnée en clair ni la clé.</p>
 */
public interface StockageArchive {

    /** Nom de l'adaptateur, rangé avec l'archive (« local », « cloudinary »). */
    String nom();

    /**
     * Refuse (ArchivageException INDISPONIBLE, message clair) si l'adaptateur n'est pas utilisable ici : jamais de
     * repli silencieux sur un autre stockage.
     */
    void verifierPret();

    /** Dépose le fichier sous un nom donné ; renvoie l'emplacement (référence opaque) à ranger avec l'archive. */
    String ecrire(String nomArchive, Path fichier) throws IOException;

    /** Relit le fichier déposé, octet pour octet. */
    InputStream lire(String emplacement) throws IOException;

    /** Supprime le fichier (sans erreur s'il n'existe plus). */
    void supprimer(String emplacement) throws IOException;

    boolean existe(String emplacement) throws IOException;
}
