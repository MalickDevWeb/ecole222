package ecole_221.sn.ecole.modules.referential.infrastructure.web;

import ecole_221.sn.ecole.modules.referential.application.dto.EffectiveReferenceValue;
import ecole_221.sn.ecole.modules.referential.application.dto.ReferenceCaller;
import ecole_221.sn.ecole.modules.referential.application.service.ReferenceCommandService;
import ecole_221.sn.ecole.modules.referential.application.service.ReferenceQueryService;
import ecole_221.sn.ecole.modules.referential.domain.model.ReferenceList;
import ecole_221.sn.ecole.shared.infrastructure.tenant.TenantContext;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Référentiels paramétrables : lecture pour tout utilisateur connecté, écriture ADMIN / SUPER_ADMIN.
 * Les droits sont vérifiés dans {@link ReferenceCommandService} (règle qui dépend de la liste et de
 * l'école : elle ne s'exprime pas en simple @PreAuthorize).
 */
@RestController
@RequestMapping("/v1/referentiels")
public class ReferenceValueController {

    private static final Set<String> ADMIN_ROLES = Set.of("ROLE_ADMIN", "ROLE_ADMINISTRATOR");
    private static final Set<String> PLATFORM_ROLES = Set.of("ROLE_SUPER_ADMIN", "ROLE_PLATFORM_ADMIN");

    private final ReferenceQueryService queries;
    private final ReferenceCommandService commands;

    public ReferenceValueController(ReferenceQueryService queries, ReferenceCommandService commands) {
        this.queries = queries;
        this.commands = commands;
    }

    @GetMapping
    public List<Map<String, String>> lists() {
        return Arrays.stream(ReferenceList.values())
                .map(l -> Map.of("slug", l.slug(), "label", l.label(), "mode", l.mode().name()))
                .toList();
    }

    @GetMapping("/{list}")
    public List<EffectiveReferenceValue> values(@PathVariable String list,
                                                @RequestParam(defaultValue = "false") boolean all) {
        return queries.effectiveValues(resolve(list), currentOrg(), all);
    }

    @PostMapping("/{list}")
    public ResponseEntity<EffectiveReferenceValue> create(@PathVariable String list, @Valid @RequestBody ReferenceValueRequest body,
                                                          @RequestParam(required = false) String scope, Authentication auth) {
        ReferenceList l = resolve(list);
        ReferenceCaller caller = caller(auth, scope);
        String code = commands.create(l, caller, body.code(), body.label(), body.color(), body.sortOrder()).code();
        return ResponseEntity.status(HttpStatus.CREATED).body(view(l, caller, code));
    }

    @PutMapping("/{list}/{code}")
    public EffectiveReferenceValue update(@PathVariable String list, @PathVariable String code,
                                          @Valid @RequestBody ReferenceValueRequest body,
                                          @RequestParam(required = false) String scope, Authentication auth) {
        ReferenceList l = resolve(list);
        ReferenceCaller caller = caller(auth, scope);
        commands.update(l, code.toUpperCase(), caller, body.label(), body.color(), body.sortOrder(), body.active());
        return view(l, caller, code.toUpperCase());
    }

    @DeleteMapping("/{list}/{code}")
    public ResponseEntity<Void> delete(@PathVariable String list, @PathVariable String code,
                                       @RequestParam(required = false) String scope, Authentication auth) {
        commands.delete(resolve(list), code.toUpperCase(), caller(auth, scope));
        return ResponseEntity.noContent().build();
    }

    private ReferenceList resolve(String slug) {
        return ReferenceList.fromSlug(slug).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Référentiel inconnu : " + slug));
    }

    private ReferenceCaller caller(Authentication auth, String scope) {
        Set<String> roles = auth == null ? Set.of() : auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority).collect(Collectors.toSet());
        boolean superAdmin = roles.stream().anyMatch(PLATFORM_ROLES::contains);
        boolean admin = superAdmin || roles.stream().anyMatch(ADMIN_ROLES::contains);
        return new ReferenceCaller(currentOrg(), admin, superAdmin, "global".equalsIgnoreCase(scope));
    }

    private String currentOrg() {
        return TenantContext.getCurrentOrganizationId().orElse(null);
    }

    private EffectiveReferenceValue view(ReferenceList list, ReferenceCaller caller, String code) {
        return queries.effectiveValues(list, caller.targetOrganizationId(), true).stream()
                .filter(v -> v.code().equals(code)).findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, code));
    }
}
