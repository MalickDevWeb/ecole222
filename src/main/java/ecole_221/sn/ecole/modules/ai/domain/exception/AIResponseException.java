package ecole_221.sn.ecole.modules.ai.domain.exception;

public class AIResponseException extends RuntimeException {
    public AIResponseException(String message) {
        super(message);
    }

    public AIResponseException(String message, Throwable cause) {
        super(message, cause);
    }
}