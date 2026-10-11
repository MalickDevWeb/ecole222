package ecole_221.sn.ecole.modules.superadmin.domain.exception;

public class GlobalUserNotFoundException extends RuntimeException {
    public GlobalUserNotFoundException(String userId) {
        super("Aucun utilisateur global trouvé avec l'ID : " + userId);
    }
}
