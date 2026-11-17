package ecole_221.sn.ecole.infrastructure.web;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;

/** Erreurs dues à la requête : refus 4xx explicite au lieu d'une erreur 500. */
class RefusExpliqueTest {

    static class SalleNotFoundException extends RuntimeException {
        SalleNotFoundException(String m) { super(m); }
    }

    private static int statut(Throwable t) {
        var r = GlobalExceptionHandler.refusExplique(t);
        return r == null ? 500 : r.getStatusCode().value();
    }

    @Test
    void casDeRequeteInvalideTraduitsEn4xx() {
        assertThat(statut(new SalleNotFoundException("Salle introuvable"))).isEqualTo(404);
        assertThat(statut(new RuntimeException("Impossible d'envoyer le message",
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Discussion introuvable.")))).isEqualTo(404);
        assertThat(statut(new RuntimeException("Impossible de créer le groupe",
                new NullPointerException("L'ID du créateur ne peut pas être null")))).isEqualTo(400);
        assertThat(statut(new NullPointerException("Le nom ne peut pas être null"))).isEqualTo(400);
        assertThat(statut(new org.springframework.dao.DataIntegrityViolationException("x",
                new RuntimeException("ERROR: value too long for type character varying(255)")))).isEqualTo(400);
        assertThat(statut(new org.springframework.dao.DataIntegrityViolationException("x",
                new RuntimeException("ERROR: duplicate key value violates unique constraint")))).isEqualTo(409);
        assertThat(statut(new org.springframework.web.HttpMediaTypeNotSupportedException("text/plain"))).isEqualTo(415);
        assertThat(statut(new java.time.format.DateTimeParseException("x", "abc", 0))).isEqualTo(400);
        assertThat(statut(new ClassCastException("class java.lang.Long cannot be cast to class java.lang.String"))).isEqualTo(400);
        assertThat(statut(new org.springframework.orm.ObjectOptimisticLockingFailureException("x", new RuntimeException()))).isEqualTo(409);
    }

    @Test
    void vraieErreurServeurResteEn500SansDetail() {
        assertThat(statut(new NullPointerException("Cannot invoke \"String.length()\" because \"x\" is null"))).isEqualTo(500);
        assertThat(statut(new IllegalStateException("bug"))).isEqualTo(500);
        assertThat(GlobalExceptionHandler.refusExplique(new SalleNotFoundException("Salle introuvable")).getBody().message())
                .isEqualTo("Salle introuvable");
    }
}
