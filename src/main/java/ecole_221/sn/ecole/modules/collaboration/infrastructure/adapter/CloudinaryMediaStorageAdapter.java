package ecole_221.sn.ecole.modules.collaboration.infrastructure.adapter;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import ecole_221.sn.ecole.modules.collaboration.domain.port.MediaStoragePort;
import ecole_221.sn.ecole.modules.collaboration.domain.exception.AudioDurationExceededException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

@Component
@Primary
public class CloudinaryMediaStorageAdapter implements MediaStoragePort {
    
    private static final Logger logger = LoggerFactory.getLogger(CloudinaryMediaStorageAdapter.class);
    private static final long MAX_AUDIO_SIZE_BYTES = 2 * 1024 * 1024; // 2MB max pour audio
    private static final long MAX_IMAGE_SIZE_BYTES = 5 * 1024 * 1024; // 5MB max pour images
    private static final long MAX_VIDEO_SIZE_BYTES = 50 * 1024 * 1024; // 50MB max pour vidéos
    private static final long MAX_FILE_SIZE_BYTES = 100 * 1024 * 1024; // 100MB max pour fichiers
    
    private final String cloudName;
    private final String apiKey;
    private final String apiSecret;
    private Cloudinary cloudinary;
    
    public CloudinaryMediaStorageAdapter(
            @Value("${cloudinary.cloud-name:}") String cloudName,
            @Value("${cloudinary.api-key:}") String apiKey,
            @Value("${cloudinary.api-secret:}") String apiSecret) {
        this.cloudName = cloudName;
        this.apiKey = apiKey;
        this.apiSecret = apiSecret;
    }
    
