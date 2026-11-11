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
}
