package ecole_221.sn.ecole.modules.collaboration.domain.model;

import ecole_221.sn.ecole.modules.collaboration.domain.valueobject.GroupChatType;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class GroupChatFactory {
    
    public static GroupChat createGroup(String name, String creatorId) {
        String id = UUID.randomUUID().toString();
        LocalDateTime createdAt = LocalDateTime.now();
        Set<String> participants = new HashSet<>();
        Set<String> admins = new HashSet<>();
        
        participants.add(creatorId);
        admins.add(creatorId);
        
        return new GroupChat(id, name, null, null, creatorId, GroupChatType.GROUP,
                          participants, admins, createdAt, createdAt, false, null, false, null, 0);
    }
    
    public static GroupChat createGroupWithAvatar(String name, String creatorId, String avatarUrl) {
        String id = UUID.randomUUID().toString();
        LocalDateTime createdAt = LocalDateTime.now();
        Set<String> participants = new HashSet<>();
        Set<String> admins = new HashSet<>();
        
        participants.add(creatorId);
        admins.add(creatorId);
        
        return new GroupChat(id, name, null, avatarUrl, creatorId, GroupChatType.GROUP,
                          participants, admins, createdAt, createdAt, false, null, false, null, 0);
    }
    
    public static GroupChat createGroupWithCapacity(String name, String creatorId, int maxParticipants) {
        String id = UUID.randomUUID().toString();
        LocalDateTime createdAt = LocalDateTime.now();
        Set<String> participants = new HashSet<>();
        Set<String> admins = new HashSet<>();
        
        participants.add(creatorId);
        admins.add(creatorId);
        
        return new GroupChat(id, name, null, null, creatorId, GroupChatType.GROUP,
                          participants, admins, createdAt, createdAt, false, null, false, null, maxParticipants);
    }
    
    public static GroupChat createIndividualChat(String user1Id, String user2Id) {
        String id = UUID.randomUUID().toString();
        LocalDateTime createdAt = LocalDateTime.now();
        Set<String> participants = new HashSet<>();
        Set<String> admins = new HashSet<>();
        
        participants.add(user1Id);
        participants.add(user2Id);
        admins.add(user1Id);
        admins.add(user2Id);
        
        return new GroupChat(id, null, null, null, user1Id, GroupChatType.INDIVIDUAL,
                          participants, admins, createdAt, createdAt, false, null, false, null, 2);
    }
    
    public static GroupChat createBroadcast(String name, String creatorId) {
        String id = UUID.randomUUID().toString();
        LocalDateTime createdAt = LocalDateTime.now();
        Set<String> participants = new HashSet<>();
        Set<String> admins = new HashSet<>();
        
        participants.add(creatorId);
        admins.add(creatorId);
        
        return new GroupChat(id, name, null, null, creatorId, GroupChatType.BROADCAST,
                          participants, admins, createdAt, createdAt, false, null, false, null, 0);
    }
    
    public static GroupChat createChannel(String name, String creatorId) {
        String id = UUID.randomUUID().toString();
        LocalDateTime createdAt = LocalDateTime.now();
        Set<String> participants = new HashSet<>();
        Set<String> admins = new HashSet<>();
        
        participants.add(creatorId);
        admins.add(creatorId);
        
        return new GroupChat(id, name, null, null, creatorId, GroupChatType.CHANNEL,
                          participants, admins, createdAt, createdAt, false, null, false, null, 0);
    }
    
    public static GroupChat reconstitute(String id, String name, String description, String avatarUrl,
                                        String creatorId, String chatType, Set<String> participantIds,
                                        Set<String> adminIds, LocalDateTime lastMessageTime,
                                        LocalDateTime createdAt, boolean muted, LocalDateTime mutedUntil,
                                        boolean archived, String pinnedMessageId, int maxParticipants) {
        return new GroupChat(id, name, description, avatarUrl, creatorId, new GroupChatType(chatType),
                          participantIds, adminIds, lastMessageTime, createdAt, muted, mutedUntil,
                          archived, pinnedMessageId, maxParticipants);
    }
}