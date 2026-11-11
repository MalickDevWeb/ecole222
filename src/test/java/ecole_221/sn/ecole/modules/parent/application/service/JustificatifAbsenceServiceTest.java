package ecole_221.sn.ecole.modules.parent.application.service;

import ecole_221.sn.ecole.modules.auth.infrastructure.persistence.UserJpaRepository;
import ecole_221.sn.ecole.modules.parent.infrastructure.persistence.JustificatifAbsenceJpaEntity;
import ecole_221.sn.ecole.modules.parent.infrastructure.persistence.JustificatifAbsenceJpaRepository;
import ecole_221.sn.ecole.modules.parent.infrastructure.persistence.ParentEnfantJpaEntity;
import ecole_221.sn.ecole.modules.parent.infrastructure.persistence.ParentEnfantJpaRepository;
import ecole_221.sn.ecole.modules.professor.infrastructure.persistence.CourseJpaRepository;
import ecole_221.sn.ecole.modules.student.infrastructure.persistence.AttendanceJpaEntity;
import ecole_221.sn.ecole.modules.student.infrastructure.persistence.AttendanceJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;

/** Justificatifs d'absence : le parent dépose, l'administration accepte (absence → excusée) ou refuse. */
class JustificatifAbsenceServiceTest {

    private JustificatifAbsenceJpaRepository justificatifs;
    private AttendanceJpaRepository presences;
    private JustificatifAbsenceService service;
    private final Map<String, JustificatifAbsenceJpaEntity> base = new HashMap<>();

    @BeforeEach
    void preparer() {
        justificatifs = Mockito.mock(JustificatifAbsenceJpaRepository.class);
        presences = Mockito.mock(AttendanceJpaRepository.class);
        ParentEnfantJpaRepository filiations = Mockito.mock(ParentEnfantJpaRepository.class);
        Clock horloge = Clock.fixed(Instant.parse("2026-11-10T09:00:00Z"), ZoneId.of("UTC"));
        service = new JustificatifAbsenceService(justificatifs, presences, filiations,
                Mockito.mock(CourseJpaRepository.class), Mockito.mock(UserJpaRepository.class), horloge);

        ParentEnfantJpaEntity lien = new ParentEnfantJpaEntity();
        lien.setParentId("par-1");
        lien.setEnfantId("e1");
        Mockito.when(filiations.findByParentId("par-1")).thenReturn(List.of(lien));
        Mockito.when(filiations.findByParentId("par-2")).thenReturn(List.of());
        presence("abs-1", "e1", "ABSENT");
        presence("ret-1", "e1", "LATE");
        presence("pre-1", "e1", "PRESENT");
        presence("exc-1", "e1", "EXCUSED");
        presence("autre", "e2", "ABSENT");
        Mockito.when(presences.findById("inconnue")).thenReturn(Optional.empty());

        Mockito.when(justificatifs.save(any())).thenAnswer(i -> { JustificatifAbsenceJpaEntity j = i.getArgument(0); base.put(j.getId(), j); return j; });
        Mockito.when(justificatifs.findById(Mockito.anyString())).thenAnswer(i -> Optional.ofNullable(base.get(i.<String>getArgument(0))));
        Mockito.when(justificatifs.findByPresenceId(Mockito.anyString())).thenAnswer(i ->
                base.values().stream().filter(j -> j.getPresenceId().equals(i.getArgument(0))).toList());
        Mockito.when(justificatifs.findByEleveId(Mockito.anyString())).thenAnswer(i ->
                base.values().stream().filter(j -> j.getEleveId().equals(i.getArgument(0))).toList());
    }

    private AttendanceJpaEntity presence(String id, String eleve, String statut) {
        AttendanceJpaEntity a = new AttendanceJpaEntity();
        a.setId(id); a.setStudentId(eleve); a.setStatus(statut); a.setCourseId("c1"); a.setDate("2026-11-09");
        Mockito.when(presences.findById(id)).thenReturn(Optional.of(a));
        return a;
    }

    private static HttpStatus code(Runnable r) {
        ResponseStatusException e = assertThrows(ResponseStatusException.class, r::run);
        return HttpStatus.valueOf(e.getStatusCode().value());
    }

