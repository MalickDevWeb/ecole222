package ecole_221.sn.ecole.modules.audit.infrastructure.web;

import ecole_221.sn.ecole.modules.audit.infrastructure.persistence.JournalAudit;
import ecole_221.sn.ecole.modules.auth.domain.model.User;
import ecole_221.sn.ecole.modules.auth.domain.model.UserFactory;
import ecole_221.sn.ecole.modules.auth.domain.valueobject.RoleUtilisateur;
import ecole_221.sn.ecole.modules.platform.organization.domain.model.Organization;
import ecole_221.sn.ecole.shared.infrastructure.tenant.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.HandlerMapping;

import java.util.Arrays;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/** Journal d'audit exhaustif des écritures (BR-05, ENF-07, 2026-11-14). */
class JournalAuditInterceptorTest {

    private final JournalAudit journal = mock(JournalAudit.class);
    private final JournalAuditInterceptor intercepteur = new JournalAuditInterceptor(journal);

    @AfterEach
    void nettoyer() {
        SecurityContextHolder.clearContext();
        TenantContext.clear();
    }

    private User connecter() {
        User u = UserFactory.create("Diop", "Awa", "awa@test.sn", "x", RoleUtilisateur.fromString("SECRETAIRE"));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(u, null, u.getAuthorities()));
        return u;
    }

    private static void ecole(String id) {
        Organization org = mock(Organization.class);
        when(org.getId()).thenReturn(id);
        TenantContext.setCurrentOrganization(org);
    }

    private static MockHttpServletRequest requete(String methode, String chemin, String motif, Map<String, String> variables) {
        MockHttpServletRequest r = new MockHttpServletRequest(methode, "/api" + chemin);
        r.setContextPath("/api");
        r.setRemoteAddr("10.0.0.7");
        r.addHeader("User-Agent", "Navigateur de test");
        r.setContent("{\"password\":\"Secret-123\",\"telephone\":\"+221 77 000 00 00\"}".getBytes());
        if (motif != null) r.setAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE, motif);
        if (variables != null) r.setAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE, variables);
        return r;
    }

    private static MockHttpServletResponse reponse(int statut) {
        MockHttpServletResponse r = new MockHttpServletResponse();
        r.setStatus(statut);
        return r;
    }

    private JournalAudit.Entree inscrite() {
        ArgumentCaptor<JournalAudit.Entree> e = ArgumentCaptor.forClass(JournalAudit.Entree.class);
        verify(journal).inscrire(e.capture());
        return e.getValue();
    }

    @Test
    void ecritureReussieInscriteAvecAuteurEcoleRouteEtSansContenu() {
        User u = connecter();
        ecole("org-ecole-221");
        intercepteur.afterCompletion(requete("PUT", "/admin/students/stu-42/toggle-status", "/admin/students/{id}/toggle-status",
                Map.of("id", "stu-42")), reponse(200), null, null);
        JournalAudit.Entree e = inscrite();
        assertThat(e.ecole()).isEqualTo("org-ecole-221");
        assertThat(e.typeAction()).isEqualTo("UPDATE");
        assertThat(e.typeEntite()).isEqualTo("admin/students");
        assertThat(e.identifiants()).isEqualTo("stu-42");
        assertThat(e.acteurId()).isEqualTo(u.getId());
        assertThat(e.acteurNom()).isEqualTo("Awa Diop");
        assertThat(e.role()).contains("SECRETAIRE");
        assertThat(e.description()).isEqualTo("Modification — PUT /admin/students/{id}/toggle-status");
        assertThat(e.ip()).isEqualTo("10.0.0.7");
        assertThat(e.module()).isEqualTo("admin");
        // jamais le contenu de la requête (mot de passe, téléphone)
        assertThat(e.toString()).doesNotContain("Secret-123").doesNotContain("+221 77");
    }

    @Test
    void creationActionEtSuppressionTypees() {
        connecter();
        intercepteur.afterCompletion(requete("DELETE", "/admin/classes/c-1", "/admin/classes/{id}", Map.of("id", "c-1")), reponse(204), null, null);
        assertThat(inscrite().typeAction()).isEqualTo("DELETE");
        reset(journal);
        intercepteur.afterCompletion(requete("POST", "/admin/classes", "/admin/classes", null), reponse(201), null, null);
        assertThat(inscrite().typeAction()).isEqualTo("CREATE");
        reset(journal);
        intercepteur.afterCompletion(requete("POST", "/admin/periodes/p-1/rouvrir", "/admin/periodes/{id}/rouvrir", Map.of("id", "p-1")),
                reponse(200), null, null);
        JournalAudit.Entree e = inscrite();
        assertThat(e.typeAction()).isEqualTo("UPDATE");
        assertThat(e.description()).isEqualTo("Action — POST /admin/periodes/{id}/rouvrir");
        assertThat(e.ecole()).isNull();   // hors école : « plateforme » fixée par JournalAudit
    }

    @Test
    void lecturesEchecsEtConnexionNonInscrits() {
        connecter();
        intercepteur.afterCompletion(requete("GET", "/admin/students", "/admin/students", null), reponse(200), null, null);
        intercepteur.afterCompletion(requete("POST", "/admin/classes", "/admin/classes", null), reponse(403), null, null);
        intercepteur.afterCompletion(requete("POST", "/admin/classes", "/admin/classes", null), reponse(200), null, new IllegalStateException("x"));
        intercepteur.afterCompletion(requete("POST", "/auth/login", null, null), reponse(200), null, null);
        intercepteur.afterCompletion(requete("POST", "/audit/logs", "/audit/logs", null), reponse(200), null, null);
        verifyNoInteractions(journal);
    }

    @ParameterizedTest
    @ValueSource(strings = {"/candidatures/submit", "/register-school", "/auth/reinitialiser-mot-de-passe"})
    void ecrituresPubliquesDurablesInscritesCommeAnonymes(String chemin) {
        ecole("org-ecole-221");
        intercepteur.afterCompletion(requete("POST", chemin, chemin, null), reponse(200), null, null);
        JournalAudit.Entree e = inscrite();
        assertThat(e.acteurId()).isNull();
        assertThat(e.acteurNom()).startsWith("anonyme");
    }

    @ParameterizedTest
    @ValueSource(strings = {"/auth/mot-de-passe-oublie", "/candidatures/suivi", "/media/upload", "/auth/logout", "/admin/classes"})
    void autresEcrituresAnonymesNonInscrites(String chemin) {
        // un anonyme ne doit pas pouvoir remplir à volonté une table qu'on ne peut plus vider
        intercepteur.afterCompletion(requete("POST", chemin, chemin, null), reponse(200), null, null);
        verifyNoInteractions(journal);
    }

    @Test
    void typeDEntiteLimiteADeuxSegmentsAvantLesIdentifiants() {
        assertThat(JournalAuditInterceptor.typeEntite("/admin/students/{id}/toggle-status")).isEqualTo("admin/students");
        assertThat(JournalAuditInterceptor.typeEntite("/vigil/{id}")).isEqualTo("vigil");
        assertThat(JournalAuditInterceptor.typeEntite("/auth/logout")).isEqualTo("auth/logout");
        assertThat(JournalAuditInterceptor.typeAction("PATCH", "/admin/x/{id}")).isEqualTo("UPDATE");
    }

    @Test
    void journalEcritEnBaseLEcolePlateformeEtLAvertissementDeSuppression() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        JournalAudit vrai = new JournalAudit(jdbc);
        vrai.inscrire(new JournalAudit.Entree(null, "u-1", "Awa Diop", "SUPER_ADMIN", "DELETE", "v1/super-admin", "e-9",
                "Suppression — DELETE /v1/super-admin/tenants/{id}", "10.0.0.7", "x".repeat(400), "v1"));
        ArgumentCaptor<Object[]> valeurs = ArgumentCaptor.forClass(Object[].class);
        verify(jdbc).update(startsWith("INSERT INTO audit_logs"), valeurs.capture());
        Object[] v = valeurs.getValue();
        assertThat(v[1]).isEqualTo(JournalAudit.PLATEFORME);
        assertThat(v[11]).isEqualTo("warning");
        assertThat(((String) v[10])).hasSize(255);   // navigateur borné à la taille de la colonne
        assertThat(Arrays.asList(v)).contains("u-1", "e-9");
    }

    @Test
    void pannePendantLInscriptionNeCassePasLaRequete() {
        JdbcTemplate jdbc = mock(JdbcTemplate.class);
        when(jdbc.update(anyString(), any(Object[].class))).thenThrow(new DataAccessResourceFailureException("base"));
        JournalAudit vrai = new JournalAudit(jdbc);
        assertThatCode(() -> vrai.inscrire(new JournalAudit.Entree("e", "u", "n", "r", "CREATE", "t", null, "d", null, null, "m")))
                .doesNotThrowAnyException();
    }
}
