package ecole_221.sn.ecole.modules.professor.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import ecole_221.sn.ecole.modules.admin.application.service.ComptesEcoleService;
import ecole_221.sn.ecole.modules.admin.infrastructure.persistence.InscriptionClasseJpaEntity;
import ecole_221.sn.ecole.modules.admin.infrastructure.persistence.InscriptionClasseJpaRepository;
import ecole_221.sn.ecole.modules.professor.infrastructure.persistence.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Lot D : un élève passe un quiz de sa classe ; limite de tentatives ; pas de correction avant la tentative. */
class QuizTentativeServiceTest {

    private final QuizJpaRepository quiz = mock(QuizJpaRepository.class);
    private final TentativeQuizJpaRepository tentatives = mock(TentativeQuizJpaRepository.class);
    private final CourseJpaRepository cours = mock(CourseJpaRepository.class);
    private final ModuleJpaRepository modules = mock(ModuleJpaRepository.class);
    private final InscriptionClasseJpaRepository inscriptions = mock(InscriptionClasseJpaRepository.class);
    private final ComptesEcoleService comptes = mock(ComptesEcoleService.class);
    private final QuizTentativeService service =
            new QuizTentativeService(quiz, tentatives, cours, modules, inscriptions, comptes);

    @BeforeEach
    void donnees() throws Exception {
        QuizJpaEntity q = new QuizJpaEntity();
        q.setId("quiz-1");
        q.setCourseId("c-1");
        q.setModuleId("m-1");
        q.setTitle("Quiz 1");
        q.setQuestionsJson(new ObjectMapper().writeValueAsString(ReglesQuizTest.QUESTIONS));
        when(quiz.findById("quiz-1")).thenReturn(Optional.of(q));
        when(quiz.findByCourseId("c-1")).thenReturn(List.of(q));
        when(cours.findById("c-1")).thenReturn(Optional.of(new CourseJpaEntity("c-1", "Algo", "classe-A", 30, "prof-a")));
        when(inscriptions.findFirstByUserId("eleve-a")).thenReturn(Optional.of(new InscriptionClasseJpaEntity("i1", "eleve-a", "classe-A")));
        when(inscriptions.findFirstByUserId("eleve-b")).thenReturn(Optional.of(new InscriptionClasseJpaEntity("i2", "eleve-b", "classe-B")));
        when(tentatives.save(any())).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void lEleveVoitLeQuizSansLesBonnesReponses() throws Exception {
        var liste = service.quizDuCoursPourEleve("c-1", "eleve-a");
        assertEquals(1, liste.size());
        String json = new ObjectMapper().writeValueAsString(liste);
        assertFalse(json.contains("correctAnswerIndex"));
        assertFalse(json.contains("bonneReponse"));
        assertEquals(true, liste.get(0).get("peutPasser"));
        assertEquals(false, liste.get(0).get("dejaFait"));
    }

    @Test
    void premiereTentativeCorrigeeEtEnregistree() {
        var res = service.passer("quiz-1", "eleve-a", Map.of("reponses", List.of(1, 0, 0)));
        assertEquals(2, res.get("score"));
        assertEquals(3, res.get("total"));
        assertEquals(0, res.get("tentativesRestantes"));
        assertNotNull(res.get("correction"), "tentatives épuisées : la correction est montrée");
        ArgumentCaptor<TentativeQuizJpaEntity> t = ArgumentCaptor.forClass(TentativeQuizJpaEntity.class);
        verify(tentatives).save(t.capture());
        assertEquals("[1,0,0]", t.getValue().getReponsesJson());
        assertEquals("eleve-a", t.getValue().getStudentId());
    }

    @Test
    void deuxiemeTentativeRefuseeAvec409() {
        when(tentatives.countByQuizIdAndStudentId("quiz-1", "eleve-a")).thenReturn(1L);
        var e = assertThrows(ResponseStatusException.class,
                () -> service.passer("quiz-1", "eleve-a", Map.of("reponses", List.of(1, 0, 1))));
        assertEquals(409, e.getStatusCode().value());
        verify(tentatives, never()).save(any());
    }

    @Test
    void tentativesConfigurablesEtCorrectionCacheeTantQuIlEnReste() {
        quiz.findById("quiz-1").orElseThrow().setMaxTentatives(2);
        var res = service.passer("quiz-1", "eleve-a", Map.of("reponses", List.of(1, 0, 1)));
        assertEquals(1, res.get("tentativesRestantes"));
        assertNull(res.get("correction"));
    }

    @Test
    void eleveDUneAutreClasseRefuse404() {
        var e = assertThrows(ResponseStatusException.class,
                () -> service.passer("quiz-1", "eleve-b", Map.of("reponses", List.of(1, 0, 1))));
        assertEquals(404, e.getStatusCode().value());
        assertEquals(404, assertThrows(ResponseStatusException.class,
                () -> service.quizDuCoursPourEleve("c-1", "eleve-b")).getStatusCode().value());
        assertEquals(404, assertThrows(ResponseStatusException.class,
                () -> service.passer("quiz-1", "sans-classe", Map.of("reponses", List.of(1, 0, 1)))).getStatusCode().value());
    }

    @Test
    void reponsesIncompletesRefusees400SansEnregistrement() {
        var e = assertThrows(ResponseStatusException.class,
                () -> service.passer("quiz-1", "eleve-a", Map.of("reponses", List.of(1))));
        assertEquals(400, e.getStatusCode().value());
        verify(tentatives, never()).save(any());
    }

    @Test
    void pasDeResultatNiDeCorrectionAvantLaTentative() {
        var e = assertThrows(ResponseStatusException.class, () -> service.mesResultats("quiz-1", "eleve-a"));
        assertEquals(404, e.getStatusCode().value());
    }

    @Test
    void resultatRelu() {
        TentativeQuizJpaEntity t = new TentativeQuizJpaEntity();
        t.setId("t1");
        t.setQuizId("quiz-1");
        t.setStudentId("eleve-a");
        t.setReponsesJson("[1,0,0]");
        t.setScore(2);
        t.setTotal(3);
        t.setDateTentative(LocalDateTime.now());
        when(tentatives.findByQuizIdAndStudentIdOrderByDateTentativeAsc("quiz-1", "eleve-a")).thenReturn(List.of(t));
        var res = service.mesResultats("quiz-1", "eleve-a");
        assertEquals(1, res.get("tentativesUtilisees"));
        @SuppressWarnings("unchecked")
        var correction = (List<Map<String, Object>>) res.get("correction");
        assertEquals(3, correction.size());
        assertEquals(false, correction.get(2).get("correcte"));
        assertEquals(1, correction.get(2).get("bonneReponse"));
    }

    @Test
    void resultatsProfesseur() {
        TentativeQuizJpaEntity t = new TentativeQuizJpaEntity();
        t.setId("t1");
        t.setQuizId("quiz-1");
        t.setStudentId("eleve-a");
        t.setReponsesJson("[1,0,0]");
        t.setScore(2);
        t.setTotal(3);
        t.setDateTentative(LocalDateTime.now());
        when(tentatives.findByQuizId("quiz-1")).thenReturn(List.of(t));
        when(inscriptions.findByClasseId("classe-A")).thenReturn(List.of(
                new InscriptionClasseJpaEntity("i1", "eleve-a", "classe-A"), new InscriptionClasseJpaEntity("i3", "eleve-c", "classe-A")));
        var ua = new ecole_221.sn.ecole.modules.auth.infrastructure.persistence.UserJpaEntity();
        ua.setId("eleve-a");
        var uc = new ecole_221.sn.ecole.modules.auth.infrastructure.persistence.UserJpaEntity();
        uc.setId("eleve-c");
        when(comptes.membres("ETUDIANT")).thenReturn(List.of(Map.entry(ua, mock(ecole_221.sn.ecole.modules.platform.organization.infrastructure.persistence.OrganizationUserJpaEntity.class)),
                Map.entry(uc, mock(ecole_221.sn.ecole.modules.platform.organization.infrastructure.persistence.OrganizationUserJpaEntity.class))));
        var r = service.resultats("c-1", "quiz-1");
        @SuppressWarnings("unchecked")
        var synthese = (Map<String, Object>) r.get("synthese");
        assertEquals(1L, synthese.get("nombreFaits"));
        assertEquals(1L, synthese.get("nombreNonFaits"));
        assertEquals(2.0, synthese.get("moyenneScore"));
        @SuppressWarnings("unchecked")
        var questions = (List<Map<String, Object>>) r.get("questions");
        assertEquals(100.0, questions.get(0).get("tauxReussite"));
        assertEquals(0.0, questions.get(2).get("tauxReussite"));
        assertEquals(404, assertThrows(ResponseStatusException.class, () -> service.resultats("c-2", "quiz-1")).getStatusCode().value());
    }
}
