package ecole_221.sn.ecole.modules.media.infrastructure.adapter.out.cloudinary;

import ecole_221.sn.ecole.modules.media.infrastructure.adapter.out.local.StockageLocalFichiers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Relecture des pièces par le serveur. Seuls les liens du compte Cloudinary de l'application (ou du stockage
 * local) sont relus : jamais une adresse quelconque (pas de requête vers un autre serveur à partir d'un dossier).
 */
class RelecturePiecesTest {

    @Test
    void lienCloudinaryDuCompteDecompose() {
        var pdf = CloudinaryStorageAdapter.analyser("https://res.cloudinary.com/moncompte/image/upload/v1794858464/file-d274c19b.pdf", "moncompte").orElseThrow();
        assertEquals("image", pdf.typeRessource());
        assertEquals("upload", pdf.typeLivraison());
        assertEquals("file-d274c19b", pdf.publicId());
        assertEquals("pdf", pdf.format());

        var dossier = CloudinaryStorageAdapter.analyser("https://res.cloudinary.com/moncompte/image/upload/c_fill,w_200/v12/ecoles/a.b/cni.PNG", "moncompte").orElseThrow();
        assertEquals("ecoles/a.b/cni", dossier.publicId());
        assertEquals("png", dossier.format());

        // « raw » : l'extension fait partie de l'identifiant
        var brut = CloudinaryStorageAdapter.analyser("https://res.cloudinary.com/moncompte/raw/upload/v3/file-1.docx", "moncompte").orElseThrow();
        assertEquals("file-1.docx", brut.publicId());
        assertEquals("", brut.format());
    }

    @Test
    void lienEtrangerJamaisRelu() {
        for (String lien : new String[]{
                "https://res.cloudinary.com/autrecompte/image/upload/v1/file-1.pdf",   // autre compte Cloudinary
                "http://res.cloudinary.com/moncompte/image/upload/v1/file-1.pdf",      // pas https
                "https://evil.example/res.cloudinary.com/moncompte/image/upload/v1/x.pdf",
                "https://res.cloudinary.com.evil.example/moncompte/image/upload/v1/x.pdf",
                "https://res.cloudinary.com/moncompte/image/upload/v1/../../x.pdf",
                "https://res.cloudinary.com/moncompte/image/upload/v1/%2E%2E/x.pdf",
                "https://res.cloudinary.com/moncompte/image/fetch/v1/https://evil.example/x.pdf",
                "file:///etc/passwd", "", "   "}) {
            assertTrue(CloudinaryStorageAdapter.analyser(lien, "moncompte").isEmpty(), lien);
        }
        assertTrue(CloudinaryStorageAdapter.analyser("https://res.cloudinary.com/x/image/upload/v1/a.pdf", "").isEmpty());
    }

    @Test
    void typeDuFichierDapresLExtension() {
        assertEquals("application/pdf", CloudinaryStorageAdapter.typeDepuisNom("releves.PDF", "application/octet-stream"));
        assertEquals("image/jpeg", CloudinaryStorageAdapter.typeDepuisNom("cni.jpeg", null));
        assertEquals("image/png", CloudinaryStorageAdapter.typeDepuisNom("photo.png", "text/html"));
        assertEquals("application/msword", CloudinaryStorageAdapter.typeDepuisNom("cv.doc", "application/msword; charset=x"));
        assertEquals("application/octet-stream", CloudinaryStorageAdapter.typeDepuisNom("sans-extension", null));
    }

    @Test
    void lienEtrangerOuInconnuRenvoieVide(@TempDir Path dossier) throws Exception {
        var adapter = new CloudinaryStorageAdapter("moncompte", "cle", "secret", false, new StockageLocalFichiers(dossier.toString()));
        assertTrue(adapter.relire("https://evil.example/x.pdf").isEmpty());
        assertTrue(adapter.relire(null).isEmpty());
        assertTrue(adapter.relire("/api/public/media/inexistant.pdf").isEmpty());
    }

    @Test
    void fichierLocalDuModeSimulationRelu(@TempDir Path dossier) throws Exception {
        var local = new StockageLocalFichiers(dossier.toString());
        var adapter = new CloudinaryStorageAdapter("", "", "", true, local);
        byte[] pdf = "%PDF-1.4 releves".getBytes();
        String lien = adapter.upload(pdf, "releves.pdf", "application/pdf").getUrl();

        var relu = adapter.relire(lien).orElseThrow();
        assertArrayEquals(pdf, relu.contenu());
        assertEquals("application/pdf", relu.typeContenu());
        // même fichier par une adresse absolue du serveur
        assertArrayEquals(pdf, adapter.relire("http://localhost:8090" + lien).orElseThrow().contenu());
        // remontée de dossier refusée
        assertTrue(adapter.relire("/api/public/media/..%2F..%2Fetc%2Fpasswd").isEmpty());
    }

    /** Le lien gardé d'un fichier confidentiel n'a plus de signature (ouvert hors de l'application : refusé). */
    @Test
    void lienConfidentielSansSignature() {
        assertEquals("https://res.cloudinary.com/c/image/authenticated/v17/file-1.pdf",
                CloudinaryStorageAdapter.sansSignature("https://res.cloudinary.com/c/image/authenticated/s--Ab_c-12XY--/v17/file-1.pdf"));
        assertEquals("https://res.cloudinary.com/c/image/upload/v1/a.png",
                CloudinaryStorageAdapter.sansSignature("https://res.cloudinary.com/c/image/upload/v1/a.png"));
        // le serveur sait toujours relire ce lien (type « authenticated »)
        var lien = CloudinaryStorageAdapter.analyser("https://res.cloudinary.com/c/image/authenticated/v17/file-1.pdf", "c").orElseThrow();
        assertEquals("authenticated", lien.typeLivraison());
        assertEquals("file-1", lien.publicId());
    }

    /** En simulation, un fichier confidentiel n'est jamais servi par /public/media, mais le serveur le relit. */
    @Test
    void fichierLocalConfidentielJamaisPublic(@TempDir Path dossier) throws Exception {
        var local = new StockageLocalFichiers(dossier.toString());
        var adapter = new CloudinaryStorageAdapter("", "", "", true, local);
        byte[] cni = "%PDF-1.4 cni".getBytes();
        String lien = adapter.upload(cni, "cni.pdf", "application/pdf", true).getUrl();
        String nom = lien.substring(lien.lastIndexOf('/') + 1);
        assertTrue(nom.startsWith(StockageLocalFichiers.PREFIXE_PRIVE), lien);

        var public_ = new ecole_221.sn.ecole.modules.media.infrastructure.adapter.out.local.FichiersLocauxController(local);
        var refus = assertThrows(org.springframework.web.server.ResponseStatusException.class, () -> public_.lire(nom));
        assertEquals(404, refus.getStatusCode().value());
        assertArrayEquals(cni, adapter.relire(lien).orElseThrow().contenu());

        // un fichier ordinaire reste servi publiquement (logos, photos)
        String logo = adapter.upload(new byte[]{(byte) 0x89, 'P', 'N', 'G'}, "logo.png", "image/png").getUrl();
        assertEquals(200, public_.lire(logo.substring(logo.lastIndexOf('/') + 1)).getStatusCode().value());
    }
}
