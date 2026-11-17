package ecole_221.sn.ecole.modules.media.infrastructure.adapter.out.local;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

/**
 * Sert un fichier rangé par {@link StockageLocalFichiers} (développement, QA). Public comme un lien Cloudinary : le logo
 * s'affiche aussi sur la page de connexion. Le nom aléatoire tient lieu de secret ; le contenu n'est jamais exécuté
 * (nosniff, bac à sable).
 */
@RestController
public class FichiersLocauxController {

    private final StockageLocalFichiers stockage;

    public FichiersLocauxController(StockageLocalFichiers stockage) {
        this.stockage = stockage;
    }

    @GetMapping("/public/media/{nom}")
    public ResponseEntity<byte[]> lire(@PathVariable String nom) throws IOException {
        // Un fichier confidentiel n'est jamais servi par son lien (seul le serveur le relit, pour une personne autorisée)
        if (nom != null && nom.startsWith(StockageLocalFichiers.PREFIXE_PRIVE)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Fichier introuvable.");
        }
        Path p = stockage.lire(nom).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Fichier introuvable."));
        String type = nom.endsWith(".svg") ? "image/svg+xml" : URLConnection.guessContentTypeFromName(nom);
        return ResponseEntity.ok()
                .contentType(type == null ? MediaType.APPLICATION_OCTET_STREAM : MediaType.parseMediaType(type))
                .cacheControl(CacheControl.maxAge(30, TimeUnit.DAYS).cachePublic())
                .header("X-Content-Type-Options", "nosniff")
                .header("Content-Security-Policy", "sandbox; default-src 'none'; img-src 'self' data:; style-src 'unsafe-inline'")
                .body(Files.readAllBytes(p));
    }
}
