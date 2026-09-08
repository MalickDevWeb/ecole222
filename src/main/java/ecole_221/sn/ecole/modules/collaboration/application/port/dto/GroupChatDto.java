package ecole_221.sn.ecole.modules.collaboration.application.port.dto;

import java.time.LocalDateTime;
import java.util.Set;

public record GroupChatDto(
    String id,
    String name,
    String description,
    String avatarUrl,
    String creatorId,
    String chatType,
    Set<String> participantIds,
    Set<String> adminIds,
    LocalDateTime lastMessageTime,
    LocalDateTime createdAt,
    boolean muted,
    LocalDateTime mutedUntil,
    boolean archived,
    String pinnedMessageId,
    int maxParticipants,
    int participantCount
) {
    public static GroupChatDto fromDomain(ecole_221.sn.ecole.modules.collaboration.domain.model.GroupChat groupChat) {
        return new GroupChatDto(
            groupChat.getId(),
            groupChat.getName(),
            groupChat.getDescription(),
            groupChat.getAvatarUrl(),
            groupChat.getCreatorId(),
            groupChat.getChatType().type(),
            groupChat.getParticipantIds(),
            groupChat.getAdminIds(),
            groupChat.getLastMessageTime(),
            groupChat.getCreatedAt(),
            groupChat.isMuted(),
            groupChat.getMutedUntil(),
            groupChat.isArchived(),
            groupChat.getPinnedMessageId(),
            groupChat.getMaxParticipants(),
            groupChat.getParticipantCount()
        );
    }
}