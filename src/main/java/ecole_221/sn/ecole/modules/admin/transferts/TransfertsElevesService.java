package ecole_221.sn.ecole.modules.admin.transferts;

import com.fasterxml.jackson.databind.ObjectMapper;
import ecole_221.sn.ecole.modules.auth.infrastructure.persistence.UserJpaRepository;
import ecole_221.sn.ecole.modules.platform.organization.application.StudentTransferApplicationService;
import ecole_221.sn.ecole.modules.platform.organization.domain.model.Organization;
import ecole_221.sn.ecole.modules.platform.organization.domain.model.OrganizationUser;
import ecole_221.sn.ecole.modules.platform.organization.domain.port.repository.OrganizationRepositoryPort;
import ecole_221.sn.ecole.modules.platform.organization.domain.port.repository.OrganizationUserRepositoryPort;
import ecole_221.sn.ecole.shared.infrastructure.tenant.TenantContext;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Transfert d'un élève entre deux écoles, comme dans la vraie vie : l'école d'origine fait la demande,
 * l'école d'accueil l'accepte ou la refuse ; à l'acceptation, l'élève change d'école et l'école d'accueil reçoit une
 * copie figée de son dossier.
 */
@Service
public class TransfertsElevesService {

    static final String EN_ATTENTE = "EN_ATTENTE";
    static final String ACCEPTEE = "ACCEPTEE";
    static final String REFUSEE = "REFUSEE";
    static final String ANNULEE = "ANNULEE";
    static final int TEXTE_MAX = 1000;

    private final DemandeTransfertJpaRepository demandes;
    private final OrganizationRepositoryPort ecoles;
    private final OrganizationUserRepositoryPort appartenances;
    private final UserJpaRepository comptes;
    private final StudentTransferApplicationService transfert;
    private final ObjectMapper json = new ObjectMapper();

    public TransfertsElevesService(DemandeTransfertJpaRepository demandes, OrganizationRepositoryPort ecoles,
                                   OrganizationUserRepositoryPort appartenances, UserJpaRepository comptes,
                                   StudentTransferApplicationService transfert) {
        this.demandes = demandes;
        this.ecoles = ecoles;
        this.appartenances = appartenances;
        this.comptes = comptes;
        this.transfert = transfert;
    }

    static String ecoleCourante() {
        return TenantContext.getCurrentOrganizationId().orElseThrow(() ->
                new ResponseStatusException(HttpStatus.BAD_REQUEST, "École inconnue : précisez l'école (X-Tenant-Slug)."));
    }

