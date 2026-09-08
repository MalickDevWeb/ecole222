package ecole_221.sn.ecole.modules.collaboration.infrastructure.adapter;

import ecole_221.sn.ecole.modules.collaboration.domain.port.MediaStoragePort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

/**
 * Implementation par défaut du MediaStoragePort pour les tests
 * Utilise des URLs simulées au lieu de Cloudinary pour les tests
 */
@Component
public class MediaStorageAdapterImpl implements MediaStoragePort {
    
    private static final Logger logger = LoggerFactory.getLogger(MediaStorageAdapterImpl.class);
    
    @Override
    public String uploadAudio(byte[] audioBytes, String fileName) throws IOException {
        String simulatedUrl = "https://simulated.cloudinary.com/audio/" + UUID.randomUUID() + "/" + fileName;
        logger.info("Upload audio simulé: {} -> {}", fileName, simulatedUrl);
        return simulatedUrl;
    }
    
    @Override
    public String uploadImage(byte[] imageBytes, String fileName) throws IOException {
        String simulatedUrl = "https://simulated.cloudinary.com/image/" + UUID.randomUUID() + "/" + fileName;
        logger.info("Upload image simulé: {} -> {}", fileName, simulatedUrl);
        return simulatedUrl;
    }
    
    @Override
    public String uploadVideo(byte[] videoBytes, String fileName) throws IOException {
        String simulatedUrl = "https://simulated.cloudinary.com/video/" + UUID.randomUUID() + "/" + fileName;
        logger.info("Upload vidéo simulé: {} -> {}", fileName, simulatedUrl);
        return simulatedUrl;
    }
    
    @Override
    public String uploadFile(byte[] fileBytes, String fileName, String contentType) throws IOException {
        String simulatedUrl = "https://simulated.cloudinary.com/file/" + UUID.randomUUID() + "/" + fileName;
        logger.info("Upload fichier simulé: {} -> {}", fileName, simulatedUrl);
        return simulatedUrl;
    }
    
    @Override
    public String uploadGroupAvatar(byte[] avatarBytes, String groupId) throws IOException {
        String simulatedUrl = "https://simulated.cloudinary.com/avatar/" + groupId;
        logger.info("Upload avatar simulé: group {} -> {}", groupId, simulatedUrl);
        return simulatedUrl;
    }
    
    @Override
    public void deleteFile(String publicId) throws IOException {
        logger.info("Suppression fichier simulée: {}", publicId);
    }
    
    @Override
    public String getFileUrl(String publicId) {
        return "https://simulated.cloudinary.com/" + publicId;
    }
}