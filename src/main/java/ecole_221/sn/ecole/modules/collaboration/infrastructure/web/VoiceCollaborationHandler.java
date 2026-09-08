package ecole_221.sn.ecole.modules.collaboration.infrastructure.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

import java.util.Map;

@Controller
public class VoiceCollaborationHandler {
    private static final Logger logger = LoggerFactory.getLogger(VoiceCollaborationHandler.class);

    /**
     * Gère les signaux vocaux pour une session de collaboration
     * Endpoint WebSocket: /app/voice/{sessionId}/signal
     * Diffusion: /topic/voice/{sessionId}/signal
     */
    @MessageMapping("/voice/{sessionId}/signal")
    @SendTo("/topic/voice/{sessionId}/signal")
    public Map<String, Object> handleVoiceSignal(@DestinationVariable String sessionId, @Payload Map<String, Object> signal) {
        logger.info("Signal vocal reçu pour session {}: {}", sessionId, signal.get("type"));
        
        // Ajouter timestamp pour synchronisation
        signal.put("timestamp", System.currentTimeMillis());
        signal.put("sessionId", sessionId);
        
        return signal;
    }

    /**
     * Gère les changements d'état audio (mute/unmute)
     * Endpoint WebSocket: /app/voice/{sessionId}/audio-state
     * Diffusion: /topic/voice/{sessionId}/audio-state
     */
    @MessageMapping("/voice/{sessionId}/audio-state")
    @SendTo("/topic/voice/{sessionId}/audio-state")
    public Map<String, Object> handleAudioState(@DestinationVariable String sessionId, @Payload Map<String, Object> state) {
        logger.info("État audio changé pour session {}: userId={}, muted={}", 
            sessionId, state.get("userId"), state.get("muted"));
        
        state.put("timestamp", System.currentTimeMillis());
        state.put("sessionId", sessionId);
        
        return state;
    }

    /**
     * Gère les participants qui rejoignent/quittent la session vocale
     * Endpoint WebSocket: /app/voice/{sessionId}/participants
     * Diffusion: /topic/voice/{sessionId}/participants
     */
    @MessageMapping("/voice/{sessionId}/participants")
    @SendTo("/topic/voice/{sessionId}/participants")
    public Map<String, Object> handleParticipants(@DestinationVariable String sessionId, @Payload Map<String, Object> participant) {
        logger.info("Participant mis à jour pour session {}: action={}, userId={}", 
            sessionId, participant.get("action"), participant.get("userId"));
        
        participant.put("timestamp", System.currentTimeMillis());
        participant.put("sessionId", sessionId);
        
        return participant;
    }
}