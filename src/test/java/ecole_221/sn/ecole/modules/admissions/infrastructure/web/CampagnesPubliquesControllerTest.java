package ecole_221.sn.ecole.modules.admissions.infrastructure.web;

import ecole_221.sn.ecole.infrastructure.config.SecurityConfigAcces;
import ecole_221.sn.ecole.modules.admissions.infrastructure.persistence.ElementPortailAdmissionJpaEntity;
import ecole_221.sn.ecole.modules.admissions.infrastructure.persistence.ElementPortailAdmissionJpaRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/** Portail public : campagnes publiées seulement, sans champ interne ; /admissions/db reste fermé. */
class CampagnesPubliquesControllerTest {

    private static ElementPortailAdmissionJpaEntity campagne(String json) {
        ElementPortailAdmissionJpaEntity e = mock(ElementPortailAdmissionJpaEntity.class);
        when(e.getContenuJson()).thenReturn(json);
        when(e.getId()).thenReturn("el");
        return e;
    }

    @Test
    void seulesLesCampagnesPublieesAvecChampsAutorises() {
        ElementPortailAdmissionJpaRepository repo = mock(ElementPortailAdmissionJpaRepository.class);
        List<ElementPortailAdmissionJpaEntity> stockees = List.of(
                campagne("{\"id\":\"c1\",\"title\":\"Licence Informatique\",\"state\":\"Ouverte\",\"fees\":25000,\"notesInternes\":\"jury: X\"}"),
                campagne("{\"id\":\"c2\",\"title\":\"Brouillon\",\"state\":\"Brouillon\"}"),
                campagne("{\"id\":\"c3\",\"title\":\"Ancienne\",\"state\":\"Archivée\"}"),
                campagne("pas du json"));
        when(repo.findByNatureOrderByPositionAsc("CAMPAGNE")).thenReturn(stockees);
        List<Map<String, Object>> r = new CampagnesPubliquesController(repo).campagnes().getBody();
        assertThat(r).hasSize(1);
        assertThat(r.get(0)).containsEntry("id", "c1").containsEntry("fees", 25000).doesNotContainKey("notesInternes");
    }

    @Test
    void routesPubliquesDuPortail() {
        assertThat(SecurityConfigAcces.lecturePublique()).contains("/candidatures/campagnes").doesNotContain("/admissions/db");
        assertThat(SecurityConfigAcces.ecriturePublique()).contains("/candidatures/suivi").doesNotContain("/admissions/db");
    }
}
