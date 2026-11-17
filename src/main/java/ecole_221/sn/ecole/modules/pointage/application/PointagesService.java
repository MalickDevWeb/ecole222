package ecole_221.sn.ecole.modules.pointage.application;

import ecole_221.sn.ecole.modules.auth.domain.valueobject.RoleUtilisateur;
import ecole_221.sn.ecole.modules.platform.organization.domain.model.OrganizationUser;
import ecole_221.sn.ecole.modules.platform.organization.domain.port.repository.OrganizationUserRepositoryPort;
import ecole_221.sn.ecole.modules.pointage.infrastructure.persistence.PointageJpaEntity;
import ecole_221.sn.ecole.modules.pointage.infrastructure.persistence.PointageJpaRepository;
import ecole_221.sn.ecole.modules.auth.domain.model.User;
import ecole_221.sn.ecole.partage.JetonBadge;
import ecole_221.sn.ecole.shared.infrastructure.tenant.TenantContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static ecole_221.sn.ecole.partage.ChampsRequete.refus;

/**
 * Pointage d'arrivée et de départ par le QR d'entrée du vigile.
 *
 * <ul>
 *   <li>Le terminal du vigile affiche le QR d'entrée de SON école, signé par le serveur et valable une minute
 *       (± 1) : {@link JetonBadge#contenuEntree(String, long)}.</li>
 *   <li>L'élève ou le professeur le scanne ; le serveur vérifie la signature, la fraîcheur, l'école et l'appartenance
 *       active du compte, puis enregistre une ARRIVÉE ou un DÉPART.</li>
 *   <li>Le serveur décide du sens : arrivée si aucun pointage aujourd'hui ou si le dernier est un départ, départ
 *       sinon. Un second scan dans les 2 minutes est refusé (double lecture accidentelle, pas un départ).</li>
 * </ul>
 * Limite connue : un QR photographié peut être transmis et scanné ailleurs pendant sa minute de validité.
 */
@Service
public class PointagesService {

    public static final String ARRIVEE = "ARRIVEE";
    public static final String DEPART = "DEPART";
    static final Duration DELAI_ENTRE_POINTAGES = Duration.ofMinutes(2);
    /** Heure du Sénégal (UTC, sans heure d'été) : le « jour » d'un pointage est celui de l'école. */
    static final ZoneId FUSEAU = ZoneId.of("Africa/Dakar");
    private static final DateTimeFormatter HEURE = DateTimeFormatter.ofPattern("HH:mm", Locale.FRENCH);

    private final PointageJpaRepository pointages;
    private final JetonBadge jetons;
    private final OrganizationUserRepositoryPort appartenances;
    private final Clock horloge;

    @Autowired
    public PointagesService(PointageJpaRepository pointages, JetonBadge jetons, OrganizationUserRepositoryPort appartenances) {
        this(pointages, jetons, appartenances, Clock.system(FUSEAU));
    }

    PointagesService(PointageJpaRepository pointages, JetonBadge jetons, OrganizationUserRepositoryPort appartenances, Clock horloge) {
        this.pointages = pointages;
        this.jetons = jetons;
        this.appartenances = appartenances;
        this.horloge = horloge;
    }

    /** QR d'entrée de l'école courante pour la minute en cours, et délai avant de le renouveler. */
    public Map<String, Object> qrEntree() {
        String ecole = ecoleCourante();
        long maintenant = horloge.millis();
        long minute = maintenant / 60_000L;
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("qrPayload", jetons.contenuEntree(ecole, minute));
        r.put("renouvelerDansSecondes", Math.max(1, 60 - (maintenant / 1000L) % 60));
        return r;
    }

