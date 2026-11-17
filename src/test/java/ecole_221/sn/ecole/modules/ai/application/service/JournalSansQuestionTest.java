package ecole_221.sn.ecole.modules.ai.application.service;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import ecole_221.sn.ecole.modules.ai.infrastructure.adapter.AIServiceAdapter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** APP-SEC-26 : la question d'un élève n'est jamais écrite dans les journaux (loi n° 2008-12). */
class JournalSansQuestionTest {

    private static final String QUESTION = "Je suis Awa Ndiaye, 77 123 45 67, ma mère est malade";
    private final AIServiceAdapter adaptateur = mock(AIServiceAdapter.class);
    private final AIService service = new AIService(adaptateur);
    private final ListAppender<ILoggingEvent> journal = new ListAppender<>();
    private final Logger logger = (Logger) LoggerFactory.getLogger(AIService.class);

    @BeforeEach
    void capter() {
        journal.start();
        logger.addAppender(journal);
        when(adaptateur.generateResponse(anyString(), any())).thenReturn("réponse");
    }

    @AfterEach
    void lacher() {
        logger.detachAppender(journal);
    }

    @Test
    void niLaQuestionNiLeSujetDansLesJournaux() {
        service.generateTutorResponse(QUESTION);
        service.generateTutorResponseWithContext(QUESTION, Map.of());
        service.generateEducationalContent("Révolution de 1848 chez Awa Ndiaye", "quiz");
        assertThat(journal.list).isNotEmpty();
        assertThat(journal.list).allSatisfy(e -> {
            assertThat(e.getFormattedMessage()).doesNotContain("Awa").doesNotContain("77 123").doesNotContain("malade");
        });
        assertThat(journal.list.get(0).getFormattedMessage()).contains(QUESTION.length() + " caractères");
    }
}
