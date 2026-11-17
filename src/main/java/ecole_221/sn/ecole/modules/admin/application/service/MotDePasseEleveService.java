package ecole_221.sn.ecole.modules.admin.application.service;

import ecole_221.sn.ecole.infrastructure.security.revocation.RevocationJetons;
import ecole_221.sn.ecole.modules.auth.infrastructure.persistence.UserJpaEntity;
import ecole_221.sn.ecole.modules.auth.infrastructure.persistence.UserJpaRepository;
import ecole_221.sn.ecole.modules.platform.organization.infrastructure.persistence.OrganizationUserJpaEntity;
import ecole_221.sn.ecole.modules.platform.organization.infrastructure.persistence.OrganizationUserJpaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Nouveau mot de passe provisoire pour un élève, remis par l'école (APP-TL-23).
 *
 * <p>Un élève inscrit sans e-mail (connexion par matricule) ne peut pas utiliser « Mot de passe oublié » (aucune
 * adresse où envoyer le lien).</p>
 *
 * <p>Ici : l'administration (module Élèves, écriture) génère un mot de passe provisoire, affiché une seule fois pour
 * être remis à l'élève (jamais envoyé, jamais journalisé) ; l'élève doit le changer à sa prochaine connexion et ses
 * sessions ouvertes sont fermées. Comptes élèves de l'école seulement (404 sinon, APP-TL-05). Un compte qui sert aussi
 * dans un autre établissement n'est pas modifié (409) : une école n'agit jamais sur l'accès d'une autre. L'action est
 * inscrite au journal d'audit (intercepteur des écritures).</p>
 */
@Service
public class MotDePasseEleveService {

    private final ComptesEcoleService comptes;
    private final OrganizationUserJpaRepository appartenances;
    private final UserJpaRepository utilisateurs;
    private final PasswordEncoder encodeur;
    /** Sessions de l'élève fermées après le changement (absent dans certains tests). */
    @Autowired(required = false)
    private RevocationJetons revocation;

    public MotDePasseEleveService(ComptesEcoleService comptes, OrganizationUserJpaRepository appartenances,
                                  UserJpaRepository utilisateurs, PasswordEncoder encodeur) {
        this.comptes = comptes;
        this.appartenances = appartenances;
        this.utilisateurs = utilisateurs;
        this.encodeur = encodeur;
    }

    @Transactional
    public Map<String, Object> nouveauMotDePasseProvisoire(String eleveId) {
        String ecole = comptes.ecoleCourante();
        UserJpaEntity eleve = comptes.membre(eleveId, "ETUDIANT");   // 404 si l'élève n'est pas de cette école
        OrganizationUserJpaEntity ici = comptes.appartenance(eleveId, "ETUDIANT").orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Compte introuvable dans cet établissement."));
        if ("TRANSFERRED".equalsIgnoreCase(ici.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Élève transféré vers une autre école : c'est sa nouvelle école qui peut lui remettre un mot de passe.");
        }
        boolean ailleurs = appartenances.findByUserId(eleveId).stream()
                .anyMatch(l -> !ecole.equals(l.getOrganizationId()) && "ACTIVE".equalsIgnoreCase(l.getStatus()));
        if (ailleurs) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Ce compte sert aussi dans un autre établissement : il n'est pas modifié ici. L'élève peut utiliser "
                            + "« Mot de passe oublié » ou s'adresser à cet établissement.");
        }

        String provisoire = ComptesEcoleService.motDePasseProvisoire();
        eleve.setPassword(encodeur.encode(provisoire));
        eleve.setDoitChangerMotDePasse(Boolean.TRUE);
        utilisateurs.save(eleve);
        if (revocation != null) {
            revocation.invaliderTout(eleveId);
        }

        boolean sansEmail = InscriptionAdmisService.sansEmail(eleve.getEmail());
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("eleveId", eleveId);
        res.put("nom", ComptesEcoleService.nomComplet(eleve));
        res.put("identifiant", sansEmail ? eleve.getMatricule() : eleve.getEmail());
        res.put("sansEmail", sansEmail);
        res.put("motDePasseProvisoire", provisoire);   // affiché une seule fois, à remettre à l'élève
        res.put("doitChangerMotDePasse", true);
        res.put("message", "Mot de passe provisoire à remettre à l'élève : il devra le changer à sa prochaine connexion. "
                + "Ses sessions ouvertes ont été fermées.");
        return res;
    }
}