    @Transactional
    public Map<String, Object> pointer(String qr) {
        String ecole = ecoleCourante();
        User compte = compteConnecte();
        String moi = compte.getId();
        if (qr == null || qr.isBlank()) {
            throw refus("Aucun QR scanné.");
        }
        JetonBadge.LectureEntree lu = jetons.lireEntree(qr.trim()).orElseThrow(() ->
                refus("Ce QR n'est pas le QR d'entrée de l'école : scannez le QR affiché par le vigile."));
        if (!lu.valide()) {
            throw refus("QR d'entrée expiré ou invalide : scannez le QR affiché en ce moment par le vigile.");
        }
        if (!ecole.equals(lu.ecoleId())) {
            throw refus("Ce QR d'entrée appartient à une autre école.");
        }
        OrganizationUser membre = appartenances.findByUserIdAndOrganizationId(moi, ecole)
                .filter(m -> m.isActive() && !m.isExpired())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Vous n'êtes pas membre actif de cette école."));

        LocalDateTime maintenant = LocalDateTime.now(horloge);
        Optional<PointageJpaEntity> dernier = pointages.findFirstByUtilisateurIdOrderByHorodatageDesc(moi);
        if (dernier.isPresent() && Duration.between(dernier.get().getHorodatage(), maintenant).compareTo(DELAI_ENTRE_POINTAGES) < 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, libelle(dernier.get().getType()) + " déjà enregistrée à "
                    + HEURE.format(dernier.get().getHorodatage()) + ". Attendez 2 minutes avant de pointer de nouveau.");
        }

        PointageJpaEntity p = new PointageJpaEntity();
        p.setId("pt-" + UUID.randomUUID());
        p.setOrganizationId(ecole);
        p.setUtilisateurId(moi);
        p.setNom(nomComplet(compte));
        p.setRole(roleNormalise(membre.getRole()));
        p.setType(prochainType(dernier, maintenant));
        p.setHorodatage(maintenant);
        pointages.save(p);

