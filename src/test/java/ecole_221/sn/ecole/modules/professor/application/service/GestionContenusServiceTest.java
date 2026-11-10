package ecole_221.sn.ecole.modules.professor.application.service;

import ecole_221.sn.ecole.modules.professor.infrastructure.persistence.*;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Lot D : modifier / supprimer les contenus sans jamais effacer en silence des données d'élèves. */
class GestionContenusServiceTest {

    private final ModuleJpaRepository modules = mock(ModuleJpaRepository.class);
    private final LessonJpaRepository supports = mock(LessonJpaRepository.class);
    private final QuizJpaRepository quiz = mock(QuizJpaRepository.class);
    private final TentativeQuizJpaRepository tentatives = mock(TentativeQuizJpaRepository.class);
    private final HomeworkJpaRepository devoirs = mock(HomeworkJpaRepository.class);
    private final ReminderJpaRepository rappels = mock(ReminderJpaRepository.class);
    private final GestionContenusService service =
            new GestionContenusService(modules, supports, quiz, tentatives, devoirs, rappels);
    private final Consumer<String> ok = c -> { };

    private QuizJpaEntity quiz1() {
        QuizJpaEntity q = new QuizJpaEntity();
        q.setId("quiz-1");
        q.setCourseId("c-1");
        q.setModuleId("m-1");
        q.setTitle("Quiz");
        when(quiz.findById("quiz-1")).thenReturn(Optional.of(q));
        when(quiz.save(any())).thenAnswer(i -> i.getArgument(0));
        return q;
    }

    @Test
    void quizDejaPasseNonModifiable() {
        quiz1();
        when(tentatives.countByQuizId("quiz-1")).thenReturn(2L);
        var e = assertThrows(ResponseStatusException.class, () -> service.modifierQuiz("quiz-1", Map.of("title", "X"), ok));
        assertEquals(409, e.getStatusCode().value());
        verify(quiz, never()).save(any());
    }

    @Test
    void quizSansTentativeModifiable() {
        quiz1();
        var v = service.modifierQuiz("quiz-1", Map.of("title", "Nouveau", "maxTentatives", 2), ok);
        assertEquals("Nouveau", v.get("title"));
        assertEquals(2, v.get("maxTentatives"));
    }

    @Test
    void suppressionDuQuizPasseExigeUneDemandeExplicite() {
        QuizJpaEntity q = quiz1();
        TentativeQuizJpaEntity t = new TentativeQuizJpaEntity();
        when(tentatives.findByQuizId("quiz-1")).thenReturn(List.of(t));
        assertEquals(409, assertThrows(ResponseStatusException.class, () -> service.supprimerQuiz("quiz-1", false, ok)).getStatusCode().value());
        verify(quiz, never()).delete(any());
        var res = service.supprimerQuiz("quiz-1", true, ok);
        assertEquals(1, res.get("tentativesSupprimees"));
        verify(tentatives).deleteAll(List.of(t));
        verify(quiz).delete(q);
    }

    @Test
    void moduleNonVideNonSupprime() {
        ModuleJpaEntity m = new ModuleJpaEntity();
        m.setId("m-1");
        m.setCourseId("c-1");
        when(modules.findById("m-1")).thenReturn(Optional.of(m));
        LessonJpaEntity l = new LessonJpaEntity();
        l.setModuleId("m-1");
        when(supports.findByCourseId("c-1")).thenReturn(List.of(l));
        assertEquals(409, assertThrows(ResponseStatusException.class, () -> service.supprimerModule("m-1", ok)).getStatusCode().value());
        verify(modules, never()).delete(any());
        when(supports.findByCourseId("c-1")).thenReturn(List.of());
        service.supprimerModule("m-1", ok);
        verify(modules).delete(m);
    }

    @Test
    void objetInexistant404EtCoursDUnCollegue403() {
        when(devoirs.findById("x")).thenReturn(Optional.empty());
        assertEquals(404, assertThrows(ResponseStatusException.class, () -> service.supprimerDevoir("x", ok)).getStatusCode().value());
        HomeworkJpaEntity h = new HomeworkJpaEntity();
        h.setId("h-1");
        h.setCourseId("c-b");
        when(devoirs.findById("h-1")).thenReturn(Optional.of(h));
        Consumer<String> interdit = c -> { throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Ce cours n'est pas attribué à votre compte."); };
        assertEquals(403, assertThrows(ResponseStatusException.class, () -> service.supprimerDevoir("h-1", interdit)).getStatusCode().value());
        verify(devoirs, never()).delete(any());
    }

    @Test
    void devoirEcheancePasseeRefusee() {
        HomeworkJpaEntity h = new HomeworkJpaEntity();
        h.setId("h-1");
        h.setCourseId("c-1");
        when(devoirs.findById("h-1")).thenReturn(Optional.of(h));
        when(devoirs.save(any())).thenAnswer(i -> i.getArgument(0));
        assertEquals(400, assertThrows(ResponseStatusException.class,
                () -> service.modifierDevoir("h-1", Map.of("deadline", "2020-01-01"), ok)).getStatusCode().value());
        assertEquals(400, assertThrows(ResponseStatusException.class,
                () -> service.modifierDevoir("h-1", Map.of("priority", "TRES_VITE"), ok)).getStatusCode().value());
        String demain = LocalDate.now().plusDays(1).toString();
        var v = service.modifierDevoir("h-1", Map.of("title", "TP 2", "priority", "haute", "deadline", demain), ok);
        assertEquals("HIGH", v.get("priority"));
        assertEquals(demain, v.get("deadline"));
    }
}
