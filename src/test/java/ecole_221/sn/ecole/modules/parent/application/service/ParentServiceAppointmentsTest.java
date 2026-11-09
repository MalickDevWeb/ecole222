package ecole_221.sn.ecole.modules.parent.application.service;

import ecole_221.sn.ecole.modules.parent.domain.model.Appointment;
import ecole_221.sn.ecole.modules.parent.domain.repository.AppointmentRepositoryPort;
import ecole_221.sn.ecole.modules.parent.domain.repository.ParentRepositoryPort;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;

/** APP-SEC-14 : un parent ne reçoit jamais les rendez-vous d'une autre famille, ni un rendez-vous inventé. */
class ParentServiceAppointmentsTest {

    private final AppointmentRepositoryPort rdv = Mockito.mock(AppointmentRepositoryPort.class);
    private final ParentService service = new ParentService(Mockito.mock(ParentRepositoryPort.class), rdv);

    @Test
    void parentSansRendezVousNeRecoitRienEtRienNestCree() {
        Mockito.when(rdv.findByParentId("p1")).thenReturn(List.of());
        Mockito.when(rdv.findAll()).thenReturn(List.of(
                Appointment.create("autre-famille", "prof", "Prof", "2026-11-10", "motif privé")));

        assertTrue(service.getParentAppointments("p1").isEmpty());
        Mockito.verify(rdv, Mockito.never()).findAll();
        Mockito.verify(rdv, Mockito.never()).save(any());
    }
}
