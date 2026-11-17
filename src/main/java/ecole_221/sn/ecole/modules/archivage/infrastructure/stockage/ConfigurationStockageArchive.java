package ecole_221.sn.ecole.modules.archivage.infrastructure.stockage;

import ecole_221.sn.ecole.modules.archivage.domain.exception.ArchivageException;
import ecole_221.sn.ecole.modules.archivage.domain.port.StockageArchive;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.Locale;

/**
 * Choix de l'adaptateur de stockage des archives par UN seul réglage, sans changer de code :
 * {@code ecole.archivage.stockage} = {@code local} (défaut, développement) ou {@code cloudinary} (production).
 * Une valeur inconnue ne casse pas le démarrage : l'archivage est alors refusé avec un message clair.
 * Un futur adaptateur compatible S3 se branchera ici.
 */
@Configuration
public class ConfigurationStockageArchive {

    @Bean
    public StockageArchive stockageArchive(
            @Value("${ecole.archivage.stockage:local}") String choix,
            @Value("${ecole.archivage.production:false}") boolean production,
            @Value("${ecole.archivage.local.dossier:${user.home}/ecole221-archives}") String dossierLocal,
            @Value("${ecole.archivage.local.autoriser-en-production:false}") boolean localEnProduction,
            @Value("${ecole.archivage.cloudinary.dossier:ecole221-archives}") String dossierCloudinary,
            @Value("${cloudinary.cloud-name:}") String cloudName,
            @Value("${cloudinary.api-key:}") String apiKey,
            @Value("${cloudinary.api-secret:}") String apiSecret) {
        String c = choix == null ? "" : choix.trim().toLowerCase(Locale.ROOT);
        return switch (c) {
            case "local" -> new StockageArchiveLocal(Path.of(dossierLocal), production, localEnProduction);
            case "cloudinary" -> new StockageArchiveCloudinary(
                    vide(cloudName) || vide(apiKey) || vide(apiSecret) ? null : new ClientCloudinaryArchivesSdk(cloudName, apiKey, apiSecret),
                    dossierCloudinary);
            default -> new StockageInconnu(choix);
        };
    }

    private static boolean vide(String s) {
        return s == null || s.isBlank();
    }

    /** Réglage inconnu : refus explicite à chaque opération, jamais de repli. */
    static final class StockageInconnu implements StockageArchive {
        private final String choix;

        StockageInconnu(String choix) {
            this.choix = choix;
        }

        @Override
        public String nom() {
            return "inconnu";
        }

        @Override
        public void verifierPret() {
            throw ArchivageException.indisponible("Stockage des archives inconnu (ecole.archivage.stockage=" + choix
                    + ") : valeurs admises local ou cloudinary.");
        }

        @Override
        public String ecrire(String nomArchive, Path fichier) {
            verifierPret();
            return null;
        }

        @Override
        public InputStream lire(String emplacement) {
            verifierPret();
            return null;
        }

        @Override
        public void supprimer(String emplacement) {
            verifierPret();
        }

        @Override
        public boolean existe(String emplacement) {
            verifierPret();
            return false;
        }
    }
}
