package ecole_221.sn.ecole.infrastructure.web;

import ecole_221.sn.ecole.shared.error.ApiErrorResponse;
import ecole_221.sn.ecole.shared.error.AppException;
import ecole_221.sn.ecole.shared.error.ErrorCode;
import ecole_221.sn.ecole.shared.infrastructure.tenant.TenantExpiredException;
import ecole_221.sn.ecole.shared.infrastructure.tenant.TenantNotFoundException;
import ecole_221.sn.ecole.shared.infrastructure.tenant.TenantSuspendedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(AppException.class)
    public ResponseEntity<ApiErrorResponse> handleAppException(AppException ex) {
        log.warn("[API Error] {} - {}", ex.getErrorCode(), ex.getMessage());
        return ResponseEntity
                .status(ex.getErrorCode().getHttpStatus())
                .body(ApiErrorResponse.of(ex.getErrorCode(), ex.getMessage(), ex.getDetails()));
    }

    @ExceptionHandler(TenantSuspendedException.class)
    public ResponseEntity<ApiErrorResponse> handleTenantSuspended(TenantSuspendedException ex) {
        log.warn("[Tenant Suspended] {}", ex.getMessage());
        return ResponseEntity
                .status(ErrorCode.TENANT_SUSPENDED.getHttpStatus())
                .body(ApiErrorResponse.of(ErrorCode.TENANT_SUSPENDED, ex.getMessage()));
    }

    @ExceptionHandler(TenantExpiredException.class)
    public ResponseEntity<ApiErrorResponse> handleTenantExpired(TenantExpiredException ex) {
        log.warn("[Tenant Expired] {}", ex.getMessage());
        return ResponseEntity
                .status(ErrorCode.TENANT_EXPIRED.getHttpStatus())
                .body(ApiErrorResponse.of(ErrorCode.TENANT_EXPIRED, ex.getMessage()));
    }

    @ExceptionHandler(TenantNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleTenantNotFound(TenantNotFoundException ex) {
        log.warn("[Tenant Not Found] {}", ex.getMessage());
        return ResponseEntity
                .status(ErrorCode.TENANT_NOT_FOUND.getHttpStatus())
                .body(ApiErrorResponse.of(ErrorCode.TENANT_NOT_FOUND, ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidationErrors(MethodArgumentNotValidException ex) {
        Map<String, Object> details = new HashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            details.put(fieldError.getField(), fieldError.getDefaultMessage());
        }
        log.warn("[Validation Error] {}", details);
        return ResponseEntity
                .badRequest()
                .body(ApiErrorResponse.of(ErrorCode.VALIDATION_ERROR, "Erreur de validation des champs du formulaire.", details));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        String msg = ex.getMessage() != null ? ex.getMessage() : "Argument invalide";
        log.warn("[IllegalArgument] {}", msg);

        // Détection intelligente des collisions d'unicité usuelles
        if (msg.contains("email") && msg.contains("déjà")) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ApiErrorResponse.of(ErrorCode.USER_EMAIL_ALREADY_EXISTS, msg));
        }
        if (msg.contains("téléphone") && msg.contains("déjà")) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ApiErrorResponse.of(ErrorCode.USER_PHONE_ALREADY_EXISTS, msg));
        }
        if (msg.contains("slug") && msg.contains("déjà")) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ApiErrorResponse.of(ErrorCode.TENANT_SLUG_ALREADY_EXISTS, msg));
        }

        return ResponseEntity
                .badRequest()
                .body(ApiErrorResponse.of(ErrorCode.VALIDATION_ERROR, msg));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalState(IllegalStateException ex) {
        String msg = ex.getMessage() != null ? ex.getMessage() : "État invalide";
        log.warn("[IllegalState] {}", msg);
        return ResponseEntity
                .badRequest()
                .body(ApiErrorResponse.of(ErrorCode.OPERATION_NOT_ALLOWED, msg));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiErrorResponse> handleResponseStatus(ResponseStatusException ex) {
        HttpStatus status = (HttpStatus) ex.getStatusCode();
        String msg = ex.getReason() != null ? ex.getReason() : status.getReasonPhrase();
        log.warn("[ResponseStatus] {} - {}", status, msg);
        
        ErrorCode code = status == HttpStatus.UNAUTHORIZED ? ErrorCode.AUTH_INVALID_CREDENTIALS :
                         status == HttpStatus.FORBIDDEN ? ErrorCode.AUTH_ACCESS_DENIED :
                         status == HttpStatus.NOT_FOUND ? ErrorCode.RESOURCE_NOT_FOUND :
                         ErrorCode.VALIDATION_ERROR;

        return ResponseEntity
                .status(status)
                .body(ApiErrorResponse.of(code, msg));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGeneralException(Exception ex) {
        log.error("[Unhandled Exception] Erreur serveur non interceptée", ex);
        String msg = ex.getMessage() != null ? ex.getMessage() : "Une erreur interne est survenue.";
        return ResponseEntity
                .internalServerError()
                .body(ApiErrorResponse.of(ErrorCode.INTERNAL_SERVER_ERROR, msg));
    }
}
