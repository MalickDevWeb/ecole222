package ecole_221.sn.ecole.architecture;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Garde-fou d'architecture (constat Kasongo SUP-QC-20261109, point 5).
 *
 * <p>La table {@code users} n'a pas de colonne d'école : lister les comptes par {@code findByRole(…)} ou
 * {@code findAll()} renvoie les comptes de TOUTES les écoles. Hors des modules légitimement globaux
 * (auth, superadmin, platform), les comptes d'une école se lisent par ses appartenances
 * ({@code ComptesEcoleService.membres(role)}). Ce test échoue si un tel appel est réintroduit.</p>
 */
class CloisonnementComptesTest {

    private static final Path SOURCES = Path.of("src/main/java/ecole_221/sn/ecole");
    private static final Pattern INTERDIT = Pattern.compile(
            "(userJpaRepository|userRepo|userRepository|userRepositoryPort|comptes)\\s*\\.\\s*(findByRole|findAll)\\s*\\(");
    private static final List<String> MODULES_GLOBAUX = List.of("/modules/auth/", "/modules/superadmin/", "/modules/platform/",
            "/infrastructure/config/SuperAdminInitializer.java");   // création du compte plateforme au démarrage

    @Test
    void aucunListageGlobalDesComptesHorsModulesPlateforme() throws IOException {
        List<String> violations = new ArrayList<>();
        try (Stream<Path> fichiers = Files.walk(SOURCES)) {
            for (Path f : fichiers.filter(p -> p.toString().endsWith(".java")).toList()) {
                String chemin = f.toString().replace('\\', '/');
                if (MODULES_GLOBAUX.stream().anyMatch(chemin::contains)) continue;
                List<String> lignes = Files.readAllLines(f);
                for (int i = 0; i < lignes.size(); i++) {
                    if (INTERDIT.matcher(lignes.get(i)).find()) {
                        violations.add(chemin + ":" + (i + 1) + " → " + lignes.get(i).trim());
                    }
                }
            }
        }
        assertTrue(violations.isEmpty(), "Listage des comptes sans filtre d'école :\n" + String.join("\n", violations));
    }
}
