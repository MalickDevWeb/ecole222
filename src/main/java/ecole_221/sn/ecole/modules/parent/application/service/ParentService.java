package ecole_221.sn.ecole.modules.parent.application.service;

import ecole_221.sn.ecole.modules.parent.application.port.dto.*;
import ecole_221.sn.ecole.modules.parent.application.port.usecase.ParentUseCase;
import ecole_221.sn.ecole.modules.parent.domain.model.Parent;
import ecole_221.sn.ecole.modules.parent.domain.model.ParentEnfant;
import ecole_221.sn.ecole.modules.parent.domain.model.Appointment;
import ecole_221.sn.ecole.modules.parent.domain.repository.AppointmentRepositoryPort;
import ecole_221.sn.ecole.modules.parent.domain.repository.ParentRepositoryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service d'application pour le module Parent.
 * Implémente les cas d'utilisation définis dans ParentUseCase.
 */
@Service
@Transactional
public class ParentService implements ParentUseCase {

    private static final Logger logger = LoggerFactory.getLogger(ParentService.class);
    
    private final ParentRepositoryPort parentRepositoryPort;
    private final AppointmentRepositoryPort appointmentRepositoryPort;

    public ParentService(ParentRepositoryPort parentRepositoryPort,
                         AppointmentRepositoryPort appointmentRepositoryPort) {
        this.parentRepositoryPort = parentRepositoryPort;
        this.appointmentRepositoryPort = appointmentRepositoryPort;
    }

    @Override
    public List<ChildDto> getEnfantsByParentId(String parentId) {
        logger.info("Récupération des enfants du parent: {}", parentId);
        
        // TODO: Implémenter avec données réelles depuis les repositories Student/ParentEnfant
        // Exemple temporaire avec données mockées
        List<ChildDto> enfants = new ArrayList<>();
        
        // Récupérer les liens parent-enfant
        List<ParentEnfant> parentEnfants = parentRepositoryPort.findParentEnfantsByParentId(parentId);
        
        for (ParentEnfant pe : parentEnfants) {
            // TODO: Récupérer les vrais détails de l'étudiant depuis StudentRepository
            // Pour l'instant, on retourne des données mockées
            enfants.add(new ChildDto(
                pe.getEnfantId(),
                "Diallo",
                "Aminata",
                "E221-2026-001",
                "Informatique",
                "L2",
                "L2-INFO-A",
                null
            ));
        }
        
        // Si aucun enfant trouvé, retourner une liste vide
        if (enfants.isEmpty()) {
            // Essayer de trouver au moins un enfant par défaut
            enfants.add(new ChildDto(
                "usr-001",
                "Sall",
                "Mamadou",
                "2026-GC-001",
                "Génie Civil",
                "L1",
                "L1-GL",
                null
            ));
        }
        
        return enfants;
    }

    @Override
    public ChildStatsDto getChildStats(String enfantId) {
        logger.info("Récupération des stats pour l'enfant: {}", enfantId);
        
        // TODO: Implémenter avec données réelles
        // Calculer les stats à partir des repositories
        
        List<ChildStatsDto.ExamDto> prochainsExamens = List.of(
            new ChildStatsDto.ExamDto("Algorithmique Avancée", "2026-08-20", "A101"),
            new ChildStatsDto.ExamDto("Base de Données", "2026-08-25", "B203")
        );
        
        List<ChildStatsDto.CourseDto> coursAujourdhui = List.of(
            new ChildStatsDto.CourseDto(
                "c1",
                getCurrentDay(),
                "08h00 - 10h00",
                "Algorithmique Avancée",
                "Dr. Sall",
                "A101",
                "CM",
                true
            )
        );
        
        return new ChildStatsDto(
            87.0,   // tauxPresence
            3,      // nombreAbsences
            2,      // nombreRetards
            14.5,   // moyenneGenerale
            24,     // creditValides
            1,      // devoirsEnRetard
            prochainsExamens,
            coursAujourdhui
        );
    }