    private static String texte(String brut, String libelle) {
        if (brut == null || brut.isBlank()) return null;
        String t = ecole_221.sn.ecole.partage.TexteNormalise.normaliser(brut);
        if (t.length() > TEXTE_MAX) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, libelle + " ne doit pas dépasser " + TEXTE_MAX + " caractères.");
        }
        return t;
    }

    /** Écoles actives vers lesquelles transférer (recherche par nom ou identifiant d'URL, 2 caractères au moins). */
    public List<Map<String, Object>> ecolesDestinataires(String recherche) {
        String ici = ecoleCourante();
        String q = recherche == null ? "" : recherche.trim().toLowerCase(Locale.ROOT);
        if (q.length() < 2) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tapez au moins 2 caractères du nom de l'école.");
        }
        return ecoles.findAll().stream()
                .filter(Organization::isActive)
                .filter(o -> !o.getId().equals(ici))
                .filter(o -> (o.getName() != null && o.getName().toLowerCase(Locale.ROOT).contains(q))
                        || (o.getSlug() != null && o.getSlug().toLowerCase(Locale.ROOT).contains(q)))
                .limit(20)
                .map(o -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", o.getId());
                    m.put("nom", o.getName());
                    m.put("ville", o.getCity());
                    m.put("slug", o.getSlug());
                    return m;
                }).toList();
    }

    private boolean eleveActifDans(String eleveId, String ecole) {
        return appartenances.findByUserIdAndOrganizationId(eleveId, ecole)
                .filter(OrganizationUser::isActive)
                .filter(m -> "ETUDIANT".equalsIgnoreCase(m.getRole()) || "STUDENT".equalsIgnoreCase(m.getRole()))
                .isPresent();
    }

    @Transactional
    public Map<String, Object> demander(String eleveId, String destinationId, String motifBrut, String auteur) {
        String source = ecoleCourante();
        if (eleveId == null || eleveId.isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choisissez l'élève à transférer.");
        if (destinationId == null || destinationId.isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choisissez l'école d'accueil.");
        if (!eleveActifDans(eleveId, source)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Élève introuvable parmi les élèves actifs de votre école.");
        }
        Organization destination = ecoles.findById(destinationId).filter(Organization::isActive).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "École d'accueil introuvable ou inactive."));
        if (destination.getId().equals(source)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "L'école d'accueil doit être différente de la vôtre.");
        }
        if (eleveActifDans(eleveId, destination.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cet élève est déjà inscrit dans l'école d'accueil.");
        }
        if (demandes.existsByEleveIdAndStatut(eleveId, EN_ATTENTE)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Une demande de transfert est déjà en attente pour cet élève.");
        }
        DemandeTransfertJpaEntity d = new DemandeTransfertJpaEntity("trf-" + UUID.randomUUID(), eleveId, source,
                destination.getId(), texte(motifBrut, "Le motif"), auteur == null ? "inconnu" : auteur);
        try {
            demandes.saveAndFlush(d);
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            // double clic : l'index unique « une seule demande en attente par élève » a tranché
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Une demande de transfert est déjà en attente pour cet élève.");
        }
        return vue(d);
    }

    /** Demandes envoyées et reçues par l'école courante. */
    public Map<String, Object> liste() {
        String ici = ecoleCourante();
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("sortants", demandes.findByEcoleSourceIdOrderByCreeLeDesc(ici).stream().map(this::vue).toList());
        r.put("entrants", demandes.findByEcoleDestinationIdOrderByCreeLeDesc(ici).stream().map(this::vue).toList());
        return r;
    }

    /** Demande reçue par l'école courante et encore en attente (sinon 404 / 409). */
    public DemandeTransfertJpaEntity demandeRecueEnAttente(String id) {
        DemandeTransfertJpaEntity d = demandes.findById(id)
                .filter(x -> x.getEcoleDestinationId().equals(ecoleCourante()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Demande de transfert introuvable."));
        if (!EN_ATTENTE.equals(d.getStatut())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cette demande a déjà été traitée (" + d.getStatut() + ").");
        }
        return d;
    }

    public Organization ecole(String id) {
        return ecoles.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "École introuvable."));
    }

    /**
     * École d'origine d'un transfert à accepter. Archivée (données retirées de la base, ou en cours de retrait) :
     * refus explicite (étude d'archivage, risque R15 : le dossier serait lu dans des tables vides et l'élève
     * arriverait avec un dossier vide, sans erreur).
     */
    public Organization ecoleOrigine(String id) {
        Organization source = ecole(id);
        if (ecole_221.sn.ecole.modules.platform.organization.domain.valueobject.OrganizationStatus.ARCHIVED.equals(source.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Transfert impossible : l'école d'origine est archivée.");
        }
        return source;
    }

    @Transactional
    public Map<String, Object> accepter(String id, Map<String, Object> dossier, String auteur) {
        DemandeTransfertJpaEntity d = demandeRecueEnAttente(id);
        ecoleOrigine(d.getEcoleSourceId());
        try {
            transfert.transferStudent(d.getEleveId(), d.getEcoleSourceId(), d.getEcoleDestinationId(),
                    d.getMotif() == null ? "Transfert accepté par l'école d'accueil" : d.getMotif());
        } catch (IllegalArgumentException | IllegalStateException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Transfert impossible : " + e.getMessage());
        }
        try {
            d.joindreDossier(json.writeValueAsString(dossier));
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new IllegalStateException("Dossier de transfert illisible", e);
        }
        d.decider(ACCEPTEE, auteur, null);
        demandes.save(d);
        return vue(d);
    }

    @Transactional
    public Map<String, Object> refuser(String id, String motifBrut, String auteur) {
        DemandeTransfertJpaEntity d = demandeRecueEnAttente(id);
        String motif = texte(motifBrut, "Le motif du refus");
        if (motif == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Indiquez le motif du refus : il sera transmis à l'école d'origine.");
        d.decider(REFUSEE, auteur, motif);
        demandes.save(d);
        return vue(d);
    }

    @Transactional
    public Map<String, Object> annuler(String id, String auteur) {
        DemandeTransfertJpaEntity d = demandes.findById(id)
                .filter(x -> x.getEcoleSourceId().equals(ecoleCourante()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Demande de transfert introuvable."));
        if (!EN_ATTENTE.equals(d.getStatut())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Seule une demande en attente peut être annulée.");
        }
        d.decider(ANNULEE, auteur, null);
        demandes.save(d);
        return vue(d);
    }

    /** Dossier remis à l'école d'accueil (lisible par les deux écoles concernées, après acceptation). */
    @SuppressWarnings("unchecked")
    public Map<String, Object> dossier(String id) {
        String ici = ecoleCourante();
        DemandeTransfertJpaEntity d = demandes.findById(id)
                .filter(x -> x.getEcoleDestinationId().equals(ici) || x.getEcoleSourceId().equals(ici))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Demande de transfert introuvable."));
        if (!ACCEPTEE.equals(d.getStatut()) || d.getDossier() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Le dossier n'est remis qu'après acceptation du transfert.");
        }
        try {
            return json.readValue(d.getDossier(), Map.class);
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new IllegalStateException("Dossier de transfert illisible", e);
        }
    }

    private Map<String, Object> vue(DemandeTransfertJpaEntity d) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", d.getId());
        Map<String, Object> eleve = new LinkedHashMap<>();
        eleve.put("id", d.getEleveId());
        comptes.findById(d.getEleveId()).ifPresent(u -> {
            eleve.put("nom", ((u.getPrenom() == null ? "" : u.getPrenom() + " ") + (u.getNom() == null ? "" : u.getNom())).trim());
            eleve.put("matricule", u.getMatricule());
        });
        m.put("eleve", eleve);
        m.put("ecoleSource", ecoleCourte(d.getEcoleSourceId()));
        m.put("ecoleDestination", ecoleCourte(d.getEcoleDestinationId()));
        m.put("motif", d.getMotif());
        m.put("statut", d.getStatut());
        m.put("creeLe", d.getCreeLe() == null ? null : d.getCreeLe().toString());
        m.put("decideLe", d.getDecideLe() == null ? null : d.getDecideLe().toString());
        m.put("motifDecision", d.getMotifDecision());
        m.put("dossierDisponible", ACCEPTEE.equals(d.getStatut()) && d.getDossier() != null);
        return m;
    }

    private Map<String, Object> ecoleCourte(String id) {
        Map<String, Object> e = new LinkedHashMap<>();
        e.put("id", id);
        ecoles.findById(id).ifPresent(o -> e.put("nom", o.getName()));
        return e;
    }
}
