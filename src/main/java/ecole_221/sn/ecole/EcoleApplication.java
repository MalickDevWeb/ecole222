package ecole_221.sn.ecole;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Application principale pour l'École 221.
 * 
 * Cette classe configure et lance l'application Spring Boot avec toutes les fonctionnalités nécessaires :
 * - Activation du cache avec Redis
 * - Activation de la programmation orientée aspect (AOP) pour le logging
 * - Activation des transactions pour JPA
 * - Activation des
 * 
 * @author École 221
 * @version 1.0
 */
@SpringBootApplication(
    scanBasePackages = "ecole_221.sn.ecole"
)
@EnableCaching
@EnableAspectJAutoProxy(proxyTargetClass = true)
@EnableTransactionManagement
@EnableAsync
public class EcoleApplication {

    /**
     * Point d'entrée principal de l'application.
     * 
     * @param args Arguments de ligne de commande
     */
    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(EcoleApplication.class);
        
        // Configuration supplémentaire si nécessaire
        // app.setAdditionalProfiles("dev");
        
        app.run(args);
    }

}