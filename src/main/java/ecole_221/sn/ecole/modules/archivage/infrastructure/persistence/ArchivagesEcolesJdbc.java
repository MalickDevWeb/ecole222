package ecole_221.sn.ecole.modules.archivage.infrastructure.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import ecole_221.sn.ecole.modules.archivage.domain.exception.ArchivageException;
import ecole_221.sn.ecole.modules.archivage.domain.model.ArchivageEcole;
import ecole_221.sn.ecole.modules.archivage.domain.model.Declenchement;
import ecole_221.sn.ecole.modules.archivage.domain.model.EtapeArchivage;
import ecole_221.sn.ecole.modules.archivage.domain.model.EtatArchivage;
import ecole_221.sn.ecole.modules.archivage.domain.port.ArchivagesEcolesPort;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Adaptateur JDBC du suivi des archivages (V24). Table de plateforme, sans @TenantId : l'école est toujours passée
 * explicitement.
 */
@Repository
public class ArchivagesEcolesJdbc implements ArchivagesEcolesPort {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String COLONNES = "id, organization_id, ecole_nom, ecole_slug, etat, etape, etat_avant_restauration, "
            + "declenchement, statut_initial, suspendue_le, motif, demande_par, demande_par_nom, demande_le, archivee_le, "
            + "conservation_jours, conservation_jusqu_au, a_confirmer_depuis, restauration_demandee_par, "
            + "restauration_demandee_le, restauree_le, supprimee_le, supprimee_par, stockage, emplacement, fichier_a_supprimer, "
            + "empreinte_sha256, taille_octets, version_schema, cle_enveloppee, cle_id, compteurs::text AS compteurs, "
            + "nombre_lignes, nombre_medias, purge::text AS purge, restes::text AS restes, conflits::text AS conflits, erreur, "
            + "tentatives, maj_le";
    private static final String VIVANTS = "('ARCHIVAGE_EN_COURS', 'ARCHIVEE', 'SUPPRESSION_A_CONFIRMER', 'RESTAURATION_EN_COURS')";

    private final JdbcTemplate jdbc;

