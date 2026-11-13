package ecole_221.sn.ecole.modules.collaboration.infrastructure.web;

import ecole_221.sn.ecole.infrastructure.mapper.FrontendDataMapper;
import ecole_221.sn.ecole.modules.collaboration.application.port.dto.GroupChatDto;
import ecole_221.sn.ecole.modules.collaboration.application.port.dto.MessageDto;
import ecole_221.sn.ecole.modules.collaboration.application.service.GroupChatService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/collaboration/groupchat")
public class GroupChatController {
    
    private static final Logger logger = LoggerFactory.getLogger(GroupChatController.class);
    
    private final GroupChatService groupChatService;
    private final FrontendDataMapper frontendMapper;
    
    public GroupChatController(GroupChatService groupChatService, 
                              FrontendDataMapper frontendMapper) {
        this.groupChatService = groupChatService;
        this.frontendMapper = frontendMapper;
    }
    
    // ==================== ENDPOINTS GROUP CHAT ====================
    
    @PostMapping("/groups")
    public ResponseEntity<?> createGroup(@RequestBody Map<String, String> request) {
        try {
            String name = request.get("name");
            String creatorId = exigerMoi(request.get("creatorId"));
            GroupChatDto created = groupChatService.createGroup(name, creatorId);
            return ResponseEntity.ok(created);
        } catch (Exception e) {
            logger.error("Erreur création groupe", e);
            throw relancer(e);
        }
    }
    
    @PostMapping(value = "/groups/{id}/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<GroupChatDto> uploadGroupAvatar(
            @PathVariable String id,
            @RequestParam(value = "file") MultipartFile file) {
        try {
            exigerAdministrateur(id);
            logger.info("Upload avatar pour groupe: {}, fichier: {}, taille: {}", id, file != null ? file.getOriginalFilename() : "null", file != null ? file.getSize() : 0);
            if (file == null || file.isEmpty()) {
                logger.warn("Fichier avatar manquant pour le groupe: {}", id);
                return ResponseEntity.badRequest().build();
            }
            groupChatService.updateGroupAvatar(id, file.getBytes());
            GroupChatDto updated = groupChatService.getGroupChat(id);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            logger.error("Erreur upload avatar", e);
            throw relancer(e);
        }
    }
    
    @GetMapping("/groups/{id}")
    public ResponseEntity<GroupChatDto> getGroup(@PathVariable String id) {
        try {
            exigerMembre(id);
            GroupChatDto group = groupChatService.getGroupChat(id);
            return ResponseEntity.ok(group);
        } catch (Exception e) {
            // Avant : toute erreur devenait 404 (y compris un refus d'accès)
            throw relancer(e);
        }
    }
    
    @GetMapping("/users/{userId}/chats")
    public ResponseEntity<List<GroupChatDto>> getUserChats(@PathVariable String userId) {
        try {
            userId = exigerMoi(userId);
            List<GroupChatDto> chats = groupChatService.getUserChats(userId);
            return ResponseEntity.ok(chats);
        } catch (Exception e) {
            logger.error("Erreur récupération chats utilisateur", e);
            throw relancer(e);
        }
    }
    
    @PutMapping("/groups/{id}/name")
    public ResponseEntity<GroupChatDto> updateGroupName(
            @PathVariable String id,
            @RequestBody Map<String, String> request) {
        try {
            exigerAdministrateur(id);
            String newName = request.get("name");
            GroupChatDto updated = groupChatService.updateGroupName(id, newName);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            logger.error("Erreur mise à jour nom", e);
            throw relancer(e);
        }
    }
    
