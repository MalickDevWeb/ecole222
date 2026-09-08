package ecole_221.sn.ecole.modules.ai.domain.exception;

public class AIServiceUnavailableException extends RuntimeException {
    private final String provider;

    public AIServiceUnavailableException(String provider) {
        super("Service IA indisponible: " + provider);
        this.provider = provider;
    }

    public AIServiceUnavailableException(String provider, Throwable cause) {
        super("Service IA indisponible: " + provider, cause);
        this.provider = provider;
    }

    public String getProvider() {
        return provider;
    }
}