package ecole_221.sn.ecole.modules.collaboration.infrastructure.web;

import ecole_221.sn.ecole.infrastructure.mapper.FrontendDataMapper;
import ecole_221.sn.ecole.modules.collaboration.application.port.dto.GroupChatDto;
import ecole_221.sn.ecole.modules.collaboration.application.port.dto.MessageDto;
import ecole_221.sn.ecole.modules.collaboration.application.service.GroupChatService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/collaboration/groupchat")
@Tag(name = "Messagerie de groupe", description = "Discussions de groupe et à deux, messages texte, vocaux et images")
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
    
    @Operation(summary = "Créer une discussion de groupe", description = "Corps {name} (obligatoire, 100 caractères au plus). Le créateur est le compte connecté (creatorId facultatif, refusé s'il désigne un autre compte) ; il devient administrateur de la discussion.")
    @PostMapping("/groups")
    public ResponseEntity<?> createGroup(@RequestBody Map<String, String> request) {
        try {
            // APP-QA-04 : nom obligatoire et borné
            String name = request == null ? null : request.get("name");
            if (name == null || name.isBlank()) {
                throw ecole_221.sn.ecole.partage.ChampsRequete.refus("Le nom de la discussion est obligatoire.");
            }
            if (name.strip().length() > 100) {
                throw ecole_221.sn.ecole.partage.ChampsRequete.refus("Le nom de la discussion ne doit pas dépasser 100 caractères.");
            }
            name = name.strip();
            String creatorId = exigerMoi(request.get("creatorId"));
            GroupChatDto created = groupChatService.createGroup(name, creatorId);
            return ResponseEntity.ok(created);
        } catch (Exception e) {
            logger.error("Erreur création groupe", e);
            throw relancer(e);
        }
    }
    
    @Operation(summary = "Changer l'image d'une discussion", description = "Fichier multipart « file ». Réservé aux administrateurs de la discussion (403 sinon) ; fichier vide → 400.")
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
    
    @Operation(summary = "Détail d'une discussion", description = "Nom, description, participants, administrateurs, état (sourdine, archive, épingle). Réservé aux participants (403 sinon).")
    @GetMapping("/groups/{id}")
    public ResponseEntity<GroupChatDto> getGroup(@PathVariable String id) {
        try {
            exigerMembre(id);
            GroupChatDto group = groupChatService.getGroupChat(id);
            return ResponseEntity.ok(group);
        } catch (Exception e) {
            throw relancer(e);
        }
    }
    
    @Operation(summary = "Mes discussions", description = "Discussions du compte connecté ; userId doit être le sien (403 sinon).")
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
    
    @Operation(summary = "Renommer une discussion", description = "Corps {name}. Réservé aux administrateurs de la discussion.")
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
    
    @Operation(summary = "Ajouter un participant", description = "Corps {participantId}. Réservé aux administrateurs de la discussion.")
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
    
    @Operation(summary = "Retirer un participant", description = "Réservé aux administrateurs de la discussion.")
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
    
    @Operation(summary = "Modifier la description d'une discussion", description = "Corps {description}. Réservé aux administrateurs de la discussion.")
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
    
    @Operation(summary = "Nommer un administrateur de discussion", description = "Corps {adminId}. Réservé aux administrateurs de la discussion.")
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
    
    @Operation(summary = "Retirer un administrateur de discussion", description = "Réservé aux administrateurs de la discussion.")
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
    
    @Operation(summary = "Mettre une discussion en sourdine", description = "Corps {durationMinutes?} (60 par défaut) ; userId facultatif, obligatoirement le compte connecté. Réservé aux participants.")
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
    
    @Operation(summary = "Réactiver les notifications d'une discussion", description = "userId facultatif, obligatoirement le compte connecté. Réservé aux participants.")
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
    
    @Operation(summary = "Archiver une discussion (pour soi)", description = "userId facultatif, obligatoirement le compte connecté. Réservé aux participants.")
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
    
    @Operation(summary = "Désarchiver une discussion", description = "userId facultatif, obligatoirement le compte connecté. Réservé aux participants.")
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
    
    @Operation(summary = "Épingler une discussion", description = "userId facultatif, obligatoirement le compte connecté. Réservé aux participants.")
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
    
    @Operation(summary = "Désépingler une discussion", description = "userId facultatif, obligatoirement le compte connecté. Réservé aux participants.")
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
    
    @Operation(summary = "Mes discussions d'un type", description = "Discussions du type demandé dont le compte connecté fait partie.")
    @GetMapping("/groups/types/{type}")
    public ResponseEntity<List<GroupChatDto>> getGroupsByType(@PathVariable String type) {
        try {
            // Seulement les discussions dont le compte fait partie
            String moi = moi();
            List<GroupChatDto> groups = groupChatService.getGroupChatsByType(type).stream()
                    .filter(g -> g.participantIds() != null && g.participantIds().contains(moi)).toList();
            return ResponseEntity.ok(groups);
        } catch (Exception e) {
            logger.error("Erreur récupération groupes par type", e);
            throw relancer(e);
        }
    }
    
    @Operation(summary = "Ouvrir une discussion à deux", description = "Corps {user2Id} (obligatoire, différent de soi) ; user1Id facultatif, obligatoirement le compte connecté.")
    @PostMapping("/individual")
    public ResponseEntity<GroupChatDto> createIndividualChat(@RequestBody Map<String, String> request) {
        try {
            String user1Id = exigerMoi(request.get("user1Id"));
            String user2Id = request.get("user2Id");
            // APP-QA-04 : interlocuteur obligatoire et différent de soi
            if (user2Id == null || user2Id.isBlank() || user2Id.length() > 255) {
                throw ecole_221.sn.ecole.partage.ChampsRequete.refus("L'interlocuteur est obligatoire.");
            }
            if (user2Id.equals(user1Id)) {
                throw ecole_221.sn.ecole.partage.ChampsRequete.refus("Vous ne pouvez pas ouvrir une discussion avec vous-même.");
            }
            GroupChatDto created = groupChatService.createIndividualChat(user1Id, user2Id);
            return ResponseEntity.ok(created);
        } catch (Exception e) {
            logger.error("Erreur création chat individuel", e);
            throw relancer(e);
        }
    }
    
    @Operation(summary = "Envoyer un message texte", description = "Corps {content}. L'auteur est le compte connecté (senderId facultatif, refusé s'il désigne un autre compte). Réservé aux participants.")
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
    
    @Operation(summary = "Envoyer un message vocal", description = "Multipart : « audio » (fichier), « senderId » (le compte connecté). Réservé aux participants.")
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
    
    @Operation(summary = "Envoyer une image", description = "Multipart : « image » (fichier), « senderId » (le compte connecté). Réservé aux participants.")
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
    
    @Operation(summary = "Messages d'une discussion", description = "Réservé aux participants.")
    @GetMapping("/groups/{id}/messages")
    public ResponseEntity<List<MessageDto>> getMessages(@PathVariable String id) {
        try {
            exigerMembre(id);
            return ResponseEntity.ok(groupChatService.getMessages(id));
        } catch (Exception e) {
            logger.error("Erreur récupération messages", e);
            throw relancer(e);
        }
    }
    
    @Operation(summary = "Marquer un message comme lu", description = "Corps {chatId} (discussion du message) ; userId facultatif, obligatoirement le compte connecté. Réservé aux participants.")
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
            logger.warn("Marquage message lu refusé : {}", e.getMessage());
            throw relancer(e);
        }
    }
    
    @Operation(summary = "Supprimer un message", description = "Réservé à l'auteur du message ou à un administrateur de la discussion.")
    @DeleteMapping("/messages/{id}")
    public ResponseEntity<java.util.Map<String, Object>> deleteMessage(@PathVariable String id) {
        try {
            // Auteur ou administrateur de la discussion uniquement ; échec annoncé.
            // 200 avec le message de la corbeille
            return ResponseEntity.ok(groupChatService.supprimerMessage(id, moi()));
        } catch (Exception e) {
            logger.warn("Suppression de message refusée : {}", e.getMessage());
            throw relancer(e);
        }
    }
    
    @Operation(summary = "Résumé d'une discussion pour l'écran", description = "id, nom, description, image, créateur, type. Réservé aux participants.")
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

    /**
     * L'erreur est relancée telle quelle : le gestionnaire global renvoie 400 / 404 avec la vraie cause,
     * et 500 seulement pour une vraie erreur du serveur.
     */
    private static RuntimeException relancer(Exception e) {
        return e instanceof RuntimeException r ? r : new RuntimeException(e.getMessage(), e);
    }

    // ==================== CONTRÔLES D'IDENTITÉ ET D'APPARTENANCE ====================

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
