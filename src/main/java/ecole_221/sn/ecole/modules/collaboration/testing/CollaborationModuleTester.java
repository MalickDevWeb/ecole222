package ecole_221.sn.ecole.modules.collaboration.testing;

import ecole_221.sn.ecole.infrastructure.logging.ModuleTestLogger;
import ecole_221.sn.ecole.modules.collaboration.application.port.dto.GroupChatDto;
import ecole_221.sn.ecole.modules.collaboration.application.port.dto.MessageDto;
import ecole_221.sn.ecole.modules.collaboration.application.service.GroupChatService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Testeur complet du module Collaboration avec logging des erreurs dans des fichiers .log
 */
@Component
public class CollaborationModuleTester {
    
    private static final Logger logger = LoggerFactory.getLogger(CollaborationModuleTester.class);
    private final ModuleTestLogger testLogger;
    private final GroupChatService groupChatService;
    
    public CollaborationModuleTester(ModuleTestLogger testLogger, GroupChatService groupChatService) {
        this.testLogger = testLogger;
        this.groupChatService = groupChatService;
    }
    
    public void runAllTests() {
        logger.info("🚀 Démarrage des tests du module Collaboration");
        testLogger.logModuleTest("Collaboration", "TEST_SUITE_START", true, "Début des tests complets", null);
        
        // Nettoyer les logs précédents
        testLogger.clearModuleLogs("Collaboration");
        
        // Exécuter tous les tests
        testGroupChatCRUD();
        testMessageCRUD();
        testMediaUpload();
        testPerformance();
        
        testLogger.logModuleTest("Collaboration", "TEST_SUITE_END", true, "Fin des tests complets", null);
        logger.info("✅ Tests du module Collaboration terminés");
    }
    
    private void testGroupChatCRUD() {
        logger.info("📝 Test CRUD GroupChat");
        testLogger.logModuleTest("Collaboration", "GROUP_CHAT_CRUD_START", true, "Début tests GroupChat", null);
        
        try {
            // CREATE
            testLogger.logCrudOperation("Collaboration", "GroupChat", "CREATE", "Test Group", true, null);
            GroupChatDto createdGroup = groupChatService.createGroup("Test Group", "user-001");
            testLogger.logModuleTest("Collaboration", "GROUP_CHAT_CREATE", true, 
                "Groupe créé: " + createdGroup.id(), null);
            
            // READ
            testLogger.logCrudOperation("Collaboration", "GroupChat", "READ", createdGroup.id(), true, null);
            GroupChatDto readGroup = groupChatService.getGroupChat(createdGroup.id());
            testLogger.logModuleTest("Collaboration", "GROUP_CHAT_READ", true, 
                "Groupe lu: " + readGroup.id(), null);
            
            // UPDATE
            testLogger.logCrudOperation("Collaboration", "GroupChat", "UPDATE", "Updated Name", true, null);
            GroupChatDto updatedGroup = groupChatService.updateGroupName(createdGroup.id(), "Updated Test Group");
            testLogger.logModuleTest("Collaboration", "GROUP_CHAT_UPDATE", true, 
                "Groupe mis à jour: " + updatedGroup.name(), null);
            
            // ADD PARTICIPANT
            testLogger.logCrudOperation("Collaboration", "GroupChat", "ADD_PARTICIPANT", "user-002", true, null);
            groupChatService.addParticipant(createdGroup.id(), "user-002");
            testLogger.logModuleTest("Collaboration", "GROUP_CHAT_ADD_PARTICIPANT", true, 
                "Participant ajouté: user-002", null);
            
            // LIST
            testLogger.logCrudOperation("Collaboration", "GroupChat", "LIST", "user-001", true, null);
            List<GroupChatDto> userChats = groupChatService.getUserChats("user-001");
            testLogger.logModuleTest("Collaboration", "GROUP_CHAT_LIST", true, 
                "Chats de l'utilisateur: " + userChats.size(), null);
            
            // DELETE
            testLogger.logCrudOperation("Collaboration", "GroupChat", "DELETE", createdGroup.id(), true, null);
            groupChatService.deleteGroup(createdGroup.id());
            testLogger.logModuleTest("Collaboration", "GROUP_CHAT_DELETE", true, 
                "Groupe supprimé: " + createdGroup.id(), null);
            
        } catch (Exception e) {
            testLogger.logModuleTest("Collaboration", "GROUP_CHAT_CRUD_ERROR", false, 
                "Erreur dans les tests GroupChat", e);
        }
        
        testLogger.logModuleTest("Collaboration", "GROUP_CHAT_CRUD_END", true, "Fin tests GroupChat", null);
    }
    
