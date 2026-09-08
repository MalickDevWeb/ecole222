package ecole_221.sn.ecole.infrastructure.mapper;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Mapper centralisé pour la conformité des données avec le frontend.
 * Toutes les transformations de données pour le frontend doivent passer par ici.
 * Si le frontend change de format, seul ce fichier doit être modifié.
 */
@Component
public class FrontendDataMapper {
    
    // ==================== MAPPING COLLABORATION ====================
    
    public Map<String, Object> mapGroupChatForFrontend(ecole_221.sn.ecole.modules.collaboration.domain.model.GroupChat groupChat) {
        Map<String, Object> frontend = new HashMap<>();
        frontend.put("id", groupChat.getId());
        frontend.put("name", groupChat.getName() != null ? groupChat.getName() : "Private Chat");
        frontend.put("description", groupChat.getDescription());
        frontend.put("avatar", groupChat.getAvatarUrl());
        frontend.put("type", groupChat.getChatType().type().toLowerCase());
        frontend.put("creatorId", groupChat.getCreatorId());
        frontend.put("participants", groupChat.getParticipantIds());
        frontend.put("admins", groupChat.getAdminIds());
        frontend.put("lastMessageTime", formatDateTime(groupChat.getLastMessageTime()));
        frontend.put("createdAt", formatDateTime(groupChat.getCreatedAt()));
        frontend.put("isMuted", groupChat.isMuted());
        frontend.put("mutedUntil", formatDateTime(groupChat.getMutedUntil()));
        frontend.put("isArchived", groupChat.isArchived());
        frontend.put("pinnedMessageId", groupChat.getPinnedMessageId());
        frontend.put("maxParticipants", groupChat.getMaxParticipants());
        frontend.put("participantCount", groupChat.getParticipantCount());
        frontend.put("unreadCount", 0); // À calculer depuis le repository
        return frontend;
    }
    
    public Map<String, Object> mapMessageForFrontend(ecole_221.sn.ecole.modules.collaboration.domain.model.Message message) {
        Map<String, Object> frontend = new HashMap<>();
        frontend.put("id", message.getId());
        frontend.put("chatId", message.getGroupId());
        frontend.put("senderId", message.getSenderId());
        frontend.put("content", message.getContent());
        frontend.put("type", message.getMessageType().type().toLowerCase());
        frontend.put("status", message.getStatus().status().toLowerCase());
        frontend.put("timestamp", formatDateTime(message.getTimestamp()));
        frontend.put("mediaUrl", message.getMediaUrl());
        frontend.put("duration", message.getDuration());
        frontend.put("formattedDuration", message.getFormattedDuration());
        frontend.put("hasMedia", message.hasMedia());
        frontend.put("isAudio", message.isAudioMessage());
        frontend.put("isSystem", message.isSystemMessage());
        frontend.put("isFromMe", false); // À déterminer depuis le contexte utilisateur
        return frontend;
    }
    
    public Map<String, Object> mapMeetForFrontend(ecole_221.sn.ecole.modules.collaboration.domain.model.Meet meet) {
        Map<String, Object> frontend = new HashMap<>();
        frontend.put("id", meet.getId());
        frontend.put("title", meet.getTitle());
        frontend.put("description", meet.getDescription());
        frontend.put("organizerId", meet.getOrganizerId());
        frontend.put("meetingLink", meet.getMeetingLink());
        frontend.put("scheduledTime", formatDateTime(meet.getScheduledTime()));
        frontend.put("status", meet.getStatus().status().toLowerCase());
        frontend.put("participants", meet.getParticipantIds());
        frontend.put("participantCount", meet.getParticipantCount());
        frontend.put("createdAt", formatDateTime(meet.getCreatedAt()));
        frontend.put("endedAt", formatDateTime(meet.getEndedAt()));
        frontend.put("maxParticipants", meet.getMaxParticipants());
        frontend.put("isUpcoming", meet.isUpcoming());
        frontend.put("isActive", meet.getStatus().isActive());
        frontend.put("isOver", meet.isOver());
        return frontend;
    }
    
    // ==================== MAPPING AUTH ====================
    
    public Map<String, Object> mapUserForFrontend(ecole_221.sn.ecole.modules.auth.domain.model.User user) {
        Map<String, Object> frontend = new HashMap<>();
        frontend.put("id", user.getId());
        frontend.put("email", user.getEmail());
        frontend.put("username", user.getUsername());
        frontend.put("firstName", user.getPrenom());
        frontend.put("lastName", user.getNom());
        frontend.put("fullName", user.getPrenom() + " " + user.getNom());
        frontend.put("role", user.getRole().value());
        frontend.put("photoUrl", user.getPhotoUrl());
        frontend.put("isActive", user.isEnabled());
        // Champs non disponibles dans le modèle User actuel - valeurs par défaut
        frontend.put("createdAt", null);
        frontend.put("lastLogin", null);
        return frontend;
    }
    
    // ==================== MAPPING ADMIN ====================
    
    public Map<String, Object> mapClasseForFrontend(ecole_221.sn.ecole.modules.admin.domain.model.Classe classe) {
        Map<String, Object> frontend = new HashMap<>();
        frontend.put("id", classe.getId());
        frontend.put("name", classe.getName());
        frontend.put("filiereId", classe.getFiliereId());
        // Champs non disponibles dans le modèle Classe actuel du module admin
        frontend.put("niveau", null);
        frontend.put("capacity", null);
        frontend.put("currentStudents", null);
        frontend.put("room", null);
        frontend.put("isActive", true);
        return frontend;
    }
    
    // ==================== UTILITAIRES ====================
    
    private String formatDateTime(java.time.LocalDateTime dateTime) {
        if (dateTime == null) return null;
        return dateTime.toString();
    }
    
    private String formatDateTime(java.time.LocalDateTime dateTime, String pattern) {
        if (dateTime == null) return null;
        return dateTime.format(java.time.format.DateTimeFormatter.ofPattern(pattern));
    }
    
    // ==================== CONFIGURATION FRONTEND ====================
    
    /**
     * Configuration des formats attendus par le frontend.
     * Modifier ces valeurs si le format frontend change.
     */
    public static class FrontendConfig {
        public static final String DATE_FORMAT = "yyyy-MM-dd";
        public static final String TIME_FORMAT = "HH:mm";
        public static final String DATETIME_FORMAT = "yyyy-MM-dd HH:mm";
        public static final String TIMEZONE = "UTC";
        
        // Noms des champs frontend
        public static final String ID_FIELD = "id";
        public static final String NAME_FIELD = "name";
        public static final String CREATED_AT_FIELD = "createdAt";
        public static final String UPDATED_AT_FIELD = "updatedAt";
    }
}