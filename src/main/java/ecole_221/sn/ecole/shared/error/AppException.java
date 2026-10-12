package ecole_221.sn.ecole.shared.error;

import java.util.Map;

public class AppException extends RuntimeException {

    private final ErrorCode errorCode;
    private final Map<String, Object> details;

    public AppException(ErrorCode errorCode) {
        super(errorCode.getDefaultMessage());
        this.errorCode = errorCode;
        this.details = Map.of();
    }

    public AppException(ErrorCode errorCode, String message) {
        super(message != null && !message.isBlank() ? message : errorCode.getDefaultMessage());
        this.errorCode = errorCode;
        this.details = Map.of();
    }

    public AppException(ErrorCode errorCode, String message, Map<String, Object> details) {
        super(message != null && !message.isBlank() ? message : errorCode.getDefaultMessage());
        this.errorCode = errorCode;
        this.details = details != null ? details : Map.of();
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public Map<String, Object> getDetails() {
        return details;
    }
}

