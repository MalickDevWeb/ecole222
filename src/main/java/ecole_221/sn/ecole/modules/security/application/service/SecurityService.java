package ecole_221.sn.ecole.modules.security.application.service;

import ecole_221.sn.ecole.modules.admin.domain.port.repository.AdminRepositoryPort;
import ecole_221.sn.ecole.modules.admin.domain.valueobject.StatutFrais;
import ecole_221.sn.ecole.modules.security.application.port.dto.VigilProfileDto;
import ecole_221.sn.ecole.modules.security.application.port.usecase.SecurityUseCase;
import ecole_221.sn.ecole.modules.security.domain.model.VigilProfile;
import ecole_221.sn.ecole.modules.security.domain.port.repository.SecurityRepositoryPort;
import ecole_221.sn.ecole.modules.security.infrastructure.persistence.VigilProfileJpaRepository;
import ecole_221.sn.ecole.modules.security.infrastructure.persistence.VigilProfileJpaEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class SecurityService implements SecurityUseCase {

    private static final Logger logger = LoggerFactory.getLogger(SecurityService.class);

    private final AdminRepositoryPort adminRepository;
    private final SecurityRepositoryPort securityRepositoryPort;
    private final VigilProfileJpaRepository vigilProfileRepository;
    private final Map<String, List<Map<String, Object>>> passagesParEcole = new java.util.concurrent.ConcurrentHashMap<>();

    private final ecole_221.sn.ecole.modules.auth.domain.port.repository.UserRepositoryPort comptes;
    private final ecole_221.sn.ecole.modules.platform.organization.domain.port.repository.OrganizationUserRepositoryPort appartenances;

    @org.springframework.beans.factory.annotation.Autowired
    public SecurityService(AdminRepositoryPort adminRepository,
                          SecurityRepositoryPort securityRepositoryPort,
                          VigilProfileJpaRepository vigilProfileRepository,
                          ecole_221.sn.ecole.modules.auth.domain.port.repository.UserRepositoryPort comptes,
                          ecole_221.sn.ecole.modules.platform.organization.domain.port.repository.OrganizationUserRepositoryPort appartenances) {
        this.adminRepository = adminRepository;
        this.securityRepositoryPort = securityRepositoryPort;
        this.vigilProfileRepository = vigilProfileRepository;
        this.comptes = comptes;
        this.appartenances = appartenances;
        // APP-SEC-18 : plus de passage « Mamadou Sall » inventé au démarrage
    }

    /** Vérification des badges QR dynamiques (2026-11-13). Clé aléatoire si le composant n'est pas injecté (tests). */
    private ecole_221.sn.ecole.partage.JetonBadge jetonBadge = ecole_221.sn.ecole.partage.JetonBadge.aleatoire();

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    public void setJetonBadge(ecole_221.sn.ecole.partage.JetonBadge jetonBadge) {
        if (jetonBadge != null) this.jetonBadge = jetonBadge;
    }

    /** Journal des passages de l'école courante (APP-SEC-18 : avant, une seule liste pour toutes les écoles). */
    private List<Map<String, Object>> journalEcole() {
        String ecole = ecole_221.sn.ecole.shared.infrastructure.tenant.TenantContext.getCurrentOrganizationId().orElse("sans-ecole");
        return passagesParEcole.computeIfAbsent(ecole, k -> new CopyOnWriteArrayList<>());
    }

    // ==================== GESTION DES PROFILS DE VIGILANCE ====================

    @Override
    public List<VigilProfileDto> getAllVigilProfiles() {
        logger.info("Récupération de tous les profils de vigilance");
        return securityRepositoryPort.findAllVigilProfiles().stream()
            .map(this::toVigilProfileDto)
            .collect(Collectors.toList());
    }

    @Override
    public VigilProfileDto getVigilProfileById(String id) {
        logger.info("Récupération du profil de vigilance: {}", id);
        return securityRepositoryPort.findVigilProfileById(id)
            .map(this::toVigilProfileDto)
            .orElse(null);
    }

    @Override
    public List<VigilProfileDto> getVigilProfilesByUserId(String userId) {
        logger.info("Récupération des profils de vigilance par utilisateur: {}", userId);
        return securityRepositoryPort.findVigilProfilesByUserId(userId).stream()
            .map(this::toVigilProfileDto)
            .collect(Collectors.toList());
    }

    @Override
    public List<VigilProfileDto> getVigilProfilesByRole(String role) {
        logger.info("Récupération des profils de vigilance par rôle: {}", role);
        return securityRepositoryPort.findVigilProfilesByRole(role).stream()
            .map(this::toVigilProfileDto)
            .collect(Collectors.toList());
    }

    @Override
    public VigilProfileDto createVigilProfile(VigilProfileDto vigilProfileDto) {
        logger.info("Création du profil de vigilance: {}", vigilProfileDto.name());
        VigilProfile vigilProfile = toVigilProfile(vigilProfileDto);
        VigilProfile savedProfile = securityRepositoryPort.saveVigilProfile(vigilProfile);
        return toVigilProfileDto(savedProfile);
    }

    @Override
    public VigilProfileDto updateVigilProfile(String id, VigilProfileDto vigilProfileDto) {
        logger.info("Mise à jour du profil de vigilance: {}", id);
        // APP-QA-04 : profil inexistant → 404 (avant : créé, nommé « Inconnu ») ; nom obligatoire, champs bornés
        VigilProfile actuel = securityRepositoryPort.findVigilProfileById(id)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Profil de vigile introuvable."));
        if (vigilProfileDto == null || vigilProfileDto.name() == null || vigilProfileDto.name().isBlank()) {
            throw ecole_221.sn.ecole.partage.ChampsRequete.refus("Le nom est obligatoire.");
        }
        for (String[] c : new String[][]{{vigilProfileDto.name(), "Le nom", "200"}, {vigilProfileDto.role(), "Le rôle", "50"},
                {vigilProfileDto.status(), "Le statut", "30"}}) {
            if (c[0] != null && c[0].strip().length() > Integer.parseInt(c[2])) {
                throw ecole_221.sn.ecole.partage.ChampsRequete.refus(c[1] + " ne doit pas dépasser " + c[2] + " caractères.");
            }
        }
        // Reconstruction avec l'ID existant via Factory
        VigilProfile updatedProfile = securityRepositoryPort.saveVigilProfile(
            ecole_221.sn.ecole.modules.security.domain.model.VigilProfileFactory.reconstitute(
                id,
                vigilProfileDto.name(),
                vigilProfileDto.role(),
                actuel.getUserId(),   // rattachement au compte : jamais modifié par ce formulaire
                vigilProfileDto.status(),
                vigilProfileDto.createdAt()
            )
        );
        return toVigilProfileDto(updatedProfile);
    }

    @Override
    public void deleteVigilProfile(String id) {
        logger.info("Suppression du profil de vigilance: {}", id);
        securityRepositoryPort.deleteVigilProfile(id);
    }

    // ==================== OPÉRATIONS DE SÉCURITÉ ====================

    @Override
    public Map<String, Object> getCheckpointStatus() {
        Map<String, String> assignment = adminRepository.getGateAssignment();
        Map<String, Object> status = new HashMap<>();
        status.put("gate", assignment.getOrDefault("gate", "Non configuré"));   // plus de portail inventé
        status.put("guard", assignment.getOrDefault("guard", "Non assigné"));   // plus de gardien inventé
        status.put("totalScansToday", journalEcole().size());
        status.put("status", "ACTIVE");
        return status;
    }

    @Override
    public Map<String, Object> processBadgeScan(String badgeId) {
        Decision d = decider(badgeId, null);
        return journaliser(d.nom(), d.autorise(), d.statutFrais(), d.message());
    }

    /** Décision d'accès pour un contenu scanné ; fraîcheur du QR jugée à la minute {@code minuteDuScan} (null = maintenant). */
    Decision decider(String badgeId, Long minuteDuScan) {
        // APP-SEC-18 : le badge est rattaché à un compte RÉEL (identifiant ou matricule), membre ACTIF de
        // l'école courante. Avant : tout badge était attribué à l'élève fictif « Mamadou Sall » (ou
        // « Awa Diop ») et l'accès décidé d'après sa situation — un badge inconnu pouvait être accepté.
        String lu = badgeId == null ? "" : badgeId.trim();
        // 2026-11-13 : badge QR dynamique (STU_SECURE:…) vérifié ICI pour toutes les routes de scan ; un élève n'entre
        // plus avec son seul matricule (tapé, photocopié ou deviné) : seul le QR signé par le serveur, valable
        // ± 1 minute, est accepté pour lui. Les badges du personnel restent des identifiants simples.
        var dynamique = jetonBadge.lire(lu, minuteDuScan);
        boolean jetonVerifie = dynamique.map(ecole_221.sn.ecole.partage.JetonBadge.Lecture::valide).orElse(false);
        if (dynamique.isPresent() && !jetonVerifie) {
            return new Decision("Inconnu", false, null, "Accès refusé : QR expiré ou invalide. Faites actualiser le badge dans l'application.");
        }
        String brut = dynamique.map(ecole_221.sn.ecole.partage.JetonBadge.Lecture::matricule).orElse(lu);
        String sansPrefixe = brut.startsWith("badge-") ? brut.substring(6) : brut;
        Optional<ecole_221.sn.ecole.modules.auth.domain.model.User> titulaire = comptes.findById(brut)
                .or(() -> comptes.findById(sansPrefixe))
                .or(() -> comptes.findByMatricule(brut))
                .or(() -> comptes.findByMatricule(sansPrefixe));
        String ecole = ecole_221.sn.ecole.shared.infrastructure.tenant.TenantContext.getCurrentOrganizationId().orElse(null);

        boolean accessGranted;
        String nom;
        String statutFrais = null;
        String msg;
        if (titulaire.isEmpty()) {
            accessGranted = false;
            nom = "Inconnu";
            msg = "Accès refusé : badge non reconnu.";
        } else {
            var u = titulaire.get();
            nom = ((u.getPrenom() == null ? "" : u.getPrenom() + " ") + (u.getNom() == null ? "" : u.getNom())).trim();
            boolean membre = ecole != null && appartenances.findByUserIdAndOrganizationId(u.getId(), ecole)
                    .map(m -> m.isActive() && !m.isExpired()).orElse(false);
            if (!membre) {
                accessGranted = false;
                msg = "Accès refusé : ce badge n'appartient pas à cet établissement.";
            } else if (u.getRole() != null && "ETUDIANT".equals(u.getRole().value()) && !jetonVerifie) {
                accessGranted = false;
                msg = "Accès refusé : présentez le badge QR de l'application (le matricule seul ne suffit pas).";
            } else if (u.getRole() != null && "ETUDIANT".equals(u.getRole().value())) {
                StatutFrais frais = adminRepository.getStudentStatus(u.getId());
                if (frais == null) {
                    // statut jamais saisi : on ne présume ni « En Règle » ni « En Retard », on le signale
                    statutFrais = "Non renseigné";
                    accessGranted = true;
                    msg = "Accès autorisé. Statut des frais non renseigné par l'administration.";
                } else {
                    statutFrais = frais.getDisplayName();
                    accessGranted = frais == StatutFrais.EN_REGLE;
                    msg = accessGranted ? "Accès autorisé."
                            : "Accès refusé. Veuillez régulariser votre situation financière auprès de la comptabilité.";
                }
            } else {
                accessGranted = true;   // personnel de l'école
                msg = "Accès autorisé (personnel).";
            }
        }

        return new Decision(nom, accessGranted, statutFrais, msg);
    }

    /** Résultat d'une décision d'accès (avant journalisation). */
    record Decision(String nom, boolean autorise, String statutFrais, String message) {
    }

    /**
     * Liste d'accès pour la décision HORS LIGNE du terminal du vigile (EF-10, 2026-11-14) : clé publique des badges
     * (vérification de la signature sans pouvoir en fabriquer), élèves actifs de l'école avec le statut de leurs frais,
     * et membres du personnel. Valable 24 h ; le terminal la retélécharge dès que le réseau revient.
     */
    @Override
    public Map<String, Object> listeAccesHorsLigne() {
        String ecole = ecole_221.sn.ecole.shared.infrastructure.tenant.TenantContext.getCurrentOrganizationId()
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.BAD_REQUEST, "École inconnue : précisez l'école (X-Tenant-Slug)."));
        List<Map<String, Object>> eleves = new java.util.ArrayList<>();
        List<Map<String, Object>> personnel = new java.util.ArrayList<>();
        for (var m : appartenances.findByOrganizationId(ecole)) {
            if (!m.isActive() || m.isExpired()) continue;
            var u = comptes.findById(m.getUserId()).orElse(null);
            if (u == null) continue;
            String nom = ((u.getPrenom() == null ? "" : u.getPrenom() + " ") + (u.getNom() == null ? "" : u.getNom())).trim();
            if ("ETUDIANT".equalsIgnoreCase(m.getRole())) {
                if (u.getMatricule() == null || u.getMatricule().isBlank()) continue;   // pas de badge sans matricule
                StatutFrais frais = adminRepository.getStudentStatus(u.getId());
                Map<String, Object> e = new HashMap<>();
                e.put("matricule", u.getMatricule());
                e.put("nom", nom);
                e.put("statutFrais", frais == null ? "Non renseigné" : frais.getDisplayName());
                e.put("autorise", frais == null || frais == StatutFrais.EN_REGLE);
                eleves.add(e);
            } else {
                Map<String, Object> p = new HashMap<>();
                p.put("id", u.getId());
                p.put("matricule", u.getMatricule());
                p.put("nom", nom);
                personnel.add(p);
            }
        }
        Map<String, Object> r = new HashMap<>();
        r.put("clePublique", jetonBadge.clePubliqueBase64());
        r.put("algorithme", "Ed25519");
        r.put("genereLe", java.time.Instant.now().toString());
        r.put("valideJusquA", java.time.Instant.now().plus(java.time.Duration.ofHours(24)).toString());
        r.put("eleves", eleves);
        r.put("personnel", personnel);
        return r;
    }

    /**
     * Scan effectué HORS LIGNE puis transmis au retour du réseau (EF-10) : le serveur refait le contrôle à la minute
     * du scan et inscrit au journal ce qui s'est réellement passé (la décision du terminal), en signalant toute
     * décision contestée.
     */
    @Override
    public Map<String, Object> enregistrerScanHorsLigne(String badgeId, java.time.Instant scanneLe, boolean autoriseParLeTerminal) {
        Decision d = decider(badgeId, scanneLe.toEpochMilli() / 60_000L);
        boolean conteste = d.autorise() != autoriseParLeTerminal;
        String msg = "Scan hors ligne du " + scanneLe + " : terminal « " + (autoriseParLeTerminal ? "Autorisé" : "Refusé")
                + " » ; contrôle du serveur : " + d.message() + (conteste ? " — DÉCISION CONTESTÉE" : "");
        Map<String, Object> log = journaliser(d.nom(), autoriseParLeTerminal, d.statutFrais(), msg);
        log.put("horsLigne", true);
        log.put("conteste", conteste);
        log.put("scanneLe", scanneLe.toString());
        return log;
    }

    private Map<String, Object> journaliser(String nom, boolean accessGranted, String statutFrais, String msg) {
        Map<String, Object> log = new HashMap<>();
        log.put("id", "scan-" + UUID.randomUUID().toString().substring(0, 8));
        log.put("studentName", nom);
        log.put("status", accessGranted ? "ENTREE" : "BLOQUE");
        log.put("timestamp", LocalDateTime.now().toString());
        log.put("statutFrais", statutFrais);
        log.put("accessGranted", accessGranted);
        log.put("message", msg);
        journalEcole().add(0, log);
        adminRepository.addRealtimeLog("Scannage de badge pour '" + nom + "' -> " + (accessGranted ? "AUTORISÉ" : "REFUSÉ"));
        return log;
    }

    @Override
    public List<Map<String, Object>> getVigilCheckIns() {
        return journalEcole();
    }

    @Override
    public Map<String, Object> getVigilProfile(String vigilId) {
        Optional<VigilProfileJpaEntity> profile = vigilProfileRepository.findById(vigilId);
        if (profile.isPresent()) {
            VigilProfileJpaEntity entity = profile.get();
            Map<String, Object> map = new HashMap<>();
            map.put("id", entity.getId());
            map.put("nom", entity.getNom());
            map.put("prenom", entity.getPrenom());
            map.put("badgeId", entity.getBadgeId());
            map.put("equipe", entity.getEquipe());
            map.put("derniereConnexion", entity.getDerniereConnexion());
            map.put("statut", entity.getStatut());
            map.put("zone", entity.getZone());
            return map;
        }
        
        // Profil non configuré : réponse explicite (avant : vigile « Mamadou Diallo » inventé)
        Map<String, Object> nonConfigure = new HashMap<>();
        nonConfigure.put("id", vigilId);
        nonConfigure.put("statut", "non_configuré");
        return nonConfigure;
    }

    // ==================== MAPPERS ====================

    private VigilProfileDto toVigilProfileDto(VigilProfile vigilProfile) {
        return new VigilProfileDto(
            vigilProfile.getId(),
            vigilProfile.getName(),
            vigilProfile.getRole(),
            vigilProfile.getUserId(),
            vigilProfile.getStatus(),
            vigilProfile.getCreatedAt()
        );
    }

    private VigilProfile toVigilProfile(VigilProfileDto dto) {
        return ecole_221.sn.ecole.modules.security.domain.model.VigilProfileFactory.reconstitute(
            dto.id(),
            dto.name(),
            dto.role(),
            dto.userId(),
            dto.status(),
            dto.createdAt()
        );
    }
}
