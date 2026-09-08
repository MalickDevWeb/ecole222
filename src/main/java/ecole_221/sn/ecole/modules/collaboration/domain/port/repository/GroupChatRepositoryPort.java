package ecole_221.sn.ecole.modules.collaboration.domain.port.repository;

import ecole_221.sn.ecole.modules.collaboration.domain.model.GroupChat;
import ecole_221.sn.ecole.modules.collaboration.domain.valueobject.GroupChatType;

import java.util.List;
import java.util.Optional;

public interface GroupChatRepositoryPort {
    GroupChat save(GroupChat groupChat);
    Optional<GroupChat> findById(String id);
    List<GroupChat> findByUserId(String userId);
    List<GroupChat> findByCreatorId(String creatorId);
    List<GroupChat> findByType(GroupChatType chatType);
    List<GroupChat> findActiveChatsByUserId(String userId);
    List<GroupChat> findArchivedChatsByUserId(String userId);
    List<GroupChat> findChatsWithUnreadMessages(String userId);
    void deleteById(String id);
    void removeParticipant(String chatId, String participantId);
}