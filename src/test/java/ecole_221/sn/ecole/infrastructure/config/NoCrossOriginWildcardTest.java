package ecole_221.sn.ecole.infrastructure.config;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * APP-SEC-12 : aucun contrôleur ne déclare @CrossOrigin("*") ; le CORS passe uniquement par
 * AllowedOrigins (SecurityConfig). AdminController est en cours de modification par le Tech Lead
 * et reste toléré tant que ce chantier n'est pas livré.
 */
class NoCrossOriginWildcardTest {

    @Test
    void aucunCrossOriginJoker() throws IOException {
        try (Stream<Path> fichiers = Files.walk(Path.of("src/main/java"))) {
            List<String> fautifs = fichiers.filter(p -> p.toString().endsWith(".java"))
                    .filter(p -> !p.getFileName().toString().equals("AdminController.java"))
                    .filter(p -> {
                        try {
                            return Files.readString(p).contains("@CrossOrigin");
                        } catch (IOException e) {
                            throw new IllegalStateException(e);
                        }
                    })
                    .map(Path::toString).toList();
            assertEquals(List.of(), fautifs);
        }
    }
}
