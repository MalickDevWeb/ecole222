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

    /** Cours introuvable : 404 (et non 500), quel que soit le module qui lève l'exception. */
    @ExceptionHandler({ecole_221.sn.ecole.partage.exception.CourseNotFoundException.class,
            ecole_221.sn.ecole.modules.professor.domain.exception.CourseNotFoundException.class})
    public ResponseEntity<ApiErrorResponse> handleCourseNotFound(RuntimeException ex) {
        log.warn("[Course Not Found] {}", ex.getMessage());
        return ResponseEntity
                .status(ErrorCode.COURSE_NOT_FOUND.getHttpStatus())
                .body(ApiErrorResponse.of(ErrorCode.COURSE_NOT_FOUND, ex.getMessage()));
    }

    /** Route inexistante : 404. */
    @ExceptionHandler(org.springframework.web.servlet.resource.NoResourceFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNoResource(org.springframework.web.servlet.resource.NoResourceFoundException ex) {
        log.debug("[Not Found] {}", ex.getMessage());
        return ResponseEntity
                .status(ErrorCode.RESOURCE_NOT_FOUND.getHttpStatus())
                .body(ApiErrorResponse.of(ErrorCode.RESOURCE_NOT_FOUND, "Route inexistante."));
    }

    /**
     * Requête mal formée (paramètre obligatoire absent, valeur de mauvais type, corps JSON illisible) :
     * 400 avec un message en français.
     */
    @ExceptionHandler({org.springframework.web.bind.MissingServletRequestParameterException.class,
            org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class,
            org.springframework.http.converter.HttpMessageNotReadableException.class,
            org.springframework.web.bind.MissingRequestHeaderException.class})
    public ResponseEntity<ApiErrorResponse> handleRequeteMalFormee(Exception ex) {
        String message;
        if (ex instanceof org.springframework.web.bind.MissingServletRequestParameterException m) {
            message = "Paramètre obligatoire manquant : " + m.getParameterName() + ".";
        } else if (ex instanceof org.springframework.web.method.annotation.MethodArgumentTypeMismatchException m) {
            message = "Valeur invalide pour le paramètre « " + m.getName() + " ».";
        } else if (ex instanceof org.springframework.web.bind.MissingRequestHeaderException m) {
            message = "En-tête obligatoire manquant : " + m.getHeaderName() + ".";
        } else {
            message = messageCorpsIllisible(ex);
        }
        log.debug("[Bad Request] {}", ex.getMessage());
        return ResponseEntity.status(org.springframework.http.HttpStatus.BAD_REQUEST)
                .body(ApiErrorResponse.of(ErrorCode.VALIDATION_ERROR, message));
    }

    /**
     * Corps JSON illisible : nomme le champ fautif quand Jackson le connaît (APP-QA-04).
     * Ex. « Le champ « capacity » n'a pas le bon type (nombre attendu). » au lieu d'un message générique.
     */
    static String messageCorpsIllisible(Throwable ex) {
        Throwable c = ex;
        while (c != null && !(c instanceof tools.jackson.databind.exc.MismatchedInputException)) {
            c = c.getCause() == c ? null : c.getCause();
        }
        if (c instanceof tools.jackson.databind.exc.MismatchedInputException m && m.getPath() != null && !m.getPath().isEmpty()) {
            String champ = m.getPath().stream()
                    .map(r -> r.getPropertyName() != null ? r.getPropertyName() : "[" + r.getIndex() + "]")
                    .reduce((x, y) -> y.startsWith("[") ? x + y : x + "." + y).orElse("?");
            Class<?> cible = m.getTargetType();
            String attendu = cible == null ? "" : String.class.equals(cible) ? " (texte attendu)"
                    : (Number.class.isAssignableFrom(cible) || (cible.isPrimitive() && cible != boolean.class)) ? " (nombre attendu)"
                    : (cible == Boolean.class || cible == boolean.class) ? " (true ou false attendu)"
                    : (java.util.Collection.class.isAssignableFrom(cible) || cible.isArray()) ? " (liste attendue)" : "";
            return "Le champ « " + champ + " » n'a pas le bon type" + attendu + ".";
        }
        return "Corps de requête illisible ou mal formé.";
    }

    /** Stockage indisponible : 503 explicite (APP-BE-05), jamais de faux succès. */
    @ExceptionHandler(ecole_221.sn.ecole.modules.media.domain.exception.StockageIndisponibleException.class)
    public ResponseEntity<ApiErrorResponse> handleStockageIndisponible(RuntimeException ex) {
        log.warn("[Storage Unavailable] {}", ex.getMessage());
        return ResponseEntity
                .status(ErrorCode.STORAGE_UNAVAILABLE.getHttpStatus())
                .body(ApiErrorResponse.of(ErrorCode.STORAGE_UNAVAILABLE, ex.getMessage()));
    }

    /** Fichier refusé (vide, trop volumineux, format ou contenu non conforme) : 400 et non 500. */
    @ExceptionHandler(ecole_221.sn.ecole.modules.media.domain.exception.InvalidFileException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidFile(RuntimeException ex) {
        log.warn("[Invalid File] {}", ex.getMessage());
        return ResponseEntity
                .status(ErrorCode.VALIDATION_ERROR.getHttpStatus())
                .body(ApiErrorResponse.of(ErrorCode.VALIDATION_ERROR, ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidationErrors(MethodArgumentNotValidException ex) {
        Map<String, Object> details = new HashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            details.put(fieldError.getField(), fieldError.getDefaultMessage());
        }
        log.warn("[Validation Error] {}", details);
        // message LISIBLE : les raisons réelles
        String raisons = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage).filter(java.util.Objects::nonNull).distinct()
                .collect(java.util.stream.Collectors.joining(" ; "));
        return ResponseEntity
                .badRequest()
                .body(ApiErrorResponse.of(ErrorCode.VALIDATION_ERROR,
                        raisons.isBlank() ? "Erreur de validation des champs du formulaire." : raisons, details));
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
        // Balayage de robustesse (qa/robustesse_api.py) : ces cas ne finissent pas en 500 « erreur interne »,
        // car il s'agit d'une requête invalide ou d'un élément introuvable.
        ResponseEntity<ApiErrorResponse> refus = refusExplique(ex);
        if (refus != null) {
            log.warn("[Requête refusée] {} — {}", refus.getStatusCode(), ex.getMessage());
            return refus;
        }
        log.error("[Unhandled Exception] Erreur serveur non interceptée", ex);
        return ResponseEntity
                .internalServerError()
                .body(ApiErrorResponse.of(ErrorCode.INTERNAL_SERVER_ERROR, "Une erreur interne inattendue est survenue. Veuillez réessayer plus tard."));
    }

    /** Réponse 4xx précise pour les erreurs dues à la requête ; null si c'est une vraie erreur du serveur. */
    static ResponseEntity<ApiErrorResponse> refusExplique(Throwable ex) {
        String nom = ex.getClass().getSimpleName();
        if (ex instanceof org.springframework.security.authorization.AuthorizationDeniedException) {
            return statut(HttpStatus.FORBIDDEN, ErrorCode.AUTH_ACCESS_DENIED, "Accès refusé : vous n'avez pas les autorisations nécessaires.");
        }
        if (ex instanceof org.springframework.web.HttpMediaTypeNotSupportedException) {
            return statut(HttpStatus.UNSUPPORTED_MEDIA_TYPE, ErrorCode.VALIDATION_ERROR,
                    "Format de requête non pris en charge : envoyez du JSON (ou un formulaire multipart pour un fichier).");
        }
        // Valeur du mauvais type dans un corps lu « à la main » (nombre, liste ou objet à la place d'un texte…)
        if (ex instanceof ClassCastException) {
            return statut(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_ERROR,
                    "Valeur de type inattendu dans la requête : vérifiez que chaque champ a le bon format (texte, nombre, liste).");
        }
        if (ex instanceof org.springframework.http.converter.HttpMessageConversionException) {
            return statut(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_ERROR, "Corps de requête illisible ou mal formé.");
        }
        if (ex instanceof org.springframework.orm.ObjectOptimisticLockingFailureException) {
            return statut(HttpStatus.CONFLICT, ErrorCode.VALIDATION_ERROR,
                    "Cet élément a été modifié ou supprimé entre-temps : rechargez la page.");
        }
        if (ex instanceof org.springframework.web.HttpRequestMethodNotSupportedException) {
            return statut(HttpStatus.METHOD_NOT_ALLOWED, ErrorCode.VALIDATION_ERROR, "Méthode HTTP non autorisée sur cette adresse.");
        }
        if (ex instanceof org.springframework.web.multipart.support.MissingServletRequestPartException
                || ex instanceof org.springframework.web.multipart.MultipartException) {
            return statut(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_ERROR, "Fichier manquant ou illisible : envoyez-le dans le champ « file ».");
        }
        if (ex instanceof org.springframework.web.multipart.MaxUploadSizeExceededException) {
            return statut(HttpStatus.PAYLOAD_TOO_LARGE, ErrorCode.VALIDATION_ERROR, "Fichier trop volumineux.");
        }
        // Cause réelle, même enveloppée (ex. collaboration : « Impossible de … » autour de « discussion introuvable ») :
        // exceptions « …NotFoundException » → 404 ; ResponseStatusException → son statut ; IllegalArgumentException → 400.
        for (Throwable t = ex; t != null; t = t.getCause()) {
            if (t.getClass().getSimpleName().endsWith("NotFoundException")) {
                return statut(HttpStatus.NOT_FOUND, ErrorCode.RESOURCE_NOT_FOUND,
                        t.getMessage() != null ? t.getMessage() : "Élément introuvable.");
            }
            if (t != ex && t instanceof org.springframework.web.server.ResponseStatusException r) {
                HttpStatus s = HttpStatus.valueOf(r.getStatusCode().value());
                return statut(s, s == HttpStatus.NOT_FOUND ? ErrorCode.RESOURCE_NOT_FOUND : ErrorCode.VALIDATION_ERROR,
                        r.getReason() != null ? r.getReason() : s.getReasonPhrase());
            }
            if (t != ex && t instanceof IllegalArgumentException && t.getMessage() != null) {
                return statut(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_ERROR, t.getMessage());
            }
            if (t instanceof NullPointerException && t.getMessage() != null && t.getMessage().contains("ne peut pas être null")) {
                return statut(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_ERROR,
                        t.getMessage().replace("ne peut pas être null", "est obligatoire") + ".");
            }
        }
        if (ex instanceof java.time.format.DateTimeParseException || ex.getCause() instanceof java.time.format.DateTimeParseException) {
            return statut(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_ERROR, "Date ou heure invalide (format attendu : AAAA-MM-JJ).");
        }
        String bd = racine(ex);
        if (ex instanceof org.springframework.dao.DataIntegrityViolationException || ex instanceof org.springframework.orm.jpa.JpaSystemException
                || nom.contains("DataException") || bd.contains("PSQLException") || bd.contains("IdentifierGenerationException")) {
            String m = bd.toLowerCase(java.util.Locale.ROOT);
            if (m.contains("invalid byte sequence") || m.contains("0x00")) {
                return statut(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_ERROR, "Le texte contient un caractère non autorisé (caractère nul).");
            }
            if (m.contains("too long") || m.contains("trop long")) {
                return statut(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_ERROR, "Une valeur dépasse la longueur autorisée.");
            }
            if (m.contains("duplicate key") || m.contains("unique")) {
                return statut(HttpStatus.CONFLICT, ErrorCode.VALIDATION_ERROR, "Cet élément existe déjà.");
            }
            if (m.contains("not-null") || m.contains("null value")) {
                return statut(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_ERROR, "Une donnée obligatoire est manquante.");
            }
            if (m.contains("identifier of entity") || m.contains("identifiergeneration")) {
                return statut(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_ERROR, "Identifiant manquant : l'élément ne peut pas être enregistré.");
            }
            if (m.contains("check constraint") || m.contains("foreign key") || m.contains("out of range") || m.contains("invalid input")) {
                return statut(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_ERROR, "Valeur refusée par la base de données : vérifiez les données envoyées.");
            }
        }
        // Champ obligatoire absent signalé explicitement par le domaine (Objects.requireNonNull(x, "… ne peut pas être null"))
        if (ex instanceof NullPointerException && ex.getMessage() != null && ex.getMessage().contains("ne peut pas être null")) {
            return statut(HttpStatus.BAD_REQUEST, ErrorCode.VALIDATION_ERROR,
                    ex.getMessage().replace("ne peut pas être null", "est obligatoire") + ".");
        }
        return null;
    }

    private static String racine(Throwable ex) {
        StringBuilder sb = new StringBuilder();
        for (Throwable t = ex; t != null && sb.length() < 4000; t = t.getCause()) {
            sb.append(t.getClass().getSimpleName()).append(": ").append(t.getMessage()).append(" | ");
        }
        return sb.toString();
    }

    private static ResponseEntity<ApiErrorResponse> statut(HttpStatus statut, ErrorCode code, String message) {
        return ResponseEntity.status(statut).body(ApiErrorResponse.of(code, message));
    }
}
