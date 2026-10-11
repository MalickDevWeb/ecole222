package ecole_221.sn.ecole.modules.superadmin.application.service;

import ecole_221.sn.ecole.modules.auth.domain.port.repository.UserRepositoryPort;
import ecole_221.sn.ecole.modules.superadmin.application.port.usecase.DeleteGlobalUserUseCase;
import ecole_221.sn.ecole.modules.superadmin.domain.exception.GlobalUserNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class DeleteGlobalUserService implements DeleteGlobalUserUseCase {

    private final UserRepositoryPort userRepositoryPort;

    public DeleteGlobalUserService(UserRepositoryPort userRepositoryPort) {
        this.userRepositoryPort = userRepositoryPort;
    }

    @Override
    public void execute(String userId) {
        // Validation métier : l'utilisateur doit exister
        userRepositoryPort.findById(userId)
                .orElseThrow(() -> new GlobalUserNotFoundException(userId));

        userRepositoryPort.deleteById(userId);
    }
}
