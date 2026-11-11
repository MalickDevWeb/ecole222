package ecole_221.sn.ecole.modules.admin.application.service;

import ecole_221.sn.ecole.modules.admin.application.service.ComptesEcoleService.CompteCree;
import ecole_221.sn.ecole.modules.admin.infrastructure.persistence.InscriptionClasseJpaEntity;
import ecole_221.sn.ecole.modules.admin.infrastructure.persistence.InscriptionClasseJpaRepository;
import ecole_221.sn.ecole.modules.auth.infrastructure.persistence.UserJpaRepository;
import ecole_221.sn.ecole.modules.planning.infrastructure.persistence.ClasseJpaEntity;
import ecole_221.sn.ecole.modules.planning.infrastructure.persistence.ClasseJpaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Import groupé d'élèves (APP-BE-11). Chaque ligne est vérifiée (nom, email, doublons dans le fichier
 * et dans l'école, classe existante). Import « tout ou rien » : une seule ligne en erreur et rien n'est créé.
 * Avant : l'écran ajoutait les élèves uniquement à l'affichage, avec moyenne, téléphone et classe inventés.
 */
@Service
public class ImportElevesService {

    public static final int LIGNES_MAX = 500;
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final ComptesEcoleService comptesEcole;
    private final UserJpaRepository comptes;
    private final ClasseJpaRepository classes;
    private final InscriptionClasseJpaRepository inscriptions;

    public ImportElevesService(ComptesEcoleService comptesEcole, UserJpaRepository comptes,
                               ClasseJpaRepository classes, InscriptionClasseJpaRepository inscriptions) {
        this.comptesEcole = comptesEcole;
        this.comptes = comptes;
        this.classes = classes;
        this.inscriptions = inscriptions;
    }

    /** Ligne à importer, telle qu'envoyée par l'écran (numéro de ligne du fichier compris). */
    public record LigneImport(Integer ligne, String nom, String email, String matricule, String telephone, String classe) {}

    /**
     * @param simulation true : vérifie seulement ; false : crée les comptes si (et seulement si) toutes les lignes sont valides
     * @return rapport {simulation, valide, crees, lignes:[{ligne, nom, email, classe, statut, erreurs, id, matricule, motDePasseProvisoire}]}
     */
    @Transactional
    public Map<String, Object> importer(List<LigneImport> lignes, boolean simulation) {
        if (lignes == null || lignes.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le fichier ne contient aucun élève.");
        }
        if (lignes.size() > LIGNES_MAX) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Le fichier contient " + lignes.size() + " élèves ; " + LIGNES_MAX + " au plus par import.");
        }
        Map<String, ClasseJpaEntity> classesParNom = new HashMap<>();
        for (ClasseJpaEntity c : classes.findAll()) {   // @TenantId : classes de l'école courante seulement
            for (String n : new String[] {c.getName(), c.getNom(), c.getCode()}) {
                if (n != null && !n.isBlank()) classesParNom.putIfAbsent(cle(n), c);
            }
        }
        Set<String> emailsVus = new HashSet<>(), matriculesVus = new HashSet<>(), telephonesVus = new HashSet<>();
        List<Map<String, Object>> rapport = new ArrayList<>();
        List<ClasseJpaEntity> classesDesLignes = new ArrayList<>();
        boolean valide = true;

        for (int i = 0; i < lignes.size(); i++) {
            LigneImport l = lignes.get(i) == null ? new LigneImport(null, null, null, null, null, null) : lignes.get(i);
            int numero = l.ligne() != null ? l.ligne() : i + 1;
            List<String> erreurs = new ArrayList<>();
            String nom = texte(l.nom());
            String email = texte(l.email()) == null ? null : texte(l.email()).toLowerCase(Locale.ROOT);
            String matricule = texte(l.matricule());
            String telephone = texte(l.telephone());
            String classeSaisie = texte(l.classe());

            if (nom == null) erreurs.add("nom manquant");
            if (email == null) {
                erreurs.add("email manquant (il sert d'identifiant de connexion)");
            } else if (!EMAIL.matcher(email).matches()) {
                erreurs.add("email « " + email + " » invalide");
            } else if (!emailsVus.add(email)) {
                erreurs.add("email « " + email + " » en double dans le fichier");
            } else if (comptes.existsByEmail(email)) {
                erreurs.add("email « " + email + " » déjà utilisé par un compte");
            }
            if (matricule != null) {
                if (!matriculesVus.add(matricule)) erreurs.add("matricule « " + matricule + " » en double dans le fichier");
                else if (comptes.findFirstByMatricule(matricule).isPresent()) erreurs.add("matricule « " + matricule + " » déjà attribué");
            }
            if (telephone != null) {
                if (!telephonesVus.add(telephone)) erreurs.add("téléphone « " + telephone + " » en double dans le fichier");
                else if (comptes.existsByTelephone(telephone)) erreurs.add("téléphone « " + telephone + " » déjà utilisé");
            }
            ClasseJpaEntity classe = null;
            if (classeSaisie != null) {
                classe = classesParNom.get(cle(classeSaisie));
                if (classe == null) erreurs.add("classe « " + classeSaisie + " » inconnue dans l'école");
            }
            classesDesLignes.add(classe);

            Map<String, Object> r = new LinkedHashMap<>();
            r.put("ligne", numero);
            r.put("nom", nom);
            r.put("email", email);
            r.put("classe", classe == null ? classeSaisie : nomClasse(classe));
            r.put("statut", erreurs.isEmpty() ? "OK" : "ERREUR");
            r.put("erreurs", erreurs);
            rapport.add(r);
            valide &= erreurs.isEmpty();
        }

        int crees = 0;
        if (!simulation && valide) {
            for (int i = 0; i < lignes.size(); i++) {
                LigneImport l = lignes.get(i);
                String[] n = ComptesEcoleService.decouperNom(texte(l.nom()));
                CompteCree c = comptesEcole.creer(n[0], n[1], texte(l.email()), texte(l.telephone()), texte(l.matricule()), "ETUDIANT", null);
                ClasseJpaEntity classe = classesDesLignes.get(i);
                if (classe != null) {
                    inscriptions.save(new InscriptionClasseJpaEntity(UUID.randomUUID().toString(), c.compte().getId(), classe.getId()));
                }
                Map<String, Object> r = rapport.get(i);
                r.put("statut", "CREE");
                r.put("id", c.compte().getId());
                r.put("matricule", c.compte().getMatricule());
                r.put("motDePasseProvisoire", c.motDePasseProvisoire());
                crees++;
            }
        }
        Map<String, Object> resultat = new LinkedHashMap<>();
        resultat.put("simulation", simulation);
        resultat.put("valide", valide);
        resultat.put("crees", crees);
        resultat.put("erreurs", rapport.stream().filter(r -> "ERREUR".equals(r.get("statut"))).count());
        resultat.put("lignes", rapport);
        return resultat;
    }

    private static String nomClasse(ClasseJpaEntity c) {
        return c.getName() != null && !c.getName().isBlank() ? c.getName() : c.getNom();
    }

    private static String cle(String s) {
        return s.trim().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }

    private static String texte(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
