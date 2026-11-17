package ecole_221.sn.ecole.modules.media.infrastructure.adapter;

import ecole_221.sn.ecole.modules.media.domain.exception.StockageIndisponibleException;
import ecole_221.sn.ecole.modules.media.infrastructure.adapter.out.cloudinary.CloudinaryStorageAdapter;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** APP-BE-05 : sans stockage configuré, erreur explicite — jamais d'image de démonstration présentée comme le document. */
class CloudinaryStockageTest {

    @Test
    void sansConfigurationErreurExplicite() {
        var adapter = new CloudinaryStorageAdapter("", "", "", false);
        assertThrows(StockageIndisponibleException.class, () -> adapter.upload(new byte[]{1, 2, 3}, "cni.pdf", "application/pdf"));
    }

    @Test
    void simulationSeulementSiDemandee() throws Exception {
        var adapter = new CloudinaryStorageAdapter("", "", "", true);
        var fichier = adapter.upload(new byte[]{1, 2, 3}, "cni.pdf", "application/pdf");
        assertNotNull(fichier);
    }

    /** En simulation, le VRAI fichier est conservé et servi. */
    @Test
    void simulationConserveLeVraiFichier(@org.junit.jupiter.api.io.TempDir java.nio.file.Path dossier) throws Exception {
        var local = new ecole_221.sn.ecole.modules.media.infrastructure.adapter.out.local.StockageLocalFichiers(dossier.toString());
        var adapter = new CloudinaryStorageAdapter("", "", "", true, local);
        byte[] logo = {(byte) 0x89, 'P', 'N', 'G', 1, 2, 3};
        var fichier = adapter.upload(logo, "logo école.png", "image/png");
        String url = fichier.getUrl();
        org.junit.jupiter.api.Assertions.assertTrue(url.startsWith("/api/public/media/") && url.endsWith(".png"), url);
        org.junit.jupiter.api.Assertions.assertFalse(url.contains("sample.jpg"));
        String nom = url.substring(url.lastIndexOf('/') + 1);
        org.junit.jupiter.api.Assertions.assertArrayEquals(logo, java.nio.file.Files.readAllBytes(local.lire(nom).orElseThrow()));
        // aucun autre fichier accessible : noms hors format refusés (remontée de dossier, etc.)
        for (String piege : new String[]{"../../etc/passwd", "..%2F..%2Fetc", nom + "/..", "abc.png", ""}) {
            org.junit.jupiter.api.Assertions.assertTrue(local.lire(piege).isEmpty(), piege);
        }
    }

    /**
     * En simulation, lien COMPLET sur l'adresse du site (app.url-frontend). Un lien relatif serait refusé
     * par les contrôles « http(s) » des dépôts.
     */
    @Test
    void simulationLienCompletAccepteAuDepot(@org.junit.jupiter.api.io.TempDir java.nio.file.Path dossier) throws Exception {
        var local = new ecole_221.sn.ecole.modules.media.infrastructure.adapter.out.local.StockageLocalFichiers(dossier.toString());
        var adapter = new CloudinaryStorageAdapter("", "", "", true, local, "http://127.0.0.1:5174/");
        byte[] cni = "%PDF-1.4 cni".getBytes();
        String lien = adapter.upload(cni, "cni.pdf", "application/pdf", true).getUrl();
        org.junit.jupiter.api.Assertions.assertTrue(lien.startsWith("http://127.0.0.1:5174/api/public/media/prive-") && lien.endsWith(".pdf"), lien);
        org.junit.jupiter.api.Assertions.assertEquals(java.util.Map.of("pieceIdentiteUrl", lien),
                ecole_221.sn.ecole.modules.admissions.application.service.SuiviCandidatureService.lirePieces(
                        java.util.Map.of("pieceIdentite", lien), false));
        org.junit.jupiter.api.Assertions.assertArrayEquals(cni, adapter.relire(lien).orElseThrow().contenu());
    }
}
