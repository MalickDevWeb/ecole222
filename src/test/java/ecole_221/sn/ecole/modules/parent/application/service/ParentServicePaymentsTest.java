package ecole_221.sn.ecole.modules.parent.application.service;

import ecole_221.sn.ecole.modules.parent.application.port.dto.PaymentDto;
import ecole_221.sn.ecole.modules.parent.domain.repository.AppointmentRepositoryPort;
import ecole_221.sn.ecole.modules.parent.domain.repository.ParentRepositoryPort;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;

/**
 * APP-BE-18 : GET /parent/paiements ne doit jamais renvoyer de transaction inventée.
 */
class ParentServicePaymentsTest {

    @Test
    void getParentPayments_sansSourceReelle_renvoieAucuneEcheanceEtStatutNonDisponible() {
        ParentRepositoryPort parentRepo = Mockito.mock(ParentRepositoryPort.class);
        AppointmentRepositoryPort appointmentRepo = Mockito.mock(AppointmentRepositoryPort.class);
        ParentService service = new ParentService(parentRepo, appointmentRepo);

        PaymentDto result = service.getParentPayments("parent-1");

        assertNotNull(result);
        assertNotNull(result.echeances());
        assertTrue(result.echeances().isEmpty(), "aucune échéance fictive ne doit être renvoyée");
        assertEquals(0L, result.totalAnnuel());
        assertEquals("non_disponible", result.statut());
        Mockito.verifyNoInteractions(parentRepo, appointmentRepo);
    }
}
