package ecole_221.sn.ecole.modules.archivage.application;

import ecole_221.sn.ecole.modules.archivage.domain.model.ArchivageEcole;
import ecole_221.sn.ecole.modules.archivage.domain.model.EtatArchivage;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Vue JSON d'un archivage pour l'écran du super-admin. Jamais exposés : la clé enveloppée, l'identifiant de clé,
 * l'emplacement dans le stockage.
 */
public final class VueArchivage {

    private VueArchivage() {
    }

    public static Map<String, Object> de(ArchivageEcole a, boolean detail) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", a.getId());
        Map<String, Object> ecole = new LinkedHashMap<>();
        ecole.put("id", a.getOrganizationId());
        ecole.put("nom", a.getEcoleNom());
        ecole.put("slug", a.getEcoleSlug());
        m.put("ecole", ecole);
        m.put("etat", a.getEtat().name());
        m.put("etatLibelle", a.getEtat().libelle());
        m.put("etape", a.getEtape() == null ? null : a.getEtape().name());
        m.put("declenchement", a.getDeclenchement().name());
        m.put("motif", a.getMotif());
        m.put("suspendueLe", a.getSuspendueLe());
        Map<String, Object> demande = new LinkedHashMap<>();
        demande.put("id", a.getDemandePar());
        demande.put("nom", a.getDemandeParNom());
        m.put("demandePar", demande);
        m.put("demandeLe", a.getDemandeLe());
        m.put("archiveeLe", a.getArchiveeLe());
        m.put("conservationJours", a.getConservationJours());
        m.put("conservationJusquAu", a.getConservationJusquAu());
        m.put("aConfirmerDepuis", a.getAConfirmerDepuis());
        m.put("restaurationDemandeeLe", a.getRestaurationDemandeeLe());
        m.put("restaureeLe", a.getRestaureeLe());
        m.put("supprimeeLe", a.getSupprimeeLe());
        m.put("supprimeePar", a.getSupprimeePar());
        m.put("stockage", a.getStockage());
        m.put("empreinteSha256", a.getEmpreinteSha256());
        m.put("tailleOctets", a.getTailleOctets());
        m.put("versionSchema", a.getVersionSchema());
        m.put("nombreLignes", a.getNombreLignes());
        m.put("nombreMedias", a.getNombreMedias());
        m.put("fichierPresent", a.getEmplacement() != null && (a.isFichierASupprimer()
                || a.getEtat() == EtatArchivage.ARCHIVEE || a.getEtat() == EtatArchivage.SUPPRESSION_A_CONFIRMER
                || a.getEtat() == EtatArchivage.RESTAURATION_EN_COURS));
        m.put("erreur", a.getErreur());
        m.put("conflits", a.getConflits());
        m.put("restes", a.getRestes());
        Map<String, Object> actions = new LinkedHashMap<>();
        actions.put("restaurer", a.getEtat() == EtatArchivage.ARCHIVEE || a.getEtat() == EtatArchivage.SUPPRESSION_A_CONFIRMER);
        actions.put("supprimerDefinitivement", a.getEtat() == EtatArchivage.SUPPRESSION_A_CONFIRMER);
        m.put("actions", actions);
        m.put("majLe", a.getMajLe());
        if (detail) {
            m.put("compteurs", a.getCompteurs());
            m.put("purge", a.getPurge());
            m.put("tentatives", a.getTentatives());
        }
        return m;
    }
}
