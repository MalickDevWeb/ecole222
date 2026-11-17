package ecole_221.sn.ecole.infrastructure.config;

import ecole_221.sn.ecole.shared.infrastructure.tenant.TenantMembershipInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final TenantMembershipInterceptor tenantMembershipInterceptor;
    private final ecole_221.sn.ecole.modules.professor.infrastructure.web.AccesCoursInterceptor accesCours;
    private final ecole_221.sn.ecole.modules.admin.rbac.PermissionsInterceptor permissions;
    private final ecole_221.sn.ecole.modules.audit.infrastructure.web.JournalAuditInterceptor journalAudit;

    public WebConfig(TenantMembershipInterceptor tenantMembershipInterceptor,
                     ecole_221.sn.ecole.modules.professor.infrastructure.web.AccesCoursInterceptor accesCours,
                     ecole_221.sn.ecole.modules.admin.rbac.PermissionsInterceptor permissions,
                     ecole_221.sn.ecole.modules.audit.infrastructure.web.JournalAuditInterceptor journalAudit) {
        this.tenantMembershipInterceptor = tenantMembershipInterceptor;
        this.accesCours = accesCours;
        this.permissions = permissions;
        this.journalAudit = journalAudit;
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // Laisser Spring Security gérer le CORS via SecurityConfig.corsConfigurationSource()
        // pour éviter tout conflit d'en-têtes et garantir allowCredentials=true de manière sécurisée.
    }

    /** SEC-003 : l'utilisateur connecté doit être membre de l'école visée par la requête. */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(tenantMembershipInterceptor).addPathPatterns("/**");
        // Droits du personnel (matrice RBAC de l'école), après le contrôle d'appartenance
        registry.addInterceptor(permissions).addPathPatterns("/**");
        // cours, modules et élèves de l'espace professeur : appartenance au professeur et à l'école
        registry.addInterceptor(accesCours).addPathPatterns("/professor/**", "/prof/**");
        // BR-05, ENF-07 : toute écriture réussie est inscrite au journal d'audit inaltérable
        registry.addInterceptor(journalAudit).addPathPatterns("/**");
    }
}