    @Test
    void le_parent_justifie_l_absence_de_son_enfant_et_c_est_enregistre() {
        Map<String, Object> j = service.deposer("par-1", "abs-1", "  Fièvre, certificat médical  ", "https://res.cloudinary.com/x/certif.pdf");
        assertEquals("EN_ATTENTE", j.get("statut"));
        assertEquals("Fièvre, certificat médical", j.get("motif"));
        assertEquals("ABSENT", j.get("statutPresence"));
        assertTrue(String.valueOf(j.get("id")).startsWith("jus-"));
        assertEquals(1, base.size(), "le justificatif est réellement enregistré");
    }

    @Test
    void erreurs_du_parent() {
        assertEquals(HttpStatus.NOT_FOUND, code(() -> service.deposer("par-1", "inconnue", "x", null)));
        assertEquals(HttpStatus.FORBIDDEN, code(() -> service.deposer("par-1", "autre", "x", null)), "enfant d'un autre parent");
        assertEquals(HttpStatus.FORBIDDEN, code(() -> service.deposer("par-2", "abs-1", "x", null)), "parent sans lien");
        assertEquals(HttpStatus.CONFLICT, code(() -> service.deposer("par-1", "pre-1", "x", null)), "présence : rien à justifier");
        assertEquals(HttpStatus.CONFLICT, code(() -> service.deposer("par-1", "exc-1", "x", null)), "déjà excusée");
        assertEquals(HttpStatus.BAD_REQUEST, code(() -> service.deposer("par-1", "abs-1", "   ", null)), "motif vide");
        assertEquals(HttpStatus.BAD_REQUEST, code(() -> service.deposer("par-1", "abs-1", "x".repeat(501), null)), "motif trop long");
        assertEquals(HttpStatus.BAD_REQUEST, code(() -> service.deposer("par-1", "abs-1", "x", "javascript:alert(1)")), "lien non http(s)");
        assertTrue(base.isEmpty(), "aucune erreur n'enregistre quoi que ce soit");
    }

    @Test
    void pas_de_doublon_tant_qu_un_justificatif_est_en_attente_ou_accepte() {
        service.deposer("par-1", "abs-1", "Malade", null);
        assertEquals(HttpStatus.CONFLICT, code(() -> service.deposer("par-1", "abs-1", "Encore", null)));
    }

    @Test
    void accepte_l_absence_devient_excusee_dans_l_appel() {
        String id = (String) service.deposer("par-1", "abs-1", "Malade", null).get("id");
        Map<String, Object> j = service.accepter("adm-1", id);
        assertEquals("ACCEPTE", j.get("statut"));
        assertEquals("EXCUSED", presences.findById("abs-1").orElseThrow().getStatus(), "même valeur que la feuille d'appel");
        assertEquals(HttpStatus.CONFLICT, code(() -> service.accepter("adm-1", id)), "déjà traité");
        assertEquals(HttpStatus.CONFLICT, code(() -> service.deposer("par-1", "abs-1", "x", null)), "absence désormais excusée");
    }

    @Test
    void accepte_un_retard_reste_un_retard() {
        String id = (String) service.deposer("par-1", "ret-1", "Bus en panne", null).get("id");
        service.accepter("adm-1", id);
        assertEquals("LATE", presences.findById("ret-1").orElseThrow().getStatus());
    }

    @Test
    void refus_motif_obligatoire_puis_nouveau_depot_possible() {
        String id = (String) service.deposer("par-1", "abs-1", "Malade", null).get("id");
        assertEquals(HttpStatus.BAD_REQUEST, code(() -> service.refuser("adm-1", id, " ")));
        Map<String, Object> j = service.refuser("adm-1", id, "Certificat illisible");
        assertEquals("REFUSE", j.get("statut"));
        assertEquals("Certificat illisible", j.get("motifRefus"));
        assertEquals("ABSENT", presences.findById("abs-1").orElseThrow().getStatus(), "un refus ne change pas l'appel");
        assertEquals("EN_ATTENTE", service.deposer("par-1", "abs-1", "Nouveau certificat", null).get("statut"));
        assertEquals("EN_ATTENTE", service.parPresence("e1").get("abs-1").get("statut"), "l'élève voit le dernier dépôt");
    }

    @Test
    void administration_statut_de_filtre_inconnu_et_justificatif_inconnu() {
        assertEquals(HttpStatus.BAD_REQUEST, code(() -> service.liste("PEUT_ETRE")));
        assertEquals(HttpStatus.NOT_FOUND, code(() -> service.accepter("adm-1", "jus-inexistant")));
    }
}
