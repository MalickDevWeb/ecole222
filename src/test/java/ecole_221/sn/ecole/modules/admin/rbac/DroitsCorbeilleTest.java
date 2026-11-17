package ecole_221.sn.ecole.modules.admin.rbac;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Corbeille (16/11) : le personnel (DIRECTEUR, SECRETAIRE…) atteint /corbeille par une route commune ; chaque
 * restauration exige ensuite, dans CorbeilleService, le droit d'écriture du module de l'élément.
 */
class DroitsCorbeilleTest {

    @Test
    void corbeilleOuverteAuPersonnelParUneRouteCommune() {
        assertThat(CataloguePermissions.routeCommune("GET", "/corbeille")).isTrue();
        assertThat(CataloguePermissions.routeCommune("POST", "/corbeille/salle/r1/restaurer")).isTrue();
        assertThat(CataloguePermissions.routeCommune("POST", "/corbeille/salle/r1/purger")).isTrue();
        assertThat(CataloguePermissions.routeCommune("GET", "/corbeilles")).isFalse();
        assertThat(CataloguePermissions.routeCommune("DELETE", "/planning/rooms/r1")).isFalse();
    }
}
