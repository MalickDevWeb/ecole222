package ecole_221.sn.ecole.modules.archivage.infrastructure.stockage;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;

/**
 * Client Cloudinary réel (SDK com.cloudinary déjà présent, voir CloudinaryStorageAdapter) : envoi en fichier brut
 * privé ({@code raw}, {@code authenticated}), relecture par lien de téléchargement signé à durée courte (API
 * {@code download}), suppression par {@code destroy}, existence par l'API d'administration.
 */
public class ClientCloudinaryArchivesSdk implements ClientCloudinaryArchives {

    private final Cloudinary cloudinary;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(20))
            .followRedirects(HttpClient.Redirect.NORMAL).build();

    public ClientCloudinaryArchivesSdk(String cloudName, String apiKey, String apiSecret) {
        this.cloudinary = new Cloudinary(ObjectUtils.asMap("cloud_name", cloudName, "api_key", apiKey,
                "api_secret", apiSecret, "secure", true));
    }

    @Override
    public void televerser(String publicId, byte[] contenu) throws IOException {
        try {
            cloudinary.uploader().upload(contenu, ObjectUtils.asMap("public_id", publicId, "resource_type", "raw",
                    "type", "authenticated", "overwrite", true, "invalidate", true));
        } catch (RuntimeException e) {
            throw new IOException("Envoi vers Cloudinary impossible", e);
        }
    }

    @Override
    public byte[] telecharger(String publicId) throws IOException {
        String url;
        try {
            url = cloudinary.privateDownload(publicId, "", ObjectUtils.asMap("resource_type", "raw",
                    "type", "authenticated", "expires_at", Instant.now().plusSeconds(600).getEpochSecond()));
        } catch (Exception e) {
            throw new IOException("Lien de lecture Cloudinary impossible", e);
        }
        try {
            HttpResponse<byte[]> r = http.send(HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofMinutes(2)).GET().build(),
                    HttpResponse.BodyHandlers.ofByteArray());
            if (r.statusCode() != 200) {
                throw new IOException("Lecture Cloudinary refusée (HTTP " + r.statusCode() + ")");
            }
            return r.body();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Lecture Cloudinary interrompue", e);
        }
    }

    @Override
    public void detruire(String publicId) throws IOException {
        try {
            cloudinary.uploader().destroy(publicId, ObjectUtils.asMap("resource_type", "raw", "type", "authenticated",
                    "invalidate", true));
        } catch (RuntimeException e) {
            throw new IOException("Suppression Cloudinary impossible", e);
        }
    }

    @Override
    public boolean existe(String publicId) throws IOException {
        try {
            cloudinary.api().resource(publicId, ObjectUtils.asMap("resource_type", "raw", "type", "authenticated"));
            return true;
        } catch (com.cloudinary.api.exceptions.NotFound e) {
            return false;
        } catch (Exception e) {
            throw new IOException("Cloudinary injoignable", e);
        }
    }
}
