package ecole_221.sn.ecole.infrastructure.config;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.AnnotatedElement;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Les @PreAuthorize ne protègent que si la sécurité par méthode est active, et une expression
 * hasAuthority('ADMIN') ne correspond jamais aux autorités « ROLE_ADMIN » produites par User.
 */
class MethodSecurityConfigTest {

    private static final Pattern AUTHORITY = Pattern.compile("has(?:Any)?Authority\\(([^)]*)\\)");

    @Test
    void methodSecurityIsEnabled() {
        assertThat(SecurityConfig.class.isAnnotationPresent(EnableMethodSecurity.class)).isTrue();
    }

    @Test
    void authorityExpressionsUseRolePrefix() throws Exception {
        var scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(RestController.class));
        List<String> wrong = new ArrayList<>();
        int checked = 0;
        for (var bean : scanner.findCandidateComponents("ecole_221.sn.ecole")) {
            Class<?> type = Class.forName(bean.getBeanClassName());
            List<AnnotatedElement> elements = new ArrayList<>(List.of(type.getDeclaredMethods()));
            elements.add(type);
            for (AnnotatedElement element : elements) {
                PreAuthorize rule = element.getAnnotation(PreAuthorize.class);
                if (rule == null) continue;
                checked++;
                Matcher m = AUTHORITY.matcher(rule.value());
                while (m.find()) {
                    for (String value : m.group(1).split(",")) {
                        if (!value.trim().replace("'", "").startsWith("ROLE_")) {
                            wrong.add(type.getSimpleName() + " : " + rule.value());
                        }
                    }
                }
            }
        }
        assertThat(checked).as("@PreAuthorize trouvés").isGreaterThan(0);
        assertThat(wrong).as("utiliser hasRole/hasAnyRole (autorités préfixées ROLE_)").isEmpty();
    }
}
