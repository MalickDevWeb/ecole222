package ecole_221.sn.ecole.modules.superadmin.domain.exception;

public class TenantEmailAlreadyExistsException extends RuntimeException {

    public TenantEmailAlreadyExistsException(String email) {
        super("Un établissement avec l'email '" + email + "' existe déjà.");
    }
}
