package ecole_221.sn.ecole.modules.student.infrastructure.web;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import ecole_221.sn.ecole.modules.student.infrastructure.persistence.LiveChatMessageJpaEntity;
import ecole_221.sn.ecole.modules.student.infrastructure.persistence.LiveChatMessageJpaRepository;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
public class LiveSessionMessageController {
    private static final Logger logger = LoggerFactory.getLogger(LiveSessionMessageController.class);

    private final LiveChatMessageJpaRepository messageRepository;

    public LiveSessionMessageController(LiveChatMessageJpaRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    /**
     * Reçoit un message de chat pour une session live donnée, le persiste en base, et le diffuse à tous les abonnés.
     * Exemple de destination côté client : /app/live-sessions/{sessionId}/chat
     */
    @MessageMapping("/live-sessions/{sessionId}/chat")
    @SendTo("/topic/live-sessions/{sessionId}/chat")
    public Map<String, Object> handleChatMessage(@DestinationVariable String sessionId, @Payload Map<String, Object> message) {
        String timestamp = java.time.Instant.now().toString();
        message.put("timestamp", timestamp);
        message.put("id", UUID.randomUUID().toString());
        
        LiveChatMessageJpaEntity entity = new LiveChatMessageJpaEntity(
                (String) message.get("id"),
                sessionId,
                (String) message.get("senderId"),
                (String) message.get("senderName"),
                (String) message.get("content"),
                timestamp
        );
        messageRepository.save(entity);
        
        return message;
    }

    /**
     * Reçoit une réaction (like, clap, etc.) pour une session live et la diffuse.
     * (Les réactions ne sont généralement pas persistées pour des raisons de volume, sauf besoin analytique).
     */
    @MessageMapping("/live-sessions/{sessionId}/reaction")
    @SendTo("/topic/live-sessions/{sessionId}/reaction")
    public Map<String, Object> handleReaction(@DestinationVariable String sessionId, @Payload Map<String, Object> reaction) {
        reaction.put("timestamp", java.time.Instant.now().toString());
        return reaction;
    }

    /**
     * Endpoint REST pour récupérer l'historique du chat lorsqu'un étudiant rejoint la session en cours.
     */
    @GetMapping("/student/live-sessions/{sessionId}/chat-history")
    public List<LiveChatMessageJpaEntity> getChatHistory(@PathVariable String sessionId) {
        return messageRepository.findBySessionIdOrderByTimestampAsc(sessionId);
    }
}
