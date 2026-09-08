package ecole_221.sn.ecole.modules.collaboration.infrastructure.persistence;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
public class CollaborationDataInitializer implements CommandLineRunner {

    private final WorkgroupJpaRepository workgroupRepository;
    private final DocumentJpaRepository documentRepository;
    private final TaskJpaRepository taskRepository;
    private final MessageJpaRepository messageRepository;
    private final MeetJpaRepository meetRepository;

    public CollaborationDataInitializer(
            WorkgroupJpaRepository workgroupRepository,
            DocumentJpaRepository documentRepository,
            TaskJpaRepository taskRepository,
            MessageJpaRepository messageRepository,
            MeetJpaRepository meetRepository) {
        this.workgroupRepository = workgroupRepository;
        this.documentRepository = documentRepository;
        this.taskRepository = taskRepository;
        this.messageRepository = messageRepository;
        this.meetRepository = meetRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (workgroupRepository.count() == 0) {
            populateWorkgroups();
            populateDocuments();
            populateTasks();
            populateMessages();
            // populateMeets(); // Temporarily disabled due to database schema issue
            System.out.println("Collaboration data initialized successfully!");
        }
    }

    private void populateWorkgroups() {
        // Groupes de travail pour les cours
        workgroupRepository.save(new WorkgroupJpaEntity(
            "group-001", 
            "Groupe Architecture Microservices", 
            "Espace de collaboration pour le cours d'Architecture Microservices - L1 Génie Civil",
            "system",
            5
        ));
        
        workgroupRepository.save(new WorkgroupJpaEntity(
            "group-002", 
            "Groupe Management Stratégique", 
            "Groupe de travail pour les projets de management - L1 Génie Civil",
            "system",
            4
        ));
        
        workgroupRepository.save(new WorkgroupJpaEntity(
            "group-003", 
            "Projet DevOps", 
            "Équipe de projet CI/CD et DevOps",
            "system",
            6
        ));
        
        workgroupRepository.save(new WorkgroupJpaEntity(
            "group-004", 
            "Génie Logiciel - Équipe A", 
            "Groupe de développement logiciel",
            "system",
            3
        ));
    }

    private void populateDocuments() {
        // Documents pour le groupe 001
        documentRepository.save(new DocumentJpaEntity(
            "doc-001",
            "group-001",
            "Cours Architecture V1.pdf",
            "PDF",
            "2.5 MB",
            "http://localhost:8080/files/architecture-v1.pdf",
            "usr-002"
        ));
        
        documentRepository.save(new DocumentJpaEntity(
            "doc-002",
            "group-001",
            "Guide Microservices.docx",
            "Word",
            "1.8 MB",
            "http://localhost:8080/files/guide-microservices.docx",
            "usr-002"
        ));
        
        // Documents pour le groupe 002
        documentRepository.save(new DocumentJpaEntity(
            "doc-003",
            "group-002",
            "Étude de Cas Management.pdf",
            "PDF",
            "3.2 MB",
            "http://localhost:8080/files/etude-cas.pdf",
            "usr-002"
        ));
        
        // Documents pour le groupe 003
        documentRepository.save(new DocumentJpaEntity(
            "doc-004",
            "group-003",
            "Dockerfile Template",
            "Text",
            "15 KB",
            "http://localhost:8080/files/dockerfile.txt",
            "usr-001"
        ));
        
        documentRepository.save(new DocumentJpaEntity(
            "doc-005",
            "group-003",
            "CI-CD Pipeline.yml",
            "YAML",
            "8 KB",
            "http://localhost:8080/files/ci-cd-pipeline.yml",
            "usr-001"
        ));
    }

