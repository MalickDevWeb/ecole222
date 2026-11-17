package ecole_221.sn.ecole.modules.admissions.application.service;

import ecole_221.sn.ecole.modules.admissions.application.service.ReglesAdmissionService.Dossier;
import ecole_221.sn.ecole.modules.admissions.application.service.ReglesAdmissionService.Evaluation;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** 16/11 : règles d'admission créées par l'école, appliquées par le serveur au dépôt. */
class ReglesAdmissionServiceTest {

    private static final LocalDate AUJOURDHUI = LocalDate.of(2026, 11, 16);

    private static Map<String, Object> regle(String nom, String session, List<Map<String, Object>> conditions, Map<String, Object> effet) {
        Map<String, Object> r = new java.util.LinkedHashMap<>();
        r.put("nom", nom);
        r.put("session", session);
        r.put("conditions", conditions);
        r.put("effet", effet);
        return ReglesAdmissionService.valider(r);
    }

    private static Map<String, Object> cond(String champ, String op, String valeur) {
        return Map.of("champ", champ, "operateur", op, "valeur", valeur);
    }

    private static Dossier dossier(String session, String nationalite, String naissance) {
        return new Dossier(session, nationalite, "F", naissance, "S2", "Passable", "2024", false);
    }

    private static final Map<String, Object> PASSEPORT = regle("Passeport pour les étrangers", null,
            List.of(cond("nationalite", "different", "SN")), Map.of("type", "EXIGER_PIECE", "libelle", "Passeport", "formats", "PDF_IMAGE"));

    @Test
    void validationRefuseLesReglesIncompletes() {
        Map<String, Object> pieceVide = Map.of("type", "EXIGER_PIECE");
        Map<String, Object> bloquer = Map.of("type", "BLOQUER", "message", "Session réservée");
        assertThrows(ResponseStatusException.class, () -> regle("", null, List.of(), bloquer));
        assertThrows(ResponseStatusException.class, () -> regle("R", null, List.of(), pieceVide));
        assertThrows(ResponseStatusException.class, () -> regle("R", null, List.of(cond("nationalite", "egal", "XX")), bloquer));
        assertThrows(ResponseStatusException.class, () -> regle("R", null, List.of(cond("age", "different", "18")), bloquer));
        assertThrows(ResponseStatusException.class, () -> regle("R", null, List.of(cond("age", "inferieur", "deux")), bloquer));
        assertThrows(ResponseStatusException.class, () -> regle("R", null, List.of(cond("salaire", "egal", "1")), bloquer));
        assertThrows(ResponseStatusException.class, () -> regle("R", null, List.of(), Map.of("type", "EFFACER")));
        // une pièce déjà demandée par le formulaire ne peut pas être « exigée » une seconde fois
        assertThrows(ResponseStatusException.class, () -> regle("R", null, List.of(),
                Map.of("type", "EXIGER_PIECE", "libelle", "Diplôme", "formats", "PDF")));
        assertEquals("passeport", ((Map<?, ?>) PASSEPORT.get("effet")).get("code"));
        assertEquals("attestation-de-stage-ete", ReglesAdmissionService.codePiece("  Attestation de stage (été) "));
    }

    @Test
    void passeportExigeSeulementPourLesEtrangers() {
        Evaluation senegalaise = ReglesAdmissionService.evaluer(List.of(PASSEPORT), dossier("camp-1", "SN", "2005-01-01"), AUJOURDHUI);
        assertTrue(senegalaise.piecesExigees().isEmpty());
        Evaluation malienne = ReglesAdmissionService.evaluer(List.of(PASSEPORT), dossier("camp-1", "ml", "2005-01-01"), AUJOURDHUI);
        assertEquals(List.of("passeport"), malienne.piecesExigees().stream().map(ReglesAdmissionService.PieceExigee::code).toList());
        // nationalité non renseignée, ou texte libre d'un ancien formulaire : condition fausse
        assertTrue(ReglesAdmissionService.evaluer(List.of(PASSEPORT), dossier("camp-1", null, null), AUJOURDHUI).piecesExigees().isEmpty());
        assertTrue(ReglesAdmissionService.evaluer(List.of(PASSEPORT), dossier("camp-1", "Malienne", null), AUJOURDHUI).piecesExigees().isEmpty());
    }