        Map<String, Object> r = vue(p);
        r.put("message", libelle(p.getType()) + " enregistrée à " + HEURE.format(maintenant) + ".");
        r.put("prochainType", ARRIVEE.equals(p.getType()) ? DEPART : ARRIVEE);
        return r;
    }

    /** Derniers pointages du compte connecté et sens du prochain. */
    @Transactional(readOnly = true)
    public Map<String, Object> mesPointages() {
        ecoleCourante();
        String moi = compteConnecte().getId();
        List<PointageJpaEntity> liste = pointages.findTop60ByUtilisateurIdOrderByHorodatageDesc(moi);
        LocalDateTime maintenant = LocalDateTime.now(horloge);
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("pointages", liste.stream().map(this::vue).toList());
        r.put("aujourdhui", liste.stream().filter(p -> p.getHorodatage().toLocalDate().equals(maintenant.toLocalDate())).count());
        r.put("prochainType", prochainType(liste.stream().findFirst(), maintenant));
        return r;
    }

    /**
     * Pointages d'une journée de l'école courante, regroupés par personne (administration, onglet « Pointages » de
     * Sécurité) : première arrivée, dernier départ, présence actuelle (dernier pointage = arrivée), détail des passages.
     *
     * @param date jour au format AAAA-MM-JJ (aujourd'hui à Dakar si absent)
     * @param role ETUDIANT, PROFESSEUR, ou absent pour tous
     * @param de   début de la plage horaire HH:MM (facultatif) : seules les personnes ayant un passage dans la plage
     * @param a    fin de la plage horaire HH:MM (facultatif) ; arrivée, départ et présence restent ceux de la journée
     */
    @Transactional(readOnly = true)
    public Map<String, Object> pointagesDuJour(String date, String role, String de, String a) {
        java.time.LocalTime heureDe = heure(de, "L'heure de début du filtre", java.time.LocalTime.MIN);
        java.time.LocalTime heureA = heure(a, "L'heure de fin du filtre", java.time.LocalTime.MAX);
        if (heureA.isBefore(heureDe)) {
            throw refus("Plage horaire invalide : l'heure de fin est avant l'heure de début.");
        }
        ecoleCourante();
        java.time.LocalDate jour;
        try {
            jour = date == null || date.isBlank() ? LocalDateTime.now(horloge).toLocalDate() : java.time.LocalDate.parse(date.trim());
        } catch (java.time.format.DateTimeParseException e) {
            throw refus("Date invalide : utilisez le format AAAA-MM-JJ.");
        }
        String filtre = role == null || role.isBlank() ? null : roleNormalise(role);
        if (filtre != null && !List.of("ETUDIANT", "PROFESSEUR").contains(filtre)) {
            throw refus("Rôle invalide : « ETUDIANT » ou « PROFESSEUR ».");
        }
        List<PointageJpaEntity> liste = pointages.findByHorodatageBetweenOrderByHorodatageAsc(
                jour.atStartOfDay(), jour.plusDays(1).atStartOfDay().minusNanos(1));
        Map<String, List<PointageJpaEntity>> parPersonne = new LinkedHashMap<>();
        for (PointageJpaEntity p : liste) {
            if (filtre == null || filtre.equals(p.getRole())) {
                parPersonne.computeIfAbsent(p.getUtilisateurId(), k -> new java.util.ArrayList<>()).add(p);
            }
        }
        List<Map<String, Object>> personnes = parPersonne.values().stream()
                .filter(siens -> siens.stream().map(p -> p.getHorodatage().toLocalTime())
                        .anyMatch(t -> !t.isBefore(heureDe) && !t.isAfter(heureA)))
                .map(siens -> {
            PointageJpaEntity premier = siens.get(0);
            PointageJpaEntity dernier = siens.get(siens.size() - 1);
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("utilisateurId", premier.getUtilisateurId());
            m.put("nom", siens.stream().map(PointageJpaEntity::getNom).filter(n -> n != null && !n.isBlank()).reduce((x, y) -> y).orElse(null));
            m.put("role", dernier.getRole());
            m.put("arrivee", siens.stream().filter(p -> ARRIVEE.equals(p.getType())).findFirst().map(p -> HEURE.format(p.getHorodatage())).orElse(null));
            m.put("depart", DEPART.equals(dernier.getType()) ? HEURE.format(dernier.getHorodatage()) : null);
            m.put("present", ARRIVEE.equals(dernier.getType()));
            m.put("passages", siens.stream().map(p -> Map.of("type", p.getType(), "heure", HEURE.format(p.getHorodatage()))).toList());
            return m;
        }).sorted(java.util.Comparator.comparing((Map<String, Object> m) -> java.util.Objects.toString(m.get("nom"), ""), String.CASE_INSENSITIVE_ORDER)).toList();
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("date", jour.toString());
        r.put("de", de == null || de.isBlank() ? null : HEURE.format(heureDe));
        r.put("a", a == null || a.isBlank() ? null : HEURE.format(heureA));
        r.put("personnes", personnes);
        r.put("total", personnes.size());
        r.put("presents", personnes.stream().filter(m -> Boolean.TRUE.equals(m.get("present"))).count());
        return r;
    }

    /** Heure HH:MM d'un filtre, ou la valeur par défaut si absente ; « 12:30 » à « 12:30:59 » pour une borne de fin. */
    private static java.time.LocalTime heure(String valeur, String libelle, java.time.LocalTime parDefaut) {
        if (valeur == null || valeur.isBlank()) return parDefaut;
        try {
            java.time.LocalTime t = java.time.LocalTime.parse(valeur.trim());
            return parDefaut == java.time.LocalTime.MAX ? t.withSecond(59).withNano(999_999_999) : t;
        } catch (java.time.format.DateTimeParseException e) {
            throw refus(libelle + " est invalide (format attendu : 08:30).");
        }
    }

    /** Arrivée si aucun pointage aujourd'hui ou si le dernier est un départ ; départ sinon. */
    static String prochainType(Optional<PointageJpaEntity> dernier, LocalDateTime maintenant) {
        return dernier.filter(d -> d.getHorodatage().toLocalDate().equals(maintenant.toLocalDate()))
                .filter(d -> ARRIVEE.equals(d.getType()))
                .map(d -> DEPART)
                .orElse(ARRIVEE);
    }

    private Map<String, Object> vue(PointageJpaEntity p) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", p.getId());
        m.put("type", p.getType());
        m.put("horodatage", p.getHorodatage().toString());
        return m;
    }

    private static String libelle(String type) {
        return DEPART.equals(type) ? "Départ" : "Arrivée";
    }

    /** Compte authentifié de la requête (jamais une valeur par défaut). */
    public static User compteConnecte() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof User u) {
            return u;
        }
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Connexion requise.");
    }

    /** « Prénom Nom », ou null (jamais l'e-mail). */
    private static String nomComplet(User u) {
        String n = ((u.getPrenom() == null ? "" : u.getPrenom().trim()) + " " + (u.getNom() == null ? "" : u.getNom().trim())).trim();
        return n.isEmpty() ? null : n;
    }

    private static String ecoleCourante() {
        return TenantContext.getCurrentOrganizationId()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "École non identifiée."));
    }

    private static String roleNormalise(String role) {
        if (role == null || role.isBlank()) return "INCONNU";
        try {
            return RoleUtilisateur.fromString(role).value();
        } catch (IllegalArgumentException inconnu) {
            return role.trim().toUpperCase(Locale.ROOT);
        }
    }
}
