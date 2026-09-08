package ecole_221.sn.ecole.modules.collaboration.domain.port.repository;

import ecole_221.sn.ecole.modules.collaboration.domain.model.Message;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface MessageRepositoryPort {
    Message save(Message message);
    Optional<Message> findById(String id);
    List<Message> findByGroupId(String groupId);
    List<Message> findByGroupIdAndTimestampAfter(String groupId, LocalDateTime timestamp);
    List<Message> findBySenderId(String senderId);
    List<Message> findUnreadMessages(String groupId, String userId);
    void deleteById(String id);
    void deleteByGroupId(String groupId);
}