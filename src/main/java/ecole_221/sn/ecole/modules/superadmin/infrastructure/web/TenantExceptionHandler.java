package ecole_221.sn.ecole.modules.superadmin.infrastructure.web;

import ecole_221.sn.ecole.modules.superadmin.domain.exception.TenantEmailAlreadyExistsException;
import ecole_221.sn.ecole.modules.superadmin.domain.exception.TenantNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Gestion centralisée des exceptions pour le module Tenants.
 */
@RestControllerAdvice(assignableTypes = TenantController.class)
public class TenantExceptionHandler {

    @ExceptionHandler(TenantNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(TenantNotFoundException ex) {
        return errorResponse(HttpStatus.NOT_FOUND, "Not Found", ex.getMessage());
    }

    @ExceptionHandler(TenantEmailAlreadyExistsException.class)
    public ResponseEntity<Map<String, Object>> handleEmailConflict(TenantEmailAlreadyExistsException ex) {
        return errorResponse(HttpStatus.CONFLICT, "Conflict", ex.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidState(IllegalStateException ex) {
        // Levé par Organisation.validate() / suspend() / reactivate() si état invalide
        return errorResponse(HttpStatus.UNPROCESSABLE_ENTITY, "Unprocessable Entity", ex.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleBadRequest(IllegalArgumentException ex) {
        return errorResponse(HttpStatus.BAD_REQUEST, "Bad Request", ex.getMessage());
    }

    // ── Utilitaire ─────────────────────────────────────────────────────────────

    private ResponseEntity<Map<String, Object>> errorResponse(
            HttpStatus status, String error, String message) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", status.value());
        body.put("error", error);
        body.put("message", message);
        return ResponseEntity.status(status).body(body);
    }
}