    public ArchivagesEcolesJdbc(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void inserer(ArchivageEcole a) {
        try {
            jdbc.update("INSERT INTO archivages_ecoles (id, organization_id, ecole_nom, ecole_slug, etat, etape, declenchement, "
                            + "statut_initial, suspendue_le, motif, demande_par, demande_par_nom, demande_le, maj_le) "
                            + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    a.getId(), a.getOrganizationId(), a.getEcoleNom(), a.getEcoleSlug(), a.getEtat().name(),
                    a.getEtape() == null ? null : a.getEtape().name(), a.getDeclenchement().name(), a.getStatutInitial(),
                    ts(a.getSuspendueLe()), a.getMotif(), a.getDemandePar(), a.getDemandeParNom(), ts(a.getDemandeLe()),
                    ts(a.getMajLe() == null ? a.getDemandeLe() : a.getMajLe()));
        } catch (DuplicateKeyException e) {
            throw ArchivageException.conflit("Une opération d'archivage est déjà en cours pour cette école.");
        }
    }

    @Override
    public void enregistrer(ArchivageEcole a) {
        if (mettreAJour(a, null) != 1) {
            throw ArchivageException.introuvable("Archivage introuvable.");
        }
    }

    @Override
    public boolean enregistrerSi(ArchivageEcole a, EtatArchivage etatAttendu) {
        return mettreAJour(a, etatAttendu) == 1;
    }

    private int mettreAJour(ArchivageEcole a, EtatArchivage etatAttendu) {
        return jdbc.update("UPDATE archivages_ecoles SET etat = ?, etape = ?, etat_avant_restauration = ?, archivee_le = ?, "
                        + "conservation_jours = ?, conservation_jusqu_au = ?, a_confirmer_depuis = ?, restauration_demandee_par = ?, "
                        + "restauration_demandee_le = ?, restauree_le = ?, supprimee_le = ?, supprimee_par = ?, stockage = ?, "
                        + "emplacement = ?, fichier_a_supprimer = ?, empreinte_sha256 = ?, taille_octets = ?, version_schema = ?, "
                        + "cle_enveloppee = ?, cle_id = ?, compteurs = CAST(? AS jsonb), nombre_lignes = ?, nombre_medias = ?, "
                        + "purge = CAST(? AS jsonb), restes = CAST(? AS jsonb), conflits = CAST(? AS jsonb), erreur = ?, "
                        + "tentatives = ?, maj_le = ? WHERE id = ? AND (CAST(? AS varchar) IS NULL OR etat = ?)",
                a.getEtat().name(), a.getEtape() == null ? null : a.getEtape().name(),
                a.getEtatAvantRestauration() == null ? null : a.getEtatAvantRestauration().name(), ts(a.getArchiveeLe()),
                a.getConservationJours(), ts(a.getConservationJusquAu()), ts(a.getAConfirmerDepuis()),
                a.getRestaurationDemandeePar(), ts(a.getRestaurationDemandeeLe()), ts(a.getRestaureeLe()),
                ts(a.getSupprimeeLe()), a.getSupprimeePar(), a.getStockage(), a.getEmplacement(), a.isFichierASupprimer(),
                a.getEmpreinteSha256(), a.getTailleOctets(), a.getVersionSchema(), a.getCleEnveloppee(), a.getCleId(),
                json(a.getCompteurs()), a.getNombreLignes(), a.getNombreMedias(), json(a.getPurge()), json(a.getRestes()),
                a.getConflits().isEmpty() ? null : json(a.getConflits()), a.getErreur(), a.getTentatives(),
                ts(a.getMajLe() == null ? LocalDateTime.now() : a.getMajLe()), a.getId(),
                etatAttendu == null ? null : etatAttendu.name(), etatAttendu == null ? null : etatAttendu.name());
    }

    @Override
    public Optional<ArchivageEcole> parId(String id) {
        return jdbc.query("SELECT " + COLONNES + " FROM archivages_ecoles WHERE id = ?", LIGNE, id).stream().findFirst();
    }

    @Override
    public Optional<ArchivageEcole> vivantDeLEcole(String organizationId) {
        return jdbc.query("SELECT " + COLONNES + " FROM archivages_ecoles WHERE organization_id = ? AND etat IN " + VIVANTS,
                LIGNE, organizationId).stream().findFirst();
    }

    @Override
    public Optional<ArchivageEcole> dernierDeLEcole(String organizationId) {
        return jdbc.query("SELECT " + COLONNES + " FROM archivages_ecoles WHERE organization_id = ? "
                + "ORDER BY demande_le DESC LIMIT 1", LIGNE, organizationId).stream().findFirst();
    }

    @Override
    public List<ArchivageEcole> lister(EtatArchivage etat, String organizationId) {
        StringBuilder sql = new StringBuilder("SELECT " + COLONNES + " FROM archivages_ecoles WHERE 1 = 1");
        List<Object> params = new ArrayList<>();
        if (etat != null) {
            sql.append(" AND etat = ?");
            params.add(etat.name());
        }
        if (organizationId != null) {
            sql.append(" AND organization_id = ?");
            params.add(organizationId);
        }
        sql.append(" ORDER BY demande_le DESC");
        return jdbc.query(sql.toString(), LIGNE, params.toArray());
    }

    @Override
    public List<ArchivageEcole> parEtat(EtatArchivage etat) {
        return jdbc.query("SELECT " + COLONNES + " FROM archivages_ecoles WHERE etat = ? ORDER BY demande_le", LIGNE, etat.name());
    }

    @Override
    public List<ArchivageEcole> echeancesAtteintes(LocalDateTime maintenant) {
        return jdbc.query("SELECT " + COLONNES + " FROM archivages_ecoles WHERE etat = 'ARCHIVEE' "
                + "AND conservation_jusqu_au <= ? ORDER BY conservation_jusqu_au", LIGNE, ts(maintenant));
    }

    @Override
    public List<ArchivageEcole> fichiersASupprimer() {
        return jdbc.query("SELECT " + COLONNES + " FROM archivages_ecoles WHERE fichier_a_supprimer "
                + "AND etat IN ('RESTAUREE', 'SUPPRIMEE_DEFINITIVEMENT', 'ECHEC') ORDER BY maj_le", LIGNE);
    }

    @Override
    public void journaliser(String archivageId, String organizationId, String evenement, EtatArchivage etat, String acteur,
                            String detail) {
        jdbc.update("INSERT INTO archivages_ecoles_journal (archivage_id, organization_id, survenu_le, evenement, etat, acteur, detail) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?)", archivageId, organizationId, ts(LocalDateTime.now()), evenement,
                etat == null ? null : etat.name(), acteur, detail == null ? null : (detail.length() <= 2000 ? detail : detail.substring(0, 2000)));
    }

    @Override
    public List<Map<String, Object>> journal(String archivageId) {
        return jdbc.query("SELECT survenu_le, evenement, etat, acteur, detail FROM archivages_ecoles_journal "
                + "WHERE archivage_id = ? ORDER BY survenu_le, id", (rs, i) -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("survenuLe", ldt(rs, "survenu_le"));
            m.put("evenement", rs.getString("evenement"));
            m.put("etat", rs.getString("etat"));
            m.put("acteur", rs.getString("acteur"));
            m.put("detail", rs.getString("detail"));
            return m;
        }, archivageId);
    }