    private void testMessageCRUD() {
        logger.info("📝 Test CRUD Message");
        testLogger.logModuleTest("Collaboration", "MESSAGE_CRUD_START", true, "Début tests Message", null);
        
        try {
            // Créer un groupe pour les tests
            GroupChatDto testGroup = groupChatService.createGroup("Message Test Group", "user-001");
            
            // CREATE TEXT MESSAGE
            testLogger.logCrudOperation("Collaboration", "Message", "CREATE_TEXT", "Hello World", true, null);
            MessageDto textMessage = groupChatService.sendTextMessage(testGroup.id(), "user-001", "Hello World");
            testLogger.logModuleTest("Collaboration", "MESSAGE_CREATE_TEXT", true, 
                "Message texte créé: " + textMessage.id(), null);
            
            // CREATE REPLY
            testLogger.logCrudOperation("Collaboration", "Message", "CREATE_REPLY", "Reply", true, null);
            MessageDto replyMessage = groupChatService.replyToMessage(testGroup.id(), "user-002", "Reply", textMessage.id());
            testLogger.logModuleTest("Collaboration", "MESSAGE_CREATE_REPLY", true, 
                "Réponse créée: " + replyMessage.id(), null);
            
            // LIST MESSAGES
            testLogger.logCrudOperation("Collaboration", "Message", "LIST", testGroup.id(), true, null);
            List<MessageDto> messages = groupChatService.getChatMessages(testGroup.id(), 10, 0);
            testLogger.logModuleTest("Collaboration", "MESSAGE_LIST", true, 
                "Messages récupérés: " + messages.size(), null);
            
            // MARK AS READ
            testLogger.logCrudOperation("Collaboration", "Message", "MARK_READ", textMessage.id(), true, null);
            groupChatService.markMessageAsRead(testGroup.id(), textMessage.id(), "user-001");
            testLogger.logModuleTest("Collaboration", "MESSAGE_MARK_READ", true, 
                "Message marqué comme lu: " + textMessage.id(), null);
            
            // DELETE MESSAGE
            testLogger.logCrudOperation("Collaboration", "Message", "DELETE", textMessage.id(), true, null);
            groupChatService.deleteMessage(textMessage.id());
            testLogger.logModuleTest("Collaboration", "MESSAGE_DELETE", true, 
                "Message supprimé: " + textMessage.id(), null);
            
            // Nettoyage
            groupChatService.deleteGroup(testGroup.id());
            
        } catch (Exception e) {
            testLogger.logModuleTest("Collaboration", "MESSAGE_CRUD_ERROR", false, 
                "Erreur dans les tests Message", e);
        }
        
        testLogger.logModuleTest("Collaboration", "MESSAGE_CRUD_END", true, "Fin tests Message", null);
    }
    
