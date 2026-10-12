package ecole_221.sn.ecole.shared.error;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiErrorResponse(
        boolean success,
        String code,
        String message,
        String error, // Alias de compatibilité
        int status,
        Instant timestamp,
        Map<String, Object> details
) {
    public static ApiErrorResponse of(ErrorCode errorCode) {
        return new ApiErrorResponse(
                false,
                errorCode.name(),
                errorCode.getDefaultMessage(),
                errorCode.getDefaultMessage(),
                errorCode.getHttpStatus().value(),
                Instant.now(),
                null
        );
    }

    public static ApiErrorResponse of(ErrorCode errorCode, String customMessage) {
        String msg = (customMessage != null && !customMessage.isBlank()) ? customMessage : errorCode.getDefaultMessage();
        return new ApiErrorResponse(
                false,
                errorCode.name(),
                msg,
                msg,
                errorCode.getHttpStatus().value(),
                Instant.now(),
                null
        );
    }

    public static ApiErrorResponse of(ErrorCode errorCode, String customMessage, Map<String, Object> details) {
        String msg = (customMessage != null && !customMessage.isBlank()) ? customMessage : errorCode.getDefaultMessage();
        return new ApiErrorResponse(
                false,
                errorCode.name(),
                msg,
                msg,
                errorCode.getHttpStatus().value(),
                Instant.now(),
                details
        );
    }
}

