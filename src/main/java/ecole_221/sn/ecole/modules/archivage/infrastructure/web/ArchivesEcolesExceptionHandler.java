package ecole_221.sn.ecole.modules.archivage.infrastructure.web;

import ecole_221.sn.ecole.modules.archivage.domain.exception.ArchivageException;
import ecole_221.sn.ecole.shared.error.ApiErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

/**
 * Refus de l'archivage traduits en réponses courtes (même enveloppe que ApiErrorResponse) :
 * INVALIDE 400, INTROUVABLE 404, CONFLIT 409, REFUS 422, INDISPONIBLE 503 ; {@code details.conflits} le cas échéant.
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = ArchivesEcolesController.class)
public class ArchivesEcolesExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ArchivesEcolesExceptionHandler.class);

    @ExceptionHandler(ArchivageException.class)
    public ResponseEntity<ApiErrorResponse> refus(ArchivageException e) {
        HttpStatus statut = switch (e.nature()) {
            case INVALIDE -> HttpStatus.BAD_REQUEST;
            case INTROUVABLE -> HttpStatus.NOT_FOUND;
            case CONFLIT -> HttpStatus.CONFLICT;
            case REFUS -> HttpStatus.UNPROCESSABLE_CONTENT;
            case INDISPONIBLE -> HttpStatus.SERVICE_UNAVAILABLE;
        };
        log.warn("[Archivage] {} — {}", statut.value(), e.getMessage());
        return ResponseEntity.status(statut).body(new ApiErrorResponse(false, "ARCHIVAGE_" + e.nature().name(),
                e.getMessage(), e.getMessage(), statut.value(), Instant.now(), e.details().isEmpty() ? null : e.details()));
    }
}
