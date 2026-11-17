package ecole_221.sn.ecole.modules.admin.rbac;

import ecole_221.sn.ecole.modules.auth.domain.model.User;
import ecole_221.sn.ecole.shared.infrastructure.tenant.TenantContext;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

/** Droits d'accès du personnel, par école. */
@RestController
@RequestMapping("/admin/rbac")
public class RbacController {

    private final PermissionsEcoleService droits;

    public RbacController(PermissionsEcoleService droits) {
        this.droits = droits;
    }

    private static String ecole() {
        return TenantContext.getCurrentOrganizationId().orElseThrow(() ->
                new ResponseStatusException(HttpStatus.BAD_REQUEST, "École inconnue : précisez l'école (X-Tenant-Slug)."));
    }

    private static String roleConnecte() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return null;
        return auth.getAuthorities().stream().map(GrantedAuthority::getAuthority).filter(a -> a.startsWith("ROLE_"))
                .map(a -> a.substring(5)).findFirst().orElse(null);
    }

    /** Seule l'administration de l'école modifie la matrice (jamais délégable : un rôle ne s'accorde pas de droits). */
    private static void exigerAdministration() {
        String r = roleConnecte();
        if (r == null || !CataloguePermissions.ROLES_ADMINISTRATION.contains(r)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Seule l'administration de l'école gère les droits d'accès.");
        }
    }

    @Operation(summary = "Matrice des droits de l'école", description = "Catalogue des permissions (modules réels), rôles du personnel et droits effectifs de chacun. Administration seulement.")
    @GetMapping
    public ResponseEntity<Map<String, Object>> matrice() {
        exigerAdministration();
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("catalogue", CataloguePermissions.description());
        r.put("roles", CataloguePermissions.ROLES_PERSONNEL.stream().map(code -> Map.of("code", code,
                "libelle", CataloguePermissions.LIBELLES_ROLES.get(code))).toList());
        r.put("matrice", droits.matrice(ecole()));
        return ResponseEntity.ok(r);
    }

    @Operation(summary = "Modifier les droits d'un rôle", description = "Corps {permissions: [clés du catalogue]} ; remplace les droits du rôle dans l'école. Administration seulement.")
    @PutMapping("/{role}")
    public ResponseEntity<Map<String, Object>> modifier(@PathVariable("role") String role, @RequestBody(required = false) Map<String, Object> corps) {
        exigerAdministration();
        Object brut = corps == null ? null : corps.get("permissions");
        if (!(brut instanceof List<?> liste)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La liste « permissions » est obligatoire (éventuellement vide).");
        }
        List<String> cles = new ArrayList<>();
        for (Object o : liste) {
            if (!(o instanceof String s)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Permission invalide : " + o + ".");
            cles.add(s);
        }
        String auteur = SecurityContextHolder.getContext().getAuthentication() != null
                && SecurityContextHolder.getContext().getAuthentication().getPrincipal() instanceof User u ? u.getId() : null;
        List<String> enregistres = droits.enregistrer(ecole(), role.trim().toUpperCase(java.util.Locale.ROOT), cles, auteur);
        return ResponseEntity.ok(Map.of("role", role.trim().toUpperCase(java.util.Locale.ROOT), "permissions", enregistres));
    }

    @Operation(summary = "Revenir aux droits par défaut d'un rôle", description = "Administration seulement.")
    @DeleteMapping("/{role}")
    public ResponseEntity<Map<String, Object>> reinitialiser(@PathVariable("role") String role) {
        exigerAdministration();
        String code = role.trim().toUpperCase(java.util.Locale.ROOT);
        if (!CataloguePermissions.ROLES_PERSONNEL.contains(code)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Rôle non configurable : " + role + ".");
        }
        droits.reinitialiser(ecole(), code);
        return ResponseEntity.ok(Map.of("role", code, "permissions", new ArrayList<>(new TreeSet<>(droits.droits(ecole(), code)))));
    }

    @Operation(summary = "Mes droits dans cette école", description = "Rôle du compte connecté et permissions effectives (menus affichés).")
    @GetMapping("/moi")
    public ResponseEntity<Map<String, Object>> moi() {
        String role = roleConnecte();
        boolean administration = role != null && CataloguePermissions.ROLES_ADMINISTRATION.contains(role);
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("role", role);
        r.put("administration", administration);
        r.put("permissions", new ArrayList<>(new TreeSet<>(droits.droits(TenantContext.getCurrentOrganizationId().orElse(null), role))));
        return ResponseEntity.ok(r);
    }
}
