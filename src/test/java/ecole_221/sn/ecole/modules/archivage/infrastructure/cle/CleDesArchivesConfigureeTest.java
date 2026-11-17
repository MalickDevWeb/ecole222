package ecole_221.sn.ecole.modules.archivage.infrastructure.cle;

import ecole_221.sn.ecole.modules.archivage.application.format.ChiffrementFlux;
import ecole_221.sn.ecole.modules.archivage.domain.exception.ArchivageException;
import ecole_221.sn.ecole.modules.archivage.domain.port.CleDesArchives;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Clé maîtresse : ARCHIVES_CLE prioritaire ; refus en production sans elle ; clé de développement à part, droits 600. */
class CleDesArchivesConfigureeTest {

    @Test
    void archivesCleValideUtiliseeTelleQuelle(@TempDir Path dossier) {
        byte[] cle = ChiffrementFlux.nouvelleCle();
        CleDesArchives.CleMaitresse m = new CleDesArchivesConfiguree(Base64.getEncoder().encodeToString(cle), true,
                dossier.resolve("dev").toString()).obtenir();
        assertThat(m.octets()).isEqualTo(cle);
        assertThat(m.identifiant()).isEqualTo(ChiffrementFlux.identifiant(cle));
        assertThat(dossier.resolve("dev")).doesNotExist();   // aucune clé de développement créée
    }

    @Test
    void archivesCleInvalideRefusee(@TempDir Path dossier) {
        String courte = Base64.getEncoder().encodeToString(new byte[16]);
        assertThatThrownBy(() -> new CleDesArchivesConfiguree(courte, false, dossier.resolve("dev").toString()).obtenir())
                .isInstanceOf(ArchivageException.class).hasMessageContaining("32 octets");
        assertThatThrownBy(() -> new CleDesArchivesConfiguree("pas du base64 !", false, dossier.resolve("dev").toString()).obtenir())
                .isInstanceOf(ArchivageException.class).hasMessageContaining("ARCHIVES_CLE invalide");
    }

    @Test
    void productionSansCleRefusee(@TempDir Path dossier) {
        assertThatThrownBy(() -> new CleDesArchivesConfiguree("", true, dossier.resolve("dev").toString()).obtenir())
                .isInstanceOf(ArchivageException.class)
                .hasMessage("Archivage refusé : clé des archives absente (ARCHIVES_CLE).")
                .extracting(e -> ((ArchivageException) e).nature()).isEqualTo(ArchivageException.Nature.INDISPONIBLE);
        assertThat(dossier.resolve("dev")).doesNotExist();
    }

    @Test
    void developpementSansCleCreeUneCleAPartADroitsRestreints(@TempDir Path dossier) throws IOException {
        Path fichier = dossier.resolve(".ecole221").resolve("cle-archives-developpement");
        CleDesArchives.CleMaitresse premiere = new CleDesArchivesConfiguree(null, false, fichier.toString()).obtenir();
        assertThat(premiere.octets()).hasSize(32);
        assertThat(PosixFilePermissions.toString(Files.getPosixFilePermissions(fichier))).isEqualTo("rw-------");
        assertThat(PosixFilePermissions.toString(Files.getPosixFilePermissions(fichier.getParent()))).isEqualTo("rwx------");
        // relue à l'identique par une autre instance (redémarrage)
        CleDesArchives.CleMaitresse relue = new CleDesArchivesConfiguree("", false, fichier.toString()).obtenir();
        assertThat(relue.octets()).isEqualTo(premiere.octets());
    }
}