    private void testMediaUpload() {
        logger.info("📝 Test Upload Media");
        testLogger.logModuleTest("Collaboration", "MEDIA_UPLOAD_START", true, "Début tests Media", null);
        
        try {
            // Créer un groupe pour les tests
            GroupChatDto testGroup = groupChatService.createGroup("Media Test Group", "user-001");
            
            // TEST IMAGE UPLOAD
            testLogger.logCrudOperation("Collaboration", "Media", "UPLOAD_IMAGE", "test_image.jpg", true, null);
            try {
                byte[] testImageData = new byte[]{1, 2, 3, 4, 5}; // Données de test
                MessageDto imageMessage = groupChatService.sendImageMessage(testGroup.id(), "user-001", testImageData);
                testLogger.logModuleTest("Collaboration", "MEDIA_UPLOAD_IMAGE", true, 
                    "Image uploadée: " + imageMessage.id(), null);
            } catch (Exception e) {
                testLogger.logModuleTest("Collaboration", "MEDIA_UPLOAD_IMAGE", false, 
                    "Erreur upload image", e);
            }
            
            // TEST AUDIO UPLOAD (2 minutes max)
            testLogger.logCrudOperation("Collaboration", "Media", "UPLOAD_AUDIO", "test_audio.mp3", true, null);
            try {
                byte[] testAudioData = new byte[]{1, 2, 3, 4, 5}; // Données de test
                MessageDto audioMessage = groupChatService.sendAudioMessage(testGroup.id(), "user-001", testAudioData, 60);
                testLogger.logModuleTest("Collaboration", "MEDIA_UPLOAD_AUDIO", true, 
                    "Audio uploadé: " + audioMessage.id(), null);
            } catch (Exception e) {
                testLogger.logModuleTest("Collaboration", "MEDIA_UPLOAD_AUDIO", false, 
                    "Erreur upload audio", e);
            }
            
            // TEST AUDIO DURATION EXCEEDED
            testLogger.logCrudOperation("Collaboration", "Media", "UPLOAD_AUDIO_EXCEEDED", "test_audio_long.mp3", true, null);
            try {
                byte[] testLongAudioData = new byte[]{1, 2, 3, 4, 5};
                MessageDto longAudioMessage = groupChatService.sendAudioMessage(testGroup.id(), "user-001", testLongAudioData, 121); // 121 secondes > 2 minutes
                testLogger.logModuleTest("Collaboration", "MEDIA_UPLOAD_AUDIO_EXCEEDED", false, 
                    "Audio trop long devrait échouer", null);
            } catch (Exception e) {
                testLogger.logModuleTest("Collaboration", "MEDIA_UPLOAD_AUDIO_EXCEEDED", true, 
                    "Audio trop long correctement rejeté: " + e.getMessage(), null);
            }
            
            // Nettoyage
            groupChatService.deleteGroup(testGroup.id());
            
        } catch (Exception e) {
            testLogger.logModuleTest("Collaboration", "MEDIA_UPLOAD_ERROR", false, 
                "Erreur dans les tests Media", e);
        }
        
        testLogger.logModuleTest("Collaboration", "MEDIA_UPLOAD_END", true, "Fin tests Media", null);
    }
    
    private void testPerformance() {
        logger.info("📝 Test Performance");
        testLogger.logModuleTest("Collaboration", "PERFORMANCE_START", true, "Début tests Performance", null);
        
        try {
            // Test création de 100 messages
            GroupChatDto testGroup = groupChatService.createGroup("Performance Test Group", "user-001");
            
            long startTime = System.currentTimeMillis();
            for (int i = 0; i < 100; i++) {
                groupChatService.sendTextMessage(testGroup.id(), "user-001", "Message " + i);
            }
            long duration = System.currentTimeMillis() - startTime;
            
            boolean acceptable = duration < 5000; // 5 secondes max pour 100 messages
            testLogger.logPerformance("Collaboration", "CREATE_100_MESSAGES", duration, acceptable);
            
            // Test récupération de 100 messages
            startTime = System.currentTimeMillis();
            List<MessageDto> messages = groupChatService.getChatMessages(testGroup.id(), 100, 0);
            duration = System.currentTimeMillis() - startTime;
            
            acceptable = duration < 1000; // 1 seconde max pour récupérer
            testLogger.logPerformance("Collaboration", "RETRIEVE_100_MESSAGES", duration, acceptable);
            
            // Nettoyage
            groupChatService.deleteGroup(testGroup.id());
            
        } catch (Exception e) {
            testLogger.logModuleTest("Collaboration", "PERFORMANCE_ERROR", false, 
                "Erreur dans les tests Performance", e);
        }
        
        testLogger.logModuleTest("Collaboration", "PERFORMANCE_END", true, "Fin tests Performance", null);
    }
}