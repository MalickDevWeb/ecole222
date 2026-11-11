package ecole_221.sn.ecole.modules.media.domain.exception;

/**
 * Stockage des fichiers indisponible (Cloudinary non configuré ou en erreur) — APP-BE-05.
 * Remplace l'ancien repli silencieux vers une image de démonstration : l'utilisateur doit
 * savoir que son document n'a PAS été enregistré.
 */
public class StockageIndisponibleException extends RuntimeException {
    public StockageIndisponibleException(String message) {
        super(message);
    }

    public StockageIndisponibleException(String message, Throwable cause) {
        super(message, cause);
    }
}
