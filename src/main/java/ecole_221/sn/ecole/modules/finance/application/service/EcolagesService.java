package ecole_221.sn.ecole.modules.finance.application.service;

import ecole_221.sn.ecole.modules.admin.application.service.ComptesEcoleService;
import ecole_221.sn.ecole.modules.auth.infrastructure.persistence.UserJpaEntity;
import ecole_221.sn.ecole.modules.finance.infrastructure.persistence.StudentFinanceJpaEntity;
import ecole_221.sn.ecole.modules.finance.infrastructure.persistence.StudentFinanceJpaRepository;
import ecole_221.sn.ecole.modules.finance.infrastructure.persistence.TransactionJpaEntity;
import ecole_221.sn.ecole.modules.finance.infrastructure.persistence.TransactionJpaRepository;
import ecole_221.sn.ecole.partage.ChampsRequete;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Écolages (frais de scolarité) — chaîne complète.
 *
 * <p>Dossier ouvert pour un VRAI élève de l'école (404 sinon), un seul par élève (409), montant dû borné ;
 * versement rattaché au dossier, montant borné, moyen dans une liste fermée, date jamais dans le futur ; payé, reste
 * dû et statut recalculés dans la même transaction. Statuts : « En Règle » (soldé), « En Avance » (payé au-delà du
 * dû), « Non soldé » (reste dû ; aucune échéance n'étant enregistrée, rien ne permet de dire « en retard »).</p>
 */
@Service
public class EcolagesService {

    public static final String EN_REGLE = "En Règle";
    public static final String EN_AVANCE = "En Avance";
    public static final String NON_SOLDE = "Non soldé";
    static final long MONTANT_MAX = 100_000_000L;
    static final List<String> MOYENS = List.of("Espèces", "Wave", "Orange Money", "Free Money", "Virement", "Chèque", "Autre");

    private final ComptesEcoleService comptes;
    private final StudentFinanceJpaRepository dossiers;
    private final TransactionJpaRepository transactions;

    public EcolagesService(ComptesEcoleService comptes, StudentFinanceJpaRepository dossiers, TransactionJpaRepository transactions) {
        this.comptes = comptes;
        this.dossiers = dossiers;
        this.transactions = transactions;
    }

    static String statut(long du, long paye) {
        if (paye > du) return EN_AVANCE;
        return paye == du ? EN_REGLE : NON_SOLDE;
    }

    static Map<String, Object> vue(StudentFinanceJpaEntity d) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", d.getId());
        m.put("studentId", d.getStudentId());
        m.put("studentName", d.getStudentName());
        m.put("classe", d.getClasse());
        m.put("totalRequired", d.getTotalRequired());
        m.put("paid", d.getPaid());
        m.put("debt", d.getDebt());
        m.put("status", d.getStatus());
        m.put("lastPaymentDate", d.getLastPaymentDate() == null ? null : d.getLastPaymentDate().toString());
        return m;
    }

    /** Corps : {eleveId, montantDu, classe?}. */
    @Transactional
    public Map<String, Object> ouvrirDossier(Map<String, Object> corps) {
        ChampsRequete.corpsRequis(corps);
        String eleveId = ChampsRequete.texte(corps, "eleveId", "L'élève", 255);
        long du = ChampsRequete.entier(corps, "montantDu", "Le montant dû", 1, MONTANT_MAX);
        String classe = ChampsRequete.texteOptionnel(corps, "classe", "La classe", 100);
        UserJpaEntity eleve = comptes.membre(eleveId, "ETUDIANT");   // 404 si l'élève n'est pas de cette école
        if (!dossiers.findAllByStudentIdOrderByCreatedAtDesc(eleveId).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Cet élève a déjà un dossier financier : encaissez ses versements sur ce dossier.");
        }
        StudentFinanceJpaEntity d = new StudentFinanceJpaEntity();
        d.setId(UUID.randomUUID().toString());
        d.setStudentId(eleveId);
        d.setStudentName(ComptesEcoleService.nomComplet(eleve));
        d.setClasse(classe);
        d.setTotalRequired(du);
        d.setPaid(0L);
        d.setDebt(du);
        d.setStatus(statut(du, 0));
        return vue(dossiers.save(d));
    }

    /** Corps : {eleveId, montant, moyen?, reference?, date? (AAAA-MM-JJ, pas dans le futur)}. */
    @Transactional
    public Map<String, Object> encaisser(Map<String, Object> corps) {
        ChampsRequete.corpsRequis(corps);
        String eleveId = ChampsRequete.texte(corps, "eleveId", "L'élève", 255);
        long montant = ChampsRequete.entier(corps, "montant", "Le montant", 1, MONTANT_MAX);
        String moyen = ChampsRequete.choix(corps, "moyen", "Le moyen de paiement", MOYENS, "Espèces");
        String reference = ChampsRequete.texteOptionnel(corps, "reference", "La référence", 60);
        String dateTexte = ChampsRequete.dateOptionnelle(corps, "date", "La date du versement");
        LocalDate date = dateTexte == null ? LocalDate.now() : LocalDate.parse(dateTexte.substring(0, 10));   // date seule retenue
        if (date.isAfter(LocalDate.now())) {
            throw ChampsRequete.refus("La date du versement ne peut pas être dans le futur.");
        }
        // dossier verrouillé : un encaissement simultané attend, puis repart du payé déjà recalculé
        StudentFinanceJpaEntity d = dossiers.verrouillerParEleve(eleveId).stream().findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Aucun dossier financier pour cet élève : ouvrez-le d'abord (montant dû)."));
        LocalDateTime quand = date.equals(LocalDate.now()) ? LocalDateTime.now() : date.atTime(LocalTime.NOON);
        TransactionJpaEntity t = new TransactionJpaEntity();
        t.setId(UUID.randomUUID().toString());
        t.setStudentFinanceId(d.getId());
        t.setStudentId(eleveId);
        t.setStudentName(d.getStudentName());
        t.setAmount(montant);
        t.setTransactionType(SituationFinanciereEleveService.TYPE_VERSEMENT);
        t.setPaymentMethod(moyen);
        t.setReference(reference);
        t.setDescription("Versement");
        t.setTransactionDate(quand);
        transactions.save(t);

        long du = d.getTotalRequired() == null ? 0 : d.getTotalRequired();
        long paye = (d.getPaid() == null ? 0 : d.getPaid()) + montant;
        d.setPaid(paye);
        d.setDebt(Math.max(0, du - paye));
        d.setStatus(statut(du, paye));
        if (d.getLastPaymentDate() == null || quand.isAfter(d.getLastPaymentDate())) {
            d.setLastPaymentDate(quand);
        }
        Map<String, Object> m = vue(dossiers.save(d));
        m.put("versementId", t.getId());
        return m;
    }
}
