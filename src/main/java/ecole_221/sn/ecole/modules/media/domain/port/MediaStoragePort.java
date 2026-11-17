package ecole_221.sn.ecole.modules.media.domain.port;

import ecole_221.sn.ecole.modules.media.domain.model.UploadedFile;
import java.io.IOException;
import java.util.Optional;

public interface MediaStoragePort {
    UploadedFile upload(byte[] fileBytes, String fileName, String contentType) throws IOException;

    /**
     * Envoi CONFIDENTIEL (17/11 : pièces d'identité, relevés, justificatifs, coffre de l'élève) : le fichier n'est pas
     * lisible par son lien ; seul le serveur le relit ({@link #relire}) pour une personne autorisée.
     */
    default UploadedFile upload(byte[] fileBytes, String fileName, String contentType, boolean confidentiel) throws IOException {
        return upload(fileBytes, fileName, contentType);
    }

    /**
     * Relit par le SERVEUR un fichier déjà envoyé (16/11 : pièces des candidats). Seuls les liens de ce stockage sont
     * acceptés (compte Cloudinary de l'application, ou fichier local du mode simulation) : jamais une adresse
     * quelconque. Vide si le lien n'appartient pas à ce stockage ; IOException si le stockage ne répond pas.
     */
    default Optional<FichierRelu> relire(String lien) throws IOException {
        return Optional.empty();
    }

    /** Contenu relu et son type MIME (ex. application/pdf, image/png). */
    record FichierRelu(byte[] contenu, String typeContenu) {}
}
