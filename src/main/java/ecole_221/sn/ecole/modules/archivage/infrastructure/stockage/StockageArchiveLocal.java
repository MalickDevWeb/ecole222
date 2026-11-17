package ecole_221.sn.ecole.modules.archivage.infrastructure.stockage;

import ecole_221.sn.ecole.modules.archivage.domain.exception.ArchivageException;
import ecole_221.sn.ecole.modules.archivage.domain.port.StockageArchive;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.regex.Pattern;

/**
 * Stockage local des archives (développement) : un dossier de la machine, utilisable aussi comme volume Docker,
 * HORS du dépôt git par défaut ({@code ${user.home}/ecole221-archives}), créé avec les droits 700, fichiers en 600.
 *
 * <p>En production il est refusé (le disque de Render est effacé à chaque déploiement) : jamais de repli silencieux
 * sur le disque. Seule exception, explicite : {@code ecole.archivage.local.autoriser-en-production=true} pour un serveur
 * dont le dossier est un disque persistant.</p>
 */
public class StockageArchiveLocal implements StockageArchive {

    private static final Pattern NOM = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._-]{0,180}");
    static final String EXTENSION = ".e221a";

    private final Path dossier;
    private final boolean production;
    private final boolean autoriseEnProduction;

    public StockageArchiveLocal(Path dossier, boolean production, boolean autoriseEnProduction) {
        this.dossier = dossier.toAbsolutePath().normalize();
        this.production = production;
        this.autoriseEnProduction = autoriseEnProduction;
    }

    @Override
    public String nom() {
        return "local";
    }

    @Override
    public void verifierPret() {
        if (production && !autoriseEnProduction) {
            throw ArchivageException.indisponible("Archivage refusé en production : le stockage local est effacé à chaque "
                    + "déploiement. Réglez ecole.archivage.stockage=cloudinary.");
        }
        try {
            if (!Files.isDirectory(dossier)) {
                Files.createDirectories(dossier);
                Files.setPosixFilePermissions(dossier, PosixFilePermissions.fromString("rwx------"));
            }
        } catch (UnsupportedOperationException e) {
            // système sans droits POSIX : dossier créé tel quel
        } catch (IOException e) {
            throw ArchivageException.indisponible("Dossier des archives inaccessible : " + dossier + ".");
        }
        if (!Files.isWritable(dossier)) {
            throw ArchivageException.indisponible("Dossier des archives en lecture seule : " + dossier + ".");
        }
    }

    private Path chemin(String emplacement) {
        if (emplacement == null || !NOM.matcher(emplacement).matches() || !emplacement.endsWith(EXTENSION)) {
            throw ArchivageException.refus("Emplacement d'archive invalide.");
        }
        return dossier.resolve(emplacement);
    }

    @Override
    public String ecrire(String nomArchive, Path fichier) throws IOException {
        verifierPret();
        String emplacement = nomArchive + EXTENSION;
        Path cible = chemin(emplacement);
        Path temporaire = dossier.resolve(emplacement + ".partiel");
        Files.copy(fichier, temporaire, StandardCopyOption.REPLACE_EXISTING);
        try {
            Files.setPosixFilePermissions(temporaire, PosixFilePermissions.fromString("rw-------"));
        } catch (UnsupportedOperationException e) {
            // système sans droits POSIX
        }
        Files.move(temporaire, cible, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        return emplacement;
    }

    @Override
    public InputStream lire(String emplacement) throws IOException {
        Path p = chemin(emplacement);
        if (!Files.exists(p)) {
            throw ArchivageException.refus("Fichier d'archive introuvable dans le stockage local.");
        }
        return Files.newInputStream(p);
    }

    @Override
    public void supprimer(String emplacement) throws IOException {
        Files.deleteIfExists(chemin(emplacement));
    }

    @Override
    public boolean existe(String emplacement) {
        return Files.exists(chemin(emplacement));
    }

    public Path dossier() {
        return dossier;
    }
}
