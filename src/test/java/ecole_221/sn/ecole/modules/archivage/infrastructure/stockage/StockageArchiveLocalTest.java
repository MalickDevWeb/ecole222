package ecole_221.sn.ecole.modules.archivage.infrastructure.stockage;

import ecole_221.sn.ecole.modules.archivage.domain.exception.ArchivageException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Stockage local : dossier à droits restreints, écriture atomique, refus en production, aucun chemin détourné. */
class StockageArchiveLocalTest {

    @Test
    void ecritRelitSupprimeAvecDroitsRestreints(@TempDir Path racine) throws IOException {
        Path dossier = racine.resolve("archives");
        StockageArchiveLocal stockage = new StockageArchiveLocal(dossier, false, false);
        stockage.verifierPret();
        assertThat(PosixFilePermissions.toString(Files.getPosixFilePermissions(dossier))).isEqualTo("rwx------");
        Path source = Files.writeString(racine.resolve("source"), "contenu chiffré");
        String emplacement = stockage.ecrire("arc-1", source);
        assertThat(emplacement).isEqualTo("arc-1.e221a");
        assertThat(PosixFilePermissions.toString(Files.getPosixFilePermissions(dossier.resolve(emplacement)))).isEqualTo("rw-------");
        try (InputStream in = stockage.lire(emplacement)) {
            assertThat(new String(in.readAllBytes())).isEqualTo("contenu chiffré");
        }
        assertThat(stockage.existe(emplacement)).isTrue();
        stockage.supprimer(emplacement);
        assertThat(stockage.existe(emplacement)).isFalse();
    }

    @Test
    void refuseEnProductionSaufAutorisationExplicite(@TempDir Path racine) {
        assertThatThrownBy(() -> new StockageArchiveLocal(racine, true, false).verifierPret())
                .isInstanceOf(ArchivageException.class)
                .hasMessageContaining("Archivage refusé en production")
                .extracting(e -> ((ArchivageException) e).nature()).isEqualTo(ArchivageException.Nature.INDISPONIBLE);
        new StockageArchiveLocal(racine, true, true).verifierPret();   // disque persistant déclaré
    }

    @Test
    void aucunEmplacementHorsDuDossier(@TempDir Path racine) {
        StockageArchiveLocal stockage = new StockageArchiveLocal(racine, false, false);
        assertThatThrownBy(() -> stockage.lire("../../etc/passwd")).isInstanceOf(ArchivageException.class);
        assertThatThrownBy(() -> stockage.lire("/etc/passwd.e221a")).isInstanceOf(ArchivageException.class);
        assertThatThrownBy(() -> stockage.supprimer("..e221a")).isInstanceOf(ArchivageException.class);
    }
}
