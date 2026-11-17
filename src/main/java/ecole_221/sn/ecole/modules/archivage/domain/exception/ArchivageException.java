package ecole_221.sn.ecole.modules.archivage.domain.exception;

import java.util.Map;

/**
 * Refus ou échec d'une opération d'archivage, avec un message court en français. La nature est traduite en code
 * HTTP par la couche web (INVALIDE 400, INTROUVABLE 404, CONFLIT 409, REFUS 422, INDISPONIBLE 503).
 */
public class ArchivageException extends RuntimeException {

    public enum Nature { INVALIDE, INTROUVABLE, CONFLIT, REFUS, INDISPONIBLE }

    private final Nature nature;
    private final transient Map<String, Object> details;

    public ArchivageException(Nature nature, String message) {
        this(nature, message, Map.of());
    }

    public ArchivageException(Nature nature, String message, Map<String, Object> details) {
        super(message);
        this.nature = nature;
        this.details = details == null ? Map.of() : details;
    }

    public ArchivageException(Nature nature, String message, Throwable cause) {
        super(message, cause);
        this.nature = nature;
        this.details = Map.of();
    }

    public Nature nature() {
        return nature;
    }

    public Map<String, Object> details() {
        return details;
    }

    public static ArchivageException invalide(String message) {
        return new ArchivageException(Nature.INVALIDE, message);
    }

    public static ArchivageException introuvable(String message) {
        return new ArchivageException(Nature.INTROUVABLE, message);
    }

    public static ArchivageException conflit(String message) {
        return new ArchivageException(Nature.CONFLIT, message);
    }

    public static ArchivageException refus(String message) {
        return new ArchivageException(Nature.REFUS, message);
    }

    public static ArchivageException indisponible(String message) {
        return new ArchivageException(Nature.INDISPONIBLE, message);
    }
}
