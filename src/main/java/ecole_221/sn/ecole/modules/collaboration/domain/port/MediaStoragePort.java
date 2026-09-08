package ecole_221.sn.ecole.modules.collaboration.domain.port;

import java.io.IOException;

/**
 * Port pour le stockage des médias sur Cloudinary.
 * Supporte tous les types de médias nécessaires pour les discussions type WhatsApp.
 */
public interface MediaStoragePort {
    
    /**
     * Upload un fichier audio (max 2 minutes = ~2MB pour MP3)
     */
    String uploadAudio(byte[] audioBytes, String fileName) throws IOException;
    
    /**
     * Upload une image
     */
    String uploadImage(byte[] imageBytes, String fileName) throws IOException;
    
    /**
     * Upload une vidéo
     */
    String uploadVideo(byte[] videoBytes, String fileName) throws IOException;
    
    /**
     * Upload un fichier quelconque
     */
    String uploadFile(byte[] fileBytes, String fileName, String contentType) throws IOException;
    
    /**
     * Upload une image de profil/avatar de groupe
     */
    String uploadGroupAvatar(byte[] avatarBytes, String groupId) throws IOException;
    
    /**
     * Supprime un fichier par son public ID
     */
    void deleteFile(String publicId) throws IOException;
    
    /**
     * Récupère l'URL d'un fichier
     */
    String getFileUrl(String publicId);
}