    private void populateTasks() {
        // Tâches pour le groupe 001
        taskRepository.save(new TaskJpaEntity(
            "task-001",
            "group-001",
            "Étudier le pattern Circuit Breaker",
            "en_cours",
            "usr-001",
            "HIGH"
        ));
        
        taskRepository.save(new TaskJpaEntity(
            "task-002",
            "group-001",
            "Implémenter un service de commandes",
            "a_faire",
            "usr-001",
            "MEDIUM"
        ));
        
        taskRepository.save(new TaskJpaEntity(
            "task-003",
            "group-001",
            "Rédiger la documentation API",
            "termine",
            "usr-002",
            "LOW"
        ));
        
        // Tâches pour le groupe 002
        taskRepository.save(new TaskJpaEntity(
            "task-004",
            "group-002",
            "Analyse SWOT de l'entreprise",
            "en_cours",
            "usr-001",
            "HIGH"
        ));
        
        taskRepository.save(new TaskJpaEntity(
            "task-005",
            "group-002",
            "Présentation stratégie marketing",
            "a_faire",
            "usr-002",
            "MEDIUM"
        ));
        
        // Tâches pour le groupe 003
        taskRepository.save(new TaskJpaEntity(
            "task-006",
            "group-003",
            "Configurer Jenkins",
            "en_cours",
            "usr-001",
            "HIGH"
        ));
        
        taskRepository.save(new TaskJpaEntity(
            "task-007",
            "group-003",
            "Déployer sur Kubernetes",
            "a_faire",
            "usr-001",
            "MEDIUM"
        ));
    }

    private void populateMessages() {
        // Messages pour le groupe 001
        messageRepository.save(new MessageJpaEntity(
            "msg-001",
            "group-001",
            "usr-002",
            "Bonjour à tous, avez-vous avancé sur le pattern Circuit Breaker?",
            LocalDateTime.now().minusDays(2)
        ));
        
        messageRepository.save(new MessageJpaEntity(
            "msg-002",
            "group-001",
            "usr-001",
            "Oui, j'ai commencé à l'implémenter. Je partage mon code demain.",
            LocalDateTime.now().minusDays(1)
        ));
        
        messageRepository.save(new MessageJpaEntity(
            "msg-003",
            "group-001",
            "usr-002",
            "Super! N'oublie pas de documenter les tests.",
            LocalDateTime.now().minusDays(1)
        ));
        
        // Messages pour le groupe 002
        messageRepository.save(new MessageJpaEntity(
            "msg-004",
            "group-002",
            "usr-002",
            "La réunion pour l'analyse SWOT est prévue demain à 14h.",
            LocalDateTime.now().minusHours(5)
        ));
        
        messageRepository.save(new MessageJpaEntity(
            "msg-005",
            "group-002",
            "usr-001",
            "C'est noté. Je prépare les documents nécessaires.",
            LocalDateTime.now().minusHours(4)
        ));
        
        // Messages pour le groupe 003
        messageRepository.save(new MessageJpaEntity(
            "msg-006",
            "group-003",
            "usr-001",
            "J'ai réussi à configurer le pipeline Jenkins!",
            LocalDateTime.now().minusHours(2)
        ));
        
        messageRepository.save(new MessageJpaEntity(
            "msg-007",
            "group-003",
            "usr-002",
            "Excellent! On peut passer au déploiement Kubernetes maintenant.",
            LocalDateTime.now().minusHours(1)
        ));
    }

    private void populateMeets() {
        // Réunions pour les professeurs et étudiants
        meetRepository.save(new MeetJpaEntity(
            "meet-001",
            "Cours Architecture Microservices - Session Live",
            "https://meet.google.com/abc-defg-hij",
            LocalDateTime.now().plusDays(1).withHour(10).withMinute(0).toString(),
            true
        ));
        
        meetRepository.save(new MeetJpaEntity(
            "meet-002",
            "Tutorial DevOps - Docker & Kubernetes",
            "https://meet.google.com/xyz-uvwx-yz",
            LocalDateTime.now().plusDays(2).withHour(14).withMinute(30).toString(),
            true
        ));
        
        meetRepository.save(new MeetJpaEntity(
            "meet-003",
            "Présentation Projets Management",
            "https://meet.google.com/pqr-stuv-wx",
            LocalDateTime.now().plusDays(3).withHour(9).withMinute(0).toString(),
            true
        ));
        
        meetRepository.save(new MeetJpaEntity(
            "meet-004",
            "Support Génie Logiciel",
            "https://meet.google.com/lmn-opqr-st",
            LocalDateTime.now().plusHours(3).toString(),
            true
        ));
    }
}