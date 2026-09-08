package ecole_221.sn.ecole.modules.dashboard.domain.exception;

public class KpiNotFoundException extends RuntimeException {
    public KpiNotFoundException(String id) {
        super("KPI non trouvé: " + id);
    }

    public KpiNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}