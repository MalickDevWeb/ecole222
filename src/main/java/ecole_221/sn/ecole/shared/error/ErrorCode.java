package ecole_221.sn.ecole.shared.error;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    // Authentification & Sécurité
    AUTH_INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "Email ou mot de passe incorrect."),
    AUTH_ACCESS_DENIED(HttpStatus.FORBIDDEN, "Accès refusé : permissions insuffisantes."),
    AUTH_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "Session expirée. Veuillez vous reconnecter."),
    AUTH_ACCOUNT_SUSPENDED(HttpStatus.FORBIDDEN, "Ce compte a été suspendu par l'administration."),

    // Établissements & Onboarding
    TENANT_SLUG_ALREADY_EXISTS(HttpStatus.CONFLICT, "Cet identifiant d'école (slug) est déjà utilisé."),
    TENANT_EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "Cette adresse email est déjà associée à un établissement."),
    TENANT_NOT_FOUND(HttpStatus.NOT_FOUND, "Établissement introuvable."),
    TENANT_SUSPENDED(HttpStatus.FORBIDDEN, "Cet établissement est temporairement suspendu."),
    TENANT_EXPIRED(HttpStatus.FORBIDDEN, "L'abonnement de cet établissement a expiré."),

    // Utilisateurs & Comptes
    USER_EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "Cette adresse email est déjà associée à un compte utilisateur."),
    USER_PHONE_ALREADY_EXISTS(HttpStatus.CONFLICT, "Ce numéro de téléphone est déjà associé à un compte."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "Utilisateur introuvable."),

    // Pédagogie & Planning
    COURSE_NOT_FOUND(HttpStatus.NOT_FOUND, "Cours introuvable."),
    CLASSE_NOT_FOUND(HttpStatus.NOT_FOUND, "Classe introuvable."),
    FILIERE_NOT_FOUND(HttpStatus.NOT_FOUND, "Filière introuvable."),
    ROOM_NOT_FOUND(HttpStatus.NOT_FOUND, "Salle introuvable."),
    SLOT_NOT_FOUND(HttpStatus.NOT_FOUND, "Créneau horaire introuvable."),

    // Validation & Système
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "Données de formulaire invalides."),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "Ressource demandée introuvable."),
    OPERATION_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "Opération non autorisée dans l'état actuel."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Une erreur inattendue est survenue sur le serveur.");

    private final HttpStatus httpStatus;
    private final String defaultMessage;

    ErrorCode(HttpStatus httpStatus, String defaultMessage) {
        this.httpStatus = httpStatus;
        this.defaultMessage = defaultMessage;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public String getDefaultMessage() {
        return defaultMessage;
    }
}

