package ecole_221.sn.ecole.modules.superadmin.domain.exception;

public class EmailAlreadyExistsException extends RuntimeException {
    public EmailAlreadyExistsException(String email) {
        super("L'email '" + email + "' est déjà utilisé par un autre utilisateur.");
    }
}
