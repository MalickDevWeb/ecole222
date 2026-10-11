package ecole_221.sn.ecole.modules.superadmin.infrastructure.web;

import ecole_221.sn.ecole.modules.superadmin.application.port.command.CreateGlobalUserCommand;
import ecole_221.sn.ecole.modules.superadmin.application.port.dto.SuperAdminUserDto;
import ecole_221.sn.ecole.modules.superadmin.application.port.usecase.CreateGlobalUserUseCase;
import ecole_221.sn.ecole.modules.superadmin.application.port.usecase.DeleteGlobalUserUseCase;
import ecole_221.sn.ecole.modules.superadmin.application.port.usecase.GetGlobalUserUseCase;
import ecole_221.sn.ecole.modules.superadmin.application.port.usecase.ListGlobalUsersUseCase;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller pour la gestion des utilisateurs globaux.
 * Accessible uniquement par les rôles SUPER_ADMIN et PLATFORM_ADMIN.
 */
@RestController
@RequestMapping({"/v1/super-admin/users", "/v1/platform/users", "/api/v1/super-admin/users", "/api/v1/platform/users"})
@PreAuthorize("hasAnyRole('SUPER_ADMIN', 'PLATFORM_ADMIN')")
public class SuperAdminUserController {

    private final ListGlobalUsersUseCase listGlobalUsersUseCase;
    private final CreateGlobalUserUseCase createGlobalUserUseCase;
    private final GetGlobalUserUseCase getGlobalUserUseCase;
    private final DeleteGlobalUserUseCase deleteGlobalUserUseCase;

    public SuperAdminUserController(
            ListGlobalUsersUseCase listGlobalUsersUseCase,
            CreateGlobalUserUseCase createGlobalUserUseCase,
            GetGlobalUserUseCase getGlobalUserUseCase,
            DeleteGlobalUserUseCase deleteGlobalUserUseCase) {
        this.listGlobalUsersUseCase = listGlobalUsersUseCase;
        this.createGlobalUserUseCase = createGlobalUserUseCase;
        this.getGlobalUserUseCase = getGlobalUserUseCase;
        this.deleteGlobalUserUseCase = deleteGlobalUserUseCase;
    }

    /**
     * Liste tous les utilisateurs de gestion globale (Super Admins, Admins).
     */
    @GetMapping
    public ResponseEntity<List<SuperAdminUserDto>> listGlobalUsers() {
        List<SuperAdminUserDto> users = listGlobalUsersUseCase.execute();
        return ResponseEntity.ok(users);
    }

    /**
     * Crée un nouvel utilisateur de gestion globale.
     * Les données sont strictement validées par jakarta.validation (@Valid).
     */
    @PostMapping
    public ResponseEntity<SuperAdminUserDto> createGlobalUser(@Valid @RequestBody CreateGlobalUserCommand command) {
        SuperAdminUserDto createdUser = createGlobalUserUseCase.execute(command);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdUser);
    }

    /**
     * Récupère un utilisateur global par son ID.
     */
    @GetMapping("/{userId}")
    public ResponseEntity<SuperAdminUserDto> getGlobalUser(@PathVariable String userId) {
        SuperAdminUserDto user = getGlobalUserUseCase.execute(userId);
        return ResponseEntity.ok(user);
    }

    /**
     * Supprime un utilisateur global par son ID.
     */
    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> deleteGlobalUser(@PathVariable String userId) {
        deleteGlobalUserUseCase.execute(userId);
        return ResponseEntity.noContent().build();
    }
}
