package ecole_221.sn.ecole.modules.notifications.application;

import ecole_221.sn.ecole.modules.admin.infrastructure.persistence.InscriptionClasseJpaEntity;
import ecole_221.sn.ecole.modules.admin.infrastructure.persistence.InscriptionClasseJpaRepository;
import ecole_221.sn.ecole.modules.notifications.infrastructure.persistence.NotificationJpaEntity;
import ecole_221.sn.ecole.modules.notifications.infrastructure.persistence.NotificationJpaRepository;
import ecole_221.sn.ecole.modules.planning.infrastructure.persistence.ClasseJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;

class NotificationsUtilisateurServiceTest {

    private final NotificationJpaRepository notifications = Mockito.mock(NotificationJpaRepository.class);
    private final InscriptionClasseJpaRepository inscriptions = Mockito.mock(InscriptionClasseJpaRepository.class);
    private final ClasseJpaRepository classes = Mockito.mock(ClasseJpaRepository.class);
    private final NotificationsUtilisateurService service = new NotificationsUtilisateurService(notifications, inscriptions, classes);

    @BeforeEach
    void preparer() {
        Mockito.when(classes.existsById("tle-s")).thenReturn(true);
        Mockito.when(inscriptions.findByClasseId("tle-s")).thenReturn(List.of(
                new InscriptionClasseJpaEntity("i1", "eleve-1", "tle-s"),
                new InscriptionClasseJpaEntity("i2", "eleve-2", "tle-s"),
                new InscriptionClasseJpaEntity("i3", "eleve-1", "tle-s")));
    }

    @Test
    void chaqueEleveInscritEstPrevenuUneSeuleFois() {
        int n = service.notifierClasse("tle-s", "  Cours annulé  ", "Le cours de maths est annulé.", "cancel");

        assertThat(n).isEqualTo(2);
        ArgumentCaptor<NotificationJpaEntity> envoi = ArgumentCaptor.forClass(NotificationJpaEntity.class);
        Mockito.verify(notifications, Mockito.times(2)).save(envoi.capture());
        assertThat(envoi.getAllValues()).extracting(NotificationJpaEntity::getRecipientId).containsExactly("eleve-1", "eleve-2");
        assertThat(envoi.getValue().getTitle()).isEqualTo("Cours annulé");
        assertThat(envoi.getValue().getCategory()).isEqualTo("cancel");
    }

    @Test
    void doubleClicNeDupliquePasLaNotification() {
        Mockito.when(notifications.existsByRecipientIdAndTitleAndMessageAndCreatedAtAfter(eq("eleve-1"), anyString(), anyString(), any()))
                .thenReturn(true);

        service.notifierClasse("tle-s", "Cours annulé", "Texte", "cancel");

        Mockito.verify(notifications, Mockito.times(1)).save(any());
    }

    @Test
    void entreesAbsurdesRefuseesAvecUnMessageExplicite() {
        assertThatThrownBy(() -> service.notifierClasse(null, "t", "d", null)).isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("classe concernée est obligatoire");
        assertThatThrownBy(() -> service.notifierClasse("tle-s", "   ", "d", null)).hasMessageContaining("titre est obligatoire");
        assertThatThrownBy(() -> service.notifierClasse("tle-s", "A".repeat(201), "d", null)).hasMessageContaining("200 caractères");
        assertThatThrownBy(() -> service.notifierClasse("tle-s", "t", "D".repeat(2001), null)).hasMessageContaining("2000 caractères");
        assertThatThrownBy(() -> service.notifierClasse("inconnue", "t", "d", null)).hasMessageContaining("Classe inconnue");
        Mockito.verify(notifications, Mockito.never()).save(any());
    }

    @Test
    void genreInconnuRamenéAInfo() {
        service.notifierClasse("tle-s", "t", "d", "<script>");
        ArgumentCaptor<NotificationJpaEntity> envoi = ArgumentCaptor.forClass(NotificationJpaEntity.class);
        Mockito.verify(notifications, Mockito.atLeastOnce()).save(envoi.capture());
        assertThat(envoi.getValue().getCategory()).isEqualTo("info");
    }

    @Test
    void lectureLimiteeEtPlusRecentesDabord() {
        NotificationJpaEntity ancienne = new NotificationJpaEntity("a", "eleve-1", "ETUDIANT", "Ancienne", "x", "info", "schedule", "normal");
        ancienne.setCreatedAt(LocalDateTime.now().minusDays(1));
        NotificationJpaEntity recente = new NotificationJpaEntity("b", "eleve-1", "ETUDIANT", "Récente", "y", "info", "cancel", "normal");
        recente.setCreatedAt(LocalDateTime.now());
        Mockito.when(notifications.findByRecipientId("eleve-1")).thenReturn(List.of(ancienne, recente));

        List<Map<String, Object>> lues = service.mesNotifications("eleve-1", 1);

        assertThat(lues).hasSize(1);
        assertThat(lues.get(0)).containsEntry("title", "Récente").containsEntry("icon", "cancel").containsEntry("desc", "y");
        assertThat(service.mesNotifications("eleve-1", -5)).hasSize(1);
    }
}