    @Override
    public List<PresenceDto> getChildPresences(String enfantId, String mois) {
        logger.info("Récupération des présences pour l'enfant: {}, mois: {}", enfantId, mois);
        
        // TODO: Implémenter avec données réelles depuis PresenceRepository
        // Si mois est null, utiliser le mois courant
        String monthToUse = mois != null ? mois : LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        
        return List.of(
            new PresenceDto("a1", "2026-08-12", "PRESENT", "Algorithmique", "Dr. Sall", "08h00"),
            new PresenceDto("a2", "2026-08-11", "RETARD", "Base de Données", "Prof. Ndiaye", "10h30"),
            new PresenceDto("a3", "2026-08-10", "ABSENT", "Réseaux", "Dr. Mbaye", "14h00")
        );
    }

    @Override
    public List<GradeDto> getChildGrades(String enfantId) {
        logger.info("Récupération des notes pour l'enfant: {}", enfantId);
        
        // TODO: Implémenter avec données réelles depuis GradeRepository
        
        // Calculer la mention pour chaque note
        return List.of(
            new GradeDto("g1", "Algorithmique Avancée", "Dr. Sall", 15.5, 14.0, 14.6, "Bien", 6),
            new GradeDto("g2", "Base de Données", "Prof. Ndiaye", 16.0, 13.5, 14.5, "Bien", 5)
        );
    }

    @Override
    public List<ScheduleDto> getChildSchedule(String enfantId) {
        logger.info("Récupération du planning pour l'enfant: {}", enfantId);
        
        // TODO: Implémenter avec données réelles depuis PlanningRepository
        
        return List.of(
            new ScheduleDto("s1", "Lundi", "08h00 - 10h00", "Algorithmique", "Dr. Sall", "A101", "CM", false),
            new ScheduleDto("s2", "Mardi", "08h00 - 10h00", "Algorithmique", "Dr. Sall", "A101", "CM", true),
            new ScheduleDto("s3", "Mardi", "10h30 - 12h30", "Base de Données", "Prof. Ndiaye", "B203", "TD", false),
            new ScheduleDto("s4", "Mercredi", "14h00 - 16h00", "Réseaux", "Dr. Mbaye", "C101", "CM", false)
        );
    }

    @Override
    public List<MessageDto> getParentMessages(String parentId) {
        logger.info("Récupération des messages pour le parent: {}", parentId);
        
        // TODO: Implémenter avec données réelles depuis MessageRepository
        
        return List.of(
            new MessageDto(
                "m1",
                "Dr. Sall",
                "Professeur - Algorithmique",
                "Progrès de votre enfant",
                "Bonjour, je souhaite vous informer qu'Aminata montre d'excellentes dispositions en algorithmique.",
                "2026-08-10",
                false,
                null
            ),
            new MessageDto(
                "m3",
                "Administration",
                "Secrétariat",
                "Réunion parents-professeurs",
                "Une réunion parents-professeurs est prévue le 30 août 2026 à 10h dans l'amphithéâtre A.",
                "2026-08-05",
                true,
                null
            )
        );
    }