    private synchronized Cloudinary getCloudinary() {
        if (cloudinary == null) {
            if (cloudName == null || cloudName.isBlank() ||
                apiKey == null || apiKey.isBlank() ||
                apiSecret == null || apiSecret.isBlank()) {
                logger.warn("[WARN] Les variables Cloudinary ne sont pas configurées. Mode Simulation activé.");
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
    public String uploadAudio(byte[] audioBytes, String fileName) throws IOException {
        validateAudioSize(audioBytes);
        
        String publicId = "audio/" + generatePublicId(fileName);
        Cloudinary client = getCloudinary();
        
        if (client == null) {
            return getSimulatedUrl("audio", fileName);
        }
        
        try {
            Map uploadResult = client.uploader().upload(audioBytes, ObjectUtils.asMap(
                    "public_id", publicId,
                    "resource_type", "video",
                    "folder", "whatsapp_audios"
            ));
            
            return (String) uploadResult.get("secure_url");
        } catch (Exception e) {
            logger.error("[ERROR] Échec de l'upload audio sur Cloudinary: {}", e.getMessage());
            return getSimulatedUrl("audio", fileName);
        }
    }
    
    @Override
    public String uploadImage(byte[] imageBytes, String fileName) throws IOException {
        validateImageSize(imageBytes);
        
        String publicId = "image/" + generatePublicId(fileName);
        Cloudinary client = getCloudinary();
        
        if (client == null) {
            return getSimulatedUrl("image", fileName);
        }
        
        try {
            Map uploadResult = client.uploader().upload(imageBytes, ObjectUtils.asMap(
                    "public_id", publicId,
                    "resource_type", "image",
                    "folder", "whatsapp_images",
                    "transformation", ObjectUtils.asMap(
                            "quality", "auto",
                            "fetch_format", "auto"
                    )
            ));
            
            return (String) uploadResult.get("secure_url");
        } catch (Exception e) {
            logger.error("[ERROR] Échec de l'upload image sur Cloudinary: {}", e.getMessage());
            return getSimulatedUrl("image", fileName);
        }
    }
    
    @Override
    public String uploadVideo(byte[] videoBytes, String fileName) throws IOException {
        validateVideoSize(videoBytes);
        
        String publicId = "video/" + generatePublicId(fileName);
        Cloudinary client = getCloudinary();
        
        if (client == null) {
            return getSimulatedUrl("video", fileName);
        }
        
        try {
            Map uploadResult = client.uploader().upload(videoBytes, ObjectUtils.asMap(
                    "public_id", publicId,
                    "resource_type", "video",
                    "folder", "whatsapp_videos"
            ));
            
            return (String) uploadResult.get("secure_url");
        } catch (Exception e) {
            logger.error("[ERROR] Échec de l'upload vidéo sur Cloudinary: {}", e.getMessage());
            return getSimulatedUrl("video", fileName);
        }
    }
    
    @Override
    public String uploadFile(byte[] fileBytes, String fileName, String contentType) throws IOException {
        validateFileSize(fileBytes);
        
        String publicId = "file/" + generatePublicId(fileName);
        Cloudinary client = getCloudinary();
        
        if (client == null) {
            return getSimulatedUrl("file", fileName);
        }
        
        try {
            Map uploadResult = client.uploader().upload(fileBytes, ObjectUtils.asMap(
                    "public_id", publicId,
                    "resource_type", "auto",
                    "folder", "whatsapp_files"
            ));
            
            return (String) uploadResult.get("secure_url");
        } catch (Exception e) {
            logger.error("[ERROR] Échec de l'upload fichier sur Cloudinary: {}", e.getMessage());
            return getSimulatedUrl("file", fileName);
        }
    }
    
    @Override
    public String uploadGroupAvatar(byte[] avatarBytes, String groupId) throws IOException {
        validateImageSize(avatarBytes);
        
        String publicId = "avatar/" + groupId;
        Cloudinary client = getCloudinary();
        
        if (client == null) {
            return getSimulatedUrl("avatar", groupId);
        }
        
        try {
            Map uploadResult = client.uploader().upload(avatarBytes, ObjectUtils.asMap(
                    "public_id", publicId,
                    "resource_type", "image",
                    "folder", "group_avatars",
                    "transformation", ObjectUtils.asMap(
                            "width", 200,
                            "height", 200,
                            "crop", "fill",
                            "quality", "auto"
                    )
            ));
            
            return (String) uploadResult.get("secure_url");
        } catch (Exception e) {
            logger.error("[ERROR] Échec de l'upload avatar sur Cloudinary: {}", e.getMessage());
            return getSimulatedUrl("avatar", groupId);
        }
    }
    
    @Override
    public void deleteFile(String publicId) throws IOException {
        Cloudinary client = getCloudinary();
        
        if (client == null) {
            logger.warn("[WARN] Mode simulation - suppression ignorée pour: {}", publicId);
            return;
        }
        
        try {
            client.uploader().destroy(publicId, ObjectUtils.asMap("resource_type", "auto"));
            logger.info("Fichier supprimé de Cloudinary: {}", publicId);
        } catch (Exception e) {
            logger.error("[ERROR] Échec de la suppression sur Cloudinary: {}", e.getMessage());
        }
    }
    
    @Override
    public String getFileUrl(String publicId) {
        Cloudinary client = getCloudinary();
        
        if (client == null) {
            return getSimulatedUrl("file", publicId);
        }
        
        try {
            return cloudinary.url().publicId(publicId).generate();
        } catch (Exception e) {
            logger.error("[ERROR] Erreur lors de la génération de l'URL: {}", e.getMessage());
            return getSimulatedUrl("file", publicId);
        }
    }
    
    // ==================== VALIDATIONS ====================
    
    private void validateAudioSize(byte[] audioBytes) {
        if (audioBytes.length > MAX_AUDIO_SIZE_BYTES) {
            throw new AudioDurationExceededException(MAX_AUDIO_SIZE_BYTES, audioBytes.length);
        }
    }
    
    private void validateImageSize(byte[] imageBytes) {
        if (imageBytes.length > MAX_IMAGE_SIZE_BYTES) {
            throw new IllegalArgumentException("L'image dépasse la taille maximale de 5MB");
        }
    }
    
    private void validateVideoSize(byte[] videoBytes) {
        if (videoBytes.length > MAX_VIDEO_SIZE_BYTES) {
            throw new IllegalArgumentException("La vidéo dépasse la taille maximale de 50MB");
        }
    }
    
    private void validateFileSize(byte[] fileBytes) {
        if (fileBytes.length > MAX_FILE_SIZE_BYTES) {
            throw new IllegalArgumentException("Le fichier dépasse la taille maximale de 100MB");
        }
    }
    
    // ==================== UTILITAIRES ====================
    
    private String generatePublicId(String fileName) {
        String timestamp = String.valueOf(System.currentTimeMillis());
        String sanitized = fileName.replaceAll("[^a-zA-Z0-9.-]", "_");
        return timestamp + "_" + sanitized;
    }
    
    private String getSimulatedUrl(String type, String identifier) {
        return String.format("https://res.cloudinary.com/demo/image/upload/v1570979139/%s_%s", type, identifier);
    }
}