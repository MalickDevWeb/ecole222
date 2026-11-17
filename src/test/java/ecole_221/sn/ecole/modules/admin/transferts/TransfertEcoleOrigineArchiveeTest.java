package ecole_221.sn.ecole.modules.admin.transferts;

import ecole_221.sn.ecole.modules.auth.infrastructure.persistence.UserJpaRepository;
import ecole_221.sn.ecole.modules.platform.organization.application.StudentTransferApplicationService;
import ecole_221.sn.ecole.modules.platform.organization.domain.model.Organization;
import ecole_221.sn.ecole.modules.platform.organization.domain.model.OrganizationFactory;
import ecole_221.sn.ecole.modules.platform.organization.domain.port.repository.OrganizationRepositoryPort;
import ecole_221.sn.ecole.modules.platform.organization.domain.port.repository.OrganizationUserRepositoryPort;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Étude d'archivage, risque R15 : une demande de transfert en attente dont l'école d'origine a été archivée ne peut
 * plus être acceptée (avant : dossier lu dans des tables vidées, élève reçu avec un dossier vide, sans erreur).
 */
class TransfertEcoleOrigineArchiveeTest {

    private static Organization ecole(String id, String statut) {
        return OrganizationFactory.reconstruct(id, "École " + id, "ECOLE", statut, id, null, null, null, null, null, null,
                null, null, null, null, LocalDateTime.of(2026, 1, 1, 0, 0), null, null, null);
    }

    private final OrganizationRepositoryPort ecoles = mock(OrganizationRepositoryPort.class);
    private final TransfertsElevesService service = new TransfertsElevesService(mock(DemandeTransfertJpaRepository.class),
            ecoles, mock(OrganizationUserRepositoryPort.class), mock(UserJpaRepository.class),
            mock(StudentTransferApplicationService.class));

    @Test
    void ecoleDOrigineArchiveeRefuseeEn409() {
        when(ecoles.findById("org-archivee")).thenReturn(Optional.of(ecole("org-archivee", "ARCHIVED")));
        assertThatThrownBy(() -> service.ecoleOrigine("org-archivee")).isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("l'école d'origine est archivée")
                .extracting(e -> ((ResponseStatusException) e).getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void ecoleDOrigineActiveOuSuspendueAcceptee() {
        when(ecoles.findById("org-active")).thenReturn(Optional.of(ecole("org-active", "ACTIVE")));
        when(ecoles.findById("org-suspendue")).thenReturn(Optional.of(ecole("org-suspendue", "SUSPENDED")));
        assertThat(service.ecoleOrigine("org-active").getId()).isEqualTo("org-active");
        assertThat(service.ecoleOrigine("org-suspendue").getId()).isEqualTo("org-suspendue");   // données encore en place
    }
}
