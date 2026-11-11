package ecole_221.sn.ecole.modules.media.infrastructure.adapter.out.cloudinary;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ecole_221.sn.ecole.modules.media.domain.model.UploadedFile;
import ecole_221.sn.ecole.modules.media.domain.model.UploadedFileFactory;
import ecole_221.sn.ecole.modules.media.domain.port.MediaStoragePort;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

@Component
public class CloudinaryStorageAdapter implements MediaStoragePort {

    private final String cloudName;
    private final String apiKey;
    private final String apiSecret;
    private Cloudinary cloudinary;

    public CloudinaryStorageAdapter(
            @Value("${cloudinary.cloud-name:}") String cloudName,
            @Value("${cloudinary.api-key:}") String apiKey,
            @Value("${cloudinary.api-secret:}") String apiSecret,
            @Value("${cloudinary.simulation:false}") boolean simulation) {
        this.simulation = simulation;
        this.cloudName = cloudName;
        this.apiKey = apiKey;
        this.apiSecret = apiSecret;
    }

    /** Simulation explicite (développement / QA isolée uniquement) : CLOUDINARY_SIMULATION=true. */
    private final boolean simulation;

    private synchronized Cloudinary getCloudinary() {
        if (cloudinary == null) {
            if (cloudName == null || cloudName.isBlank() ||
                apiKey == null || apiKey.isBlank() ||
                apiSecret == null || apiSecret.isBlank()) {
                System.out.println("[WARN] Les variables Cloudinary ne sont pas configurées. Mode Simulation activé.");
                return null;
            }
            cloudinary = new Cloudinary(ObjectUtils.asMap(
                    "cloud_name", cloudName,
                    "api_key", apiKey,
                    "api_secret", apiSecret,
                    "secure", true
            ));
        }
        return cloudinary;
    }

    @Override
    @SuppressWarnings("rawtypes")
    public UploadedFile upload(byte[] fileBytes, String fileName, String contentType) throws IOException {
        String id = "file-" + UUID.randomUUID().toString().substring(0, 8);
        Cloudinary client = getCloudinary();

        if (simulation) {
            String simulatedUrl = "https://res.cloudinary.com/demo/image/upload/v1570979139/sample.jpg";
            return UploadedFileFactory.create(fileName, simulatedUrl, "simulation-" + id, fileBytes.length, contentType);
        }

        if (client == null) {
            throw new ecole_221.sn.ecole.modules.media.domain.exception.StockageIndisponibleException(
                    "Stockage des fichiers non configuré : le document n'a pas été enregistré.");
        }

        try {
            Map uploadResult = client.uploader().upload(fileBytes, ObjectUtils.asMap(
                    "public_id", id,
                    "resource_type", "auto"
            ));

            String url = (String) uploadResult.get("secure_url");
            String publicId = (String) uploadResult.get("public_id");

            return UploadedFileFactory.create(fileName, url, publicId, fileBytes.length, contentType);
        } catch (Exception e) {
            System.err.println("[ERROR] Échec de l'upload sur Cloudinary: " + e.getMessage());
            // APP-BE-05 : erreur explicite au lieu d'une image de démonstration présentée comme le document
            throw new ecole_221.sn.ecole.modules.media.domain.exception.StockageIndisponibleException(
                    "Le stockage des fichiers a échoué : le document n'a pas été enregistré. Réessayez plus tard.", e);
        }
    }
}
