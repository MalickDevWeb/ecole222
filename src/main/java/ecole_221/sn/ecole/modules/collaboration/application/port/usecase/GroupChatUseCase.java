package ecole_221.sn.ecole.modules.collaboration.application.port.usecase;

import ecole_221.sn.ecole.modules.collaboration.application.port.dto.GroupChatDto;
import ecole_221.sn.ecole.modules.collaboration.application.port.dto.MessageDto;
import ecole_221.sn.ecole.modules.collaboration.domain.model.GroupChat;
import ecole_221.sn.ecole.modules.collaboration.domain.model.Message;

import java.io.IOException;
import java.util.List;

public interface GroupChatUseCase {
    
    // ==================== GESTION DES GROUPES ====================
    
    GroupChatDto createGroup(String name, String creatorId);
    GroupChatDto createGroupWithAvatar(String name, String creatorId, byte[] avatarBytes) throws IOException;
    GroupChatDto createIndividualChat(String user1Id, String user2Id);
    GroupChatDto getGroupChat(String chatId);
    List<GroupChatDto> getUserChats(String userId);
    List<GroupChatDto> getGroupChatsByType(String chatType);
    
    GroupChatDto updateGroupName(String chatId, String newName);
    GroupChatDto updateGroupAvatar(String chatId, byte[] avatarBytes) throws IOException;
    GroupChatDto updateGroupDescription(String chatId, String description);
    
    void addParticipant(String chatId, String participantId);
    void removeParticipant(String chatId, String participantId);
    void addAdmin(String chatId, String adminId);
    void removeAdmin(String chatId, String adminId);
    
    void muteChat(String chatId, String userId, long durationMinutes);
    void unmuteChat(String chatId, String userId);
    void archiveChat(String chatId, String userId);
    void unarchiveChat(String chatId, String userId);
    
    void pinMessage(String chatId, String messageId);
    void unpinMessage(String chatId);
    
    void deleteGroup(String chatId);
    
    // ==================== GESTION DES MESSAGES ====================
    
    MessageDto sendTextMessage(String chatId, String senderId, String content);
    MessageDto sendAudioMessage(String chatId, String senderId, byte[] audioBytes, long duration) throws IOException;
    MessageDto sendImageMessage(String chatId, String senderId, byte[] imageBytes) throws IOException;
    MessageDto sendVideoMessage(String chatId, String senderId, byte[] videoBytes, long duration) throws IOException;
    MessageDto sendFileMessage(String chatId, String senderId, String fileName, byte[] fileBytes) throws IOException;
    
    MessageDto replyToMessage(String chatId, String senderId, String content, String originalMessageId);
    MessageDto forwardMessage(String chatId, String senderId, String originalMessageId);
    
    List<MessageDto> getChatMessages(String chatId, int limit, int offset);
    List<MessageDto> getMessagesAfter(String chatId, String lastMessageId);
    List<MessageDto> searchMessages(String chatId, String query);
    
    void markMessageAsRead(String chatId, String messageId, String userId);
    void markAllMessagesAsRead(String chatId, String userId);
    
    void deleteMessage(String messageId);
    void deleteMessageForEveryone(String messageId, String userId);
    
    // ==================== STATISTIQUES ====================
    
    long getUnreadMessageCount(String chatId, String userId);
    long getTotalUnreadCount(String userId);
}