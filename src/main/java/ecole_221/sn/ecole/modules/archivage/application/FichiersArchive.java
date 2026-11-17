package ecole_221.sn.ecole.modules.archivage.application;

import ecole_221.sn.ecole.modules.archivage.domain.exception.ArchivageException;
import ecole_221.sn.ecole.modules.archivage.domain.model.ArchivageEcole;
import ecole_221.sn.ecole.modules.archivage.domain.port.StockageArchive;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Fichiers temporaires de l'archivage (droits 600, supprimés après usage) et relecture vérifiée d'une archive depuis
 * son stockage : l'empreinte SHA-256 et la taille du fichier relu doivent être celles du fichier écrit, AVANT tout
 * déchiffrement.
 */
public class FichiersArchive {

    private final Path dossierTemporaire;

    public FichiersArchive(Path dossierTemporaire) {
        this.dossierTemporaire = dossierTemporaire;
    }

    public Path temporaire(String prefixe) throws IOException {
        Files.createDirectories(dossierTemporaire);
        try {
            return Files.createTempFile(dossierTemporaire, prefixe, ".tmp",
                    PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------")));
        } catch (UnsupportedOperationException e) {
            return Files.createTempFile(dossierTemporaire, prefixe, ".tmp");
        }
    }

    public static MessageDigest sha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Copie locale du fichier relu depuis le stockage, après contrôle de l'empreinte et de la taille. */
    public Path relireVerifie(StockageArchive stockage, ArchivageEcole a) throws IOException {
        if (a.getEmplacement() == null || a.getEmpreinteSha256() == null) {
            throw ArchivageException.refus("Archive sans fichier enregistré.");
        }
        Path relu = temporaire("relu-");
        MessageDigest md = sha256();
        long taille;
        try (InputStream in = stockage.lire(a.getEmplacement());
             OutputStream out = new DigestOutputStream(Files.newOutputStream(relu), md)) {
            taille = in.transferTo(out);
        } catch (IOException | RuntimeException e) {
            Files.deleteIfExists(relu);
            throw e;
        }
        if ((a.getTailleOctets() != null && taille != a.getTailleOctets())
                || !HexFormat.of().formatHex(md.digest()).equals(a.getEmpreinteSha256())) {
            Files.deleteIfExists(relu);
            throw ArchivageException.refus("Archive relue différente de l'archive écrite (empreinte SHA-256).");
        }
        return relu;
    }
}
