package ecole_221.sn.ecole.modules.auth.infrastructure.web;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Enregistre le contrôle du mot de passe provisoire AVANT les autres intercepteurs (école, cours). */
@Configuration
public class MotDePasseProvisoireWebConfig implements WebMvcConfigurer {

    private final MotDePasseProvisoireInterceptor interceptor;

    public MotDePasseProvisoireWebConfig(MotDePasseProvisoireInterceptor interceptor) {
        this.interceptor = interceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(interceptor).addPathPatterns("/**").order(Ordered.HIGHEST_PRECEDENCE);
    }
}