    @Test
    void sessionAgeEtRegleDesactivee() {
        Map<String, Object> mineurs = regle("Mineurs", "camp-L2", List.of(cond("age", "inferieur", "18")),
                Map.of("type", "AVERTIR", "message", "Autorisation parentale à vérifier"));
        Dossier mineur16ans = dossier("camp-L2", "SN", "2009-11-17");
        assertEquals(List.of("Mineurs : Autorisation parentale à vérifier"),
                ReglesAdmissionService.evaluer(List.of(mineurs), mineur16ans, AUJOURDHUI).avertissements());
        // autre session : règle ignorée ; 18 ans pile : non mineur ; date absente : condition fausse
        assertTrue(ReglesAdmissionService.evaluer(List.of(mineurs), dossier("camp-M1", "SN", "2009-11-17"), AUJOURDHUI).avertissements().isEmpty());
        assertTrue(ReglesAdmissionService.evaluer(List.of(mineurs), dossier("camp-L2", "SN", "2008-11-16"), AUJOURDHUI).avertissements().isEmpty());
        assertTrue(ReglesAdmissionService.evaluer(List.of(mineurs), dossier("camp-L2", "SN", null), AUJOURDHUI).avertissements().isEmpty());
        mineurs.put("actif", false);
        assertTrue(ReglesAdmissionService.evaluer(List.of(mineurs), mineur16ans, AUJOURDHUI).avertissements().isEmpty());
    }

    @Test
    void conditionsCombineesParEtSansAccentsNiCasse() {
        Map<String, Object> r = regle("Série S et mention", null,
                List.of(cond("serieBac", "egal", "s2"), cond("mentionBac", "egal", "passable")),
                Map.of("type", "BLOQUER", "message", "Mention assez bien exigée pour cette session"));
        assertEquals(1, ReglesAdmissionService.evaluer(List.of(r), dossier(null, "SN", null), AUJOURDHUI).blocages().size());
        Dossier bien = new Dossier(null, "SN", "F", null, "S2", "Très bien", "2024", false);
        assertTrue(ReglesAdmissionService.evaluer(List.of(r), bien, AUJOURDHUI).blocages().isEmpty());
    }

    @Test
    void depotRefuseSiBlocageOuPieceManquanteOuMauvaisFormat() {
        Map<String, Object> pdfSeulement = regle("Attestation", null, List.of(),
                Map.of("type", "EXIGER_PIECE", "libelle", "Attestation de stage", "formats", "PDF"));
        Dossier d = dossier("camp-1", "ML", "2005-01-01");
        Evaluation ev = ReglesAdmissionService.evaluer(List.of(PASSEPORT, pdfSeulement), d, AUJOURDHUI);

        var manque = assertThrows(ResponseStatusException.class, () -> ReglesAdmissionService.appliquer(ev, d, Map.of()));
        assertTrue(manque.getReason().contains("Passeport"), manque.getReason());
        var format = assertThrows(ResponseStatusException.class, () -> ReglesAdmissionService.appliquer(ev, d, Map.of(
                "passeport", "https://res.cloudinary.com/x/image/upload/v1/p.png",
                "attestation-de-stage", "https://res.cloudinary.com/x/image/upload/v1/a.jpg")));
        assertTrue(format.getReason().contains("PDF"), format.getReason());
        assertThrows(ResponseStatusException.class, () -> ReglesAdmissionService.appliquer(ev, d, Map.of(
                "passeport", "javascript:alert(1).png", "attestation-de-stage", "https://x/a.pdf")));

        Map<String, String> ajouts = ReglesAdmissionService.appliquer(ev, d, Map.of(
                "passeport", "https://res.cloudinary.com/x/image/upload/v1/p.png",
                "attestation-de-stage", "https://res.cloudinary.com/x/image/upload/v1/a.pdf?dl=1"));
        assertEquals("camp-1", ajouts.get("campagneId"));
        assertEquals("Passeport", ajouts.get("pieceSuppLibelle.passeport"));
        assertTrue(ajouts.get("pieceSupp.attestation-de-stage").endsWith("a.pdf?dl=1"));

        Map<String, Object> bloque = regle("Fermé aux transferts", null, List.of(),
                Map.of("type", "BLOQUER", "message", "Cette session n'accepte pas les transferts."));
        Evaluation evb = ReglesAdmissionService.evaluer(List.of(bloque), d, AUJOURDHUI);
        var refus = assertThrows(ResponseStatusException.class, () -> ReglesAdmissionService.appliquer(evb, d, Map.of()));
        assertEquals("Cette session n'accepte pas les transferts.", refus.getReason());
    }

    @Test
    void depotParentEtAge() {
        assertEquals(17, ReglesAdmissionService.age("2009-01-01", AUJOURDHUI));
        assertNull(ReglesAdmissionService.age("2030-01-01", AUJOURDHUI));
        assertNull(ReglesAdmissionService.age("01/01/2009", AUJOURDHUI));
        Map<String, Object> parents = regle("Dépôt parent", null, List.of(cond("depotParent", "egal", "oui")),
                Map.of("type", "AVERTIR", "message", "Vérifier le lien de parenté"));
        Dossier parDepot = new Dossier(null, "SN", "F", null, null, null, null, true);
        assertEquals(1, ReglesAdmissionService.evaluer(List.of(parents), parDepot, AUJOURDHUI).avertissements().size());
        assertTrue(ReglesAdmissionService.evaluer(List.of(parents), dossier(null, "SN", null), AUJOURDHUI).avertissements().isEmpty());
    }
}
