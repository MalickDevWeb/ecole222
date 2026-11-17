package ecole_221.sn.ecole.modules.archivage.infrastructure.stockage;

import ecole_221.sn.ecole.modules.archivage.domain.exception.ArchivageException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Stockage Cloudinary : découpage en morceaux, manifeste, relecture vérifiée, suppression — sans réseau (faux client). */
class StockageArchiveCloudinaryTest {

    /** Faux Cloudinary en mémoire, qui refuse comme la vraie offre gratuite un fichier brut de plus de 10 Mo. */
    static final class FauxCloudinary implements ClientCloudinaryArchives {
        final Map<String, byte[]> fichiers = new LinkedHashMap<>();
        int limite = 10_000_000;

        @Override
        public void televerser(String publicId, byte[] contenu) throws IOException {
            if (contenu.length >= limite) throw new IOException("Fichier brut trop volumineux");
            fichiers.put(publicId, contenu.clone());
        }

        @Override
        public byte[] telecharger(String publicId) throws IOException {
            byte[] b = fichiers.get(publicId);
            if (b == null) throw new IOException("introuvable");
            return b.clone();
        }

        @Override
        public void detruire(String publicId) {
            fichiers.remove(publicId);
        }

        @Override
        public boolean existe(String publicId) {
            return fichiers.containsKey(publicId);
        }
    }

    private static Path fichier(Path dossier, int taille) throws IOException {
        byte[] b = new byte[taille];
        new Random(taille).nextBytes(b);
        return Files.write(dossier.resolve("archive-" + taille), b);
    }

    @Test
    void morceauxSousLaLimiteDesFichiersBrutsGratuits() {
        assertThat(StockageArchiveCloudinary.MORCEAU).isLessThan(10_000_000);
    }

    @Test
    void decoupeRelitEtSupprime(@TempDir Path dossier) throws IOException {
        FauxCloudinary faux = new FauxCloudinary();
        faux.limite = 1000;
        StockageArchiveCloudinary stockage = new StockageArchiveCloudinary(faux, "ecole221-archives", 400);
        Path source = fichier(dossier, 1000);   // 400 + 400 + 200
        String emplacement = stockage.ecrire("arc-1", source);
        assertThat(emplacement).isEqualTo("ecole221-archives/arc-1/manifeste.json");
        assertThat(faux.fichiers.keySet()).containsExactly("ecole221-archives/arc-1/partie-0001.bin",
                "ecole221-archives/arc-1/partie-0002.bin", "ecole221-archives/arc-1/partie-0003.bin", emplacement);
        assertThat(faux.fichiers.values()).allSatisfy(b -> assertThat(b.length).isLessThan(faux.limite));
        try (InputStream in = stockage.lire(emplacement)) {
            assertThat(in.readAllBytes()).isEqualTo(Files.readAllBytes(source));
        }
        assertThat(stockage.existe(emplacement)).isTrue();
        stockage.supprimer(emplacement);
        assertThat(faux.fichiers).isEmpty();
        assertThat(stockage.existe(emplacement)).isFalse();
        stockage.supprimer(emplacement);   // sans erreur s'il n'existe plus
    }

    @Test
    void unMorceauAltereEstRefuseALaRelecture(@TempDir Path dossier) throws IOException {
        FauxCloudinary faux = new FauxCloudinary();
        StockageArchiveCloudinary stockage = new StockageArchiveCloudinary(faux, "x", 100);
        String emplacement = stockage.ecrire("arc-2", fichier(dossier, 250));
        faux.fichiers.get("x/arc-2/partie-0002.bin")[5] ^= 1;
        assertThatThrownBy(() -> {
            try (InputStream in = stockage.lire(emplacement)) {
                in.readAllBytes();
            }
        }).isInstanceOf(ArchivageException.class).hasMessageContaining("Morceau 2");
    }

    @Test
    void refusClairSansConfiguration(@TempDir Path dossier) throws IOException {
        StockageArchiveCloudinary stockage = new StockageArchiveCloudinary(null, "x");
        Path f = fichier(dossier, 10);
        assertThatThrownBy(stockage::verifierPret).isInstanceOf(ArchivageException.class)
                .hasMessageContaining("Cloudinary n'est pas configuré")
                .extracting(e -> ((ArchivageException) e).nature()).isEqualTo(ArchivageException.Nature.INDISPONIBLE);
        assertThatThrownBy(() -> stockage.ecrire("arc-3", f)).isInstanceOf(ArchivageException.class);
    }

    @Test
    void choixDuStockageParUnSeulReglage(@TempDir Path dossier) {
        ConfigurationStockageArchive config = new ConfigurationStockageArchive();
        assertThat(config.stockageArchive("local", false, dossier.toString(), false, "d", "", "", "").nom()).isEqualTo("local");
        assertThat(config.stockageArchive("cloudinary", true, dossier.toString(), false, "d", "", "", "").nom()).isEqualTo("cloudinary");
        assertThatThrownBy(() -> config.stockageArchive("cloudinary", true, dossier.toString(), false, "d", "", "", "").verifierPret())
                .hasMessageContaining("Cloudinary n'est pas configuré");
        assertThatThrownBy(() -> config.stockageArchive("s3", false, dossier.toString(), false, "d", "", "", "").verifierPret())
                .isInstanceOf(ArchivageException.class).hasMessageContaining("inconnu");
    }
}