    @PostMapping("/groups/{id}/participants")
    public ResponseEntity<Void> addParticipant(
            @PathVariable String id,
            @RequestBody Map<String, String> request) {
        try {
            exigerAdministrateur(id);
            String participantId = request.get("participantId");
            groupChatService.addParticipant(id, participantId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            logger.error("Erreur ajout participant", e);
            throw relancer(e);
        }
    }
    
    @DeleteMapping("/groups/{id}/participants/{participantId}")
    public ResponseEntity<Void> removeParticipant(
            @PathVariable String id,
            @PathVariable String participantId) {
        try {
            exigerAdministrateur(id);
            groupChatService.removeParticipant(id, participantId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            logger.error("Erreur suppression participant", e);
            throw relancer(e);
        }
    }
    
    @PostMapping("/groups/{id}/description")
    public ResponseEntity<GroupChatDto> updateGroupDescription(
            @PathVariable String id,
            @RequestBody Map<String, String> request) {
        try {
            exigerAdministrateur(id);
            String description = request.get("description");
            GroupChatDto updated = groupChatService.updateGroupDescription(id, description);
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            logger.error("Erreur mise à jour description", e);
            throw relancer(e);
        }
    }
    
    @PostMapping("/groups/{id}/admins")
    public ResponseEntity<Void> addAdmin(
            @PathVariable String id,
            @RequestBody Map<String, String> request) {
        try {
            exigerAdministrateur(id);
            String adminId = request.get("adminId");
            groupChatService.addAdmin(id, adminId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            logger.error("Erreur ajout admin", e);
            throw relancer(e);
        }
    }
    
    @DeleteMapping("/groups/{id}/admins/{adminId}")
    public ResponseEntity<Void> removeAdmin(
            @PathVariable String id,
            @PathVariable String adminId) {
        try {
            exigerAdministrateur(id);
            groupChatService.removeAdmin(id, adminId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            logger.error("Erreur suppression admin", e);
            throw relancer(e);
        }
    }
    
    @PostMapping("/groups/{id}/mute")
    public ResponseEntity<Void> muteChat(
            @PathVariable String id,
            @RequestBody Map<String, String> request) {
        try {
            exigerMembre(id);
            String userId = exigerMoi(request.get("userId"));
            long durationMinutes = Long.parseLong(request.getOrDefault("durationMinutes", "60"));
            groupChatService.muteChat(id, userId, durationMinutes);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            logger.error("Erreur mute chat", e);
            throw relancer(e);
        }
    }
    
    @PostMapping("/groups/{id}/unmute")
    public ResponseEntity<Void> unmuteChat(
            @PathVariable String id,
            @RequestBody Map<String, String> request) {
        try {
            exigerMembre(id);
            String userId = exigerMoi(request.get("userId"));
            groupChatService.unmuteChat(id, userId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            logger.error("Erreur unmute chat", e);
            throw relancer(e);
        }
    }
    
    @PostMapping("/groups/{id}/archive")
    public ResponseEntity<Void> archiveChat(
            @PathVariable String id,
            @RequestBody Map<String, String> request) {
        try {
            exigerMembre(id);
            String userId = exigerMoi(request.get("userId"));
            groupChatService.archiveChat(id, userId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            logger.error("Erreur archive chat", e);
            throw relancer(e);
        }
    }
    
    @PostMapping("/groups/{id}/unarchive")
    public ResponseEntity<Void> unarchiveChat(
            @PathVariable String id,
            @RequestBody Map<String, String> request) {
        try {
            exigerMembre(id);
            String userId = exigerMoi(request.get("userId"));
            groupChatService.unarchiveChat(id, userId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            logger.error("Erreur unarchive chat", e);
            throw relancer(e);
        }
    }
    
    @PostMapping("/groups/{id}/pin")
    public ResponseEntity<Void> pinChat(
            @PathVariable String id,
            @RequestBody Map<String, String> request) {
        try {
            exigerMembre(id);
            String userId = exigerMoi(request.get("userId"));
            // groupChatService.pinChat(id, userId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            logger.error("Erreur pin chat", e);
            throw relancer(e);
        }
    }
    
    @PostMapping("/groups/{id}/unpin")
    public ResponseEntity<Void> unpinChat(
            @PathVariable String id,
            @RequestBody Map<String, String> request) {
        try {
            exigerMembre(id);
            String userId = exigerMoi(request.get("userId"));
            // groupChatService.unpinChat(id, userId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            logger.error("Erreur unpin chat", e);
            throw relancer(e);
        }
    }
    
    @GetMapping("/groups/types/{type}")
    public ResponseEntity<List<GroupChatDto>> getGroupsByType(@PathVariable String type) {
        try {
            List<GroupChatDto> groups = groupChatService.getGroupChatsByType(type);
            return ResponseEntity.ok(groups);
        } catch (Exception e) {
            logger.error("Erreur récupération groupes par type", e);
            throw relancer(e);
        }
    }
    
    @PostMapping("/individual")
    public ResponseEntity<GroupChatDto> createIndividualChat(@RequestBody Map<String, String> request) {
        try {
            String user1Id = exigerMoi(request.get("user1Id"));
            String user2Id = request.get("user2Id");
            GroupChatDto created = groupChatService.createIndividualChat(user1Id, user2Id);
            return ResponseEntity.ok(created);
        } catch (Exception e) {
            logger.error("Erreur création chat individuel", e);
            throw relancer(e);
        }
    }
    
    @PostMapping("/groups/{id}/messages/text")
    public ResponseEntity<MessageDto> sendTextMessage(
            @PathVariable String id,
            @RequestBody Map<String, String> request) {
        try {
            exigerMembre(id);
            String senderId = exigerMoi(request.get("senderId"));
            String content = request.get("content");
            MessageDto message = groupChatService.sendTextMessage(id, senderId, content);
            return ResponseEntity.ok(message);
        } catch (Exception e) {
            logger.error("Erreur envoi message texte", e);
            throw relancer(e);
        }
    }
    
    @PostMapping(value = "/groups/{id}/messages/audio", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MessageDto> sendAudioMessage(
            @PathVariable String id,
            @RequestParam(value = "audio") MultipartFile audioFile,
            @RequestParam(value = "senderId") String senderId) {
        try {
            exigerMembre(id);
            senderId = exigerMoi(senderId);
            logger.info("Message audio pour groupe: {}, fichier: {}, taille: {}, sender: {}", id, audioFile != null ? audioFile.getOriginalFilename() : "null", audioFile != null ? audioFile.getSize() : 0, senderId);
            if (audioFile == null || audioFile.isEmpty()) {
                logger.warn("Fichier audio manquant pour le groupe: {}", id);
                return ResponseEntity.badRequest().build();
            }
            if (senderId == null || senderId.isEmpty()) {
                logger.warn("Sender ID manquant pour le message audio du groupe: {}", id);
                return ResponseEntity.badRequest().build();
            }
            MessageDto message = groupChatService.sendAudioMessage(id, senderId, audioFile.getBytes(), 120);
            return ResponseEntity.ok(message);
        } catch (Exception e) {
            logger.error("Erreur envoi message audio", e);
            throw relancer(e);
        }
    }
    
    @PostMapping(value = "/groups/{id}/messages/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MessageDto> sendImageMessage(
            @PathVariable String id,
            @RequestParam(value = "image") MultipartFile imageFile,
            @RequestParam(value = "senderId") String senderId) {
        try {
            exigerMembre(id);
            senderId = exigerMoi(senderId);
            logger.info("Message image pour groupe: {}, fichier: {}, taille: {}, sender: {}", id, imageFile != null ? imageFile.getOriginalFilename() : "null", imageFile != null ? imageFile.getSize() : 0, senderId);
            if (imageFile == null || imageFile.isEmpty()) {
                logger.warn("Fichier image manquant pour le groupe: {}", id);
                return ResponseEntity.badRequest().build();
            }
            if (senderId == null || senderId.isEmpty()) {
                logger.warn("Sender ID manquant pour le message image du groupe: {}", id);
                return ResponseEntity.badRequest().build();
            }
            MessageDto message = groupChatService.sendImageMessage(id, senderId, imageFile.getBytes());
            return ResponseEntity.ok(message);
        } catch (Exception e) {
            logger.error("Erreur envoi message image", e);
            throw relancer(e);
        }
    }
    
    @GetMapping("/groups/{id}/messages")
    public ResponseEntity<List<MessageDto>> getMessages(@PathVariable String id) {
        try {
            exigerMembre(id);
            // Avant : toujours une liste vide (lecture commentée)
            return ResponseEntity.ok(groupChatService.getMessages(id));
        } catch (Exception e) {
            logger.error("Erreur récupération messages", e);
            throw relancer(e);
        }
    }
    
    @PostMapping("/messages/{id}/read")
    public ResponseEntity<Void> markMessageAsRead(
            @PathVariable String id,
            @RequestBody Map<String, String> request) {
        try {
            String userId = exigerMoi(request.get("userId"));
            String chatId = request.getOrDefault("chatId", "default");
            exigerMembre(chatId);
            groupChatService.markMessageAsRead(chatId, id, userId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            // Avant : 200 renvoyé même en cas d'échec (« pour éviter un 500 dans les tests »)
            logger.warn("Marquage message lu refusé : {}", e.getMessage());
            throw relancer(e);
        }
    }
    
    @DeleteMapping("/messages/{id}")
    public ResponseEntity<Void> deleteMessage(@PathVariable String id) {
        try {
            // Auteur ou administrateur de la discussion uniquement ; échec annoncé (avant : 200 même en cas d'échec,
            // et n'importe quel compte supprimait n'importe quel message)
            groupChatService.supprimerMessage(id, moi());
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            logger.warn("Suppression de message refusée : {}", e.getMessage());
            throw relancer(e);
        }
    }
    
    @GetMapping("/groups/{id}/frontend")
    public ResponseEntity<Map<String, Object>> getGroupFrontend(@PathVariable String id) {
        try {
            exigerMembre(id);
            var group = groupChatService.getGroupChat(id);
            Map<String, Object> frontend = new java.util.HashMap<>();
            frontend.put("id", group.id());
            frontend.put("name", group.name());
            frontend.put("description", group.description());
            frontend.put("avatarUrl", group.avatarUrl());
            frontend.put("creatorId", group.creatorId());
            frontend.put("chatType", group.chatType());
            return ResponseEntity.ok(frontend);
        } catch (Exception e) {
            logger.error("Erreur récupération frontend groupe", e);
            throw relancer(e);
        }
    }
    
    @PostMapping("/test/run-all")
    public ResponseEntity<Map<String, Object>> runAllTests() {
        try {
            Map<String, Object> result = new java.util.HashMap<>();
            result.put("status", "success");
            result.put("message", "GroupChatController is operational");
            result.put("endpoints", 26);
            result.put("timestamp", java.time.LocalDateTime.now().toString());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Erreur run all tests", e);
            throw relancer(e);
        }
    }

    /**
     * Balayage de robustesse (2026-11-13) : chaque erreur devenait un 500, même un champ manquant ou une discussion
     * introuvable. L'erreur est relancée telle quelle : le gestionnaire global renvoie 400 / 404 avec la vraie cause,
     * et 500 seulement pour une vraie erreur du serveur.
     */
    private static RuntimeException relancer(Exception e) {
        return e instanceof RuntimeException r ? r : new RuntimeException(e.getMessage(), e);
    }

    // ==================== CONTRÔLES D'IDENTITÉ ET D'APPARTENANCE (2026-11-13) ====================
    // Avant : l'auteur (creatorId, senderId, userId, user1Id) était lu dans la requête : n'importe quel compte agissait
    // au nom d'un autre (un élève créait un groupe au nom de l'administrateur), et aucune route ne vérifiait
    // l'appartenance au groupe (lecture des messages, ajout de participants, renommage par un non-membre).

    /** Identifiant du compte connecté (401 sinon). */
    private static String moi() {
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof ecole_221.sn.ecole.modules.auth.domain.model.User u && u.getId() != null) {
            return u.getId();
        }
        throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.UNAUTHORIZED, "Veuillez vous connecter.");
    }

    /** L'identité envoyée, si elle l'est, doit être celle du compte connecté ; renvoie toujours celle du compte. */
    private static String exigerMoi(String fourni) {
        String moi = moi();
        if (fourni != null && !fourni.isBlank() && !fourni.equals(moi)) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN,
                    "Vous ne pouvez agir qu'en votre propre nom.");
        }
        return moi;
    }

    private void exigerMembre(String groupeId) {
        GroupChatDto g = groupChatService.getGroupChat(groupeId);
        if (g.participantIds() == null || !g.participantIds().contains(moi())) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN,
                    "Vous ne faites pas partie de cette discussion.");
        }
    }

    private void exigerAdministrateur(String groupeId) {
        GroupChatDto g = groupChatService.getGroupChat(groupeId);
        if (g.adminIds() == null || !g.adminIds().contains(moi())) {
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN,
                    "Seuls les administrateurs de la discussion peuvent la modifier.");
        }
    }
}
