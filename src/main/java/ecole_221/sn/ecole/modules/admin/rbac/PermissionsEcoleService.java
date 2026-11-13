package ecole_221.sn.ecole.modules.admin.rbac;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/** Matrice des droits du personnel, propre à chaque école et appliquée par le serveur (2026-11-13). */
@Service
public class PermissionsEcoleService {

    static final String CONFIGURE = "*";

    private final PermissionRoleJpaRepository permissions;

    public PermissionsEcoleService(PermissionRoleJpaRepository permissions) {
        this.permissions = permissions;
    }

    /** Droits effectifs d'un rôle dans une école : ceux enregistrés par l'école, sinon les droits par défaut. */
    public Set<String> droits(String ecole, String role) {
        if (ecole == null || role == null) return Set.of();
        if (CataloguePermissions.ROLES_ADMINISTRATION.contains(role)) return Set.copyOf(CataloguePermissions.cles());
        if (!CataloguePermissions.ROLES_PERSONNEL.contains(role)) return Set.of();
        List<PermissionRoleJpaEntity> lignes = permissions.findByOrganizationIdAndRole(ecole, role);
        if (lignes.stream().noneMatch(l -> CONFIGURE.equals(l.getPermission()))) {
            return CataloguePermissions.parDefaut(role);
        }
        return lignes.stream().map(PermissionRoleJpaEntity::getPermission)
                .filter(p -> !CONFIGURE.equals(p)).collect(Collectors.toUnmodifiableSet());
    }

    public boolean autorise(String ecole, String role, String permission) {
        return permission != null && droits(ecole, role).contains(permission);
    }

    /** Matrice complète de l'école (rôle → droits triés). */
    public Map<String, List<String>> matrice(String ecole) {
        Map<String, List<String>> m = new LinkedHashMap<>();
        for (String role : CataloguePermissions.ROLES_PERSONNEL) {
            m.put(role, new ArrayList<>(new TreeSet<>(droits(ecole, role))));
        }
        return m;
    }

    /** Remplace les droits d'un rôle dans l'école. Clés inconnues refusées (pas de droit « inventé »). */
    @Transactional
    public List<String> enregistrer(String ecole, String role, Collection<String> nouveaux, String auteur) {
        if (!CataloguePermissions.ROLES_PERSONNEL.contains(role)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Rôle non configurable : " + role + ". Rôles du personnel : " + String.join(", ", CataloguePermissions.ROLES_PERSONNEL) + ".");
        }
        Set<String> propres = new TreeSet<>();
        for (String p : nouveaux) {
            if (p == null || !CataloguePermissions.cleConnue(p.trim())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Permission inconnue : " + p + ".");
            }
            propres.add(p.trim());
        }
        permissions.supprimerRole(ecole, role);
        permissions.flush();
        permissions.save(new PermissionRoleJpaEntity(ecole, role, CONFIGURE, auteur));
        for (String p : propres) {
            permissions.save(new PermissionRoleJpaEntity(ecole, role, p, auteur));
        }
        return new ArrayList<>(propres);
    }

    /** Revient aux droits par défaut pour ce rôle. */
    @Transactional
    public void reinitialiser(String ecole, String role) {
        permissions.supprimerRole(ecole, role);
    }
}
