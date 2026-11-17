package ecole_221.sn.ecole.infrastructure.config;

import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.cfg.CoercionAction;
import tools.jackson.databind.cfg.CoercionInputShape;
import tools.jackson.databind.type.LogicalType;

/**
 * JSON reçu : un nombre ou un booléen n'est plus converti en silence en texte (APP-QA-04).
 *
 * <p>{@code {"name": -999999999999}} ou {@code {"name": true}} : 400 « Le champ « name » n'a pas le bon type (texte
 * attendu). ». Un texte contenant un nombre (« 30 ») reste accepté pour un champ numérique. Les corps libres
 * ({@code Map<String, Object>}) ne sont pas concernés : leurs champs sont contrôlés un à un
 * ({@code ChampsRequete}).</p>
 */
@Configuration
public class JsonStrictConfig {

    @Bean
    JsonMapperBuilderCustomizer texteSansConversionSilencieuse() {
        return builder -> builder.withCoercionConfig(LogicalType.Textual, c -> c
                .setCoercion(CoercionInputShape.Integer, CoercionAction.Fail)
                .setCoercion(CoercionInputShape.Float, CoercionAction.Fail)
                .setCoercion(CoercionInputShape.Boolean, CoercionAction.Fail));
    }
}