    @Override
    public MessageDto sendMessage(String parentId, SendMessageRequestDto request) {
        logger.info("Envoi de message par le parent: {}, à: {}", parentId, request.destinataireId());
        
        // TODO: Implémenter avec sauvegarde en base de données
        // Vérifier que destinataireId existe
        // Sauvegarder le message
        
        return new MessageDto(
            "msg-" + UUID.randomUUID().toString().substring(0, 8),
            "Parent",
            "PARENT",
            request.sujet(),
            request.message(),
            LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE),
            false,
            null
        );
    }

    @Override
    public MessageDto markMessageAsRead(String messageId, String parentId) {
        logger.info("Marquage du message {} comme lu par le parent: {}", messageId, parentId);
        
        // TODO: Implémenter avec mise à jour en base de données
        
        return new MessageDto(
            messageId,
            "Expéditeur",
            "Rôle",
            "Sujet",
            "Message",
            "2026-08-10",
            true,  // lu = true
            null
        );
    }

    @Override
    public List<ApplicationDto> getParentApplications(String parentId) {
        logger.info("Récupération des candidatures pour le parent: {}", parentId);
        
        // TODO: Implémenter avec données réelles depuis ApplicationRepository
        
        return List.of(
            new ApplicationDto(
                "d-123",
                "Aminata Diallo",
                "Informatique",
                "L1",
                "2026-08-12",
                "EN_ATTENTE",
                40,
                "Vérification",
                null
            )
        );
    }

    @Override
    public String enrollChild(String parentId, EnrollChildPayloadDto payload) {
        logger.info("Soumission de dossier de candidature par le parent: {}, enfant: {} {}", 
                   parentId, payload.nom(), payload.prenom());
        
        // TODO: Implémenter avec sauvegarde en base de données
        // Générer un dossierId
        String dossierId = "d-" + UUID.randomUUID().toString().substring(0, 8);
        
        return dossierId;
    }

    @Override
    public PaymentDto getParentPayments(String parentId) {
        logger.info("Récupération des paiements pour le parent: {}", parentId);
        
        // TODO: Implémenter avec données réelles depuis PaymentRepository
        
        List<PaymentDto.PaymentInstallmentDto> echeances = List.of(
            new PaymentDto.PaymentInstallmentDto(
                "p1",
                "1ère tranche",
                300000L,
                "2026-09-01",
                "PAYE",
                "2026-08-28"
            ),
            new PaymentDto.PaymentInstallmentDto(
                "p2",
                "2ème tranche",
                275000L,
                "2026-11-01",
                "EN_COURS",
                null
            ),
            new PaymentDto.PaymentInstallmentDto(
                "p3",
                "3ème tranche",
                275000L,
                "2026-01-15",
                "A_PAYER",
                null
            )
        );
        
        return new PaymentDto(850000L, echeances);
    }

    @Override
    public ParentProfileDto updateParentProfile(String parentId, ParentProfileDto profileDto) {
        logger.info("Mise à jour du profil du parent: {}", parentId);
        
        // TODO: Implémenter avec mise à jour en base de données
        
        return profileDto;
    }

    @Override
    public String requestAppointment(String parentId, AppointmentRequestDto request) {
        logger.info("Demande de RDV par le parent: {}, avec professeur: {}, date: {}", 
                   parentId, request.professeurId(), request.dateSouhaitee());
        
        String profNom = request.professeurId() != null && request.professeurId().contains("—") 
            ? request.professeurId() 
            : "Dr. Sall (Algorithmique & IA)";
            
        Appointment appointment = Appointment.create(
            parentId,
            request.professeurId(),
            profNom,
            request.dateSouhaitee(),
            request.motif()
        );
        
        Appointment saved = appointmentRepositoryPort.save(appointment);
        return saved.getId();
    }

    @Override
    public List<Map<String, Object>> getParentAppointments(String parentId) {
        List<Appointment> list = appointmentRepositoryPort.findByParentId(parentId);
        
        if (list.isEmpty()) {
            list = appointmentRepositoryPort.findAll();
        }
        
        if (list.isEmpty()) {
            Appointment init = Appointment.create(
                parentId,
                "usr-002",
                "Dr. Sall (Algorithmique & IA)",
                "2026-10-20 à 15h30",
                "Bilan académique et progression de l'élève"
            );
            appointmentRepositoryPort.save(init);
            list = List.of(init);
        }
        
        return list.stream().map(a -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", a.getId());
            map.put("parentId", a.getParentId());
            map.put("professeurId", a.getProfesseurId());
            map.put("professeurNom", a.getProfesseurNom());
            map.put("dateSouhaitee", a.getDateSouhaitee());
            map.put("motif", a.getMotif());
            map.put("status", a.getStatus());
            map.put("meetUrl", a.getMeetUrl());
            map.put("createdAt", a.getCreatedAt() != null ? a.getCreatedAt().toString() : "");
            return map;
        }).toList();
    }

    @Override
    public String justifyAbsence(String presenceId, String parentId, String motif, String fileUrl) {
        logger.info("Justification d'absence {} par le parent: {}, motif: {}", 
                   presenceId, parentId, motif);
        
        // TODO: Implémenter avec sauvegarde en base de données
        
        return "justif-" + UUID.randomUUID().toString().substring(0, 8);
    }

    // Helper method to get current day in French
    private String getCurrentDay() {
        String day = LocalDate.now().getDayOfWeek().name();
        return switch (day) {
            case "MONDAY" -> "Lundi";
            case "TUESDAY" -> "Mardi";
            case "WEDNESDAY" -> "Mercredi";
            case "THURSDAY" -> "Jeudi";
            case "FRIDAY" -> "Vendredi";
            case "SATURDAY" -> "Samedi";
            case "SUNDAY" -> "Dimanche";
            default -> "Lundi";
        };
    }
}