    private static final RowMapper<ArchivageEcole> LIGNE = (rs, i) -> {
        ArchivageEcole a = ArchivageEcole.reconstituer(rs.getString("id"), rs.getString("organization_id"),
                rs.getString("ecole_nom"), rs.getString("ecole_slug"), Declenchement.valueOf(rs.getString("declenchement")),
                rs.getString("statut_initial"), ldt(rs, "suspendue_le"), rs.getString("motif"), rs.getString("demande_par"),
                rs.getString("demande_par_nom"), ldt(rs, "demande_le"), EtatArchivage.valueOf(rs.getString("etat")),
                rs.getString("etape") == null ? null : EtapeArchivage.valueOf(rs.getString("etape")),
                rs.getString("etat_avant_restauration") == null ? null : EtatArchivage.valueOf(rs.getString("etat_avant_restauration")));
        a.hydrater(ldt(rs, "archivee_le"), (Integer) rs.getObject("conservation_jours"), ldt(rs, "conservation_jusqu_au"),
                ldt(rs, "a_confirmer_depuis"), rs.getString("restauration_demandee_par"), ldt(rs, "restauration_demandee_le"),
                ldt(rs, "restauree_le"), ldt(rs, "supprimee_le"), rs.getString("supprimee_par"), rs.getString("stockage"),
                rs.getString("emplacement"), rs.getBoolean("fichier_a_supprimer"), rs.getString("empreinte_sha256"),
                (Long) rs.getObject("taille_octets"), rs.getString("version_schema"), rs.getString("cle_enveloppee"),
                rs.getString("cle_id"), compteurs(rs.getString("compteurs")), (Long) rs.getObject("nombre_lignes"),
                (Integer) rs.getObject("nombre_medias"), compteurs(rs.getString("purge")), compteurs(rs.getString("restes")),
                conflits(rs.getString("conflits")), rs.getString("erreur"), rs.getInt("tentatives"), ldt(rs, "maj_le"));
        return a;
    };

    private static LocalDateTime ldt(ResultSet rs, String colonne) throws SQLException {
        Timestamp t = rs.getTimestamp(colonne);
        return t == null ? null : t.toLocalDateTime();
    }

    private static Timestamp ts(LocalDateTime d) {
        return d == null ? null : Timestamp.valueOf(d);
    }

    private static String json(Object o) {
        if (o == null || (o instanceof Map<?, ?> m && m.isEmpty())) return null;
        try {
            return JSON.writeValueAsString(o);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    private static Map<String, Long> compteurs(String json) {
        if (json == null) return null;
        try {
            return JSON.readValue(json, new TypeReference<LinkedHashMap<String, Long>>() { });
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    private static List<Map<String, Object>> conflits(String json) {
        if (json == null) return null;
        try {
            return JSON.readValue(json, new TypeReference<List<Map<String, Object>>>() { });
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }
}
