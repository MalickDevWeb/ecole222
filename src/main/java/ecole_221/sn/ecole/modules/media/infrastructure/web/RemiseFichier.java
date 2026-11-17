package ecole_221.sn.ecole.modules.media.infrastructure.web;

import ecole_221.sn.ecole.modules.media.domain.port.MediaStoragePort;
import ecole_221.sn.ecole.modules.media.domain.port.MediaStoragePort.FichierRelu;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

/**
 * Remise d'un fichier relu par le serveur (16-17/11) : pièces de candidature, justificatifs d'absence, coffre de l'élève.
 * L'appelant a déjà vérifié QUI demande et QUEL dossier ; ici : relecture par le stockage (fichier confidentiel compris),
 * type affichable ou téléchargement, aucun cache, pas d'interprétation du contenu par le navigateur.
 */
public final class RemiseFichier {

    private static final Logger log = LoggerFactory.getLogger(RemiseFichier.class);

    /** Seuls ces types s'affichent dans le navigateur ; tout autre fichier est proposé au téléchargement. */
    public static final Map<String, String> AFFICHABLES = Map.of(
            "application/pdf", "pdf", "image/png", "png", "image/jpeg", "jpg", "image/webp", "webp", "image/gif", "gif");

    private RemiseFichier() { }

    /**
     * @param lien    lien enregistré (null → 404 « pas joint »)
     * @param nomBase nom proposé au navigateur, sans extension (« releves », « justificatif »)
     */
    public static ResponseEntity<byte[]> remettre(MediaStoragePort stockage, String lien, String nomBase) {
        if (lien == null || lien.isBlank()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Cette pièce n'a pas été jointe au dossier.");
        }
        Optional<FichierRelu> fichier;
        try {
            fichier = stockage.relire(lien);
        } catch (IOException e) {
            log.warn("Fichier « {} » : relecture impossible ({})", nomBase, e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "La pièce n'a pas pu être récupérée auprès du stockage des fichiers. Réessayez dans un instant.");
        }
        if (fichier.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "La pièce est introuvable dans le stockage des fichiers.");
        }
        String type = fichier.get().typeContenu();
        String extension = AFFICHABLES.get(type);
        boolean affichable = extension != null;
        String nom = nomBase.replaceAll("[^A-Za-z0-9_-]", "-");
        return ResponseEntity.ok()
                .contentType(affichable ? MediaType.parseMediaType(type) : MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        (affichable ? "inline" : "attachment") + "; filename=\"" + nom + (affichable ? "." + extension : "") + "\"")
                .header("X-Content-Type-Options", "nosniff")
                .cacheControl(CacheControl.noStore().cachePrivate())
                .body(fichier.get().contenu());
    }
}
