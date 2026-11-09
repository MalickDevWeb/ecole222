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
    private final ecole_221.sn.ecole.modules.auth.domain.port.repository.UserRepositoryPort userRepositoryPort;

    @org.springframework.beans.factory.annotation.Autowired
    public ParentService(ParentRepositoryPort parentRepositoryPort,
                         AppointmentRepositoryPort appointmentRepositoryPort,
                         ecole_221.sn.ecole.modules.auth.domain.port.repository.UserRepositoryPort userRepositoryPort) {
        this.parentRepositoryPort = parentRepositoryPort;
        this.appointmentRepositoryPort = appointmentRepositoryPort;
        this.userRepositoryPort = userRepositoryPort;
    }

    /** Compatibilité (tests) : sans annuaire des comptes, les enfants sont renvoyés sans nom. */
    public ParentService(ParentRepositoryPort parentRepositoryPort,
                         AppointmentRepositoryPort appointmentRepositoryPort) {
        this(parentRepositoryPort, appointmentRepositoryPort, null);
    }

    @Override
    public List<ChildDto> getEnfantsByParentId(String parentId) {
        logger.info("Récupération des enfants du parent: {}", parentId);
        // APP-BE-19 (sécurité) : uniquement les liens parent ↔ enfant enregistrés.
        // Avant : tout parent sans enfant recevait l'élève « usr-001 » par défaut, et comme ce
        // résultat sert au contrôle de filiation du contrôleur, n'importe quel parent pouvait
        // consulter les notes, présences et statistiques de cet élève. Les enfants réels
        // étaient en outre tous renvoyés sous le nom fictif « Aminata Diallo ».
        List<ChildDto> enfants = new ArrayList<>();
        for (ParentEnfant pe : parentRepositoryPort.findParentEnfantsByParentId(parentId)) {
            var compte = userRepositoryPort == null ? Optional.<ecole_221.sn.ecole.modules.auth.domain.model.User>empty()
                    : userRepositoryPort.findById(pe.getEnfantId());
            enfants.add(new ChildDto(
                pe.getEnfantId(),
                compte.map(u -> u.getNom()).orElse(null),
                compte.map(u -> u.getPrenom()).orElse(null),
                compte.map(u -> u.getMatricule()).orElse(null),
                null, null, null, null   // filière, niveau, classe, photo : pas encore de source fiable
            ));
        }
        return enfants;
    }

    @Override
    public ChildStatsDto getChildStats(String enfantId) {
        logger.info("Récupération des stats pour l'enfant: {}", enfantId);
        // APP-BE-19 : calculées UNIQUEMENT à partir des présences et notes réellement saisies.
        // Avant : 87 % de présence, 14,5 de moyenne, examens et cours « Dr. Sall » inventés.
        // Sans donnée : null (rien d'inventé). Crédits, devoirs, examens, cours du jour : pas de source → null / vide.
        var presences = parentRepositoryPort.findPresencesByEnfantId(enfantId);
        Double taux = null;
        Integer absences = null;
        Integer retards = null;
        if (!presences.isEmpty()) {
            long presents = presences.stream().filter(p -> "PRESENT".equalsIgnoreCase(p.statut())).count();
            long retardsN = presences.stream().filter(p -> "RETARD".equalsIgnoreCase(p.statut()) || "LATE".equalsIgnoreCase(p.statut())).count();
            long absN = presences.stream().filter(p -> "ABSENT".equalsIgnoreCase(p.statut())).count();
            taux = Math.round((presents + retardsN) * 1000.0 / presences.size()) / 10.0;
            absences = (int) absN;
            retards = (int) retardsN;
        }
        var moyennes = getChildGrades(enfantId).stream().map(GradeDto::moyenne).filter(java.util.Objects::nonNull).toList();
        Double moyenne = moyennes.isEmpty() ? null
                : Math.round(moyennes.stream().mapToDouble(Double::doubleValue).average().orElse(0) * 100) / 100.0;
        return new ChildStatsDto(taux, absences, retards, moyenne, null, null, List.of(), List.of());
    }

    @Override
    public List<PresenceDto> getChildPresences(String enfantId, String mois) {
        logger.info("Récupération des présences pour l'enfant: {}, mois: {}", enfantId, mois);
        // APP-BE-19 : présences réellement saisies par les professeurs (table attendances), filtrées par mois si fourni
        return parentRepositoryPort.findPresencesByEnfantId(enfantId).stream()
                .filter(p -> mois == null || mois.isBlank() || (p.date() != null && p.date().startsWith(mois)))
                .map(p -> new PresenceDto(p.id(), p.date(), p.statut(),
                        p.coursNom() != null ? p.coursNom() : p.coursId(), nomProfesseur(p.professeurId()), null))
                .toList();
    }

    @Override
    public List<GradeDto> getChildGrades(String enfantId) {
        logger.info("Récupération des notes pour l'enfant: {}", enfantId);
        // APP-BE-19 : notes réellement saisies par les professeurs (table grades) — plus de notes inventées.
        // Moyenne = formule du module professeur (40 % CC + 60 % examen), seulement si les deux notes existent.
        // Mention et ECTS : aucun barème validé → null (rien d'inventé).
        return parentRepositoryPort.findNotesByEnfantId(enfantId).stream().map(n -> new GradeDto(
                n.id(),
                n.coursNom() != null ? n.coursNom() : n.coursId(),
                nomProfesseur(n.professeurId()),
                n.cc(),
                n.examen(),
                (n.cc() != null && n.examen() != null)
                        ? ecole_221.sn.ecole.modules.professor.application.port.dto.GradeDto.calculateFinalGrade(n.cc(), n.examen())
                        : null,
                null,
                null
        )).toList();
    }

    private String nomProfesseur(String professeurId) {
        if (professeurId == null || userRepositoryPort == null) {
            return null;
        }
        return userRepositoryPort.findById(professeurId)
                .map(u -> ((u.getPrenom() == null ? "" : u.getPrenom() + " ") + (u.getNom() == null ? "" : u.getNom())).trim())
                .orElse(null);
    }

    @Override
    public List<ScheduleDto> getChildSchedule(String enfantId) {
        logger.info("Récupération du planning pour l'enfant: {}", enfantId);
        // APP-BE-19 : aucun lien fiable élève → classe → séances n'existe encore ; liste vide plutôt
        // qu'un emploi du temps inventé (« Dr. Sall », « Prof. Ndiaye »…).
        return List.of();
    }

    @Override
    public List<MessageDto> getParentMessages(String parentId) {
        logger.info("Récupération des messages pour le parent: {}", parentId);
        // APP-BE-19 : aucune messagerie persistée → liste vide (avant : messages inventés, « Dr. Sall » / « Aminata »)
        return List.of();
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
        // APP-BE-19 : aucun lien parent → candidature persisté → liste vide (avant : dossier « Aminata Diallo » inventé)
        return List.of();
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
    @Transactional(readOnly = true)
    public PaymentDto getParentPayments(String parentId) {
        logger.info("Récupération des paiements pour le parent: {}", parentId);
        
        // APP-BE-18 : aucune source réelle de paiements n'est branchée (pas de PaymentRepository).
        // On ne renvoie JAMAIS de transaction fictive : réponse vide explicitement marquée « non_disponible ».
        // Quand une source réelle existera, renvoyer ses échéances avec statut PaymentDto.STATUT_DISPONIBLE.
        return PaymentDto.nonDisponible();
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
        
        // APP-BE-19 : nom réel du professeur choisi (avant : « Dr. Sall » pour tout identifiant)
        String profNom = nomProfesseur(request.professeurId());
            
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
        // APP-SEC-14 : uniquement les rendez-vous du parent connecté.
        // Avant : liste vide → findAll() (rendez-vous de TOUTES les familles), puis création
        // en base d'un faux rendez-vous « Dr. Sall » si la table était vide.
        List<Appointment> list = appointmentRepositoryPort.findByParentId(parentId);

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
