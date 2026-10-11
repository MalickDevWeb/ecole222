package ecole_221.sn.ecole.modules.superadmin.domain.exception;

public class TenantNotFoundException extends RuntimeException {

    public TenantNotFoundException(String id) {
        super("Aucun établissement trouvé avec l'ID : " + id);
    }
}
