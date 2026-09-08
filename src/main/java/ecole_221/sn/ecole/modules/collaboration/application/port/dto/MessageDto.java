package ecole_221.sn.ecole.modules.collaboration.application.port.dto;

import java.time.LocalDateTime;

public record MessageDto(
    String id,
    String groupId,
    String senderId,
    String content,
    String messageType,
    String status,
    LocalDateTime timestamp,
    String mediaUrl,
    Long duration,
    String formattedDuration,
    boolean hasMedia,
    boolean isAudioMessage,
    boolean isSystemMessage
) {
    public static MessageDto fromDomain(ecole_221.sn.ecole.modules.collaboration.domain.model.Message message) {
        return new MessageDto(
            message.getId(),
            message.getGroupId(),
            message.getSenderId(),
            message.getContent(),
            message.getMessageType().type(),
            message.getStatus().status(),
            message.getTimestamp(),
            message.getMediaUrl(),
            message.getDuration(),
            message.getFormattedDuration(),
            message.hasMedia(),
            message.isAudioMessage(),
            message.isSystemMessage()
        );
    }
}