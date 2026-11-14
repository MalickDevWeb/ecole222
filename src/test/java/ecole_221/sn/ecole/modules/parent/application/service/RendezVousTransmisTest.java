package ecole_221.sn.ecole.modules.parent.application.service;

import ecole_221.sn.ecole.modules.parent.application.port.dto.AppointmentRequestDto;
import ecole_221.sn.ecole.modules.parent.domain.model.Appointment;
import ecole_221.sn.ecole.modules.parent.domain.repository.AppointmentRepositoryPort;
import ecole_221.sn.ecole.modules.parent.domain.repository.ParentRepositoryPort;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * 2026-11-14 : une demande de rendez-vous n'est plus « confirmée » d'office avec un lien Meet inventé ; elle reste en
 * attente et arrive réellement chez le professeur, par la messagerie parents ↔ école.
 */
class RendezVousTransmisTest {

    private final AppointmentRepositoryPort rdv = mock(AppointmentRepositoryPort.class);
    private final MessagerieParentsService messagerie = mock(MessagerieParentsService.class);
    private final ParentService service = new ParentService(mock(ParentRepositoryPort.class), rdv);

    @Test
    void demandeEnAttenteSansLienEtTransmiseAuProfesseur() {
        service.setMessagerie(messagerie);
        when(rdv.save(any())).thenAnswer(i -> i.getArgument(0));
        String demain = LocalDate.now().plusDays(1).toString();
        service.requestAppointment("p1", new AppointmentRequestDto("prof-1", demain, "Parler des notes de Awa"));

        ArgumentCaptor<Appointment> a = ArgumentCaptor.forClass(Appointment.class);
        verify(rdv).save(a.capture());
        assertThat(a.getValue().getStatus()).isEqualTo("EN_ATTENTE");
        assertThat(a.getValue().getMeetUrl()).isNull();
        verify(messagerie).envoyerParParent(eq("p1"), eq("prof-1"), contains("Demande de rendez-vous"), contains("Parler des notes de Awa"));
    }

    @Test
    void plusAucunLienInventeALaCreation() {
        Appointment a = Appointment.create("p1", "prof-1", "Prof", "2026-12-01", "motif");
        assertThat(a.getMeetUrl()).isNull();
        assertThat(a.getStatus()).isEqualTo(Appointment.STATUT_EN_ATTENTE);
    }
}
