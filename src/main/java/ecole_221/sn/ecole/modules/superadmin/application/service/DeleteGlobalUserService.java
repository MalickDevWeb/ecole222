package ecole_221.sn.ecole.modules.superadmin.application.service;

import ecole_221.sn.ecole.modules.auth.domain.model.User;
import ecole_221.sn.ecole.modules.auth.domain.port.repository.UserRepositoryPort;
import ecole_221.sn.ecole.modules.auth.domain.valueobject.RoleUtilisateur;
import ecole_221.sn.ecole.modules.superadmin.application.port.usecase.DeleteGlobalUserUseCase;
import ecole_221.sn.ecole.modules.superadmin.domain.exception.GlobalUserNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * Suppression d'un compte de la plateforme.
 *
 * <p>Garde-fous (test du 2026-11-10 : le super-admin a pu supprimer SON PROPRE compte, la plateforme
 * n'avait plus aucun super-admin) :</p>
 * <ul>
 *   <li>on ne peut pas supprimer le compte avec lequel on est connecté ;</li>
 *   <li>on ne peut pas supprimer le dernier SUPER_ADMIN.</li>
 * </ul>
 */
@Service
public class DeleteGlobalUserService implements DeleteGlobalUserUseCase {

    private final UserRepositoryPort userRepositoryPort;

    public DeleteGlobalUserService(UserRepositoryPort userRepositoryPort) {
        this.userRepositoryPort = userRepositoryPort;
    }

    @Override
    public void execute(String userId) {
        User cible = userRepositoryPort.findById(userId)
                .orElseThrow(() -> new GlobalUserNotFoundException(userId));

        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof User moi && moi.getId().equals(cible.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Vous ne pouvez pas supprimer le compte avec lequel vous êtes connecté.");
        }
        if (RoleUtilisateur.SUPER_ADMIN.equals(cible.getRole())
                && userRepositoryPort.findByRole(RoleUtilisateur.SUPER_ADMIN).size() <= 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Impossible de supprimer le dernier super-administrateur de la plateforme.");
        }
        userRepositoryPort.deleteById(userId);
    }
}
