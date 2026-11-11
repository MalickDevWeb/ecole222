package ecole_221.sn.ecole.modules.parent.application.service;

import ecole_221.sn.ecole.modules.professor.application.service.FeuilleAppelService;

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
    private final ecole_221.sn.ecole.modules.parent.domain.repository.CandidaturesParentPort candidaturesPort;

    @org.springframework.beans.factory.annotation.Autowired
    public ParentService(ParentRepositoryPort parentRepositoryPort,
                         AppointmentRepositoryPort appointmentRepositoryPort,
                         ecole_221.sn.ecole.modules.auth.domain.port.repository.UserRepositoryPort userRepositoryPort,
                         ecole_221.sn.ecole.modules.parent.domain.repository.CandidaturesParentPort candidaturesPort) {
        this.parentRepositoryPort = parentRepositoryPort;
        this.appointmentRepositoryPort = appointmentRepositoryPort;
        this.userRepositoryPort = userRepositoryPort;
        this.candidaturesPort = candidaturesPort;
    }

    /** Compatibilité (tests) : sans dossiers de candidature. */
    public ParentService(ParentRepositoryPort parentRepositoryPort,
                         AppointmentRepositoryPort appointmentRepositoryPort,
                         ecole_221.sn.ecole.modules.auth.domain.port.repository.UserRepositoryPort userRepositoryPort) {
        this(parentRepositoryPort, appointmentRepositoryPort, userRepositoryPort, null);
    }

    /** Compatibilité (tests) : sans annuaire des comptes, les enfants sont renvoyés sans nom. */
    public ParentService(ParentRepositoryPort parentRepositoryPort,
                         AppointmentRepositoryPort appointmentRepositoryPort) {
        this(parentRepositoryPort, appointmentRepositoryPort, null, null);
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
        // Même règle que la feuille d'appel : taux = (présences + retards) / séances notées (excusées comprises)
        var presences = parentRepositoryPort.findPresencesByEnfantId(enfantId).stream()
                .filter(p -> p.coursNom() != null && FeuilleAppelService.statutAffiche(p.statut()) != null).toList();
        Double taux = null;
        Integer absences = null;
        Integer retards = null;
        if (!presences.isEmpty()) {
            long presents = presences.stream().filter(p -> "PRESENT".equals(FeuilleAppelService.statutAffiche(p.statut()))).count();
            long retardsN = presences.stream().filter(p -> "RETARD".equals(FeuilleAppelService.statutAffiche(p.statut()))).count();
            long absN = presences.stream().filter(p -> "ABSENT".equals(FeuilleAppelService.statutAffiche(p.statut()))).count();
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
        // Statuts normalisés comme la feuille d'appel (LATE → RETARD, EXCUSED → EXCUSE) ; les auto-pointages
        // et statuts inconnus sont écartés (avant : « LATE » brut faisait planter la page Présences du parent).
        return parentRepositoryPort.findPresencesByEnfantId(enfantId).stream()
                .filter(p -> mois == null || mois.isBlank() || (p.date() != null && p.date().startsWith(mois)))
                .filter(p -> p.coursNom() != null && FeuilleAppelService.statutAffiche(p.statut()) != null)
                .map(p -> new PresenceDto(p.id(), p.date(), FeuilleAppelService.statutAffiche(p.statut()),
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
    public List<ApplicationDto> getParentApplications(String parentId) {
        logger.info("Récupération des candidatures pour le parent: {}", parentId);
        if (candidaturesPort == null) {
            return List.of();
        }
        // Vrais dossiers déposés par CE parent dans l'école courante (avant : liste vide ou dossier inventé)
        return candidaturesPort.listerParParent(parentId).stream().map(ParentService::versDto).toList();
    }

    @Override
    @Transactional
    public String enrollChild(String parentId, EnrollChildPayloadDto payload) {
        if (candidaturesPort == null) {
            throw new IllegalStateException("Le dépôt de candidature n'est pas disponible.");
        }
        if (payload == null) {
            throw new IllegalArgumentException("Le dossier de candidature est vide.");
        }
        String prenom = obligatoire(payload.prenom(), "Le prénom de l'enfant est obligatoire.");
        String nom = obligatoire(payload.nom(), "Le nom de l'enfant est obligatoire.");
        String email = obligatoire(payload.email(), "L'adresse e-mail est obligatoire.").toLowerCase(java.util.Locale.ROOT);
        if (!email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            throw new IllegalArgumentException("L'adresse e-mail « " + email + " » n'est pas valide.");
        }
        candidaturesPort.exigerOuverture();   // l'école doit accepter des candidatures aujourd'hui (403 sinon)
        String filiere = obligatoire(payload.filiere(), "La filière demandée est obligatoire.");
        String niveau = obligatoire(payload.niveau(), "Le niveau demandé est obligatoire.");
        if (candidaturesPort.emailDejaCandidat(email)) {
            // « email » + « déjà » : renvoyé en 409 par le gestionnaire d'erreurs global
            throw new IllegalArgumentException("Une candidature existe déjà avec l'email " + email + " dans cette école.");
        }
        java.util.Map<String, String> complement = new java.util.LinkedHashMap<>();
        complement.put("niveau", niveau);
        complement.put("dateNaissance", vide(payload.dateNaissance()));
        complement.put("lieuNaissance", vide(payload.lieuNaissance()));
        complement.put("nationalite", vide(payload.nationalite()));
        complement.put("sexe", vide(payload.sexe()));
        complement.put("adresse", vide(payload.adresse()));
        complement.put("diplomePrecedent", vide(payload.diplomePrecedent()));
        complement.put("etablissementPrecedent", vide(payload.etablissementPrecedent()));
        complement.put("anneeBac", vide(payload.anneeBac()));
        complement.put("mentionBac", vide(payload.mentionBac()));
        complement.put("deposePar", "parent");
        String dossierId = candidaturesPort.deposer(parentId, prenom, nom, email, vide(payload.telephone()),
                filiere + " — " + niveau, complement);
        logger.info("Candidature {} déposée par le parent {}", dossierId, parentId);
        return dossierId;
    }

    private static String obligatoire(String valeur, String message) {
        if (valeur == null || valeur.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return valeur.trim();
    }

    private static String vide(String valeur) {
        return valeur == null || valeur.isBlank() ? null : valeur.trim();
    }

    /** Statut d'admission → vocabulaire de l'espace parent ; progression selon l'étape réelle du dossier. */
    private static ApplicationDto versDto(ecole_221.sn.ecole.modules.parent.domain.model.CandidatureParent c) {
        String statut = switch (c.statut() == null ? "" : c.statut()) {
            case "ACCEPTED", "ACCEPTE", "FINALISE" -> "ACCEPTE";
            case "REJECTED", "REFUSE" -> "REFUSE";
            default -> "EN_ATTENTE";
        };
        int progression = switch (c.etape() == null ? "" : c.etape()) {
            case "docs" -> 40;
            case "interview" -> 70;
            case "admitted", "rejected" -> 100;
            default -> 15;
        };
        if (!"EN_ATTENTE".equals(statut)) {
            progression = 100;
        }
        String depot = c.depot() == null ? null : c.depot().toLocalDate().toString();
        return new ApplicationDto(c.id(), c.enfant(), c.filiere(), c.niveau(), depot, statut, progression,
                c.etape(), c.commentaire());
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
    public ParentProfileDto getParentProfile(String parentId) {
        // Coordonnées enregistrées (table parents) ; à défaut, l'email du compte et rien d'inventé.
        var fiche = parentRepositoryPort.findParentByUserId(parentId);
        String emailCompte = emailDuCompte(parentId);
        return fiche.map(p -> new ParentProfileDto(emailCompte != null ? emailCompte : p.getEmail(),
                        p.getTelephone(), p.getAdresse(), p.getProfession()))
                .orElse(new ParentProfileDto(emailCompte, null, null, null));
    }

    @Override
    @Transactional
    public ParentProfileDto updateParentProfile(String parentId, ParentProfileDto profileDto) {
        logger.info("Mise à jour du profil du parent: {}", parentId);
        // Avant : renvoyait le profil reçu sans rien enregistrer (faux succès, APP-TL-13).
        if (profileDto == null) {
            throw new IllegalArgumentException("Aucune information de profil reçue.");
        }
        String telephone = facultatif(profileDto.telephone());
        if (telephone != null && !telephone.matches("^\\+?[0-9][0-9 .-]{6,18}[0-9]$")) {
            throw new IllegalArgumentException("Le numéro de téléphone « " + telephone
                    + " » n'est pas valide : chiffres uniquement, avec indicatif facultatif (ex. +221 77 123 45 67).");
        }
        String adresse = facultatif(profileDto.adresse());
        String profession = facultatif(profileDto.profession());
        if (adresse != null && adresse.length() > 255) {
            throw new IllegalArgumentException("L'adresse est trop longue (255 caractères au plus).");
        }
        if (profession != null && profession.length() > 255) {
            throw new IllegalArgumentException("La profession est trop longue (255 caractères au plus).");
        }
        var compte = userRepositoryPort == null ? Optional.<ecole_221.sn.ecole.modules.auth.domain.model.User>empty()
                : userRepositoryPort.findById(parentId);
        String email = compte.map(u -> u.getEmail()).orElse(null);   // l'email de connexion ne se change pas ici
        Parent fiche = parentRepositoryPort.findParentByUserId(parentId)
                .map(p -> Parent.reconstitute(p.getId(), p.getUserId(), p.getNom(), p.getPrenom(),
                        email != null ? email : p.getEmail(), telephone, adresse, profession,
                        p.getCreatedAt(), LocalDateTime.now()))
                .orElseGet(() -> Parent.create(parentId, compte.map(u -> u.getNom()).orElse(null),
                        compte.map(u -> u.getPrenom()).orElse(null), email, telephone, adresse, profession));
        Parent enregistre = parentRepositoryPort.saveParent(fiche);
        return new ParentProfileDto(enregistre.getEmail(), enregistre.getTelephone(),
                enregistre.getAdresse(), enregistre.getProfession());
    }

    private String emailDuCompte(String userId) {
        if (userRepositoryPort == null) return null;
        return userRepositoryPort.findById(userId).map(u -> u.getEmail()).orElse(null);
    }

    private static String facultatif(String valeur) {
        return valeur == null || valeur.isBlank() ? null : valeur.trim();
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
