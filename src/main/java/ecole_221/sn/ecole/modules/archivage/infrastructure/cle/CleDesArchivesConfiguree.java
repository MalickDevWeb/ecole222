package ecole_221.sn.ecole.modules.archivage.infrastructure.cle;

import ecole_221.sn.ecole.modules.archivage.application.format.ChiffrementFlux;
import ecole_221.sn.ecole.modules.archivage.domain.exception.ArchivageException;
import ecole_221.sn.ecole.modules.archivage.domain.port.CleDesArchives;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Base64;

/**
 * Clé maîtresse des archives.
 * <ul>
 *   <li>ARCHIVES_CLE (propriété {@code ecole.archivage.cle}) : 32 octets en base64. Toujours prioritaire.</li>
 *   <li>Sans elle, en production ({@code ecole.archivage.production=true}) : archivage REFUSÉ.</li>
 *   <li>Sans elle, en développement : clé de développement créée à part (fichier à droits 600, jamais dans le dossier
 *       des archives), avec un avertissement dans le journal.</li>
 * </ul>
 * La clé n'est jamais écrite dans le journal applicatif.
 */
@Component
public class CleDesArchivesConfiguree implements CleDesArchives {

    private static final Logger log = LoggerFactory.getLogger(CleDesArchivesConfiguree.class);

    private final String cleBase64;
    private final boolean production;
    private final Path fichierDeveloppement;
    private CleMaitresse cleDeveloppement;

    public CleDesArchivesConfiguree(@Value("${ecole.archivage.cle:}") String cleBase64,
                                    @Value("${ecole.archivage.production:false}") boolean production,
                                    @Value("${ecole.archivage.cle-developpement:${user.home}/.ecole221/cle-archives-developpement}")
                                    String fichierDeveloppement) {
        this.cleBase64 = cleBase64 == null ? "" : cleBase64.trim();
        this.production = production;
        this.fichierDeveloppement = Path.of(fichierDeveloppement);
    }

    @Override
    public CleMaitresse obtenir() {
        if (!cleBase64.isEmpty()) {
            return decoder(cleBase64, "ARCHIVES_CLE invalide : 32 octets encodés en base64 attendus.");
        }
        if (production) {
            throw ArchivageException.indisponible("Archivage refusé : clé des archives absente (ARCHIVES_CLE).");
        }
        return cleDeDeveloppement();
    }

    private static CleMaitresse decoder(String base64, String messageErreur) {
        byte[] octets;
        try {
            octets = Base64.getDecoder().decode(base64.trim());
        } catch (IllegalArgumentException e) {
            throw ArchivageException.indisponible(messageErreur);
        }
        if (octets.length != 32) {
            throw ArchivageException.indisponible(messageErreur);
        }
        return new CleMaitresse(octets, ChiffrementFlux.identifiant(octets));
    }

    private synchronized CleMaitresse cleDeDeveloppement() {
        if (cleDeveloppement != null) {
            return cleDeveloppement;
        }
        try {
            if (!Files.exists(fichierDeveloppement)) {
                creer();
            }
            cleDeveloppement = decoder(Files.readString(fichierDeveloppement, StandardCharsets.US_ASCII),
                    "Clé de développement des archives illisible : " + fichierDeveloppement + ".");
            return cleDeveloppement;
        } catch (IOException e) {
            throw ArchivageException.indisponible("Clé de développement des archives inaccessible : " + fichierDeveloppement + ".");
        }
    }

    private void creer() throws IOException {
        Path dossier = fichierDeveloppement.toAbsolutePath().getParent();
        if (dossier != null && !Files.exists(dossier)) {
            Files.createDirectories(dossier);
            restreindre(dossier, "rwx------");
        }
        try {
            Files.writeString(fichierDeveloppement, Base64.getEncoder().encodeToString(ChiffrementFlux.nouvelleCle()),
                    StandardCharsets.US_ASCII, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
        } catch (FileAlreadyExistsException e) {
            return;   // créée entre-temps par un autre fil : on la relit
        }
        restreindre(fichierDeveloppement, "rw-------");
        log.warn("Clé de DÉVELOPPEMENT des archives créée dans {} (droits 600). Ne jamais l'utiliser en production : "
                + "réglez ARCHIVES_CLE.", fichierDeveloppement);
    }

    static void restreindre(Path chemin, String droits) {
        try {
            Files.setPosixFilePermissions(chemin, PosixFilePermissions.fromString(droits));
        } catch (UnsupportedOperationException | IOException e) {
            log.warn("Droits restreints impossibles sur {} : {}", chemin, e.getMessage());
        }
    }
}
