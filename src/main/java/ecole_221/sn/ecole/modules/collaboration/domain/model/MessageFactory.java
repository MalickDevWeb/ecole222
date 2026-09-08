package ecole_221.sn.ecole.modules.collaboration.domain.model;

import ecole_221.sn.ecole.modules.collaboration.domain.valueobject.MessageStatus;
import ecole_221.sn.ecole.modules.collaboration.domain.valueobject.MessageType;
import ecole_221.sn.ecole.modules.collaboration.domain.exception.AudioDurationExceededException;

import java.time.LocalDateTime;
import java.util.UUID;

public class MessageFactory {
    
    private static final long MAX_AUDIO_DURATION_SECONDS = 120; // 2 minutes maximum
    
    public static Message createTextMessage(String groupId, String senderId, String content) {
        String id = UUID.randomUUID().toString();
        LocalDateTime timestamp = LocalDateTime.now();
        
        return new Message(id, groupId, senderId, content, MessageType.TEXT, 
                          MessageStatus.SENT, timestamp, null, null);
    }
    
    public static Message createAudioMessage(String groupId, String senderId, String mediaUrl, long duration) {
        validateAudioDuration(duration);
        
        String id = UUID.randomUUID().toString();
        LocalDateTime timestamp = LocalDateTime.now();
        
        return new Message(id, groupId, senderId, "🎤 Message vocal", MessageType.AUDIO, 
                          MessageStatus.SENT, timestamp, mediaUrl, duration);
    }
    
    public static Message createImageMessage(String groupId, String senderId, String mediaUrl) {
        String id = UUID.randomUUID().toString();
        LocalDateTime timestamp = LocalDateTime.now();
        
        return new Message(id, groupId, senderId, "📷 Photo", MessageType.IMAGE, 
                          MessageStatus.SENT, timestamp, mediaUrl, null);
    }
    
    public static Message createVideoMessage(String groupId, String senderId, String mediaUrl, long duration) {
        String id = UUID.randomUUID().toString();
        LocalDateTime timestamp = LocalDateTime.now();
        
        return new Message(id, groupId, senderId, "🎥 Vidéo", MessageType.VIDEO, 
                          MessageStatus.SENT, timestamp, mediaUrl, duration);
    }
    
    public static Message createFileMessage(String groupId, String senderId, String fileName, String mediaUrl) {
        String id = UUID.randomUUID().toString();
        LocalDateTime timestamp = LocalDateTime.now();
        
        return new Message(id, groupId, senderId, "📎 " + fileName, MessageType.FILE, 
                          MessageStatus.SENT, timestamp, mediaUrl, null);
    }
    
    public static Message createReplyMessage(String groupId, String senderId, String content, 
                                            String originalMessageId) {
        String id = UUID.randomUUID().toString();
        LocalDateTime timestamp = LocalDateTime.now();
        String replyContent = "↩️ Réponse: " + content;
        
        return new Message(id, groupId, senderId, replyContent, MessageType.TEXT, 
                          MessageStatus.SENT, timestamp, null, null);
    }
    
    public static Message createForwardedMessage(String groupId, String senderId, String content, 
                                               String originalSenderId) {
        String id = UUID.randomUUID().toString();
        LocalDateTime timestamp = LocalDateTime.now();
        String forwardedContent = "↪️ Transféré de " + originalSenderId + ": " + content;
        
        return new Message(id, groupId, senderId, forwardedContent, MessageType.TEXT, 
                          MessageStatus.SENT, timestamp, null, null);
    }
    
    public static Message createSystemMessage(String groupId, String content) {
        String id = UUID.randomUUID().toString();
        LocalDateTime timestamp = LocalDateTime.now();
        
        return new Message(id, groupId, "SYSTEM", content, MessageType.SYSTEM, 
                          MessageStatus.SENT, timestamp, null, null);
    }
    
    public static Message createMemberJoinedMessage(String groupId, String memberName) {
        return createSystemMessage(groupId, "👋 " + memberName + " a rejoint le groupe");
    }
    
    public static Message createMemberLeftMessage(String groupId, String memberName) {
        return createSystemMessage(groupId, "👋 " + memberName + " a quitté le groupe");
    }
    
    public static Message createGroupCreatedMessage(String groupId, String creatorName) {
        return createSystemMessage(groupId, "🎉 Groupe créé par " + creatorName);
    }
    
    public static Message createGroupNameChangedMessage(String groupId, String oldName, String newName) {
        return createSystemMessage(groupId, "📝 Le groupe a été renommé de '" + oldName + "' en '" + newName + "'");
    }
    
    private static void validateAudioDuration(long duration) {
        if (duration > MAX_AUDIO_DURATION_SECONDS) {
            throw new AudioDurationExceededException(MAX_AUDIO_DURATION_SECONDS, duration);
        }
        if (duration <= 0) {
            throw new IllegalArgumentException("La durée de l'audio doit être positive");
        }
    }
    
    public static Message reconstitute(String id, String groupId, String senderId, String content,
                                      String messageType, String status, LocalDateTime timestamp,
                                      String mediaUrl, Long duration) {
        return new Message(id, groupId, senderId, content, 
                          new MessageType(messageType), 
                          new MessageStatus(status), 
                          timestamp, mediaUrl, duration);
    }
}