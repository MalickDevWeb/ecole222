package ecole_221.sn.ecole.modules.media.infrastructure.adapter.out.local;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Fichiers envoyés, rangés sur le disque du serveur quand Cloudinary n'est pas utilisé (CLOUDINARY_SIMULATION=true :
 * poste de développement, QA).
 *
 * <p>Nom de fichier aléatoire (non devinable), seul accès possible : {@link #lire(String)} refuse tout nom qui ne suit
 * pas ce format (aucune remontée de dossier).</p>
 */
@Component
public class StockageLocalFichiers {

    /** « prive- » : fichier confidentiel, relu par le serveur seulement (jamais servi par /public/media). */
    public static final String PREFIXE_PRIVE = "prive-";
    private static final Pattern NOM = Pattern.compile("^(prive-)?[a-f0-9]{32}\\.[a-z0-9]{1,5}$");
    private static final Pattern EXTENSION = Pattern.compile("^[a-z0-9]{1,5}$");
    private static final Map<String, String> EXTENSIONS = Map.of(
            "image/png", "png", "image/jpeg", "jpg", "image/gif", "gif", "image/webp", "webp", "image/svg+xml", "svg",
            "application/pdf", "pdf", "text/plain", "txt");

    private final Path dossier;

    public StockageLocalFichiers(@Value("${ecole.media.dossier-local:${user.home}/.ecole221/media}") String dossier) {
        this.dossier = Paths.get(dossier).toAbsolutePath().normalize();
    }

    /** Enregistre le fichier et renvoie son nom (ex. 3f2a…e1.png). */
    public String enregistrer(byte[] contenu, String nomOrigine, String typeContenu) throws IOException {
        return enregistrer(contenu, nomOrigine, typeContenu, false);
    }

    /** Enregistre le fichier ; confidentiel : nom « prive-… », jamais servi publiquement. */
    public String enregistrer(byte[] contenu, String nomOrigine, String typeContenu, boolean confidentiel) throws IOException {
        Files.createDirectories(dossier);
        String nom = (confidentiel ? PREFIXE_PRIVE : "") + UUID.randomUUID().toString().replace("-", "") + "." + extension(nomOrigine, typeContenu);
        Files.write(dossier.resolve(nom), contenu);
        return nom;
    }

    /** Chemin du fichier, seulement pour un nom au format attendu et existant. */
    public Optional<Path> lire(String nom) {
        if (nom == null || !NOM.matcher(nom).matches()) {
            return Optional.empty();
        }
        Path p = dossier.resolve(nom).normalize();
        return p.startsWith(dossier) && Files.isRegularFile(p) ? Optional.of(p) : Optional.empty();
    }

    static String extension(String nomOrigine, String typeContenu) {
        if (nomOrigine != null && nomOrigine.lastIndexOf('.') >= 0) {
            String e = nomOrigine.substring(nomOrigine.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
            if (EXTENSION.matcher(e).matches()) {
                return e.equals("jpeg") ? "jpg" : e;
            }
        }
        return typeContenu == null ? "bin" : EXTENSIONS.getOrDefault(typeContenu.toLowerCase(Locale.ROOT), "bin");
    }
}
