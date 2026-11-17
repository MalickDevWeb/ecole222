package ecole_221.sn.ecole.modules.admin.application.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import ecole_221.sn.ecole.modules.admin.infrastructure.persistence.InscriptionClasseJpaEntity;
import ecole_221.sn.ecole.modules.admin.infrastructure.persistence.InscriptionClasseJpaRepository;
import ecole_221.sn.ecole.modules.admin.rbac.CataloguePermissions;
import ecole_221.sn.ecole.modules.admin.rbac.PermissionsEcoleService;
import ecole_221.sn.ecole.modules.admissions.application.service.EtatsCandidature;
import ecole_221.sn.ecole.modules.admissions.domain.valueobject.CandidateStatus;
import ecole_221.sn.ecole.modules.admissions.infrastructure.persistence.CandidateJpaEntity;
import ecole_221.sn.ecole.modules.admissions.infrastructure.persistence.CandidateJpaRepository;
import ecole_221.sn.ecole.modules.auth.infrastructure.persistence.UserJpaEntity;
import ecole_221.sn.ecole.modules.auth.infrastructure.persistence.UserJpaRepository;
import ecole_221.sn.ecole.modules.finance.application.service.EcolagesService;
import ecole_221.sn.ecole.modules.parent.infrastructure.persistence.ParentEnfantJpaEntity;
import ecole_221.sn.ecole.modules.parent.infrastructure.persistence.ParentEnfantJpaRepository;
import ecole_221.sn.ecole.modules.planning.infrastructure.persistence.ClasseJpaEntity;
import ecole_221.sn.ecole.modules.planning.infrastructure.persistence.ClasseJpaRepository;
import ecole_221.sn.ecole.partage.ChampsRequete;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Inscription d'un candidat ADMIS en élève de l'école (APP-TL-23, décision du 14/11/2026).
 *
 * <p>Avant : POST /admissions/automation/matricules calculait des matricules sans rien enregistrer ; les admis étaient
 * recréés à la main dans Gestion Élèves, sans lien avec leur dossier ni avec le parent qui l'avait déposé.</p>
 *
 * <p>Règles (une seule transaction, dossier verrouillé) :</p>
 * <ul>
 *   <li>décision ACCEPTED ; dossier pas encore inscrit (un second appel → 409, jamais un second compte) ;</li>
 *   <li>frais de dossier confirmés par l'école (guichet, Wave, Orange Money…) : déjà confirmés, ou confirmés dans la
 *       même demande (moyen + référence) ; la déclaration du candidat sur le portail ne compte pas ;</li>
 *   <li>compte élève : matricule ETU-AAAA-XXXXXX, mot de passe provisoire renvoyé une seule fois (jamais envoyé par
 *       e-mail) ; élève sans e-mail personnel → connexion par matricule, identifiant technique sur le domaine réservé
 *       {@value #DOMAINE_SANS_EMAIL} (jamais livrable, jamais affiché comme une adresse) ;</li>
 *   <li>téléphone du dossier non copié sur le compte élève (souvent celui du parent) ;</li>
 *   <li>parent qui a déposé le dossier lié (relation choisie, AUTRE par défaut) ; garant d'un dossier public lié sur
 *       demande ; classe et dossier financier facultatifs, jamais déduits ;</li>
 *   <li>INE facultatif, saisi par l'école, jamais généré, unique s'il est renseigné.</li>
 * </ul>
 */
@Service
public class InscriptionAdmisService {

    private static final Logger logger = LoggerFactory.getLogger(InscriptionAdmisService.class);
    private static final ObjectMapper JSON = new ObjectMapper();

    /** Domaine réservé (RFC 6761) : aucune adresse n'y est jamais livrée. */
    public static final String DOMAINE_SANS_EMAIL = "sans-email.invalid";
    static final List<String> MOYENS_FRAIS = List.of("Espèces", "Wave", "Orange Money", "Free Money", "Virement", "Chèque", "Autre");
    static final long MONTANT_MAX = 100_000_000L;

    public static final String NON_ADMIS = "Seul un dossier admis peut être inscrit (statut actuel : %s).";
    public static final String FRAIS_NON_CONFIRMES = "Frais de dossier non confirmés par l'école : enregistrez le paiement "
            + "(guichet, Wave, Orange Money…) avant l'inscription.";
    public static final String DEJA_INSCRIT = "Dossier déjà inscrit : élève %s.";
    public static final String EMAIL_PRIS = "L'adresse « %s » est déjà utilisée par un autre compte : saisissez l'adresse "
            + "personnelle de l'élève ou cochez « sans e-mail » (connexion par matricule).";
    public static final String INE_PRIS = "Cet INE est déjà attribué à un autre élève.";

    private final CandidateJpaRepository candidats;
    private final ComptesEcoleService comptes;
    private final UserJpaRepository utilisateurs;
    private final ParentsEleveService parents;
    private final ParentEnfantJpaRepository liens;
    private final ClasseJpaRepository classes;
    private final InscriptionClasseJpaRepository inscriptionsClasses;
    private final EcolagesService ecolages;
    private final PermissionsEcoleService droits;

    public InscriptionAdmisService(CandidateJpaRepository candidats, ComptesEcoleService comptes, UserJpaRepository utilisateurs,
                                   ParentsEleveService parents, ParentEnfantJpaRepository liens, ClasseJpaRepository classes,
                                   InscriptionClasseJpaRepository inscriptionsClasses, EcolagesService ecolages,
                                   PermissionsEcoleService droits) {
        this.candidats = candidats;
        this.comptes = comptes;
        this.utilisateurs = utilisateurs;
        this.parents = parents;
        this.liens = liens;
        this.classes = classes;
        this.inscriptionsClasses = inscriptionsClasses;
        this.ecolages = ecolages;
        this.droits = droits;
    }

    @Transactional
    public Map<String, Object> inscrire(String candidatureId, Map<String, Object> corps) {
        Map<String, Object> c0 = corps == null ? Map.of() : corps;
        Map<String, Object> dossierFinancier = sousObjet(c0, "dossierFinancier", "Le dossier financier");
        Map<String, Object> fraisDossier = sousObjet(c0, "fraisDossier", "Les frais de dossier");
        exigerDroits(dossierFinancier != null);

        // ---- 1. Lecture et contrôle du corps AVANT toute écriture (400) ----
        String emailSaisi = ChampsRequete.texteOptionnel(c0, "emailEleve", "L'e-mail de l'élève", 255);
        boolean sansEmail = Boolean.TRUE.equals(ChampsRequete.booleenOptionnel(c0, "sansEmail", "« Sans e-mail »"));
        if (sansEmail && emailSaisi != null) {
            throw ChampsRequete.refus("Choisissez : une adresse e-mail pour l'élève, ou « sans e-mail » (connexion par matricule).");
        }
        String ine = ChampsRequete.texteOptionnel(c0, "ine", "L'INE", 30);
        if (ine != null && !ine.matches("^[\\p{L}\\p{N}][\\p{L}\\p{N} ./-]*$")) {
            throw ChampsRequete.refus("L'INE ne peut contenir que des lettres, des chiffres, des espaces, « . », « / » et « - ».");
        }
        String relation = ChampsRequete.choix(c0, "relationParent", "Le lien de parenté", ParentsEleveService.RELATIONS, "AUTRE");
        boolean lierGarant = Boolean.TRUE.equals(ChampsRequete.booleenOptionnel(c0, "lierGarant", "« Lier le garant »"));
        String eleveExistantId = ChampsRequete.texteOptionnel(c0, "eleveExistantId", "L'élève existant", 255);
        String moyenFrais = fraisDossier == null ? null
                : ChampsRequete.choix(fraisDossier, "moyen", "Le moyen de paiement des frais de dossier", MOYENS_FRAIS, null);
        String referenceFrais = fraisDossier == null ? null
                : ChampsRequete.texteOptionnel(fraisDossier, "reference", "La référence du paiement", 60);
        Integer montantSaisi = dossierFinancier == null ? null
                : ChampsRequete.entierOptionnel(dossierFinancier, "montantDu", "Le montant dû", 1, MONTANT_MAX);
        String classeId = ChampsRequete.texteOptionnel(c0, "classeId", "La classe", 255);
        ClasseJpaEntity classe = classeId == null ? null : classes.findById(classeId)   // @TenantId : classes de l'école
                .orElseThrow(() -> ChampsRequete.refus("Classe inconnue dans cet établissement."));
        Long montantDu = null;
        if (dossierFinancier != null) {
            if (montantSaisi != null) {
                montantDu = montantSaisi.longValue();
            } else if (classe != null && classe.getFraisAnnuels() != null && classe.getFraisAnnuels() > 0) {
                montantDu = classe.getFraisAnnuels();
            } else {
                throw ChampsRequete.refus("Indiquez le montant dû du dossier financier (la classe choisie n'a pas de frais annuels enregistrés).");
            }
        }

        // ---- 2. Dossier verrouillé jusqu'à la fin de la transaction ----
        CandidateJpaEntity c = candidats.findPourInscription(candidatureId == null ? "" : candidatureId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Candidature introuvable dans cet établissement."));
        if (c.estInscrit()) {
            String matricule = utilisateurs.findById(c.getEleveId()).map(UserJpaEntity::getMatricule).orElse(c.getEleveId());
            throw new ResponseStatusException(HttpStatus.CONFLICT, String.format(DEJA_INSCRIT, matricule));
        }
        CandidateStatus statut = statut(c.getAdmissionStatus());
        if (statut != CandidateStatus.ACCEPTED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, String.format(NON_ADMIS, statut == null ? "inconnu" : statut.name()));
        }
        if (!Boolean.TRUE.equals(c.getRegistrationFeePaid())) {
            if (moyenFrais == null) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, FRAIS_NON_CONFIRMES);
            }
            c.setRegistrationFeePaid(true);
            c.setRegistrationFeeReference(referenceFrais);
            c.setAdditionalInfo(avecMoyenDePaiement(c.getAdditionalInfo(), moyenFrais));
        }

        UserJpaEntity parentDeposant = c.getParentId() == null ? null : utilisateurs.findById(c.getParentId()).orElse(null);

        // ---- 3. Compte élève (créé, ou élève déjà créé à la main rattaché) ----
        UserJpaEntity eleve;
        String provisoire = null;
        boolean compteCree;
        boolean connexionParMatricule;
        if (eleveExistantId != null) {
            eleve = comptes.membre(eleveExistantId, "ETUDIANT");   // 404 si l'élève n'est pas de cette école
            if (candidats.existsByEleveId(eleve.getId())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Cet élève est déjà rattaché à un autre dossier de candidature.");
            }
            compteCree = false;
            connexionParMatricule = sansEmail(eleve.getEmail());
        } else {
            String email = sansEmail ? null : (emailSaisi != null ? emailSaisi : c.getEmail());
            email = email == null || email.isBlank() ? null : email.trim().toLowerCase(Locale.ROOT);
            if (email != null && sansEmail(email)) {
                throw ChampsRequete.refus("Adresse e-mail invalide.");
            }
            if (email != null) {
                Optional<UserJpaEntity> pris = utilisateurs.findByEmail(email);
                if (pris.isPresent()) {
                    boolean adresseDuParent = emailSaisi == null && parentDeposant != null
                            && pris.get().getId().equals(parentDeposant.getId());
                    if (!adresseDuParent) {
                        throw new ResponseStatusException(HttpStatus.CONFLICT, String.format(EMAIL_PRIS, email));
                    }
                    email = null;   // dossier déposé avec l'adresse du parent : l'élève se connecte par matricule
                }
            }
            String[] nom = nom(c);
            String matricule = comptes.nouveauMatriculeEleve();
            String identifiant = email != null ? email : ComptesEcoleService.identifiantSansEmail(matricule);
            ComptesEcoleService.CompteCree cree = comptes.creer(nom[0], nom[1], identifiant, null, matricule, "ETUDIANT", null);
            eleve = cree.compte();
            provisoire = cree.motDePasseProvisoire();
            compteCree = true;
            connexionParMatricule = email == null;
        }
        if (ine != null && !ine.equalsIgnoreCase(eleve.getIne())) {
            if (utilisateurs.existsByIneIgnoreCase(ine)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, INE_PRIS);
            }
            eleve.setIne(ine);
            utilisateurs.save(eleve);
        }

        // ---- 4. Classe (choisie par l'agent, jamais déduite) ----
        if (classe != null) {
            List<InscriptionClasseJpaEntity> existantes = inscriptionsClasses.findByUserId(eleve.getId());
            if (existantes.isEmpty()) {
                inscriptionsClasses.save(new InscriptionClasseJpaEntity(UUID.randomUUID().toString(), eleve.getId(), classe.getId()));
            } else {
                InscriptionClasseJpaEntity i = existantes.get(0);
                i.setClasseId(classe.getId());
                inscriptionsClasses.save(i);
            }
        }

        // ---- 5. Parent : celui qui a déposé le dossier, ou le garant d'un dossier public (sur demande) ----
        Map<String, Object> parent = null;
        if (parentDeposant != null) {
            parent = lierParent(eleve.getId(), parentDeposant.getEmail(), null, null, relation);
        } else if (lierGarant) {
            Map<String, String> info = informations(c.getAdditionalInfo());
            String emailGarant = info.get("garantEmail");
            if (emailGarant == null || emailGarant.isBlank()) {
                throw ChampsRequete.refus("Le dossier ne contient pas l'e-mail du garant : liez le parent depuis la fiche de l'élève.");
            }
            parent = lierParent(eleve.getId(), emailGarant, info.get("garantNom"), info.get("garantTelephone"), relation);
        }

        // ---- 6. Dossier financier (facultatif) ----
        Map<String, Object> dossier = null;
        if (montantDu != null) {
            Map<String, Object> demande = new LinkedHashMap<>();
            demande.put("eleveId", eleve.getId());
            demande.put("montantDu", montantDu);
            if (classe != null) demande.put("classe", nomClasse(classe));
            dossier = ecolages.ouvrirDossier(demande);
        }

        // ---- 7. Dossier inscrit : figé ----
        c.setAdmissionStatus(CandidateStatus.FINALISE.name());
        EtatsCandidature.apresDecision(c);   // étape « admitted »
        c.setEleveId(eleve.getId());
        c.setInscritLe(Instant.now());
        c.setInscritPar(auteur());
        c.setUpdatedAt(LocalDateTime.now());
        candidats.save(c);
        logger.info("Dossier {} inscrit : élève {} ({})", c.getId(), eleve.getId(), compteCree ? "compte créé" : "compte existant rattaché");

        Map<String, Object> vueEleve = new LinkedHashMap<>();
        vueEleve.put("id", eleve.getId());
        vueEleve.put("nom", ComptesEcoleService.nomComplet(eleve));
        vueEleve.put("matricule", eleve.getMatricule());
        vueEleve.put("sansEmail", connexionParMatricule);
        vueEleve.put("email", connexionParMatricule ? null : eleve.getEmail());
        vueEleve.put("identifiant", connexionParMatricule ? eleve.getMatricule() : eleve.getEmail());
        vueEleve.put("ine", eleve.getIne());
        vueEleve.put("classeId", classe == null ? null : classe.getId());
        vueEleve.put("classe", classe == null ? null : nomClasse(classe));

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("candidatureId", c.getId());
        res.put("statut", c.getAdmissionStatus());
        res.put("compteCree", compteCree);
        res.put("eleve", vueEleve);
        if (provisoire != null) {
            res.put("motDePasseProvisoire", provisoire);   // affiché une seule fois, jamais envoyé par e-mail
        }
        res.put("parent", parent);
        res.put("dossierFinancier", dossier);
        return res;
    }

    /** Identifiant technique d'un élève sans e-mail (domaine réservé) : jamais affiché comme une adresse. */
    public static boolean sansEmail(String email) {
        return email != null && email.toLowerCase(Locale.ROOT).endsWith("@" + DOMAINE_SANS_EMAIL);
    }

    /** Lien parent ↔ élève ; un lien déjà présent est conservé tel quel (aucun doublon, aucune erreur). */
    private Map<String, Object> lierParent(String eleveId, String email, String nom, String telephone, String relation) {
        if (email == null || email.isBlank()) {
            return null;
        }
        Optional<UserJpaEntity> compte = utilisateurs.findByEmail(email.trim().toLowerCase(Locale.ROOT));
        if (compte.isPresent()) {
            Optional<ParentEnfantJpaEntity> lien = liens.findByParentIdAndEnfantId(compte.get().getId(), eleveId);
            if (lien.isPresent()) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("parentId", compte.get().getId());
                m.put("nom", ComptesEcoleService.nomComplet(compte.get()));
                m.put("relation", lien.get().getRelation());
                m.put("compteCree", false);
                return m;
            }
        }
        Map<String, Object> demande = new LinkedHashMap<>();
        demande.put("email", email.trim());
        if (nom != null) demande.put("nom", nom);
        if (telephone != null) demande.put("telephone", telephone);
        demande.put("relation", relation);
        return parents.lier(eleveId, demande);
    }

    /** 403 si le rôle du personnel n'a pas les droits nécessaires dans l'école (l'administration les a tous). */
    private void exigerDroits(boolean finances) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        Set<String> roles = auth == null ? Set.of() : auth.getAuthorities().stream().map(GrantedAuthority::getAuthority)
                .filter(a -> a.startsWith("ROLE_")).map(a -> a.substring(5)).collect(Collectors.toSet());
        if (roles.stream().anyMatch(CataloguePermissions.ROLES_ADMINISTRATION::contains)) {
            return;
        }
        List<String> requis = new ArrayList<>(List.of(CataloguePermissions.ecriture("admissions"), CataloguePermissions.ecriture("eleves")));
        if (finances) requis.add(CataloguePermissions.ecriture("finances"));
        String role = roles.stream().filter(CataloguePermissions.ROLES_PERSONNEL::contains).findFirst().orElse(null);
        String ecole = comptes.ecoleCourante();
        if (role == null || requis.stream().anyMatch(p -> !droits.autorise(ecole, role, p))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Inscrire un admis demande les droits « Admissions » et « Élèves » "
                    + "en écriture" + (finances ? ", ainsi que « Finances » en écriture pour ouvrir le dossier financier" : "") + ".");
        }
    }

    private static String auteur() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return null;
        if (auth.getPrincipal() instanceof ecole_221.sn.ecole.modules.auth.domain.model.User u) return u.getId();
        return auth.getName();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> sousObjet(Map<String, Object> corps, String cle, String libelle) {
        Object v = corps.get(cle);
        if (v == null) return null;
        if (!(v instanceof Map<?, ?> m)) {
            throw ChampsRequete.refus(libelle + " : objet attendu.");
        }
        return (Map<String, Object>) m;
    }

    private static CandidateStatus statut(String brut) {
        try {
            return brut == null || brut.isBlank() ? CandidateStatus.EN_ATTENTE : CandidateStatus.from(brut);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static String[] nom(CandidateJpaEntity c) {
        boolean prenom = c.getFirstName() != null && !c.getFirstName().isBlank();
        boolean nom = c.getLastName() != null && !c.getLastName().isBlank();
        if (prenom || nom) {
            return new String[] {prenom ? c.getFirstName().trim() : "", nom ? c.getLastName().trim() : ""};
        }
        return ComptesEcoleService.decouperNom(c.getFullName());
    }

    private static String nomClasse(ClasseJpaEntity c) {
        return c.getName() != null ? c.getName() : c.getNom();
    }

    /** Informations texte du dossier (JSON additional_info) ; vide si illisible. */
    static Map<String, String> informations(String json) {
        if (json == null || json.isBlank()) return Map.of();
        try {
            Map<String, Object> brut = JSON.readValue(json, new TypeReference<Map<String, Object>>() {});
            Map<String, String> m = new LinkedHashMap<>();
            brut.forEach((k, v) -> { if (v != null) m.put(k, String.valueOf(v)); });
            return m;
        } catch (Exception e) {
            return Map.of();
        }
    }

    /** Ajoute le moyen de paiement des frais de dossier au complément du dossier (lu par GET /admissions/db). */
    static String avecMoyenDePaiement(String json, String moyen) {
        Map<String, Object> m = new LinkedHashMap<>();
        if (json != null && !json.isBlank()) {
            try {
                m.putAll(JSON.readValue(json, new TypeReference<Map<String, Object>>() {}));
            } catch (Exception e) {
                logger.warn("Complément de dossier illisible : remplacé par le seul moyen de paiement");
            }
        }
        m.put("paymentMethod", moyen);
        try {
            return JSON.writeValueAsString(m);
        } catch (Exception e) {
            throw new IllegalStateException("Complément de dossier non enregistrable.", e);
        }
    }
}
