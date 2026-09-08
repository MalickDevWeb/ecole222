package ecole_221.sn.ecole.modules.security.domain.exception;

public class VigilProfileNotFoundException extends RuntimeException {
    public VigilProfileNotFoundException(String id) {
        super("Profil de vigilance non trouvé: " + id);
    }

    public VigilProfileNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}