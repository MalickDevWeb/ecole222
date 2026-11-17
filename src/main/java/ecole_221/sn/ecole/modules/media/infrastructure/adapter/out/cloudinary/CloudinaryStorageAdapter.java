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

    @org.springframework.beans.factory.annotation.Autowired
    public CloudinaryStorageAdapter(
            @Value("${cloudinary.cloud-name:}") String cloudName,
            @Value("${cloudinary.api-key:}") String apiKey,
            @Value("${cloudinary.api-secret:}") String apiSecret,
            @Value("${cloudinary.simulation:false}") boolean simulation,
            ecole_221.sn.ecole.modules.media.infrastructure.adapter.out.local.StockageLocalFichiers stockageLocal) {
        this.simulation = simulation;
        this.cloudName = cloudName;
        this.apiKey = apiKey;
        this.apiSecret = apiSecret;
        this.stockageLocal = stockageLocal;
    }

    /** Tests : stockage local dans un dossier temporaire. */
    public CloudinaryStorageAdapter(String cloudName, String apiKey, String apiSecret, boolean simulation) {
        this(cloudName, apiKey, apiSecret, simulation, new ecole_221.sn.ecole.modules.media.infrastructure.adapter.out.local.StockageLocalFichiers(
                System.getProperty("java.io.tmpdir") + "/ecole221-media-tests"));
    }

    /** Simulation : le VRAI fichier est rangé sur le disque du serveur et servi par GET /api/public/media/{nom}. */
    private final ecole_221.sn.ecole.modules.media.infrastructure.adapter.out.local.StockageLocalFichiers stockageLocal;

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
    public UploadedFile upload(byte[] fileBytes, String fileName, String contentType) throws IOException {
        return upload(fileBytes, fileName, contentType, false);
    }

    /**
     * 17/11 — fichier confidentiel : rangé chez Cloudinary en type « authenticated » (jamais livré sans signature) ; le
     * lien gardé par l'application est SANS signature : copié et ouvert hors de l'application, il est refusé (401).
     * Seul le serveur le relit (téléchargement signé, {@link #relire}) pour une personne autorisée.
     */
    @Override
    @SuppressWarnings("rawtypes")
    public UploadedFile upload(byte[] fileBytes, String fileName, String contentType, boolean confidentiel) throws IOException {
        String id = "file-" + UUID.randomUUID().toString().substring(0, 8);
        Cloudinary client = getCloudinary();

        if (simulation) {
            // 16/11 : avant, l'image de démonstration de Cloudinary (« sample.jpg ») était renvoyée à la place du fichier
            String nom = stockageLocal.enregistrer(fileBytes, fileName, contentType, confidentiel);
            return UploadedFileFactory.create(fileName, "/api/public/media/" + nom, "local-" + nom, fileBytes.length, contentType);
        }

        if (client == null) {
            throw new ecole_221.sn.ecole.modules.media.domain.exception.StockageIndisponibleException(
                    "Stockage des fichiers non configuré : le document n'a pas été enregistré.");
        }

        try {
            Map<String, Object> options = new java.util.HashMap<>(ObjectUtils.asMap(
                    "public_id", id,
                    "resource_type", "auto"
            ));
            if (confidentiel) options.put("type", "authenticated");
            Map uploadResult = client.uploader().upload(fileBytes, options);

            String url = (String) uploadResult.get("secure_url");
            if (confidentiel) url = sansSignature(url);
            String publicId = (String) uploadResult.get("public_id");

            return UploadedFileFactory.create(fileName, url, publicId, fileBytes.length, contentType);
        } catch (Exception e) {
            System.err.println("[ERROR] Échec de l'upload sur Cloudinary: " + e.getMessage());
            // APP-BE-05 : erreur explicite au lieu d'une image de démonstration présentée comme le document
            throw new ecole_221.sn.ecole.modules.media.domain.exception.StockageIndisponibleException(
                    "Le stockage des fichiers a échoué : le document n'a pas été enregistré. Réessayez plus tard.", e);
        }
    }

    /** Retire la signature (« /s--…--/ ») d'un lien Cloudinary : sans elle, un fichier « authenticated » est refusé. */
    static String sansSignature(String url) {
        return url == null ? null : url.replaceFirst("/s--[^/]+--/", "/");
    }

    // ───────────────────────── Relecture par le serveur (16/11) ─────────────────────────
    // Les comptes Cloudinary gratuits refusent de DIFFUSER les PDF (401 « deny or ACL failure ») : l'administration
    // ne pouvait pas ouvrir les relevés ni les diplômes. Le serveur récupère le fichier par l'API authentifiée de
    // Cloudinary (téléchargement signé), indépendante de ce réglage, et ne le remet qu'à un utilisateur autorisé.

    /** …/res.cloudinary.com/{compte}/{image|raw|video}/{upload|authenticated|private}/[transformations/]v{n}/{public_id}[.{format}] */
    private static final java.util.regex.Pattern LIEN_CLOUDINARY = java.util.regex.Pattern.compile(
            "^https://res\\.cloudinary\\.com/([A-Za-z0-9_-]+)/(image|raw|video)/(upload|authenticated|private)/(?:[^?#]*?/)?v\\d+/([^?#]+)$");
    /** Fichier du mode simulation, servi par GET /api/public/media/{nom}. */
    private static final java.util.regex.Pattern LIEN_LOCAL = java.util.regex.Pattern.compile(
            "^(?:https?://[^/?#]+)?/api/public/media/([^/?#]+)$");
    /** Au-delà de la limite d'envoi (10 Mo), avec de la marge. */
    static final int TAILLE_MAX_RELUE = 20 * 1024 * 1024;
    private static final java.net.http.HttpClient HTTP = java.net.http.HttpClient.newBuilder()
            .connectTimeout(java.time.Duration.ofSeconds(10))
            .followRedirects(java.net.http.HttpClient.Redirect.NORMAL)
            .build();

    /** Lien Cloudinary décomposé : seul un lien du compte de l'application est retenu. */
    record LienCloudinary(String typeRessource, String typeLivraison, String publicId, String format) {}

    static java.util.Optional<LienCloudinary> analyser(String lien, String compte) {
        if (lien == null || compte == null || compte.isBlank()) {
            return java.util.Optional.empty();
        }
        java.util.regex.Matcher m = LIEN_CLOUDINARY.matcher(lien.trim());
        if (!m.matches() || !m.group(1).equals(compte)) {
            return java.util.Optional.empty();
        }
        String typeRessource = m.group(2);
        String chemin = java.net.URLDecoder.decode(m.group(4), java.nio.charset.StandardCharsets.UTF_8);
        if (chemin.contains("..")) {
            return java.util.Optional.empty();
        }
        // Fichier « raw » : l'extension fait partie de l'identifiant ; image/vidéo : l'extension est le format
        int point = chemin.lastIndexOf('.');
        if ("raw".equals(typeRessource) || point < 0 || point < chemin.lastIndexOf('/')) {
            return java.util.Optional.of(new LienCloudinary(typeRessource, m.group(3), chemin, ""));
        }
        return java.util.Optional.of(new LienCloudinary(typeRessource, m.group(3), chemin.substring(0, point),
                chemin.substring(point + 1).toLowerCase(java.util.Locale.ROOT)));
    }

    /** Type MIME d'après l'extension pour les formats affichables ; sinon celui annoncé par le stockage. */
    static String typeDepuisNom(String nom, String typeAnnonce) {
        String ext = nom == null || nom.lastIndexOf('.') < 0 ? "" : nom.substring(nom.lastIndexOf('.') + 1).toLowerCase(java.util.Locale.ROOT);
        return switch (ext) {
            case "pdf" -> "application/pdf";
            case "png" -> "image/png";
            case "jpg", "jpeg" -> "image/jpeg";
            case "webp" -> "image/webp";
            case "gif" -> "image/gif";
            default -> typeAnnonce == null || typeAnnonce.isBlank()
                    ? "application/octet-stream" : typeAnnonce.split(";")[0].trim().toLowerCase(java.util.Locale.ROOT);
        };
    }

    @Override
    public java.util.Optional<FichierRelu> relire(String lien) throws IOException {
        if (lien == null || lien.isBlank()) {
            return java.util.Optional.empty();
        }
        java.util.regex.Matcher local = LIEN_LOCAL.matcher(lien.trim());
        if (local.matches()) {
            java.util.Optional<java.nio.file.Path> fichier = stockageLocal.lire(local.group(1));
            if (fichier.isEmpty()) {
                return java.util.Optional.empty();
            }
            byte[] contenu = java.nio.file.Files.readAllBytes(fichier.get());
            return java.util.Optional.of(new FichierRelu(contenu,
                    typeDepuisNom(local.group(1), java.nio.file.Files.probeContentType(fichier.get()))));
        }
        java.util.Optional<LienCloudinary> lc = analyser(lien, cloudName);
        if (lc.isEmpty()) {
            return java.util.Optional.empty();
        }
        Cloudinary client = getCloudinary();
        if (client == null) {
            throw new IOException("Stockage Cloudinary non configuré sur ce serveur.");
        }
        String adresse;
        try {
            adresse = client.privateDownload(lc.get().publicId(), lc.get().format(), ObjectUtils.asMap(
                    "resource_type", lc.get().typeRessource(),
                    "type", lc.get().typeLivraison()));
        } catch (Exception e) {
            throw new IOException("Lien de téléchargement Cloudinary impossible à signer.", e);
        }
        java.net.http.HttpResponse<java.io.InputStream> reponse;
        try {
            reponse = HTTP.send(java.net.http.HttpRequest.newBuilder(java.net.URI.create(adresse))
                            .timeout(java.time.Duration.ofSeconds(30)).GET().build(),
                    java.net.http.HttpResponse.BodyHandlers.ofInputStream());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Téléchargement Cloudinary interrompu.", e);
        }
        try (java.io.InputStream corps = reponse.body()) {
            if (reponse.statusCode() == 404) {
                return java.util.Optional.empty();
            }
            if (reponse.statusCode() != 200) {
                throw new IOException("Cloudinary a refusé le téléchargement (HTTP " + reponse.statusCode() + ").");
            }
            byte[] contenu = corps.readNBytes(TAILLE_MAX_RELUE + 1);
            if (contenu.length > TAILLE_MAX_RELUE) {
                throw new IOException("Fichier trop volumineux pour être relu.");
            }
            String nom = lc.get().format().isEmpty() ? lc.get().publicId() : lc.get().publicId() + "." + lc.get().format();
            return java.util.Optional.of(new FichierRelu(contenu,
                    typeDepuisNom(nom, reponse.headers().firstValue("Content-Type").orElse(null))));
        }
    }
}
